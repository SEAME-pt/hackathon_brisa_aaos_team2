# Vehicle Location Tracker

An Android Automotive OS application that tracks vehicle location in real-time using a foreground service with background location permissions.

## Features

- **Real-time Location Tracking**: Continuously monitors vehicle location using GPS
- **Foreground Service**: Runs as a persistent service with user-visible notification
- **Background Location Access**: Works even when the app is in the background
- **High Accuracy GPS**: Uses standard Android Location API for precise location data
- **Automotive OS Optimized**: Designed specifically for Android Automotive OS

## Permissions Required

The app requires the following permissions:

1. **ACCESS_FINE_LOCATION**: For precise GPS location access
2. **ACCESS_COARSE_LOCATION**: For approximate location access
3. **ACCESS_BACKGROUND_LOCATION**: For location tracking when app is in background (Android 10+)
4. **FOREGROUND_SERVICE**: For running the location service in foreground
5. **FOREGROUND_SERVICE_LOCATION**: For location-specific foreground service (Android 14+)

## How It Works

### Permission Flow
1. App requests `ACCESS_FINE_LOCATION` permission
2. If granted, app requests `ACCESS_BACKGROUND_LOCATION` permission (Android 10+)
3. Once all permissions are granted, the foreground service starts automatically

### Location Service
- **LocationForegroundService**: Main service that handles location tracking
- Updates location every 5 seconds with high accuracy using GPS and Network providers
- Sends location data to a customizable endpoint (currently logs to console)
- Shows persistent notification with current coordinates
- Handles location provider status changes and availability

### Location Data
The service captures the following location information:
- Latitude and Longitude
- Accuracy
- Timestamp
- Speed (if available)
- Bearing (direction)
- Altitude (if available)

## Implementation Details

### Key Components

1. **MainActivity**: Handles permission requests and service lifecycle
2. **LocationForegroundService**: Core location tracking service
3. **AndroidManifest.xml**: Declares permissions and service
4. **build.gradle.kts**: Includes Google Play Services Location dependency

### Service Configuration
- **Update Interval**: 5 seconds
- **Minimum Distance**: 10 meters
- **Providers**: GPS (high accuracy) and Network (fallback)
- **Last Known Location**: Retrieved on service start

## Setup Instructions

1. **Build the Project**:
   ```bash
   ./gradlew build
   ```

2. **Install on Automotive Device**:
   ```bash
   adb install automotive/build/outputs/apk/debug/automotive-debug.apk
   ```

3. **Grant Permissions**:
   - Launch the app
   - Grant location permissions when prompted
   - Grant background location permission when prompted

## Customization

### Server Integration
To send location data to your server, modify the `sendLocationToServer()` method in `LocationForegroundService.kt`:

```kotlin
private fun sendLocationToServer(location: Location) {
    val locationData = mapOf(
        "latitude" to location.latitude,
        "longitude" to location.longitude,
        "accuracy" to location.accuracy,
        "timestamp" to location.time,
        "speed" to location.speed,
        "bearing" to location.bearing,
        "altitude" to location.altitude
    )

    // Add your HTTP client implementation here
    // Example: Retrofit, OkHttp, etc.
}
```

### Update Intervals
Modify the constants in `LocationForegroundService.kt`:
```kotlin
private const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
private const val LOCATION_MIN_DISTANCE = 10f // 10 meters
```

## Troubleshooting

### Common Issues

1. **Permission Denied**: Ensure all location permissions are granted in app settings
2. **Service Not Starting**: Check if foreground service permissions are granted
3. **No Location Updates**: Verify GPS is enabled and location services are active
4. **Background Location Not Working**: Ensure `ACCESS_BACKGROUND_LOCATION` is granted

### Debug Information
Check logcat for detailed service logs:
```bash
adb logcat | grep LocationForegroundService
```

## Dependencies

- **AndroidX Core**: For core Android functionality
- **AndroidX AppCompat**: For backward compatibility
- **Standard Android Location API**: Built into Android framework

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Support

For issues and questions, please create an issue in the repository.
