#!/bin/sh
# copy output unsigned apk to b2g source code's OTA folder
cp '/home/b2g/src/b2g_dev/AndroidStudioProjects/b2g-updater/app/build/outputs/apk/app-debug.apk' /home/b2g/src/b2g_dev/jb/tcbin_misc/OTA
cd /home/b2g/src/b2g_dev/jb/tcbin_misc/OTA
java -jar signapk.jar -w release.x509.pem release.pk8 app-debug.apk signed.apk
adb remount
adb uninstall com.zone24x7.b2gdev.b2g_updater
adb shell rm /system/app/signed.apk
adb push signed.apk /system/app
adb shell reboot

