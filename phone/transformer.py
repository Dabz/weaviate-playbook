import os

import weaviate
from weaviate.auth import Auth
from weaviate.collections.classes.config import DataType
from weaviate_agents.transformation import TransformationAgent
from weaviate_agents.transformation.classes import Operations

wcd_url = os.environ.get("WCD_URL", default="")
wcd_api_key = os.environ.get("WCD_API_SECRET", default="")
openai_key = os.environ.get("OPENAI_API_SECRET", default="")

client = weaviate.connect_to_weaviate_cloud(cluster_url=wcd_url, auth_credentials=Auth.api_key(wcd_api_key), headers={
    "X-Openai-Api-Key": openai_key
})

collection_name = "CellPhonesBills"

add_summary = Operations.append_property(
    property_name="summary",
    data_type=DataType.TEXT,
    view_properties=["invoice_id", "phone_number", "customer_id", "billing_start_date", "billing_end_date",
                     "status", "extra_texts_charge", "extra_minutes_charge", "total", "texts_sent", "customer_name",
                     "minutes_used", "roaming_charge", "taxes", "base_plan"],
    instruction="""Summarize the invoice in an human friendly format, all information must be summarized"""
)

agent = TransformationAgent(
    client=client,
    collection=collection_name,
    operations=[
        add_summary
    ],
)

response = agent.update_all()

client.close()
