package io.weaviate.performance;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.weaviate.client6.v1.api.Config;
import io.weaviate.client6.v1.api.WeaviateClient;
import io.weaviate.client6.v1.api.collections.CollectionHandle;
import io.weaviate.client6.v1.api.collections.query.ConsistencyLevel;
import io.weaviate.client6.v1.api.collections.query.Hybrid;
import io.weaviate.client6.v1.api.collections.query.Metadata;
import io.weaviate.client6.v1.api.collections.query.NearVector;
import io.weaviate.client6.v1.internal.TokenProvider;
import io.weaviate.client6.v1.internal.grpc.protocol.WeaviateProtoSearchGet;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;


public class HybridQueryTest {

    class MyMetatadata implements Metadata {

        @Override
        public void appendTo(WeaviateProtoSearchGet.MetadataRequest.Builder metadata) {
            metadata.setScore(true);
            metadata.setExplainScore(true);
        }
    }

    public WeaviateClient getClient() throws FileNotFoundException {
        var clusterUrl = System.getenv("WCS_CLUSTER_URL");
        var accessToken = System.getenv("WCS_API_KEY");

        if (clusterUrl == null || accessToken == null) {
            var tokenPath = System.getProperty("user.home") + "/.config/wcs/token.json";
            var accessPath = System.getProperty("user.home") + "/.config/wcs/prod/sessions/default.json";

            Gson gson = new Gson();
            JsonObject tokenJson = gson.fromJson(new FileReader(tokenPath), JsonObject.class);
            JsonObject accessJson = gson.fromJson(new FileReader(accessPath), JsonObject.class);

            clusterUrl = accessJson.get("cluster").getAsJsonObject().get("url").getAsString();
            accessToken = tokenJson.get("access_token").getAsString();
            var refreshToken = tokenJson.get("refresh_token").getAsString();
        }

        return new WeaviateClient(
                new Config.WeaviateCloud(clusterUrl, TokenProvider.staticToken(new TokenProvider.Token(accessToken))).build()
        );
    }

    public void test() throws Exception {
        var client = getClient();
        var collection = client.collections.use(Dataset.COLLECTION_NAME);
        var queryLatencyHistogram = getHistogram();

        var testDataset = getTestDataset(collection);

        var testStartTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - testStartTime) < 30_000) {
            for (Map.Entry<String, float[]> entry : testDataset.entrySet()) {
                var start = System.currentTimeMillis();
                var resp = collection.query.hybrid(new Hybrid(
                                new Hybrid.Builder(entry.getKey())
                                        .nearVector(new NearVector(new NearVector.Builder(entry.getValue())))
                                        .limit(20)
                                        .alpha(0.7f)
                                        .returnMetadata(Metadata.MetadataField.SCORE)
                                        .consistencyLevel(ConsistencyLevel.ONE)
                        )
                );
                var end = System.currentTimeMillis();
                queryLatencyHistogram.update(end - start);
            }
        }
    }

    private static Histogram getHistogram() {
        var metricRegistry = new MetricRegistry();
        Histogram queryLatency = metricRegistry.histogram(name(HybridQueryTest.class, "weaviate-query-latency"));
        var reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.SECONDS);
        return queryLatency;
    }

    private static Map<String, float[]> getTestDataset(CollectionHandle<Map<String, Object>> collection) throws IOException {
        Gson gson = new Gson();
        String jsonFile = new String(Objects.requireNonNull(HybridQueryTest.class.getClassLoader().getResourceAsStream("embedding_loaded.json")).readAllBytes());
        JsonObject jsonObject = gson.fromJson(jsonFile, JsonObject.class);
        Map<String, float[]> testDataset = new HashMap<>();

        for (var line : jsonObject.keySet()) {
            var array = jsonObject.getAsJsonArray(line);
            var vector = new float[array.size()];
            for (int i = 0; i < array.size(); i++) {
                vector[i] = array.get(i).getAsFloat();
            }

            testDataset.put(line, vector);
        }

        return testDataset;
    }

    public static void main(String[] args) {
        HybridQueryTest app = new HybridQueryTest();
        try {
            app.test();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
