package org.dataspread.sheetanalyzer.mainTest;

import org.dataspread.sheetanalyzer.SheetAnalyzer;
import org.dataspread.sheetanalyzer.dependency.util.PatternType;
import org.dataspread.sheetanalyzer.util.Pair;
import org.dataspread.sheetanalyzer.util.Ref;
import org.dataspread.sheetanalyzer.util.RefImpl;

import java.io.PrintWriter;

public class MainTestUtil {

    public static void writePerSheetStat(SheetAnalyzer sheetAnalyzer,
                                         PrintWriter statPW,
                                         boolean inRowCompression) {
        String fileName = sheetAnalyzer.getFileName().replace(",", "-");
        long numFormulae = sheetAnalyzer.getNumOfFormulae();
        long numEdges = sheetAnalyzer.getNumEdges();
        long numVertices = sheetAnalyzer.getNumVertices();
        long numCompEdges = sheetAnalyzer.getNumCompEdges();
        long numCompVertices = sheetAnalyzer.getNumCompVertices();

        Pair<Ref, Long> mostDeps = new Pair(new RefImpl(-1, -1), 0);
        Pair<Ref, Long> longestDeps = new Pair(new RefImpl(-1, -1), 0);

        long[] numCompEdgesPerPattern = new long[PatternType.values().length];
        long[] numEdgesPerPattern = new long[PatternType.values().length];

        if (!inRowCompression) {
            mostDeps = sheetAnalyzer.getRefWithMostDeps();
            longestDeps = sheetAnalyzer.getRefWithLongestDepChain();
            sheetAnalyzer.getTACODepGraphs().forEach((sheetName, tacoGraph) -> {
                tacoGraph.forEach((prec, depWithMetaList) -> {
                    depWithMetaList.forEach(depWithMeta -> {
                        Ref dep = depWithMeta.getRef();
                        PatternType patternType = depWithMeta.getPatternType();

                        int patternIndex = patternType.ordinal();
                        numCompEdgesPerPattern[patternIndex] += 1;

                        long numPatternEdges = dep.getCellCount();
                        if (patternType.ordinal() >= PatternType.TYPEFIVE.ordinal() &&
                                patternType != PatternType.NOTYPE) {
                            long gap = patternType.ordinal() - PatternType.TYPEFIVE.ordinal() + 1;
                            numPatternEdges = (numPatternEdges - 1) / (gap + 1) + 1;
                        }
                        numEdgesPerPattern[patternIndex] += numPatternEdges;
                    });
                });
            });
        }

        if (numEdges >= 10) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(fileName).append(",")
                    .append(numFormulae).append(",")
                    .append(numVertices).append(",")
                    .append(numEdges).append(",")
                    .append(numCompVertices).append(",")
                    .append(numCompEdges);
            if (!inRowCompression) {
                stringBuilder.append(",")
                        .append(mostDeps.first).append(",")
                        .append(mostDeps.second).append(",")
                        .append(longestDeps.first).append(",")
                        .append(longestDeps.second).append(",");
                for (int pIdx = 0; pIdx < numCompEdgesPerPattern.length; pIdx++) {
                    stringBuilder.append(numCompEdgesPerPattern[pIdx]).append(",")
                            .append(numEdgesPerPattern[pIdx]).append(",");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1)
                        .append("\n");
            } else {
                stringBuilder.append("\n");
            }
            statPW.write(stringBuilder.toString());
        }
    }

    public static Ref cellStringToRef(String cellString) {
        int colIndex = cellString.charAt(0) - 'A';
        int rowIndex = Integer.parseInt(cellString.substring(1)) - 1;

        return new RefImpl(rowIndex, colIndex);
    }
}
