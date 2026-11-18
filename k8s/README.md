# Лабораторна №3 — розгортання в Kubernetes (minikube)

Усі об'єкти лежать у namespace `devopslabka`, по одному файлу на об'єкт:

| Файл | Об'єкт | Призначення |
|------|--------|-------------|
| `00-namespace.yaml` | Namespace | окремий простір імен проєкту |
| `mongo-pvc.yaml` | PersistentVolumeClaim | диск 1Gi для даних MongoDB |
| `mongo-deployment.yaml` | Deployment | MongoDB 7, 1 репліка, проби `mongosh ping` |
| `mongo-service.yaml` | Service (ClusterIP) | `mongo:27017` всередині кластера |
| `backend-configmap.yaml` | ConfigMap | несекретні налаштування backend |
| `backend-secret.yaml` | Secret | рядок підключення до БД і JWT-секрет |
| `backend-deployment.yaml` | Deployment | Spring Boot, 2 репліки, requests/limits, startup/readiness/liveness `/health` |
| `backend-service.yaml` | Service (ClusterIP) | `backend:8080` — сюди nginx проксує `/api` |
| `frontend-deployment.yaml` | Deployment | nginx + статика, 2 репліки, проби на `/` |
| `frontend-service.yaml` | Service (NodePort 30080) | доступ з хоста |
| `ingress.yaml` | Ingress | `http://devopslabka.local` → frontend |

Образи: `ghcr.io/zelderian/devopslabka-backend` і `ghcr.io/zelderian/devopslabka-frontend` з тегами версій
`1.0.0`, `1.1.0` (збирає CI з git-тегів `v1.0.0`, `v1.1.0`; пакети в GHCR мають бути публічними).

## Параметри

ConfigMap `backend-config` (змінні оточення backend):

| Ключ | Значення | Опис |
|------|----------|------|
| `APP_CSRF_ENABLED` | `true` | CSRF-захист для запитів з Basic-авторизацією |
| `JWT_EXPIRATION_MS` | `3600000` | час життя JWT (1 год) |
| `LOGGING_LEVEL_ROOT` | `INFO` | рівень логування Spring Boot |

Secret `backend-secret`:

| Ключ | Опис |
|------|------|
| `MONGODB_URI` | рядок підключення до MongoDB (`mongodb://mongo:27017/websecurity26`) |
| `JWT_SECRET` | ключ підпису JWT (навчальне значення) |

## Розгортання

```bash
minikube start --driver=docker
kubectl get nodes
kubectl get pods -n kube-system
minikube addons enable metrics-server
minikube addons enable ingress

kubectl apply -f k8s/                     # усе однією командою
kubectl get pods,deploy,rs,svc,ingress,pvc -n devopslabka
```

Відкрити застосунок:
- найпростіше: `minikube service frontend -n devopslabka` (відкриє браузер);
- через Ingress: додати `127.0.0.1 devopslabka.local` у `C:\Windows\System32\drivers\etc\hosts`,
  у окремому терміналі `minikube tunnel`, відкрити http://devopslabka.local.

Логін: `admin@mail.com / admin`.

## Команди для демонстрації (пп. 5–7)

```bash
NS="-n devopslabka"

# 5d. значення ConfigMap і Secret у контейнері
kubectl exec $NS deploy/backend -- printenv APP_CSRF_ENABLED JWT_EXPIRATION_MS MONGODB_URI

# 6a. самовідновлення
kubectl get pods $NS -l app=backend
kubectl delete pod $NS <ім'я-пода-backend>
kubectl get pods $NS -l app=backend -w

# 6b. масштабування і розподіл трафіку ("instance" = ім'я пода, що відповів)
kubectl scale deploy/backend $NS --replicas=4
kubectl run curl $NS --rm -it --image=curlimages/curl -- \
  sh -c 'for i in $(seq 1 10); do curl -s http://backend:8080/health; echo; done'

# 6c/6e. rolling update на 1.1.0; у другому терміналі цикл запитів — відповіді не перериваються
kubectl run curl $NS --rm -it --image=curlimages/curl -- \
  sh -c 'while true; do curl -s http://backend:8080/version; echo; sleep 0.5; done'
kubectl set image deploy/backend backend=ghcr.io/zelderian/devopslabka-backend:1.1.0 $NS
kubectl rollout status deploy/backend $NS

# 6d. історія і відкат
kubectl rollout history deploy/backend $NS
kubectl rollout undo deploy/backend $NS
kubectl rollout history deploy/backend $NS

# 7. діагностика: неіснуючий тег
kubectl set image deploy/backend backend=ghcr.io/zelderian/devopslabka-backend:9.9.9 $NS
kubectl get pods $NS                       # ErrImagePull / ImagePullBackOff
kubectl describe pod $NS <ім'я-пода>        # розділ Events: "manifest unknown"
kubectl get events $NS --sort-by=.lastTimestamp
kubectl logs $NS deploy/backend
kubectl exec -it $NS deploy/backend -- sh
kubectl rollout undo deploy/backend $NS    # виправлення
```

Видалити все: `kubectl delete -f k8s/`.
