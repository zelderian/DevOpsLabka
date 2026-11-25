# Лабораторна №4 — Helm-чарт `devopslabka`

Чарт описує ті самі об'єкти, що й `k8s/` з лабораторної №3: backend, frontend, MongoDB з PVC,
ConfigMap, Secret, Service-и, Ingress. Додатково є pre-upgrade хук і тест релізу (`helm test`).

## version і appVersion

- `version: 0.1.0` — версія **чарта**: змінюється, коли змінюються шаблони або структура values
  (наприклад, додали Ingress → 0.2.0).
- `appVersion: "1.0.0"` — версія **застосунку**: тег образів backend/frontend за замовчуванням,
  якщо не задано `image.tag`. Вона ж — git-тег `v1.0.0`, з якого CI зібрав образи.

Один і той самий чарт 0.1.0 може розгортати застосунок 1.0.0 або 1.1.0 (через `--set image.tag=1.1.0`).

## Параметри values

| Параметр | За замовчуванням (dev) | Опис |
|----------|-----------------------|------|
| `image.tag` | `""` → `appVersion` | тег образів backend і frontend |
| `image.pullPolicy` | `IfNotPresent` | політика завантаження образів |
| `backend.image.repository` | `ghcr.io/zelderian/devopslabka-backend` | репозиторій образу backend |
| `backend.replicaCount` | `1` | кількість реплік backend |
| `backend.service.type` / `.port` | `ClusterIP` / `8080` | Service backend |
| `backend.resources` | 200m/384Mi → 1/768Mi | requests / limits |
| `backend.config.*` | `APP_CSRF_ENABLED`, `JWT_EXPIRATION_MS`, `LOGGING_LEVEL_ROOT` | ConfigMap → змінні оточення |
| `frontend.image.repository` | `ghcr.io/zelderian/devopslabka-frontend` | репозиторій образу frontend |
| `frontend.replicaCount` | `1` | кількість реплік frontend |
| `frontend.service.type` / `.port` / `.nodePort` | `NodePort` / `80` / `30080` | Service frontend (доступ з хоста: http://localhost:30080) |
| `frontend.resources` | 50m/32Mi → 200m/128Mi | requests / limits |
| `mongo.image.repository` / `.tag` | `mongo` / `7` | образ MongoDB |
| `mongo.persistence.enabled` | `true` | PVC для даних (false → emptyDir) |
| `mongo.persistence.size` | `1Gi` | розмір PVC |
| `secrets.mongodbUri` | `""` → `mongodb://<release>-mongo:27017/websecurity26` | Secret: рядок підключення |
| `secrets.jwtSecret` | навчальне значення | Secret: ключ підпису JWT |
| `existingSecret` | `""` | ім'я Secret, створеного вручну; тоді чарт свій Secret не створює |
| `ingress.enabled` | `false` | створювати Ingress |
| `ingress.className` / `.host` | `nginx` / `devopslabka.local` | клас і хост Ingress |
| `hooks.preUpgradeDbCheck` | `true` | pre-upgrade Job: перевірка, що MongoDB відповідає |
| `tests.enabled` | `true` | Pod для `helm test` |

Середовища:

| | `values-dev.yaml` | `values-prod.yaml` |
|---|---|---|
| тег образу | `1.0.0` | `1.1.0` |
| репліки backend / frontend | 1 / 1 | 3 / 2 |
| ресурси backend | 100m/256Mi → 500m/512Mi | 500m/512Mi → 2/1Gi |
| доступ | NodePort | NodePort + Ingress `devopslabka.local` |
| логування | DEBUG | WARN |

## Встановлення

```bash
# dev
helm install devopslabka charts/devopslabka -n devopslabka --create-namespace -f charts/devopslabka/values-dev.yaml
# prod
helm install devopslabka charts/devopslabka -n devopslabka --create-namespace -f charts/devopslabka/values-prod.yaml \
  --set secrets.jwtSecret=<свій-секрет>
```

## Команди для звіту (пп. 6–9)

```bash
# 6b. різниця між середовищами без встановлення
helm template devopslabka charts/devopslabka -f charts/devopslabka/values-dev.yaml  > dev.yaml
helm template devopslabka charts/devopslabka -f charts/devopslabka/values-prod.yaml > prod.yaml
diff dev.yaml prod.yaml        # PowerShell: Compare-Object (Get-Content dev.yaml) (Get-Content prod.yaml)

# 7. перевірка і встановлення
helm lint charts/devopslabka
helm template devopslabka charts/devopslabka            # порівняти з k8s/
kubectl delete -f k8s/                                   # прибрати реліз лаби 3
helm install devopslabka charts/devopslabka -n devopslabka --create-namespace -f charts/devopslabka/values-dev.yaml
helm list -n devopslabka
helm status devopslabka -n devopslabka
helm get values devopslabka -n devopslabka
kubectl get secret -n devopslabka -l owner=helm          # стан релізу: Secret sh.helm.release.v1.devopslabka.vN

# 8. життєвий цикл
helm upgrade devopslabka charts/devopslabka -n devopslabka -f charts/devopslabka/values-dev.yaml --set image.tag=1.1.0
helm upgrade devopslabka charts/devopslabka -n devopslabka -f charts/devopslabka/values-prod.yaml
helm history devopslabka -n devopslabka
helm rollback devopslabka 1 -n devopslabka
helm history devopslabka -n devopslabka                  # rollback з'являється як нова ревізія
helm uninstall devopslabka -n devopslabka
kubectl get all,pvc,cm,secret -n devopslabka             # порожньо

# 9b. хук і тест
kubectl get jobs -n devopslabka -w                       # під час helm upgrade видно pre-upgrade-db-check
helm test devopslabka -n devopslabka
```

**Пріоритет значень** (від найслабшого до найсильнішого): `values.yaml` чарта → файли `-f`
(у порядку перелічення, останній перемагає) → `--set` / `--set-string`. Тому
`-f values-prod.yaml --set image.tag=1.2.0` встановить тег `1.2.0`, а решту — з prod.
