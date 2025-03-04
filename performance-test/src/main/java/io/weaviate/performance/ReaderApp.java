package io.weaviate.performance;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.batch.model.ObjectGetResponse;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.data.replication.model.ConsistencyLevel;
import io.weaviate.client.v1.experimental.Collection;
import io.weaviate.client.v1.experimental.SearchResult;
import io.weaviate.client.v1.misc.model.SQConfig;
import io.weaviate.client.v1.misc.model.VectorIndexConfig;
import io.weaviate.client.v1.schema.model.DataType;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroReadSupport;
import org.apache.parquet.hadoop.ParquetReader;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReaderApp {
    private WeaviateClient client;
    private final String collectionName = "Wikipedia";

    public static void main(String[] args) throws IOException, InterruptedException {
        String url = System.getenv("WCS_API_URL");
        String grpcUrl = System.getenv("WCS_GRPC_URL");

        var app = new ReaderApp(url, grpcUrl);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Files.list(java.nio.file.Path.of("../datasets/wikipedia-22-12-en-embeddings/data/")).forEach((path) -> {
            executorService.submit(() -> {
                try {
                    System.out.println("Reading: " + path);
                    var loaderApp = new ReaderApp(url, grpcUrl);
                    loaderApp.readParquetToWeaviate(path.toAbsolutePath().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
        });

        executorService.shutdown();
    }

    public ReaderApp(String url, String grpcUrl) {
        createWeaviateClient(url, grpcUrl);
    }

    public void readParquetToWeaviate(String filePath) throws IOException {
        try (ParquetReader<GenericRecord> reader = ParquetReader.builder(new AvroReadSupport(), new Path(filePath)).build()) {

            long count = 0;
            while (true) {
                var record = reader.read();
                if (record == null) {
                    break;
                }

                var embedding = (GenericData.Array<GenericRecord>) record.get("emb");
                var title = (String) record.get("title");
                Float[] vector = embedding.stream().map((elt -> elt.get(0))).toArray(Float[]::new);


                Collection<Map> collection = client.collections.use(collectionName, Map.class);

                var start = new Date();
                SearchResult<Map> mapSearchResult = collection.query.nearVector(
                        ArrayUtils.toPrimitive(vector),
                        options -> options.limit(1)
                );
                var end = new Date();
                for (SearchResult.SearchObject<Map> object : mapSearchResult.objects) {
                    System.out.println(String.format("Searched %s, got %s - took %dms",
                            title,
                            object.properties.get("title"),
                            end.getTime() - start.getTime()));
                }

            }
        }
    }

    private void createWeaviateClient(String url, String grpcUrl) {
        final WeaviateClient client;
        String[] splitUrl = url.split("://");
        String[] splitGrpcUrl = grpcUrl.split("://");
        Config config = new Config(splitUrl[0], splitUrl[1]);
        config.setGRPCHost(splitGrpcUrl[1]);
        config.setGRPCSecured(splitUrl[0].equals("https"));

        this.client =new WeaviateClient(config);
    }
}
