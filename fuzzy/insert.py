import weaviate
from weaviate.collections.classes.config import Property, DataType, Tokenization, Configure

client = weaviate.connect_to_local()

collection_name = "Companies"

if client.collections.exists(collection_name):
    client.collections.delete(collection_name)

client.collections.create(
    name=collection_name,
    vector_config=Configure.Vectors.self_provided(),
    properties=[
        Property(name="name", data_type=DataType.TEXT, tokenization=Tokenization.WORD),
        Property(name="name_trig", data_type=DataType.TEXT, tokenization=Tokenization.TRIGRAM),
    ]
)

data = [
    "P&G",
    "l'Oréal",
    "Societe Generale",
    "Morgan Stanley",
    "GE",
    "HSBC",
    "Goldman Sacks",
    "3M",
    "Lorale LLC",
    "Society General Limited",
    "Morgan Stannard",
    "HSB Co.",
    "Goldman Snacks Co",
]

with client.collections.get(collection_name).batch.dynamic() as batch:
    for d in data:
        batch.add_object(
            properties={
                "name": d,
                "name_trig": d,
            }
        )

print("Failed objects: %s" % client.batch.failed_objects)
