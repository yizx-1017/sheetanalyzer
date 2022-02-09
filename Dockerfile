#
# Build stage
#
FROM maven:3.8.4-jdk-11 AS build
COPY src /home/sheetanalyzer/src
COPY pom.xml /home/sheetanalyzer
RUN mvn -f /home/sheetanalyzer/pom.xml clean package

COPY sheets /home/test/sheets
COPY runtest.sh /home/test
RUN  mkdir /home/test/results
#ENTRYPOINT ["java", "-classpath", "/home/sheetanalyzer/target/sheetanalyzer-1.0.6-SNAPSHOT.jar", "org.dataspread.sheetanalyzer.mainTest.TestSheetAnalyzer"]
#CMD ["/home/test/sheets/test.xls", "/home/test/results"]
