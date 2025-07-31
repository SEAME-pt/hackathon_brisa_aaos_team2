# Via Verde Android App

A modern Android application for vehicle toll payment tracking using location services. Built with clean architecture principles and modern Android development practices.

## 🚗 Overview

Via Verde is an Android automotive app that tracks vehicle location for toll payment purposes. The app provides continuous location tracking, user authentication, and a clean interface for managing toll payments.

## 🏗️ Architecture

The app follows **Clean Architecture** principles with a modular structure:

```
automotive/src/main/java/com/example/viaverde/
├── core/           # Core utilities, security, and DI
├── data/           # Data layer (repositories, models, datasources)
├── domain/         # Business logic (usecases, repositories interfaces)
├── presentation/   # UI layer (activities, fragments, viewmodels)
└── service/        # Background services
```

### Architecture Layers

#### **Presentation Layer** (`presentation/`)
- **Activities**: `MainActivity`, `LoginActivity`, `SplashActivity`
- **Fragments**: `HomeFragment`, `SettingsFragment`, `AccountFragment`
- **UI Components**: Custom bottom navigation, layouts, and UI logic

#### **Domain Layer** (`domain/`)
- **Use Cases**: `LoginUseCase`, `LogoutUseCase`
- **Repository Interfaces**: `AuthRepository`, `LocationRepository`
- **Business Logic**: Core application rules and data models

#### **Data Layer** (`data/`)
- **Repositories**: `AuthRepositoryImpl`, `LocationRepositoryImpl`
- **Data Sources**: Local storage, network APIs
- **Models**: `User`, `AuthToken`, `Location`

#### **Core Layer** (`core/`)
- **Dependency Injection**: Hilt modules and components
- **Security**: `SecurityUtils`, `SecureStorageManager`
- **Network**: `SecureNetworkManager`
- **Utilities**: Base classes and helper functions

## 🛠️ Technology Stack

### **Core Technologies**
- **Language**: Kotlin
- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 36 (Android 14)
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt (Dagger)

### **Key Libraries**
- **UI**: Material Design Components, ConstraintLayout
- **Networking**: OkHttp with logging interceptor
- **Security**: Android Security Crypto
- **Coroutines**: Kotlin Coroutines for async operations
- **Lifecycle**: Android Architecture Components

## 📱 Features

### **Authentication**
- User login with email/password
- Secure token storage using Android Security Crypto
- Automatic session management

### **Location Tracking**
- **Foreground Service**: Continuous location tracking
- **Background Location**: Location updates when app is in background (Android 10+)
- **Permission Management**: Comprehensive permission handling for all Android versions
- **Notification**: Persistent notification showing tracking status

### **User Interface**
- **Custom Bottom Navigation**: Via Verde logo, Account, and Settings tabs
- **Dynamic Logo**: Color logo on Home, B&W on other pages
- **Responsive Design**: Centered layouts with modern UI
- **Dark Mode Disabled**: Consistent light theme

### **Settings & Preferences**
- **Auto-start Toggle**: Automatically start location tracking on device boot
- **Permission Management**: View and manage app permissions
- **User Account**: Display logged-in user information

## 🔐 Security Features

### **Data Protection**
- **Encrypted Storage**: All sensitive data encrypted using Android Security Crypto
- **Token Management**: Secure storage and handling of authentication tokens
- **Network Security**: Secure network communication with proper certificate handling

### **Permission Security**
- **Runtime Permissions**: Proper handling of all required permissions
- **Background Location**: Secure background location access (Android 10+)
- **Foreground Service**: Proper foreground service implementation (Android 14+)

## 📋 Permission Requirements

The app requires the following permissions:

### **Location Permissions**
- `ACCESS_FINE_LOCATION` - Precise location tracking
- `ACCESS_COARSE_LOCATION` - Approximate location tracking
- `ACCESS_BACKGROUND_LOCATION` - Background location (Android 10+)

### **System Permissions**
- `FOREGROUND_SERVICE_LOCATION` - Foreground service for location (Android 14+)
- `POST_NOTIFICATIONS` - Notification display (Android 13+)

### **Permission Flow**
1. **Background Location** (Android 10+) - Automatically grants basic location
2. **Basic Location** - Fallback if background location is denied
3. **Foreground Service** - Required for continuous tracking
4. **Notifications** - Required for persistent notification

## 🚀 Getting Started

### **Prerequisites**
- Android Studio Arctic Fox or later
- Android SDK 28+
- Kotlin 1.8+
- Java 11+

### **Installation**
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run on an Android device or emulator

### **Build Configuration**
```kotlin
android {
    compileSdk = 36
    minSdk = 28
    targetSdk = 36

    defaultConfig {
        applicationId = "com.example.viaverde"
        versionCode = 1
        versionName = "1.0"
    }
}
```

## 📁 Project Structure

### **Key Files and Directories**

#### **Application Entry Point**
- `ViaVerdeApplication.kt` - Application class with Hilt setup

#### **Main Activity**
- `MainActivity.kt` - Main activity with custom navigation and permission handling
- `activity_main.xml` - Main layout with custom bottom navigation

#### **Authentication**
- `LoginActivity.kt` - User authentication
- `activity_register.xml` - Login screen layout

#### **Fragments**
- `HomeFragment.kt` - Home screen with service status
- `SettingsFragment.kt` - Settings with auto-start toggle
- `AccountFragment.kt` - User account information

#### **Background Service**
- `LocationForegroundService.kt` - Continuous location tracking service

#### **Core Components**
- `SecurityUtils.kt` - Security utilities and email masking
- `SecureStorageManager.kt` - Encrypted data storage
- `SecureNetworkManager.kt` - Secure network communication

## 🔧 Development Guidelines

### **Code Style**
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comprehensive logging for debugging
- Include proper error handling

### **Architecture Principles**
- **Separation of Concerns**: Each layer has specific responsibilities
- **Dependency Inversion**: High-level modules don't depend on low-level modules
- **Single Responsibility**: Each class has one reason to change
- **Open/Closed**: Open for extension, closed for modification

### **Testing**
- Unit tests for use cases and repositories
- Integration tests for data layer
- UI tests for critical user flows

## 🐛 Troubleshooting

### **Common Issues**

#### **Permission Denied**
- Ensure all required permissions are granted
- Check Android version compatibility
- Verify permission request flow

#### **Location Service Not Starting**
- Check if foreground service permission is granted (Android 14+)
- Verify notification permission (Android 13+)
- Ensure location permissions are granted

#### **Build Errors**
- Clean and rebuild project
- Sync Gradle files
- Check Android Studio version compatibility

## 📄 License

This project is proprietary software for Via Verde toll payment system.

## 🤝 Contributing

1. Follow the established architecture patterns
2. Add comprehensive logging for new features
3. Update this README for significant changes
4. Test thoroughly on different Android versions

## 📞 Support

For technical support or questions about the codebase, please refer to the development team documentation or contact the project maintainers.
