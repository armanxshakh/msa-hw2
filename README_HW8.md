# HW8 — Stream Processing

## 1. Цель

Реализовать взаимодействие сервисов заказа, биллинга и уведомлений с использованием брокера сообщений.

В рамках практической части выбран вариант **Event Collaboration через Kafka**.

## 2. Сервисы

В решении используются:

- `msa-hw2` — User/Auth Service, порт `8000`;
- `billing-service` — Billing Service, порт `8001`;
- `order-service` — Order Service, порт `8002`;
- `notification-service` — Notification Service, порт `8003`;
- Kafka в режиме KRaft;
- PostgreSQL;
- NGINX Ingress.

Внешняя точка входа:

```text
http://arch.homework
```

## 3. Варианты архитектуры

### Вариант 1 — только HTTP

Все сервисы вызывают друг друга синхронно по HTTP.

Пример:

```text
Client -> User Service -> Billing Service
Client -> Order Service -> Billing Service -> Notification Service
```

Плюсы:

- простая реализация;
- легко отлаживать;
- понятный request/response flow.

Минусы:

- сильная связанность сервисов;
- недоступность одного сервиса влияет на всю цепочку;
- длинная последовательность синхронных вызовов.

### Вариант 2 — брокер только для уведомлений

User Service и Billing Service, а также Order Service и Billing Service продолжают взаимодействовать по HTTP.

После обработки заказа Order Service публикует событие для Notification Service через брокер.

Плюсы:

- Notification Service становится асинхронным;
- Order Service не ждёт сохранения уведомления.

Минусы:

- основная часть бизнес-процесса остаётся синхронной;
- сохраняется сильная связанность между Order и Billing.

### Вариант 3 — Event Collaboration

В этом варианте сервисы взаимодействуют через события Kafka.

```text
User Service
    |
    | user-created
    v
Kafka
    |
    v
Billing Service

Order Service
    |
    | order-created
    v
Kafka
    |
    v
Billing Service
    |
    | payment-result
    v
Kafka
   / \
  v   v
Order Service
Notification Service
```

Этот вариант выбран для практической реализации.

Причины выбора:

- слабая связанность сервисов;
- асинхронное взаимодействие;
- каждый сервис отвечает только за свою область;
- хорошо соответствует stream/event-driven архитектуре;
- легко добавлять новых consumers без изменения существующих producers.

## 4. Kafka topics

Используются три topic:

```text
user-created
order-created
payment-result
```

### user-created

Publisher:

```text
User Service
```

Consumer:

```text
Billing Service
```

Назначение:

После регистрации пользователя Billing Service автоматически создаёт ему billing account с нулевым балансом.

### order-created

Publisher:

```text
Order Service
```

Consumer:

```text
Billing Service
```

Назначение:

Billing Service получает созданный заказ, проверяет баланс и при наличии средств списывает стоимость заказа.

### payment-result

Publisher:

```text
Billing Service
```

Consumers:

```text
Order Service
Notification Service
```

Order Service меняет статус:

```text
PENDING -> PAID
```

или:

```text
PENDING -> FAILED
```

Notification Service сохраняет уведомление:

```text
Письмо счастья
```

или:

```text
Письмо горя
```

## 5. Kafka

Kafka установлена через Helm.

Конфигурация:

```text
helm/kafka-values.yaml
```

Установка:

```powershell
helm install kafka oci://registry-1.docker.io/bitnamicharts/kafka -f helm/kafka-values.yaml
```

Обновление:

```powershell
helm upgrade kafka oci://registry-1.docker.io/bitnamicharts/kafka -f helm/kafka-values.yaml
```

Kafka работает в namespace:

```text
default
```

Service:

```text
kafka:9092
```

Внутренний адрес:

```text
kafka.default.svc.cluster.local:9092
```

Проверка topics:

```powershell
kubectl exec -it kafka-controller-0 -- kafka-topics.sh --bootstrap-server kafka:9092 --list
```

## 6. Kubernetes

Основные сервисы запускаются в namespace:

```text
default
```

Ingress controller находится в namespace:

```text
m
```

Проверка pod:

```powershell
kubectl get pods
```

Проверка ingress:

```powershell
kubectl get ingress
```

После перезапуска Minikube при необходимости:

```powershell
minikube update-context
```

Для доступа к:

```text
http://arch.homework
```

в отдельном PowerShell с правами администратора используется:

```powershell
minikube tunnel
```

## 7. Проверка бизнес-сценария

Реализован следующий сценарий.

### Шаг 1

Создать пользователя.

После события:

```text
user-created
```

должен автоматически появиться billing account.

### Шаг 2

Пополнить баланс на:

```text
1000
```

### Шаг 3

Создать заказ стоимостью:

```text
300
```

Первоначальный статус:

```text
PENDING
```

После обработки:

```text
PAID
```

### Шаг 4

Проверить баланс.

Ожидаемый результат:

```text
700
```

### Шаг 5

Проверить уведомление.

Ожидается:

```text
Письмо счастья
```

### Шаг 6

Создать заказ стоимостью:

```text
1000
```

при текущем балансе `700`.

### Шаг 7

После обработки заказ должен получить статус:

```text
FAILED
```

Баланс должен остаться:

```text
700
```

### Шаг 8

Проверить уведомления.

Должно появиться:

```text
Письмо горя
```

## 8. Postman / Newman

Коллекция:

```text
postman/HW8-Stream-Processing.postman_collection.json
```

Запуск:

```powershell
newman run postman/HW8-Stream-Processing.postman_collection.json `
  --env-var "baseUrl=http://arch.homework" `
  --verbose
```

Результат успешного запуска:

```text
iterations       1   failed 0
requests        11   failed 0
test-scripts    16   failed 0
assertions      24   failed 0
```

Итого:

```text
24 assertions
0 failed
```

Дополнительные HTTP-запросы внутри коллекции используются для проверки асинхронной обработки Kafka.

## 9. API contracts

HTTP API описан в:

```text
docs/hw8/openapi.yaml
```

Kafka events описаны в:

```text
docs/hw8/asyncapi.yaml
```

## 10. Диаграммы

Draw.io файл:

```text
docs/hw8/HW8_Stream_Processing_Architecture.drawio
```

В файле представлены:

1. HTTP-only;
2. broker only for notifications;
3. Event Collaboration;
4. реализованная архитектура HW8 в Kubernetes.

## 11. Итоговая архитектура

В практической части используется Event Collaboration.

Основная последовательность:

```text
Register user
  -> user-created
  -> Billing account created

Create order
  -> order-created
  -> Billing checks balance
  -> payment-result
  -> Order status updated
  -> Notification saved
```

Сервисы не вызывают друг друга напрямую для выполнения основного бизнес-процесса заказа. Обмен состоянием выполняется через Kafka events.
