### Summary

* This application checks for system updates, downloads and then initiates its install
* This is an Android Studio (1.5) application
* The built APK needs to be signed with the target device's platform key and then copied over to it's /system/app folder for deployment

### Details

* This app gets the latest released version number from the file in the following url https://www.dropbox.com/s/5bzkyu7ik3tkww5/latest-update?dl=1
* If the latest update's version number is greater than the current devices build ID, it will check for the release update package on GitHub.
* Release updates are expected to be in the following format: `https://github.com/b2gdev/Android-JB-4.1.2/releases/download/[version number]/update-[version number].zip`

### Limitations

* Any update version that is greater than the current build ID will be downloaded without explicitly being checked for compatibility
* No support to automatically check for updates
