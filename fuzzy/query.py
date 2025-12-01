import weaviate

client = weaviate.connect_to_local()

collection_name = "Companies"

queries = [
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

collection = client.collections.get(collection_name)
number_of_error = 0

for query in queries:
    res = collection.query.bm25(query=query,
                                query_properties=["name^2", "name_trig"],
                                limit=1)
    res_company = ""
    for obj in res.objects:
        res_company = obj.properties["name"]
        break

    if res_company != query:
        number_of_error += 1
        print("Expecting %s, got %s" % (query, res_company))

print("Number of errors: %d" % number_of_error)
