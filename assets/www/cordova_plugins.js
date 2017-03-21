cordova.define('cordova/plugin_list', function(require, exports, module) {
module.exports = [
    {
        "id": "cordova-plugin-chrome-apps-power.power",
        "file": "plugins/cordova-plugin-chrome-apps-power/power.js",
        "pluginId": "cordova-plugin-chrome-apps-power",
        "clobbers": [
            "chrome.power"
        ]
    },
    {
        "id": "cordova-plugin-device.device",
        "file": "plugins/cordova-plugin-device/www/device.js",
        "pluginId": "cordova-plugin-device",
        "clobbers": [
            "device"
        ]
    },
    {
        "id": "cordova-plugin-dialogs.notification",
        "file": "plugins/cordova-plugin-dialogs/www/notification.js",
        "pluginId": "cordova-plugin-dialogs",
        "merges": [
            "navigator.notification"
        ]
    },
    {
        "id": "cordova-plugin-dialogs.notification_android",
        "file": "plugins/cordova-plugin-dialogs/www/android/notification.js",
        "pluginId": "cordova-plugin-dialogs",
        "merges": [
            "navigator.notification"
        ]
    },
    {
        "id": "cordova-plugin-android-permissions.Permissions",
        "file": "plugins/cordova-plugin-android-permissions/www/permissions.js",
        "pluginId": "cordova-plugin-android-permissions",
        "clobbers": [
            "cordova.plugins.permissions"
        ]
    }
];
module.exports.metadata = 
// TOP OF METADATA
{
    "cordova-plugin-chrome-apps-power": "1.0.4",
    "cordova-plugin-crosswalk-webview": "2.2.0",
    "cordova-plugin-device": "1.1.4",
    "cordova-plugin-dialogs": "1.3.1",
    "cordova-plugin-iosrtc": "3.2.2",
    "cordova-plugin-whitelist": "1.3.1",
    "cordova-plugin-android-permissions": "0.10.0"
};
// BOTTOM OF METADATA
});