package org.dataspread.sheetanalyzer.systest;

import org.dataspread.sheetanalyzer.SheetAnalyzer;
import org.dataspread.sheetanalyzer.util.SheetNotSupportedException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TestTACOPatterns {

    static String numRefDistFile = "numRefDist.csv";
    static String numEdgesFile = "stat.csv";

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Need two arguments: \n" +
                    "1) a xls(x) file \n" +
                    "2) a folder for stat output \n");
            System.exit(-1);
        }

        HashMap<Integer, Long> numRefDist = new HashMap<>();

        String statFolder = args[1];
        String numRefDistPath = statFolder + "/" + numRefDistFile;
        String statPath = statFolder + "/" + numEdgesFile;

        File inputFile = new File(args[0]);
        try (PrintWriter distPW = new PrintWriter(new FileWriter(numRefDistPath, true));
             PrintWriter statPW = new PrintWriter(new FileWriter(statPath, true))) {

            String filePath = inputFile.getAbsolutePath();
            try {
                SheetAnalyzer sheetAnalyzer = SheetAnalyzer.createSheetAnalyzer(filePath);
                MainTestUtil.writePerSheetStat(sheetAnalyzer, statPW);

                Map<Integer, Integer> numRefDistPerSheet = sheetAnalyzer.getRefDistribution();
                numRefDistPerSheet.forEach((numRefs, count) -> {
                    long existingCount = numRefDist.getOrDefault(numRefs, 0L);
                    numRefDist.put(numRefs, existingCount + count);
                });

            } catch (SheetNotSupportedException e) {
                System.out.println(e.getMessage());
            }

            numRefDist.forEach((numRefs, count) ->
                    distPW.write(numRefs + "," + count + "\n"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
