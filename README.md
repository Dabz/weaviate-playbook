# Weaviate playbooks

This repository contains different set of examples to operate and use Weaviate.
Those examples are not production ready and are there to provide example of configuration, usage and to be able to quickly test and understand certains feature. 
This is a constant work in progress, Pull Requests are welcomed nad feel free to open an Issue if you would like further clarification or a new playbooks.

Example have been build leveraging [Weaviate official docker images](https://weaviate.io/developers/weaviate/installation/docker-compose)

More information regarding Weaviate configuration could be found on [Weaviate official documentation](https://weaviate.io/developers/weaviate).

## Operational playbooks

### 3 nodes cluster

This playbooks contains an example of configuration to run Weaviate as a cluster with 3 nodes and a replication factor of 3.
NGINX is used as a load balancer to distribute read & write to the 3 nodes. 
Prometheus and Grafana are also included to observe the proper distribution of read & write requests. 

<details>
<summary><b>Important configuration</b></summary>
<pre>
CLUSTER_HOSTNAME: 'node1'
CLUSTER_GOSSIP_BIND_PORT: '7100'
CLUSTER_DATA_BIND_PORT: '7101'
RAFT_JOIN: 'node1,node2,node3'
RAFT_BOOTSTRAP_EXPECT: 3
</pre>
</details>

### Monitoring

Contains an example of integration with Prometheus and Grafana to monitor Weaviate. 
The playbook contains example dashboards and alerts that should be suitable for most.

<details>
<summary><b>Important configuration</b></summary>
<pre>
PROMETHEUS_MONITORING_ENABLED: true
</pre>
</details>

### Kubernetes (k8s)

Simple example of deployment on Kubernetes. 

### OIDC

Example of configuration for OIDC/OAuth integration with Weaviate.
The playbook is instantiating & configuring a Keycloak server for OIDC.

<details>
<summary><b>Important configuration</b></summary>
<pre>
AUTHENTICATION_OIDC_ENABLED: 'true'
AUTHENTICATION_OIDC_ISSUER: 'http://keycloak:7080/realms/weaviate'
AUTHENTICATION_OIDC_CLIENT_ID: 'weaviate'
AUTHENTICATION_OIDC_SKIP_CLIENT_ID_CHECK: 'true'
AUTHENTICATION_OIDC_USERNAME_CLAIM: 'sub'
</pre>
</details>

### RBAC

RBAC example to provide fine-grained privileges to users in Weaviate.
The docker-compose will instantiate a Weaviate instance configured for RBAC.
The README of this repository contains instruction to create custom role with the `weaviate-cli` command.

<details>
<summary><b>Important configuration</b></summary>
<pre>
AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED: 'false'
AUTHORIZATION_ENABLE_RBAC: 'true'
AUTHENTICATION_APIKEY_ENABLED: 'true'
AUTHENTICATION_APIKEY_ALLOWED_KEYS: 'admin,app'
AUTHENTICATION_APIKEY_USERS: 'admin,app'
AUTHORIZATION_ADMIN_USERS: 'admin'
</pre>
</details>

<details>
<summary><b>How to grant permissions</b></summary>
<pre>
$ weaviate-cli --config-file config.json create role --role_name app_role -p 'r_data:Wiki' -p 'r_collection:*'
$ weaviate-cli --config-file config.json assign role --role_name app_role --user_name app
</pre>
</details>


## Developers playbook

### Wiki

Fetching and inserting the Wikipedia database into Weaviate.

### Trivia

Fetching and inserting the OpenTrivia database into Weaviate.
