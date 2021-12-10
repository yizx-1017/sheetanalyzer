package org.dataspread.sheetanalyzer;

import org.dataspread.sheetanalyzer.dependency.DependencyGraph;
import org.dataspread.sheetanalyzer.dependency.DependencyGraphTACO;
import org.dataspread.sheetanalyzer.dependency.util.RefWithMeta;
import org.dataspread.sheetanalyzer.parser.POIParser;
import org.dataspread.sheetanalyzer.parser.SpreadsheetParser;
import org.dataspread.sheetanalyzer.util.CellContent;
import org.dataspread.sheetanalyzer.util.Ref;
import org.dataspread.sheetanalyzer.util.SheetData;
import org.dataspread.sheetanalyzer.util.SheetNotSupportedException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SheetAnalyzer {
    private final SpreadsheetParser parser;
    private final String fileName;
    private final HashMap<String, SheetData> sheetDataMap;
    private final HashMap<String, DependencyGraph> depGraphMap;

    public SheetAnalyzer(String filePath) throws SheetNotSupportedException {
        parser = new POIParser(filePath);
        fileName = parser.getFileName();
        sheetDataMap = parser.getSheetData();

        depGraphMap = new HashMap<>();
        genDepGraphFromSheetData(depGraphMap);
    }

    public SheetAnalyzer(String sheetName,
                         HashMap<Ref, HashSet<Ref>> sheetDeps,
                         HashMap<Ref, CellContent> sheetContent) {
        parser = null;
        fileName = "NoFile";

        sheetDataMap = new HashMap<>();
        SheetData sheetData = new SheetData(sheetName);
        sheetDeps.forEach(sheetData::addDeps);
        sheetContent.forEach(sheetData::addContent);
        sheetDataMap.put(sheetName, sheetData);

        depGraphMap = new HashMap<>();
        genDepGraphFromSheetData(depGraphMap);

    }

    private void genDepGraphFromSheetData(HashMap<String, DependencyGraph> inputDepGraphMap) {
        boolean isRowWise = false;
        sheetDataMap.forEach((sheetName, sheetData) -> {
            DependencyGraph depGraph = new DependencyGraphTACO();
            sheetData.getSortedDeps(isRowWise).forEach(depPair -> {
                Ref dep = depPair.first;
                HashSet<Ref> precSet = depPair.second;
                precSet.forEach(prec -> {
                    depGraph.add(prec, dep);
                });
            });
            inputDepGraphMap.put(sheetName, depGraph);
        });
    }

    public HashMap<String, DependencyGraph> getDependencyGraphs() {
        return depGraphMap;
    }

    public Set<Ref> getDependents(String sheetName, Ref ref) {
        return depGraphMap.get(sheetName).getDependents(ref);
    }

    public HashMap<String, HashMap<Ref, List<RefWithMeta>>> getTACODepGraphs() {
        HashMap<String, HashMap<Ref, List<RefWithMeta>>> tacoDepGraphs = new HashMap<>();
        depGraphMap.forEach((sheetName, depGraph) -> {
            tacoDepGraphs.put(sheetName, ((DependencyGraphTACO)depGraph).getCompressedGraph());
        });
        return tacoDepGraphs;
    }

    /* Return the cell that has the longest org.dataspread.sheetanalyzer.dependency chain
    * */
    public Ref getRefWithLongestDepChain() {
        return null;
    }

    /* Return the cell that has the largest number of dependencies
     * */
    public Ref getRefWithMostDeps() {
        return null;
    }

    public boolean includeDerivedColumnOnly () {
        return false;
    }

    public HashSet<RefWithMeta> extractDerivedColumns() {
        return new HashSet<>();
    }

    public boolean isTabularSheet() {
        return false;
    }

    public int getNumSheets() {
        return sheetDataMap.size();
    }

}
