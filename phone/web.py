import os

import weaviate
from fastapi import FastAPI, Request
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from weaviate.auth import Auth
from weaviate.collections.classes.filters import Filter
from weaviate.collections.classes.grpc import Rerank
from weaviate.collections.classes.tenants import Tenant, TenantActivityStatus
from weaviate_agents.query import QueryAgent

app = FastAPI()

templates = Jinja2Templates(directory="./")
app.mount("/images", StaticFiles(directory="./images"))
app.mount("/css", StaticFiles(directory="./css"))

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

qa = QueryAgent(client=client, collections=[collection_name])


@app.get("/web")
def serve_home(request: Request):
    return templates.TemplateResponse("index.html", context={"request": request})


@app.get("/products")
def serve_home(request: Request):
    return templates.TemplateResponse("product.html", context={"request": request})


@app.get(path="/query/{tenant}/{type}/{query}")
def query(tenant: str, type: str, query: str):
    if type == 'agent':
        return query_agent(query, tenant)
    if type == 'hybrid':
        return query_hybrid(query, tenant)
    if type == 'vector':
        return query_vector(query, tenant)
    if type == 'keyword':
        return query_keyword(query, tenant)
    raise Exception(f"Unknown query type: {query}")


@app.get(path="/tenants")
def query():
    tenants = client.collections.get(collection_name).tenants.get()
    return [t for t in tenants]


@app.get(path="/select/tenant/{tenant}")
def select_tenant(tenant: str):
    tenants = client.collections.get(collection_name).tenants.get()
    client.collections.get(collection_name).tenants.update(
        tenants=[Tenant(name=t, activity_status=TenantActivityStatus.INACTIVE) for t in tenants])

    client.collections.get(collection_name).tenants.update(
        tenants=[Tenant(name=tenant, activity_status=TenantActivityStatus.ACTIVE)])

    return [t for t in tenants]


def query_agent(query: str, tenant: str):
    result = qa.run(query=query)
    object_ids = [s.object_id for s in result.sources]
    if object_ids and len(object_ids) > 0:
        objects = client.collections.get(collection_name).query.fetch_objects(
            filters=Filter.by_property("_id").contains_any(object_ids))
        return result.model_dump() | {"objects": [o.properties for o in objects.objects]}
    else:
        return result.model_dump() | {"objects": []}


prompt = (
    "You are an agent working at the cell-phone service provider. YOU MUST RELY ONLY ON THE PROVIDED CONTEXT. IF YOU ARE NOT SURE, REDIRECT THE USER TO ASSISTANCE."
    "Great %s and try to reply the best you can to his query: %s"
)


def query_hybrid(query: str, tenant: str):
    rag = (client.collections.get(collection_name)
           .with_tenant(tenant=tenant)
           .generate.hybrid(query=query,
                            alpha=0.5,
                            grouped_task=prompt % (tenant, query),
                            grouped_properties=["summary"],
                            return_properties=["invoice_id", "customer_name"],
                            limit=2))
    return {"objects": [{"invoice_id": o.properties["invoice_id"]} for o in rag.objects],
            "final_answer": rag.generative.text}


def query_vector(query: str, tenant: str):
    rag = (client.collections.get(collection_name).with_tenant(tenant=tenant)
           .generate.near_text(query=query,
                               grouped_task=prompt % (tenant, query),
                               grouped_properties=["summary"],
                               return_properties=["invoice_id", "customer_name"],
                               limit=2))
    return {"objects": [{"invoice_id": o.properties["invoice_id"]} for o in rag.objects],
            "final_answer": rag.generative.text}


def query_keyword(query: str, tenant: str):
    rag = (client.collections.get(collection_name).with_tenant(tenant=tenant)
           .generate.bm25(query=query,
                          grouped_task=prompt % (tenant, query),
                          return_properties=["invoice_id", "customer_name"],
                          grouped_properties=["summary"],
                          limit=2))
    return {"objects": [{"invoice_id": o.properties["invoice_id"]} for o in rag.objects],
            "final_answer": rag.generative.text if rag.generative else ""}


product_collection_name = "Products"

qa = QueryAgent(client=client, collections=["Products"])


@app.get(path="/products/query/{type}/{query}")
def query(type: str, query: str):
    if type == 'agent':
        return product_query_agent(query)
    if type == 'hybrid':
        return product_query_hybrid(query)
    if type == 'vector':
        return product_query_vector(query)
    if type == 'keyword':
        return product_query_keyword(query)
    raise Exception(f"Unknown query type: {query}")


def product_query_agent(query: str):
    result = qa.run(query=query)
    object_ids = [s.object_id for s in result.sources]
    if object_ids and len(object_ids) > 0:
        objects = client.collections.get(product_collection_name).query.fetch_objects(
            filters=Filter.by_property("_id").contains_any(object_ids))
        return result.model_dump() | {"objects": [o.properties for o in objects.objects]}
    else:
        return result.model_dump() | {"objects": []}


product_prompt = "Your name is Rohan and you are a virtual assistant, with the previous context, reply concisely and professionally to the query %s"


def product_query_hybrid(query: str):
    rag = (client.collections.get(product_collection_name)
           .query.hybrid(query=query,
                         # grouped_properties=["title", "description"],
                         rerank=Rerank(prop="title",
                                       query=query)))
    return {"objects": [o.properties for o in rag.objects]}


def product_query_vector(query: str):
    rag = client.collections.get(product_collection_name).query.near_text(query=query,
                                                                          #   grouped_task=product_prompt % query,
                                                                          # grouped_properties=["title"]),
                                                                          limit=20)
    return {"objects": [o.properties for o in rag.objects]}


def product_query_keyword(query: str):
    rag = client.collections.get(product_collection_name).query.bm25(query=query,
                                                                     # grouped_task=product_prompt % query,
                                                                     # grouped_properties=["title"],
                                                                     limit=20)
    return {"objects": [o.properties for o in rag.objects]}
