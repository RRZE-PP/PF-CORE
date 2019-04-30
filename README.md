# PowerFolder client with OpenJDK

Tested with

```
~/.PowerFolder$ java -version
openjdk version "1.8.0_191"
OpenJDK Runtime Environment (build 1.8.0_191-8u191-b12-2ubuntu0.18.04.1-b12)
OpenJDK 64-Bit Server VM (build 25.191-b12, mixed mode)
```
## OpenSource version

How to download, build and run the open source client:
```
 apt-get install ant
 git clone git@github.com:RRZE-PP/PF-CORE.git
 cd PF-CORE
 ant
 java -jar dist/PowerFolder.jar
 ```
 ## OpenSource version with PRO features
 
 For using PRO client features place your legaly obtained plugin class files inside the `static/` directory.
