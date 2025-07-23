import datetime
import json
import os

import weaviate
from ollama import ChatResponse, chat
from weaviate.auth import Auth
from weaviate.collections.classes.config import Configure, Property, DataType
from weaviate.collections.classes.config_vector_index import VectorFilterStrategy

wcd_url = os.environ.get("WCD_URL", default="")
wcd_api_key = os.environ.get("WCD_API_SECRET", default="")
openai_key = os.environ.get("OPENAI_API_SECRET", default="")
cohere_key = os.environ.get("COHERE_API_SECRET", default="")

client = weaviate.connect_to_weaviate_cloud(cluster_url=wcd_url,
                                            auth_credentials=Auth.api_key(wcd_api_key),
                                            headers={
                                                "X-Cohere-Api-Key": cohere_key,
                                                "X-Openai-Api-Key": openai_key
                                            })

collection_name = "CellPhonesBills"

if client.collections.exists(collection_name):
    client.collections.delete(collection_name)

client.collections.create(
    name=collection_name,
    vectorizer_config=[
        Configure.NamedVectors.none(name="weaviate",
                                    vector_index_config=Configure.VectorIndex.hnsw(
                                        quantizer=Configure.VectorIndex.Quantizer.pq(),
                                        filter_strategy=VectorFilterStrategy.ACORN
                                    ))
    ],
    generative_config=Configure.Generative.openai(),
    multi_tenancy_config=Configure.multi_tenancy(enabled=True,
                                                 auto_tenant_creation=True,
                                                 auto_tenant_activation=True),
    replication_config=Configure.replication(factor=3, async_enabled=True),
    properties=[
        Property(name="invoice_id", data_type=DataType.TEXT, skip_vectorization=True),
        Property(name="customer_id", data_type=DataType.TEXT, skip_vectorization=True),
        Property(name="phone_number", data_type=DataType.TEXT),
        Property(name="billing_start_date", data_type=DataType.DATE),
        Property(name="billing_end_date", data_type=DataType.DATE),
        Property(name="minutes_used", data_type=DataType.NUMBER),
        Property(name="texts_sent", data_type=DataType.NUMBER),
        Property(name="data_used_gb", data_type=DataType.NUMBER),
        Property(name="base_plan", data_type=DataType.NUMBER),
        Property(name="extra_minutes_charge", data_type=DataType.NUMBER),
        Property(name="extra_texts_charge", data_type=DataType.NUMBER),
        Property(name="extra_data_charge", data_type=DataType.NUMBER),
        Property(name="roaming_charge", data_type=DataType.NUMBER),
        Property(name="taxes", data_type=DataType.NUMBER),
        Property(name="total", data_type=DataType.NUMBER),
        Property(name="status", data_type=DataType.TEXT)
    ]
)

collection = client.collections.get(collection_name)
datetime_format = '%Y-%m-%d'


def get_summary(json: dict):
    text = ""
    for key, value in json.items():
        text += f"{key}: {str(value)}\n"
    response: ChatResponse = chat(model='gemma3:1b', messages=[
        {
            'role': 'user',
            'content': "Please provide and analysis of the following invoices, at the end, you must provide all the required information. The invoice is: \n" + text
        },
    ])
    return response.message.content


bills = json.load(open("./data.json"))
with client.batch.dynamic() as batch:
    for bill in bills:
        tenant_id = bill["customer_id"]
        bill["billing_start_date"] = datetime.datetime.strptime(bill["billing_start_date"], datetime_format)
        bill["billing_end_date"] = datetime.datetime.strptime(bill["billing_end_date"], datetime_format)
        bill["summary"] = get_summary(bill)
        batch.add_object(
            collection=collection_name,
            tenant=tenant_id,
            properties=bill
        )
        if batch.number_errors > 0:
            break
if len(collection.batch.failed_objects) > 0:
    print(collection.batch.failed_objects)
