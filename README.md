# MSA HW — RESTful CRUD

Spring Boot приложение с CRUD для пользователей.

Используется:
- Spring Boot
- PostgreSQL
- Docker
- Kubernetes
- Helm
- Nginx Ingress
- Postman / Newman

Docker image:

```text
armandockerrr/msa-hw2:latest
```

## API

Base URL:

```text
http://arch.homework
```

Методы:

```text
POST   /user
GET    /user/{id}
PUT    /user/{id}
DELETE /user/{id}

GET    /health
```

---

## Запуск

### 1. Запустить Minikube

```bash
minikube start --driver=docker
```

### 2. Создать Secret для PostgreSQL

```bash
kubectl apply -f k8s/00-postgres-secret.yaml
```

### 3. Установить PostgreSQL через Helm

```bash
helm install postgres oci://registry-1.docker.io/bitnamicharts/postgresql -f helm/postgres-values.yaml
```

Дождаться запуска БД:

```bash
kubectl wait --for=condition=Ready pod/postgres-postgresql-0 --timeout=180s
```

### 4. Применить миграцию

```bash
kubectl apply -f k8s/01-migration-configmap.yaml
kubectl apply -f k8s/02-db-migration-job.yaml
```

Дождаться выполнения:

```bash
kubectl wait --for=condition=complete job/db-migration --timeout=180s
```

Проверить:

```bash
kubectl logs job/db-migration
```

Ожидаемый результат:

```text
CREATE TABLE
```

### 5. Запустить приложение

```bash
kubectl apply -f k8s/03-app-configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

Проверить:

```bash
kubectl get pods
kubectl get services
kubectl get ingress
```

---

## Ingress

В файл hosts добавить:

```text
127.0.0.1 arch.homework
```

Для Windows:

```text
C:\Windows\System32\drivers\etc\hosts
```

Запустить:

```bash
minikube tunnel
```

Проверка:

```bash
curl http://arch.homework/health
```

Ответ:

```json
{"status":"OK"}
```

---

## Postman

Коллекция:

```text
postman/MSA_HW_CRUD.postman_collection.json
```

Содержит проверки:

```text
CREATE USER
GET USER
UPDATE USER
DELETE USER
GET DELETED USER
```

Запуск через Newman:

```bash
newman.cmd run postman/MSA_HW_CRUD.postman_collection.json
```

Успешный результат:

```text
failed: 0
```

---

## Структура Kubernetes

```text
k8s/
├── 00-postgres-secret.yaml
├── 01-migration-configmap.yaml
├── 02-db-migration-job.yaml
├── 03-app-configmap.yaml
├── deployment.yaml
├── service.yaml
└── ingress.yaml
```

PostgreSQL values:

```text
helm/postgres-values.yaml
```

## Мониторинг Prometheus и Grafana

Сервис инструментирован с помощью Spring Boot Actuator и Micrometer для сбора метрик Prometheus.

### Состав мониторинга

Используются:

- Prometheus;
- Grafana;
- метрики nginx-ingress-controller;
- метрики Spring Boot приложения.

Prometheus получает метрики приложения через:

```text
/actuator/prometheus
```

Для приложения собираются:

- RPS по API-методам;
- latency p50 / p95 / p99 / max;
- количество HTTP 500 ошибок.

Для nginx-ingress-controller собираются:

- общий RPS сервиса;
- latency p50 / p95 / p99 / max;
- количество HTTP 500 ошибок.

### Установка Prometheus

```powershell
helm install prometheus prometheus-community/prometheus `
  --namespace monitoring `
  --create-namespace `
  -f helm/prometheus-values.yaml
```

### Установка Grafana

```powershell
helm install grafana grafana-community/grafana `
  --namespace monitoring `
  -f helm/grafana-values.yaml
```

### Включение метрик nginx-ingress-controller

```powershell
helm upgrade nginx ingress-nginx/ingress-nginx `
  --namespace m `
  -f helm/nginx_ingress.yaml
```

### Открытие Grafana

```powershell
kubectl port-forward -n monitoring svc/grafana 3000:80
```

Grafana доступна по адресу:

```text
http://localhost:3000
```

### Нагрузочное тестирование

Для генерации нагрузки используется ранее подготовленная Postman/Newman коллекция CRUD-запросов:

```powershell
newman.cmd run postman\MSA_HW_CRUD.postman_collection.json `
  -n 300 `
  --delay-request 200
```

Нагрузочное тестирование выполнялось в течение примерно 5 минут.

### Grafana Dashboard

Экспортированный dashboard сохранён в:

```text
grafana/msa-service-monitoring.json
```

Dashboard содержит следующие панели:

1. RPS по API-методам;
2. API Latency p50 / p95 / p99 / max;
3. HTTP 500 Errors по API-методам;
4. общий RPS nginx-ingress;
5. Ingress Latency p50 / p95 / p99 / max;
6. Ingress HTTP 500 Errors.

### Alerting

В Grafana настроены правила оповещения:

- при появлении HTTP 500 ошибок;
- при превышении p95 latency значения 500 ms.

## Централизованное логирование

Для централизованного сбора и анализа логов используется EFK-стек:

- Elasticsearch;
- Fluent Bit;
- Kibana.

Приложение пишет структурированные JSON-логи в stdout. Fluent Bit развёрнут как DaemonSet и автоматически собирает логи Kubernetes-контейнеров, обогащает их метаданными Kubernetes и отправляет в Elasticsearch.

### Структурированное логирование приложения

В логах фиксируются:

- запуск и остановка приложения — `INFO`;
- входящие HTTP-запросы: метод, путь, IP — `INFO`;
- успешные CRUD-операции — `INFO`;
- ошибки валидации — `WARN`;
- ошибки БД и приложения — `ERROR`;
- начало и завершение Job миграции — `INFO`.

Для CRUD-операций используются дополнительные структурированные поля:

```text
operation
user_id
```

Для HTTP-запросов:

```text
http_method
path
client_ip
status
duration_ms
```

Для ошибок:

```text
error_type
```

### Namespace для EFK

```powershell
kubectl create namespace logging
```

### Установка Elasticsearch

```powershell
kubectl apply -f k8s/logging/01-elasticsearch-services.yaml
kubectl apply -f k8s/logging/02-elasticsearch-statefulset.yaml
```

### Установка Kibana

```powershell
kubectl apply -f k8s/logging/03-kibana.yaml
```

### Установка Fluent Bit

```powershell
kubectl apply -f k8s/logging/04-fluent-bit-configmap.yaml
kubectl apply -f k8s/logging/05-fluent-bit-daemonset.yaml
```

### Проверка компонентов

```powershell
kubectl get pods -n logging
```

Ожидаемые компоненты:

```text
elasticsearch-0
kibana-...
fluent-bit-...
```

### Открытие Kibana

```powershell
minikube service kibana -n logging --url
```

### Elasticsearch

Для локального доступа:

```powershell
kubectl port-forward -n logging svc/elasticsearch 9200:9200
```

Проверка индексов:

```powershell
curl.exe "http://localhost:9200/_cat/indices?v"
```

Fluent Bit создаёт индексы:

```text
fluent-bit-logs-*
```

### Kibana Data View

В Kibana:

```text
Stack Management → Data Views → Create data view
```

Настройки:

```text
Name: fluent-bit-logs
Index pattern: fluent-bit-logs-*
Timestamp field: @timestamp
```

### Примеры фильтров в Discover

Логи приложения:

```text
kubernetes.namespace_name: "default" AND kubernetes.pod_name: msa-hw2-*
```

INFO:

```text
level: "INFO"
```

Ошибки валидации:

```text
level: "WARN" AND error_type: "VALIDATION_ERROR"
```

Ошибки БД:

```text
level: "ERROR" AND error_type: "DATABASE_ERROR"
```

Создание пользователя:

```text
level: "INFO" AND operation: "CREATE"
```

Поиск конкретного пользователя:

```text
user_id: 1
```

Логи миграции БД:

```text
service: "db-migration"
```

### Перезапуск Job миграции

```powershell
kubectl delete job db-migration
kubectl apply -f k8s/02-db-migration-job.yaml
kubectl logs job/db-migration
```

В логах Job фиксируются события:

```text
MIGRATION_START
MIGRATION_FINISH
```

### Скриншоты

Скриншоты работы Kibana находятся в:

```text
docs/logging/
```