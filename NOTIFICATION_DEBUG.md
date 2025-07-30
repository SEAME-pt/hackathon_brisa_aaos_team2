# Notification Debugging Guide

If you're not seeing notifications from the Vehicle Location Tracker app, follow these steps to troubleshoot:

## 1. Check Notification Permissions

### For Android 13+ (API 33+):
- Go to **Settings > Apps > Auto0s > Notifications**
- Ensure notifications are **enabled**
- Check if the app has **notification permission**

### For all Android versions:
- Go to **Settings > Apps > Auto0s > Permissions**
- Ensure **POST_NOTIFICATIONS** permission is granted

## 2. Check Service Notifications

1. **Launch the app**
2. **Grant all required permissions**
3. **Check if you see the service notification**

If no service notification appears, the issue is with the foreground service.

## 3. Check Logcat for Errors

Run this command to see detailed logs:
```bash
adb logcat | grep -E "(LocationForegroundService|MainActivity|Notification)"
```

Look for these specific log messages:
- `"Notification channel created successfully"`
- `"Notification created successfully"`
- `"Foreground service started with notification"`
- Any error messages starting with `"Error"`

## 4. Common Issues and Solutions

### Issue: No notifications at all
**Solution:**
- Check notification permissions in system settings
- Ensure the app has POST_NOTIFICATIONS permission
- Verify the service is starting properly

### Issue: Service notification doesn't appear
**Solution:**
- Check if the service is actually starting
- Look for "LocationForegroundService started" in logs
- Verify location permissions are granted

### Issue: Notification appears briefly then disappears
**Solution:**
- Check if the service is being killed by the system
- Look for "LocationForegroundService destroyed" in logs
- Ensure the service has proper foreground service permissions

### Issue: Notification shows but no location updates
**Solution:**
- Check if location permissions are granted
- Verify GPS is enabled on the device
- Look for location-related log messages

## 5. Manual Testing Steps

1. **Clear app data and cache**
2. **Uninstall and reinstall the app**
3. **Grant all permissions when prompted**
4. **Check notification settings**
5. **Check if the service starts properly**
6. **Start the location service**
7. **Check logcat for any errors**

## 6. Automotive OS Specific Notes

- Some automotive systems may have different notification behaviors
- Check if the automotive system has notification restrictions
- Verify the app is properly installed as an automotive app
- Some automotive systems may require specific notification channels

## 7. Debug Commands

### Check if service is running:
```bash
adb shell dumpsys activity services | grep LocationForegroundService
```

### Check notification channels:
```bash
adb shell dumpsys notification | grep -A 10 "Vehicle Location"
```

### Check app permissions:
```bash
adb shell dumpsys package com.example.auto0s | grep -A 5 -B 5 "permission"
```

## 8. Expected Behavior

When working correctly, you should see:
1. **Permission request dialogs** when first launching the app
2. **Persistent notification** with "Vehicle Location Tracking" title
3. **Location coordinates** in the notification text (after location is obtained)
4. **Log messages** indicating successful service startup

If any of these steps fail, check the corresponding section above for troubleshooting steps.
