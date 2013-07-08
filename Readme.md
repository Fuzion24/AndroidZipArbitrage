Android Master Keys
===================

This is a POC example for Android bug 8219321 (master keys):
-details: https://plus.google.com/110558071969009568835/posts/FkNvkEcTiTB
-details: https://plus.google.com/113331808607528811927/posts/GxDA6111vYy
-bug report: https://jira.cyanogenmod.org/browse/CYAN-1602
-patch: http://review.cyanogenmod.org/#/c/45251/

This app will take an original apk and append on any files that are different in a given zip file (or modified APK). It does *not* duplicate zip entries that are the exact same file as to cut down on file bloat.

To build a jar: 
```
sbt assembly
```

Run it as:
```
java -jar androidmasterkeys_2.10-0.1.jar -a Orig.apk -z modifiedAPK.apk
```
