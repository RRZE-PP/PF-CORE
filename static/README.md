# Extracting files for PRO

If you own a legal copy of the PRO client you can add some class files from the PRO-jar to this folder to include them in the OpenSource version for testing.

The files that need to be copied are listed in the `pro-files.lst` file.

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


