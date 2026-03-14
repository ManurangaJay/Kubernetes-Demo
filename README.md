# E-Commerce Microservices on Kubernetes

This repository demonstrates the deployment, orchestration, and scaling of a Java Spring Boot 3 microservices architecture using Kubernetes. It simulates a high-concurrency e-commerce checkout flow where an orchestrator service securely communicates with internal domain services using Kubernetes CoreDNS.

## Architecture Overview

The system consists of three independent microservices:
* **Order Service (Port 8080):** The API Gateway/Orchestrator. Receives the checkout request and synchronously calls Inventory and Payment.
* **Inventory Service (Port 8081):** Manages stock availability.
* **Payment Service (Port 8082):** Processes dummy transactions.

**Key Kubernetes Concepts Implemented:**
* **Deployments:** Managing stateless application pods.
* **Services (ClusterIP & NodePort):** Internal DNS resolution and external load balancing.
* **Horizontal Scaling:** Replicating pods to handle traffic spikes.
* **Resource Quotas:** Preventing Out-Of-Memory (OOM) node crashes.
* **ConfigMaps:** Injecting external configuration without rebuilding images.

## Prerequisites

* Java 21 (JDK)
* Maven
* Docker
* A local Kubernetes cluster (Minikube, k3d, kind, or Docker Desktop)
* `kubectl` CLI installed and configured

## Step 1: Build & Containerize

Before deploying to Kubernetes, each Spring Boot application must be compiled and packaged into a Docker image using a Java 21 runtime (`eclipse-temurin:21-jre-alpine`). Run these commands in the root directory of each respective microservice:

**1. Inventory Service:**
```bash
mvn clean package -DskipTests
docker build -t localdev/inventory-service:v3 .
```

**2. Payment Service:**
```bash
mvn clean package -DskipTests
docker build -t localdev/payment-service:v2 .
```

**3. Order Service:**
```bash
mvn clean package -DskipTests
docker build -t localdev/order-service:v2 .
```

## Step 2: Deploy to Kubernetes

All Kubernetes infrastructure is declared in the `ecommerce-app.yaml` manifest. This includes the ConfigMap, Deployments (with CPU/Memory limits), and Services.

Apply the desired state to the cluster:
```bash
kubectl apply -f ecommerce-app.yaml
```

Verify the pods are spinning up successfully:
```bash
# Watch the pod creation process in real-time
kubectl get pods -w

# Verify the internal and external services are active
kubectl get svc
```

## Step 3: Accessing the Application (Local Testing)

Because local machine loopbacks (`localhost`) do not directly map to the Kubernetes virtual network, use `kubectl port-forward` to create a secure tunnel. *(Note: We map local port 9090 to container port 80 to avoid conflicts with background Java processes on port 8080).*

Leave this command running in a dedicated terminal window:
```bash
kubectl port-forward svc/order-service 9090:80
```

Test the Checkout Flow via cURL or Postman:
```http
POST http://localhost:9090/api/orders
Content-Type: application/json

{
  "productId": "X999",
  "quantity": 2,
  "price": 50.0
}
```
**Expected Response (200 OK):** `Order Placed Successfully. Order ID: <uuid>`

## Step 4: Scaling & Load Balancing

To handle increased traffic, instruct Kubernetes to scale the Order Service. Kubernetes will instantly provision new pods and configure the `order-service` to round-robin traffic between them.

Scale the deployment to 3 replicas:
```bash
kubectl scale deployment order-deployment --replicas=3
```

Verify the load balancer:
Send 5-10 requests via Postman, then check the aggregated logs to see traffic distributed across different thread processes and pod instances:
```bash
# Tail the last 10 log entries across ALL order service pods simultaneously
kubectl logs -l app=order --tail=10
```

## Step 5: External Configuration (ConfigMap)

The `ecommerce-app.yaml` includes a ConfigMap that dynamically injects the allowed `productId` into the Inventory Service at runtime, demonstrating the separation of configuration from code.

To change the allowed product from `X999` to something else:
1.  Update the `ALLOWED_PRODUCT` value in `ecommerce-app.yaml`.
2.  Reapply the configuration: `kubectl apply -f ecommerce-app.yaml`

Kubernetes will automatically roll out new pods with the updated environment variables.

## Useful Debugging Commands

If a pod fails (e.g., `CrashLoopBackOff` or `Error`), use these commands to diagnose the root cause:

```bash
# 1. Check the exact reason a pod was terminated by the node (e.g., OOMKilled)
kubectl describe pod <exact-pod-name>

# 2. Check the Spring Boot/Java application logs for stack traces
kubectl logs <exact-pod-name>

# 3. Check logs for a previous, crashed instance of a pod
kubectl logs <exact-pod-name> -p
```