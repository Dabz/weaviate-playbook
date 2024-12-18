# RBAC playbook

Weaviate supports RBAC in version greater than 1.28.x.
RBAC allows to define fine-grained permissions for users.
The whole list of possible permissions is available on https://weaviate.io/developers/weaviate/configuration/roles


The concept is relateively simple:

1. New custom role could be created
1. Permissions can be assigned to those new roles
1. User can be assigned roles

## How to run

```bash
# Starting Weaviate with RBAC enabled
docker-compose up -d
```

```bash
# Creating a new role that could only get collection config and access data from the Wiki collection
weaviate-cli --config-file config.json create role --role_name app_role -p 'r_data:Wiki' -p 'r_collection:*'

# Assigning this role to the "app" user
weaviate-cli --config-file config.json assign role --role_name app_role --user_name app
```
