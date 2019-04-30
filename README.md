# PowerFolder client with OpenJDK

Tested with

```
~/.PowerFolder$ java -version
openjdk version "1.8.0_191"
OpenJDK Runtime Environment (build 1.8.0_191-8u191-b12-2ubuntu0.18.04.1-b12)
OpenJDK 64-Bit Server VM (build 25.191-b12, mixed mode)
```
## Running the client

This is how to download, build and run the open source client:
```
 apt-get install ant git
 git clone git@github.com:RRZE-PP/PF-CORE.git
 cd PF-CORE
 ant
 java -jar dist/PowerFolder.jar
 ```
 
 ## OpenSource version with PRO features
 
 For using PRO client features place your legaly obtained plugin class files inside the `static/` directory and rebuild the project.
 
 ### Extracting files for PRO

If you own a legal copy of the PRO client you can add some class files from the PRO-jar to this folder to include them in the OpenSource version for testing.

The files that need to be copied are listed in the `static/pro-files.lst` file.

If you have a PRO-jar available you can extract the needed files with these commands:
```
cd PF-CORE/static
jar xf ${PATH_TO_PRO_JAR} @./pro-files.txt
```

Example for the FAUbox client:

```
cd PF-CORE/static
jar xf /usr/share/FAUbox/FAUbox.jar@./pro-files.txt
```

## Running the PRO client

After adding the jar files you need to rebuild the project before to enable the pro features
```
 ant
 java -jar dist/PowerFolder.jar
 ```


