package org.dataspread.sheetanalyzer.mainTest;

import org.dataspread.sheetanalyzer.SheetAnalyzer;
import org.dataspread.sheetanalyzer.util.SheetNotSupportedException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TestSheetAnalyzer {

    static String numRefDistFile = "numRefDist.csv";
    static String numFormulaeFile = "numFormulae.csv";
    static String inRowStatFile = "inRow.csv";

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Wrong argument number; we need a folder for xls files " +
                    "and a folder for stat output");
            System.exit(-1);
        }

        boolean inRowCompression = true;
        HashMap<Integer, Long> numRefDist = new HashMap<>();
        List<Long> numFormulae = new LinkedList<>();
        List<String> fileName = new LinkedList<>();
        List<Long> numCompEdges = new LinkedList<>();
        List<Long> numEdges = new LinkedList<>();

        String xlsFolder = args[0];
        File dir = new File(xlsFolder);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            int counter = 0;
            for (File file: directoryListing) {
                String xlsPath = file.getAbsolutePath();
                try {
                    SheetAnalyzer sheetAnalyzer = new SheetAnalyzer(xlsPath, inRowCompression);
                    numFormulae.add(sheetAnalyzer.getNumOfFormulae());
                    numCompEdges.add(sheetAnalyzer.getNumCompEdges());
                    numEdges.add(sheetAnalyzer.getNumEdges());
                    fileName.add(file.getName());

                    HashMap<Integer, Integer> numRefDistPerSheet = sheetAnalyzer.getRefDistribution();
                    numRefDistPerSheet.forEach((numRefs, count) -> {
                        long existingCount = numRefDist.getOrDefault(numRefs, 0L);
                        numRefDist.put(numRefs, existingCount + count);
                    });

                } catch (SheetNotSupportedException ignored) {

                }
                counter += 1;
                System.out.println("[" + counter + "/" +
                        directoryListing.length + "]: "+ file.getName() + " finished");
            }
        }

        String statFolder = args[1];
        String numRefDistPath = statFolder + "/" + numRefDistFile;
        String numFormulaePath = statFolder + "/" + numFormulaeFile;
        String numEdgesPath = statFolder + "/" + inRowStatFile;

        try (PrintWriter distPW = new PrintWriter(new FileWriter(numRefDistPath));
             PrintWriter formPW = new PrintWriter(new FileWriter(numFormulaePath));
             PrintWriter edgePW = new PrintWriter(new FileWriter(numEdgesPath))) {

            numRefDist.forEach((numRefs, count) ->
                    distPW.write(numRefs + "," + count + "\n"));

            for (int i = 0; i < numFormulae.size(); i++) {
                formPW.write(fileName.get(i) + "," + numFormulae.get(i) + "\n");
                if (numEdges.get(i) >= 10) edgePW.write(fileName.get(i) + "," + numCompEdges.get(i) + "," + numEdges.get(i) + "\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
