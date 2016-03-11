### Summary

* This application checks for system updates, downloads and then initiates its install
* This is an Android Studio (1.5) application
* The built APK needs to be signed with the target device's platform key and then copied over to it's /system/app folder for deployment

### Details

* This app gets the latest released version number from the file in the following url https://github.com/b2gdev//Android-JB-4.1.2/releases/download/
* If the latest update's version number is greater than the current devices build ID and if its compatible with the device, it will check for the release update package on GitHub. The compatibility is checked using a file at the location hhttps://raw.githubusercontent.com/b2gdev/Android-JB-4.1.2/dev/device/ti/beagleboard/updateInfo.xml. Compatible version should be specified in this file manually.
* Release updates are expected to be in the following format: `https://github.com/b2gdev/Android-JB-4.1.2/releases/download/[version number]/update-[version number].zip`

### New features in v2.0

* Any update version that is greater than the current build ID will be downloaded after explicitly being checked for compatibility
* support to automatically check for updates
