import weaviate

client = weaviate.connect_to_local()

collection_name = "Companies"

queries = [
    ("P&G", "P&G"),
    ("l'Oréal", "l'Oréal"),
    ("l'Oréa", "l'Oréal"),
    ("Societe Generale", "Societe Generale"),
    ("Morgan Stanley", "Morgan Stanley"),
    ("GE", "GE"),
    ("HSBC", "HSBC"),
    ("Goldman Sacks", "Goldman Sacks"),
    ("3M", "3M"),
    ("Lorale LLC", "Lorale LLC"),
    ("Loral LLC", "Lorale LLC"),
    ("Society General Limited", "Society General Limited"),
    ("Morgan Stannard", "Morgan Stannard"),
    ("HSB Co.", "HSB Co."),
    ("Goldman Snacks Co", "Goldman Snacks Co"),
]

collection = client.collections.get(collection_name)
number_of_error = 0

for query, expected_result in queries:
    res = collection.query.bm25(query=query,
                                query_properties=["name^2", "name_trig"],
                                limit=1)
    res_company = ""
    for obj in res.objects:
        res_company = obj.properties["name"]
        break

    if res_company != expected_result:
        number_of_error += 1
        print("Expecting %s, got %s" % (query, res_company))

print("Number of errors: %d" % number_of_error)
