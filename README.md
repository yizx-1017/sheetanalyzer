# SheetAnalyzer

SheetAnalyzer is a library for analyzing the depenency and formula structure of a spreadsheet

## API

[SheetAnalyzer.java](https://github.com/dataspread/sheetanalyzer/blob/main/src/main/java/org/dataspread/sheetanalyzer/SheetAnalyzer.java) provides the API for use 

## Test

```shell
mvn test
```

## Deployment

```shell
mvn versions:set -DnewVersion=1.2.3
```

```shell
mvn clean deploy -P release
```