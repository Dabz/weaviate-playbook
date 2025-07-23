import json
import os

import weaviate
from weaviate.auth import Auth

wcd_url = os.environ.get("WCD_URL", default="")
wcd_api_key = os.environ.get("WCD_API_SECRET", default="")
openai_key = os.environ.get("OPENAI_API_SECRET", default="")

client = weaviate.connect_to_weaviate_cloud(cluster_url=wcd_url, auth_credentials=Auth.api_key(wcd_api_key), headers={
    "X-Openai-Api-Key": openai_key
})
product_collection_name = "Products"

products = json.load(open("./products.json"))
with client.batch.dynamic() as batch:
    for product in products:
        batch.add_object(
            collection=product_collection_name,
            properties=product
        )
        if batch.number_errors > 0:
            break
if len(client.batch.failed_objects) > 0:
    print(client.batch.failed_objects)

client.close()
