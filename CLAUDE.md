# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Meteo is an Android weather monitoring application built with Kotlin and Jetpack Compose. It connects to meteorological stations to display real-time weather data on maps and charts. The app uses Hilt for dependency injection, Retrofit for networking, and Yandex MapKit for map functionality.

## Development Commands

**Note: All Gradle commands are executed through Android Studio UI, not console commands.**

### Build and Run (via Android Studio)
- **Build → Clean Project** - Clean build artifacts
- **Build → Rebuild Project** - Build the entire project
- **Build → Build APK(s)** - Build debug APK
- **Build → Generate Signed Bundle/APK** - Build release APK
- **Run → Run 'app'** - Install and run debug APK on connected device

### Testing (via Android Studio)
- **Right-click on test directories → Run Tests** - Run unit tests
- **Run → Run All Tests** - Run all tests in the project
- **Right-click on androidTest → Run Tests** - Run instrumented tests on connected device

### Code Quality (via Android Studio)
- **Analyze → Inspect Code** - Run Android lint checks and code analysis
- **Build → Analyze APK** - Analyze APK size and content

## Architecture

**⚠️ IMPORTANT: The codebase has been refactored to follow SOLID principles. See SOLID Refactoring section below.**

### Core Components
- **App.kt** - Hilt application class for dependency injection setup
- **MainActivity.kt** - Main activity using LocationPermissionManager and MapKitLifecycleManager
- **MeteoModule.kt + RepositoryModule.kt** - Hilt modules for dependency injection
- **Utility Classes** - SensorDataCache, RetryPolicy, StationDataTransformer extracted for SRP

### Navigation Structure
- **Navigation.kt** - Jetpack Navigation setup with sealed class Screen definitions
- Three main screens: Login, Map (stations overview), Chart (detailed sensor data)
- Authentication-based navigation flow (login required)
- **Separated ViewModels**: MapViewModel and ChartViewModel instead of monolithic MeteoViewModel

### Network Layer (SOLID Refactored)
- **Interfaces** (ISP compliance):
  - `HttpClient` - Abstract HTTP operations
  - `SecureStorage` - Abstract secure storage operations
  - `SensorDataRepository` - Sensor data operations only
  - `StationRepository` - Station management operations only
  - `ParameterMetadataRepository` - Parameter metadata operations only
- **Implementations**:
  - `OkHttpClientWrapper` - HTTP client implementation
  - `SharedPreferencesStorage` - Secure storage implementation
  - `NetworkSensorDataRepository` - Sensor data repository
  - `NetworkStationRepository` - Station repository
  - `NetworkParameterMetadataRepository` - Parameter metadata repository
- **AuthManager.kt** - Uses SecureStorage interface instead of direct SharedPreferences
- Base URL: `http://84.237.1.131:8085/api/` ⚠️ **NEEDS UPDATE to API v1**

### UI Architecture (SOLID Refactored)
- **MVVM pattern** with focused ViewModels following Single Responsibility Principle:
  - `MapViewModel` - Map UI state, station loading, latest sensor data
  - `ChartViewModel` - Chart UI state, historical data, date range selection
  - `LoginViewModel` - Authentication operations
- **Jetpack Compose** for UI with Material3 design
- **Hilt navigation compose** for ViewModel injection
- **State management** using StateFlow and collectAsState

### SOLID Refactoring Summary
The codebase has been comprehensively refactored to follow SOLID principles:

1. **Single Responsibility Principle (SRP)**:
   - Split MeteoViewModel into MapViewModel and ChartViewModel
   - Extracted utility classes: SensorDataCache, RetryPolicy, StationDataTransformer
   - Separated MainActivity concerns with LocationPermissionManager and MapKitLifecycleManager

2. **Open/Closed Principle (OCP)**:
   - Strategy pattern for HTTP interceptors
   - Configurable retry policies and data transformation

3. **Liskov Substitution Principle (LSP)**:
   - Consistent interface contracts with proper async/sync patterns

4. **Interface Segregation Principle (ISP)**:
   - Split large MeteoRepository into focused interfaces
   - Separated HTTP, storage, and repository concerns

5. **Dependency Inversion Principle (DIP)**:
   - All dependencies now use abstractions instead of concrete classes
   - Proper dependency injection for all utility classes

### Key Features
- **Yandex Maps integration** with weather station markers
- **Interactive charts** using YCharts library for sensor data visualization
- **Authentication system** with JWT tokens
- **Real-time data** from weather stations
- **Date range selection** for historical data viewing
- **Multiple weather parameters** (temperature, humidity, pressure, etc.)

### Data Models
- **StationWithLocation.kt** - Weather station data with geographic coordinates
- **API data classes** in MeteoApiService.kt (SensorDataPoint, StationInfo, ParameterMetadata)

### Configuration Notes
- **Yandex MapKit API key** is set in MainActivity companion object
- **Location permissions** required for map functionality
- **Internet permissions** for API communication
- **Minimum SDK 26**, target SDK 35
- **Kotlin 2.0** with Compose compiler plugin

### Important Dependencies
- Hilt 2.51.1 for dependency injection
- Retrofit 2.9.0 for networking
- Jetpack Compose with BOM 2025.04.01
- Yandex MapKit KMP for maps
- YCharts 2.1.0 for data visualization
- Core Splashscreen for app launch experience

## ✅ Completed Tasks

### API v1 Migration: ✅ COMPLETED
The application has been successfully migrated to API v1 with all new features implemented:

1. **Completed Changes**:
   - ✅ Base URL updated to `/api/v1/`
   - ✅ JWT token system with access + refresh tokens implemented
   - ✅ Station numbers migrated to 8-digit format (station_number)
   - ✅ All responses handle `{"success": true, "data": {...}}` format
   - ✅ Station management UI added with full CRUD operations

2. **Updated Files**:
   - ✅ `MeteoApiService.kt` - All endpoints and data models updated
   - ✅ `AuthManager.kt` - JWT refresh token support added
   - ✅ Repository implementations - ApiResponse wrapper handling
   - ✅ UI components - StationManagementScreen added
   - ✅ Navigation updated for new features

3. **Implemented Features**:
   - ✅ Station coordinates from API response (latitude/longitude)
   - ✅ Favorite stations functionality
   - ✅ Bulk data retrieval for all user stations
   - ✅ Station parameter discovery
   - ✅ Add/Edit/Delete stations UI
   - ✅ Station validation and error handling

### SOLID Refactoring Status: ✅ COMPLETED
All SOLID principles have been successfully implemented. The codebase now follows clean architecture patterns with proper separation of concerns, dependency injection, and interface-based design.