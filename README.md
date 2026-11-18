# DevOpsLabka

[![CI](https://github.com/zelderian/DevOpsLabka/actions/workflows/ci.yml/badge.svg)](https://github.com/zelderian/DevOpsLabka/actions/workflows/ci.yml)

Проєкт для лабораторних робіт з предмету «Технології DevOps».
Вебзастосунок з двох сервісів і бази даних:

| Сервіс | Технології | Порт | Опис |
|--------|-----------|------|------|
| `backend/` | Java 25, Spring Boot 4.1, Spring Security, JWT, Maven | 8080 | REST API `/api/v1/items`, логін `/api/v1/auth/authenticate`, ролі ADMIN / MANAGER / USER |
| `frontend/` | HTML + JavaScript, Node 22 (збірка, тести, ESLint), nginx | 3000 | Вебінтерфейс: логін, список записів, додавання/видалення; проксує `/api` на backend |
| `mongo` | MongoDB 7 | 27017 | База даних |

```
браузер ──► frontend (nginx :3000) ──/api──► backend (Spring Boot :8080) ──► MongoDB (:27017)
```

Тестові користувачі: `admin@mail.com / admin`, `manager@mail.com / manager`, `user@mail.com / user`.

## Структура репозиторію

```
.github/workflows/ci.yml   CI-пайплайн (лабораторна №2)
docker-compose.yml         підіймає весь проєкт (лабораторна №1)
k8s/                       маніфести Kubernetes (лабораторна №3), README з командами
charts/devopslabka/        Helm-чарт (лабораторна №4), README з параметрами
backend/
  Dockerfile               багатоетапна збірка: Maven → JRE 25
  checkstyle.xml           правила статичного аналізу
  pom.xml, src/            код і тести (JUnit: AccessTest, JwtTest)
  http/, postman/          запити для ручної перевірки API
frontend/
  Dockerfile               багатоетапна збірка: Node → nginx
  nginx.conf               роздача статики + проксі /api → backend
  src/                     index.html, app.js, lib.js, style.css
  test/                    юніт-тести (node:test)
  eslint.config.js         правила ESLint
```

## Лабораторна №1 — Docker

Запуск усього проєкту однією командою (потрібен Docker Desktop):

```bash
docker compose up -d --build
```

- Вебінтерфейс: http://localhost:3000
- API: http://localhost:8081/api/v1/items (Basic або Bearer авторизація)
- Зупинити: `docker compose down` (з видаленням даних БД: `docker compose down -v`)

Що є в проєкті:
- `backend/Dockerfile` — етап `build` збирає jar через Maven, фінальний образ містить лише JRE і jar, запускається не від root;
- `frontend/Dockerfile` — етап `build` збирає статику через Node, фінальний образ — nginx (unprivileged);
- `docker-compose.yml` — MongoDB (з healthcheck), backend (чекає, поки БД стане healthy), frontend.
  Адреса БД і JWT-секрет передаються через змінні середовища `MONGODB_URI` і `JWT_SECRET`.

Локальний запуск без Docker (для розробки):

```bash
docker compose up -d mongo
cd backend && mvn spring-boot:run          # http://localhost:8080
cd frontend && npm ci && npm test && npm run lint && npm run build
```

## Лабораторна №2 — Continuous Integration (GitHub Actions)

Файл: `.github/workflows/ci.yml`. Запускається на кожен **push** у будь-яку гілку і на кожен **Pull Request** у `main`;
статус видно прямо в PR.

| Job | Кроки | Кеш |
|-----|-------|-----|
| `backend (build, lint, test)` | Build (`mvn compile`) → Lint (Checkstyle) → Test (`mvn test`) → Package jar | Maven (`setup-java cache: maven`) |
| `frontend (build, lint, test)` | `npm ci` → Lint (ESLint) → Build → Test (`node --test`) | npm (`setup-node cache: npm`) |
| `docker (backend)`, `docker (frontend)` | Build image → Trivy scan → Login ghcr.io → Push | Docker layers (`type=gha`) |

- `backend` і `frontend` — окремі незалежні job'и, виконуються паралельно; падіння одного не скасовує інший.
- Лінтер — окремий блокуючий крок перед тестами. Порушення Checkstyle/ESLint завершують job з помилкою
  і показуються в PR як анотації до рядків коду (problem matcher).
- `docker` залежить від обох job'ів (`needs: [backend, frontend]`), збирає образ кожного сервісу з його Dockerfile (matrix),
  сканує його Trivy (CRITICAL-вразливості, для яких є виправлення, блокують пайплайн).
- Публікація в **GitHub Container Registry** — лише для `push` (не для PR). Авторизація через `secrets.GITHUB_TOKEN`,
  жодних паролів у репозиторії. Теги:
  - `<повний commit SHA>` і `run-<номер запуску>` — унікальні теги збірки;
  - `latest` — лише для гілки `main`.

### Перевірка опублікованих образів

```bash
docker pull ghcr.io/zelderian/devopslabka-backend:latest
docker pull ghcr.io/zelderian/devopslabka-frontend:latest
docker run --rm -p 8080:8080 ghcr.io/zelderian/devopslabka-backend:latest
```

> Нові пакети в ghcr.io за замовчуванням приватні: або зробіть їх публічними
> (GitHub → Packages → пакет → Package settings → Change visibility → Public),
> або спершу виконайте `docker login ghcr.io` з Personal Access Token (`read:packages`).

### Захист гілки `main`

Settings → Branches → Add branch ruleset (або Add classic branch protection rule) для `main`:
- Require a pull request before merging;
- Require status checks to pass → додати `backend (build, lint, test)`, `frontend (build, lint, test)`,
  `k8s (manifests, helm lint)`, `docker (backend)`, `docker (frontend)`;
- Block force pushes / заборонити прямий push.

Демонстрація заблокованого PR: створити гілку, зламати тест (наприклад, змінити очікуваний статус у
`backend/src/test/.../AccessTest.java`) або додати невикористаний імпорт/змінну, відкрити PR → перевірка червона,
кнопка Merge заблокована.

## Випуск версій (для лабораторних 3–5)

Образи з тегом версії збирає CI з git-тегу:

```bash
git tag v1.0.0 && git push origin v1.0.0     # -> ghcr.io/zelderian/devopslabka-{backend,frontend}:1.0.0
# невелика видима зміна, commit, PR, merge
git tag v1.1.0 && git push origin v1.1.0     # -> ...:1.1.0
```

Версія вшивається в образ (`APP_VERSION`) і видна на `GET /version`, `GET /health` бекенду
(у `/health` також `instance` — ім'я пода, що відповів) і внизу сторінки фронтенду.

## Лабораторна №3 — Kubernetes

Маніфести в `k8s/`, розгортання однією командою `kubectl apply -f k8s/`.
Порядок розгортання, параметри ConfigMap/Secret і команди для демонстрації — у [k8s/README.md](k8s/README.md).

## Лабораторна №4 — Helm

Чарт `charts/devopslabka`, середовища `values-dev.yaml` і `values-prod.yaml`, pre-upgrade хук і `helm test`.
Таблиця параметрів і команди — у [charts/devopslabka/README.md](charts/devopslabka/README.md).
CI-job `k8s (manifests, helm lint)` перевіряє маніфести і чарт (`helm lint`, `kubeconform`).

## Лабораторна №5 — GitOps (Argo CD)

Окремий репозиторій конфігурації [DevOpsLabka-gitops](https://github.com/zelderian/DevOpsLabka-gitops):
App of Apps, чарт з ЛР-4 і `values-minikube.yaml`; реліз — зміна тегу образу комітом.

## Попередні лабораторні з вебзахисту

Backend — це проєкт `web-security26` (лаби 1–9: CRUD, Basic auth, CSRF, ролі, анотації, JUnit-тести, JWT).
HTTP-запити до кожної лаби лежать у `backend/http/`, колекція Postman — у `backend/postman/`.
