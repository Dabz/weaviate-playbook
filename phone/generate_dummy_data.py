import json
import random
from datetime import datetime

from faker import Faker


# Function to generate a random phone number
def generate_phone_number():
    return "+1555" + "".join([str(random.randint(0, 9)) for _ in range(7)])


invoices = []
faker = Faker()
names = [faker.unique.first_name() for _ in range(100)]

for i in range(100):
    customer_id = names[i]
    customer_name = names[i]
    phone_number = generate_phone_number()
    for j in range(5):
        start_date = datetime(2025, 5 - j, 1)
        end_date = datetime(2025, 5 - j, 28)
        minutes_used = random.randint(100, 2000)
        texts_sent = random.randint(50, 3000)
        data_used_gb = round(random.uniform(1.0, 25.0), 2)
        roaming_charge = round(random.uniform(0.0, 50.0), 2)
        base_plan = random.choice([40.00, 50.00, 60.00, 75.00])
        extra_minutes_charge = round(max(0, (minutes_used - 1000) * 0.05), 2)
        extra_texts_charge = round(max(0, (texts_sent - 1000) * 0.01), 2)
        extra_data_charge = round(max(0, (data_used_gb - 10.0) * 2.0), 2)
        taxes = round(
            (base_plan + extra_minutes_charge + extra_texts_charge + extra_data_charge + roaming_charge) * 0.1, 2)
        total = round(
            base_plan + extra_minutes_charge + extra_texts_charge + extra_data_charge + roaming_charge + taxes, 2)
        status = random.choice(["Paid", "Unpaid", "Pending"])

        invoice = {
            "invoice_id": f"CUST_{str(i + 1).zfill(3)}_INV{str(j + 1).zfill(3)}",
            "customer_id": customer_id,
            "customer_name": customer_name,
            "phone_number": phone_number,
            "billing_start_date": start_date.strftime("%Y-%m-%d"),
            "billing_end_date": end_date.strftime("%Y-%m-%d"),
            "minutes_used": minutes_used,
            "texts_sent": texts_sent,
            "data_used_gb": data_used_gb,
            "base_plan": base_plan,
            "extra_minutes_charge": extra_minutes_charge,
            "extra_texts_charge": extra_texts_charge,
            "extra_data_charge": extra_data_charge,
            "roaming_charge": roaming_charge,
            "taxes": taxes,
            "total": total,
            "status": status
        }
        invoices.append(invoice)

open("data.json", "w").write(json.dumps(invoices))
