package io.weaviate.performance;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.batch.model.ObjectGetResponse;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.misc.model.SQConfig;
import io.weaviate.client.v1.misc.model.VectorIndexConfig;
import io.weaviate.client.v1.schema.model.DataType;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroReadSupport;
import org.apache.parquet.conf.ParquetConfiguration;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.LocalInputFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class LoaderApp {
    private WeaviateClient client;
    private final String collectionName = "Wikipedia";

    public static void main(String[] args) throws IOException, InterruptedException {
        String url = System.getenv("WCS_API_URL");
        String grpcUrl = System.getenv("WCS_GRPC_URL");

        var app = new LoaderApp(url, grpcUrl);
        app.createCollection();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Files.list(java.nio.file.Path.of("../datasets/wikipedia-22-12-en-embeddings/data/")).forEach((path) -> {
            executorService.submit(() -> {
                try {
                    System.out.println("Loading: " + path);
                    var loaderApp = new LoaderApp(url, grpcUrl);
                    loaderApp.sendParquetToWeaviate(path.toAbsolutePath().toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        executorService.shutdown();
    }

    public LoaderApp(String url, String grpcUrl) {
        createWeaviateClient(url, grpcUrl);
    }

    public void createCollection() {
        client.schema().classCreator()
                .withClass(WeaviateClass.builder()
                        .className(collectionName)
                        .properties(List.of(
                                        Property.builder().name("title")
                                                .dataType(List.of(DataType.TEXT))
                                                .build()
                                        ,
                                        Property.builder().name("views")
                                                .dataType(List.of(DataType.NUMBER))
                                                .build()
                                )
                        ).vectorIndexConfig(VectorIndexConfig.builder().sq(SQConfig.builder().build()).build())
                        .build()
                ).run();
    }

    public void sendParquetToWeaviate(String filePath) throws IOException {
        try (ObjectsBatcher batch = client.batch().objectsAutoBatcher();
             ParquetReader<GenericRecord> reader = ParquetReader.builder(new AvroReadSupport(), new Path(filePath)).build()) {

            long count = 0;
            while (true) {
                var record = reader.read();
                if (record == null) {
                    break;
                }

                var embedding = (GenericData.Array<GenericRecord>) record.get("emb");
                var title = (String) record.get("title");
                var views = (Float) record.get("views");
                Float[] vector = embedding.stream().map((elt -> elt.get(0))).toArray(Float[]::new);

                batch.withObject(WeaviateObject.builder()
                        .className(collectionName)
                        .properties(Map.of("title", title, "views", views))
                        .vector(vector)
                        .build());
                count += 1 ;
                if (count % 100000 == 0) {
                    Result<ObjectGetResponse[]> results = batch.run();
                    if (results.hasErrors()) {
                        System.err.println(results.getError().getMessages());
                    }
                }
            }
            batch.run();
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
