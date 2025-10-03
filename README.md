# Meteo - Weather Monitoring Android App

Современное Android приложение для мониторинга метеорологических станций с отображением данных в реальном времени на карте и графиках.

![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Language](https://img.shields.io/badge/language-Kotlin-blue.svg)
![MinSDK](https://img.shields.io/badge/minSdk-26-orange.svg)
![TargetSDK](https://img.shields.io/badge/targetSdk-35-orange.svg)

## 📋 Содержание

- [О проекте](#о-проекте)
- [Возможности](#возможности)
- [Технологии](#технологии)
- [Архитектура](#архитектура)
- [Установка и настройка](#установка-и-настройка)
- [Конфигурация](#конфигурация)
- [API Документация](#api-документация)
- [Структура проекта](#структура-проекта)

## 🌦️ О проекте

**Meteo** — это мобильное приложение для мониторинга погодных условий с метеорологических станций. Приложение предоставляет:
- Интерактивную карту с расположением метеостанций
- Отображение данных в реальном времени
- Исторические графики погодных параметров
- Управление станциями и настройками параметров
- Персонализацию через избранные станции

## ✨ Возможности

### 🗺️ Карта станций
- Интерактивная карта с метеостанциями на базе Yandex MapKit
- Отображение текущих данных при клике на станцию
- Фильтрация по избранным станциям

### 📊 Графики и аналитика
- Построение графиков для любого параметра
- Выбор временного диапазона
- Визуализация исторических данных
- Поддержка различных метеопараметров

### 🔧 Управление станциями
- Добавление/удаление станций по 8-значному коду
- Переименование станций (пользовательские названия)
- Избранные станции для быстрого доступа
- Настройка видимости параметров для каждой станции

### 🔐 Безопасность
- JWT аутентификация (access + refresh tokens)
- Шифрованное хранение токенов (EncryptedSharedPreferences AES256-GCM)
- Network Security Configuration
- Certificate Pinning готов к использованию

## 🛠️ Технологии

### Core
- **Kotlin 2.0.0** - язык разработки
- **Jetpack Compose (BOM 2025.04.01)** - современный UI toolkit
- **Material Design 3** - дизайн система

### Архитектурные компоненты
- **MVVM** - архитектурный паттерн
- **SOLID Principles** - принципы проектирования
- **Hilt 2.51.1** - Dependency Injection
- **Kotlin Coroutines** - асинхронность
- **StateFlow** - reactive state management

### Сеть и данные
- **Retrofit 2.9.0** - HTTP клиент
- **OkHttp 4.12.0** - networking
- **Gson** - JSON сериализация
- **EncryptedSharedPreferences** - безопасное хранение

### UI и визуализация
- **Yandex MapKit KMP** - карты
- **YCharts 2.1.0** - графики
- **Compose Navigation** - навигация
- **Core SplashScreen** - экран загрузки

## 🏗️ Архитектура

Приложение следует **MVVM паттерну** с четким разделением ответственности согласно **SOLID принципам**:

```
app/
├── common/           # Общие утилиты и константы
├── config/           # Конфигурация приложения
├── data/             # Data models
├── network/          # API слой
│   ├── MeteoApiService.kt
│   ├── AuthManager.kt
│   └── MeteoModule.kt (DI)
├── repository/       # Репозитории (интерфейсы + реализации)
│   ├── interfaces/
│   └── impl/
├── storage/          # Безопасное хранение
├── ui/               # UI слой (Compose)
│   ├── login/
│   ├── map/
│   ├── chart/
│   ├── stations/
│   └── components/
└── utils/            # Вспомогательные классы
```

### Ключевые принципы

#### Single Responsibility Principle (SRP)
- Разделены ViewModels: `MapViewModel`, `ChartViewModel`, `LoginViewModel`
- Выделены утилиты: `SensorDataCache`, `RetryPolicy`, `StationDataTransformer`

#### Interface Segregation Principle (ISP)
- `SensorDataRepository` - только операции с данными датчиков
- `StationRepository` - только управление станциями
- `SecureStorage` - только операции хранения

#### Dependency Inversion Principle (DIP)
- Все зависимости через интерфейсы
- Hilt для автоматической инъекции зависимостей

## 🚀 Установка и настройка

### Требования
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17+
- Android SDK 26+
- Gradle 8.0+

### Шаги установки

1. **Клонируйте репозиторий**
```bash
git clone https://github.com/yourusername/meteo.git
cd meteo
```

2. **Создайте файл `local.properties`**

Скопируйте `local.properties.example` в `local.properties`:
```bash
cp local.properties.example local.properties
```

3. **Настройте API ключи в `local.properties`**
```properties
# Yandex MapKit API Key
YANDEX_MAPKIT_API_KEY=your_yandex_mapkit_api_key

# API Configuration
DEFAULT_BASE_URL=https://your-api-server.com/api/v1/
API_HOST=your-api-server.com
```

4. **Соберите проект**

В Android Studio: `Build → Rebuild Project`

Или через Gradle:
```bash
./gradlew build
```

5. **Запустите приложение**

В Android Studio: `Run → Run 'app'`

Или через adb:
```bash
./gradlew installDebug
```

## ⚙️ Конфигурация

### local.properties

Конфиденциальные данные хранятся в `local.properties`:

```properties
# Yandex MapKit API Key
YANDEX_MAPKIT_API_KEY=your_api_key_here

# API Configuration
DEFAULT_BASE_URL=https://api.example.com/api/v1/
API_HOST=api.example.com
```

### BuildConfig

Значения из `local.properties` автоматически добавляются в `BuildConfig`:
- `BuildConfig.YANDEX_MAPKIT_API_KEY`
- `BuildConfig.DEFAULT_BASE_URL`
- `BuildConfig.API_HOST`
- `BuildConfig.ENABLE_NETWORK_LOGGING` (debug/release)

### Security Config

Настройки безопасности в `DefaultConfigProvider.kt`:

**Debug конфигурация:**
- Certificate Pinning: выключен
- Network Logging: включен
- Cleartext Traffic: разрешен

**Release конфигурация:**
- Certificate Pinning: включен
- Network Logging: выключен
- Cleartext Traffic: запрещен (рекомендуется)

## 📡 API Документация

### Базовый URL
```
https://your-server.com/api/v1/
```

### Эндпоинты

#### Аутентификация
```http
POST /auth/login
POST /auth/register
POST /auth/refresh
GET  /auth/me
```

#### Станции
```http
GET    /stations
POST   /stations
PATCH  /stations/{station_number}
DELETE /stations/{station_number}
GET    /stations/{station_number}/parameters
PATCH  /stations/{station_number}/parameters/{parameter_code}
```

#### Данные
```http
GET /data/latest
GET /data/{station_number}/latest
GET /data/{station_number}/{parameter_code}/history
```

### Формат ответа

Все API v1 эндпоинты используют обертку `ApiResponse`:
```json
{
  "success": true,
  "data": { ... }
}
```

### Аутентификация

JWT токены передаются через заголовок:
```http
Authorization: Bearer <access_token>
```

**Refresh token flow:**
```
1. Access token истек
2. Автоматический запрос /auth/refresh
3. Получение нового access token
4. Повтор оригинального запроса
```

### Рекомендации для Production

1. **Включите Certificate Pinning** в `DefaultConfigProvider.kt`
2. **Отключите Cleartext Traffic** в `AndroidManifest.xml`:
```xml
android:usesCleartextTraffic="false"
```
3. **Используйте только HTTPS** в `DEFAULT_BASE_URL`
4. **Включите ProGuard/R8** для обфускации кода

## 📁 Структура проекта

```
Meteo/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/shestikpetr/meteo/
│   │   │   │   ├── App.kt                    # Hilt Application
│   │   │   │   ├── MainActivity.kt           # Main Activity
│   │   │   │   ├── cache/                    # Кэширование
│   │   │   │   ├── common/                   # Константы и утилиты
│   │   │   │   ├── config/                   # Конфигурация
│   │   │   │   ├── data/                     # Data models
│   │   │   │   ├── network/                  # API и сеть
│   │   │   │   ├── repository/               # Репозитории
│   │   │   │   ├── service/                  # Services
│   │   │   │   ├── storage/                  # Хранилище
│   │   │   │   ├── ui/                       # UI (Compose)
│   │   │   │   └── utils/                    # Утилиты
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                             # Unit tests
│   │   └── androidTest/                      # Instrumented tests
│   ├── build.gradle.kts                      # App build config
│   └── proguard-rules.pro                    # ProGuard rules
├── gradle/
│   └── libs.versions.toml                    # Version catalog
├── build.gradle.kts                          # Project build config
├── settings.gradle.kts                       # Project settings
├── local.properties.example                  # Example config
├── .gitignore                                # Git ignore rules
├── CLAUDE.md                                 # Claude Code instructions
└── README.md                                 # This file
```

## 📄 Лицензия

Этот проект находится в разработке. Лицензия будет добавлена позже.

## Автор

Шестопалов Пётр Андреевич

## 📞 Контакты

- **GitHub**: [shestikpetr](https://github.com/shestikpetr)
- **Email**: [shestikpetr@gmail.com](mailto:shestikpetr@gmail.com)
- **Telegram**: [@shestikpetr](https://t.me/shestikpetr)
