#!/bin/sh
adb remount
adb uninstall com.zone24x7.b2gdev.b2g_updater
adb shell rm /system/app/signed.apk
