package org.dataspread.sheetanalyzer.mainTest;

import org.dataspread.sheetanalyzer.SheetAnalyzer;
import org.dataspread.sheetanalyzer.util.SheetNotSupportedException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class TestSheetAnalyzer {

    static String numRefDistFile = "numRefDist.csv";
    static String numEdgesFile = "stat.csv";

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Need two arguments: \n" +
                    "1) a folder for xls(x) files or a xls(x) file \n" +
                    "2) a folder for stat output \n");
            System.exit(-1);
        }

        boolean inRowCompression = false;
        HashMap<Integer, Long> numRefDist = new HashMap<>();

        String statFolder = args[1];
        String numRefDistPath = statFolder + "/" + numRefDistFile;
        String statPath = statFolder + "/" + numEdgesFile;

        File inputFile = new File(args[0]);
        File [] fileArray;
        if (inputFile.isDirectory()) fileArray = inputFile.listFiles();
        else fileArray = new File[] {inputFile};

        if (fileArray != null) {
            int counter = 0;
            try (PrintWriter distPW = new PrintWriter(new FileWriter(numRefDistPath, true));
                 PrintWriter statPW = new PrintWriter(new FileWriter(statPath, true))) {

                for (File file: fileArray) {
                    counter += 1;
                    System.out.println("[" + counter + "/" +
                            fileArray.length + "]: "+ "processing " + file.getName());
                    String filePath = file.getAbsolutePath();
                    try {
                        SheetAnalyzer sheetAnalyzer = new SheetAnalyzer(filePath, inRowCompression);
                        MainTestUtil.writePerSheetStat(sheetAnalyzer, statPW, inRowCompression);

                        HashMap<Integer, Integer> numRefDistPerSheet = sheetAnalyzer.getRefDistribution();
                        numRefDistPerSheet.forEach((numRefs, count) -> {
                            long existingCount = numRefDist.getOrDefault(numRefs, 0L);
                            numRefDist.put(numRefs, existingCount + count);
                        });

                    } catch (SheetNotSupportedException e) {
                        System.out.println(e.getMessage());
                    }
                }

                if (!inRowCompression) {
                    numRefDist.forEach((numRefs, count) ->
                            distPW.write(numRefs + "," + count + "\n"));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
