# MeteoApp API Documentation

Базовый URL: `http://84.237.1.131:8085`

## Аутентификация

API использует JWT токены для аутентификации. Все эндпоинты (кроме регистрации и входа) требуют заголовок Authorization:

```
Authorization: Bearer <access_token>
```

### Типы токенов
- **Access Token**: Срок действия 24 часа, используется для API запросов
- **Refresh Token**: Срок действия 30 дней, используется для обновления access токена

---

## Эндпоинты аутентификации

### POST `/api/v1/auth/register`
Регистрация нового пользователя

**Тело запроса:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

**Ответ успешный (201):**
```json
{
  "success": true,
  "data": {
    "user_id": 123,
    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
  }
}
```

**Ошибки:**
- `400` - Отсутствуют данные или неверный формат
- `409` - Пользователь уже существует

---

### POST `/api/v1/auth/login`
Вход пользователя

**Тело запроса:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Ответ успешный (200):**
```json
{
  "success": true,
  "data": {
    "user_id": 123,
    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refresh_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
  }
}
```

**Ошибки:**
- `400` - Отсутствуют данные
- `401` - Неверные учетные данные

---

### POST `/api/v1/auth/refresh`
Обновление access токена

**Заголовки:**
```
Authorization: Bearer <refresh_token>
```

**Ответ успешный (200):**
```json
{
  "success": true,
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

**Ошибки:**
- `401` - Refresh токен недействителен или просрочен

---

### GET `/api/v1/auth/me`
Получить информацию о текущем пользователе

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Ответ успешный (200):**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "username": "user123",
    "email": "user@example.com",
    "role": "user",
    "is_active": true
  }
}
```

---

## Эндпоинты станций

### GET `/api/v1/stations`
Получить все станции пользователя

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Ответ успешный (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "station_number": "12345678",
      "name": "Станция №1",
      "custom_name": "Моя станция",
      "display_name": "Моя станция",
      "location": "Москва",
      "latitude": 55.7558,
      "longitude": 37.6176,
      "altitude": 156.0,
      "is_favorite": false,
      "is_active": true,
      "parameters": ["T", "H", "P"]
    }
  ]
}
```

---

### POST `/api/v1/stations`
Добавить станцию пользователю

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Тело запроса:**
```json
{
  "station_number": "12345678",
  "custom_name": "Моя станция" // необязательно
}
```

**Ответ успешный (201):**
```json
{
  "success": true,
  "data": {
    "user_station_id": 15,
    "station_number": "12345678",
    "name": "Моя станция",
    "parameters": ["T", "H", "P"]
  }
}
```

**Ошибки:**
- `400` - Не указан номер станции или неверный формат (должен содержать 8 цифр)
- `404` - Станция с указанным номером не существует в базе данных датчиков
- `409` - Станция уже добавлена к пользователю

---

### PUT `/api/v1/stations/<station_number>`
Обновить настройки станции пользователя

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Параметры URL:**
- `station_number` - номер станции (8 цифр)

**Тело запроса:**
```json
{
  "custom_name": "Новое название", // необязательно
  "is_favorite": true // необязательно
}
```

**Ответ успешный (200):**
```json
{
  "success": true
}
```

**Ошибки:**
- `404` - Станция не найдена у пользователя

---

### DELETE `/api/v1/stations/<station_number>`
Удалить станцию у пользователя

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Параметры URL:**
- `station_number` - номер станции (8 цифр)

**Ответ успешный (200):**
```json
{
  "success": true
}
```

**Ошибки:**
- `404` - Станция не найдена или не принадлежит пользователю

---

### GET `/api/v1/stations/<station_number>/parameters`
Получить параметры станции

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Параметры URL:**
- `station_number` - номер станции (8 цифр)

**Ответ успешный (200):**
```json
{
  "success": true,
  "data": [
    {
      "code": "T",
      "name": "Температура",
      "unit": "°C",
      "description": "Температура воздуха",
      "category": "temperature"
    },
    {
      "code": "H",
      "name": "Влажность",
      "unit": "%",
      "description": "Относительная влажность воздуха",
      "category": "humidity"
    }
  ]
}
```

**Ошибки:**
- `404` - Станция не найдена или нет доступа

---

## Эндпоинты данных датчиков

### GET `/api/v1/sensors/<station_number>/<parameter>/latest`
Получить последнее значение параметра

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Параметры URL:**
- `station_number` - номер станции (8 цифр)
- `parameter` - код параметра (например, "T", "H", "P")

**Ответ успешный (200):**
```json
{
  "success": true,
  "data": {
    "time": 1672531200,
    "value": 23.5,
    "parameter": "T",
    "station": "12345678"
  }
}
```

**Ошибки:**
- `404` - Станция не найдена, нет доступа или нет данных

---

### GET `/api/v1/sensors/<station_number>/<parameter>`
Получить временной ряд данных

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Параметры URL:**
- `station_number` - номер станции (8 цифр)
- `parameter` - код параметра (например, "T", "H", "P")

**Параметры запроса:**
- `start_time` (необязательно) - начальное время в Unix timestamp
- `end_time` (необязательно) - конечное время в Unix timestamp
- `limit` (необязательно) - максимальное количество записей (по умолчанию: 1000)

**Пример запроса:**
```
GET /api/v1/sensors/12345678/T?start_time=1672531200&end_time=1672617600&limit=500
```

**Ответ успешный (200):**
```json
{
  "success": true,
  "data": [
    {
      "time": 1672531200,
      "value": 23.5,
      "parameter": "T",
      "station": "12345678"
    },
    {
      "time": 1672531800,
      "value": 24.1,
      "parameter": "T",
      "station": "12345678"
    }
  ]
}
```

---

### GET `/api/v1/sensors/<station_number>/latest`
Получить последние значения всех параметров станции

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Параметры URL:**
- `station_number` - номер станции (8 цифр)

**Ответ успешный (200):**
```json
{
  "success": true,
  "data": [
    {
      "time": 1672531200,
      "value": 23.5,
      "parameter": "T",
      "station": "12345678"
    },
    {
      "time": 1672531200,
      "value": 65.2,
      "parameter": "H",
      "station": "12345678"
    },
    {
      "time": 1672531200,
      "value": 1013.25,
      "parameter": "P",
      "station": "12345678"
    }
  ]
}
```

---

### GET `/api/v1/sensors/latest`
Получить последние данные всех станций пользователя

**Заголовки:**
```
Authorization: Bearer <access_token>
```

**Ответ успешный (200):**
```json
{
  "success": true,
  "data": {
    "12345678": [
      {
        "time": 1672531200,
        "value": 23.5,
        "parameter": "T",
        "station": "12345678"
      },
      {
        "time": 1672531200,
        "value": 65.2,
        "parameter": "H",
        "station": "12345678"
      }
    ],
    "87654321": [
      {
        "time": 1672531200,
        "value": 18.7,
        "parameter": "T",
        "station": "87654321"
      }
    ]
  }
}
```

---

## Коды ошибок

### Общие коды ошибок
- `400` - Неверный запрос (отсутствуют обязательные поля, неверный формат данных)
- `401` - Не авторизован (отсутствует или недействителен токен)
- `403` - Доступ запрещен
- `404` - Ресурс не найден
- `409` - Конфликт (например, пользователь уже существует)
- `500` - Внутренняя ошибка сервера

### Формат ответа с ошибкой
```json
{
  "error": "Описание ошибки"
}
```

---

## Примеры использования

### Полный цикл работы с API

1. **Регистрация/Вход:**
```bash
curl -X POST http://84.237.1.131:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "myuser", "password": "mypassword"}'
```

2. **Добавление станции:**
```bash
curl -X POST http://84.237.1.131:8085/api/v1/stations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{"station_number": "12345678", "custom_name": "Моя станция"}'
```

3. **Получение последних данных:**
```bash
curl -X GET http://84.237.1.131:8085/api/v1/sensors/12345678/latest \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

4. **Получение временного ряда:**
```bash
curl -X GET "http://84.237.1.131:8085/api/v1/sensors/12345678/T?start_time=1672531200&limit=100" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Примечания

- Все времена указываются в Unix timestamp (секунды с 1 января 1970 UTC)
- Номера станций должны содержать ровно 8 цифр
- API поддерживает CORS для мобильных приложений
- Максимальный размер запроса: лимит определяется сервером
- Рекомендуется использовать HTTPS в продакшене