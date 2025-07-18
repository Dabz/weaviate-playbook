package io.weaviate.performance;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.weaviate.client6.v1.api.Config;
import io.weaviate.client6.v1.api.WeaviateClient;
import io.weaviate.client6.v1.api.collections.CollectionHandle;
import io.weaviate.client6.v1.api.collections.query.Bm25;
import io.weaviate.client6.v1.api.collections.query.ConsistencyLevel;
import io.weaviate.client6.v1.api.collections.query.Hybrid;
import io.weaviate.client6.v1.api.collections.query.NearVector;
import io.weaviate.client6.v1.internal.TokenProvider;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class HybridQueryTest {

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
        while (System.currentTimeMillis() - testStartTime < 30_000) {
            for (Map.Entry<String, Float[]> entry : testDataset.entrySet()) {
                var start = System.currentTimeMillis();
                collection.query.hybrid(new Hybrid(
                                new Hybrid.Builder(entry.getKey())
                                        .nearVector(new NearVector(new NearVector.Builder(entry.getValue())))
                                        .limit(5)
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

    private static Map<String, Float[]> getTestDataset(CollectionHandle<Map<String, Object>> collection) {
        Map<String, Float[]> testDataset = new HashMap<>();
        for (var query : Dataset.TEST_DATA) {
            var queryResponse = collection.query.bm25(
                    new Bm25(
                            new Bm25.Builder("query")
                                    .queryProperties()
                                    .limit(1)
                    )
            );
            var object = queryResponse.objects().getFirst();
            var vector = object.vectors().getDefaultSingle();
            testDataset.put(query, vector);
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
