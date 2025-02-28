# Kubernetes on EKS quickstarto

```bash
$> eksctl create cluster -f cluster.yaml
$> eksctl create nodegroup -f cluster.yaml
$> eksctl create addon \
  --name "aws-ebs-csi-driver" \
  --cluster damien-cluster-3 --region eu-west-3 \
  --service-account-role-arn 'arn:aws:iam::097705893007:role/AmazonEKSPodIdentityAmazonEBSCSIDriverRole'
```

```bash
$> helm repo add weaviate https://weaviate.github.io/weaviate-helm
$> helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
```

```bash
$> helm upgrade --install prometheus prometheus-community/kube-prometheus-stack  --namespace prometheus --values prometheus.yaml
$> helm upgrade --install "weaviate"  weaviate/weaviate  --namespace "weaviate"  --values ./simple.yml
```

## Accessing Grafana

```bash
kubectl port-forward -n prometheus svc/prometheus-grafana 8000:80
```
