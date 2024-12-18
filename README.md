# Weaviate playbooks

This repository contains different set of examples to operate and use Weaviate.
Those examples are not production ready and are there to provide example of configuration, usage and to be able to quickly test and understand certains feature. 
This is a constant work in progress, Pull Requests are welcomed nad feel free to open an Issue if you would like further clarification or a new playbooks.

## Operational playbooks

### 3 nodes cluster

This playbooks contains an example of configuration to run Weaviate as a cluster with 3 nodes and a replication factor of 3.
NGINX is used as a load balancer to distribute read & write to the 3 nodes. 
Prometheus and Grafana are also included to observe the proper distribution of read & write requests. 

### Monitoring

Contains an example of integration with Prometheus and Grafana to monitor Weaviate. 
The playbook contains example dashboards and alerts that should be suitable for most.

### Kubernetes (k8s)

Simple example of deployment on Kubernetes. 

### OIDC

Example of configuration for OIDC/OAuth integration with Weaviate.
The playbook is instantiating & configuring a Keycloak server for OIDC.


### RBAC

RBAC example to provide fine-grained privileges to users in Weaviate.
The docker-compose will instantiate a Weaviate instance configured for RBAC.
The README of this repository contains instruction to create custom role with the `weaviate-cli` command.

## Developers playbook

### Wiki

Fetching and inserting the Wikipedia database into Weaviate.

### Trivia

Fetching and inserting the OpenTrivia database into Weaviate.
