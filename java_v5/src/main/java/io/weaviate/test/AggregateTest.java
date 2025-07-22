package io.weaviate.test;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateAuthClient;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.auth.exception.AuthException;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.fields.Field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class AggregateTest {

    public static Double getCount(WeaviateClient client, String collectionName) {
        Field meta = Field.builder()
                .name("meta")
                .fields(new Field[]{
                        Field.builder().name("count").build()
                }).build();

        Result<GraphQLResponse> result = client.graphQL().aggregate()
                .withClassName(collectionName)
                .withFields(meta)
                .run();

        if (result.hasErrors()) {
            throw new RuntimeException(result.getError().toString());
        }

        var castResult = (GraphQLResponse<Map<String, Map<String, List<Map<String, Map<String, Object>>>>>>) result.getResult();

        var count = castResult.getData().get("Aggregate").get(collectionName).get(0).get("meta").get("count").toString();
        return Double.parseDouble(count);
    }

    public static void main(String[] args) throws Exception {
        String wcdUrl = System.getenv("WCD_URL");
        String wcdAccessToken = System.getenv("WCD_API_SECRET");
        wcdUrl = wcdUrl.replaceAll("https://", "");
        var client = WeaviateAuthClient.apiKey(new Config("https", wcdUrl), wcdAccessToken);
        var collectionName = "Test";

        var previousCount = getCount(client, collectionName);

        ObjectsBatcher batcher = client.batch().objectsBatcher();
        List<Map<String, Object>> dataObjs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("Test", String.format("Object %s", i));  // Replace with your actual objects
            dataObjs.add(properties);
            batcher.withObject(WeaviateObject.builder()
                    .className(collectionName)
                    .properties(properties)
                    .build()
            );
            batcher.flush();
        }

        var newCount = getCount(client, collectionName);
        System.out.printf("Old count: %f, new count: %f%n", previousCount, newCount);
    }

}