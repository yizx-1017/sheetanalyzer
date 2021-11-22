import dependency.DependencyGraph;
import dependency.DependencyGraphTACO;
import dependency.util.RefWithMeta;
import parser.POIParser;
import parser.SpreadsheetParser;
import util.Ref;
import util.SheetData;
import util.SheetNotSupportedException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
            depGraphMap.put(sheetName, depGraph);
        });
    }

    public HashMap<String, DependencyGraph> getDependencyGraphs() {
        return depGraphMap;
    }

    public HashMap<String, HashMap<Ref, List<RefWithMeta>>> getTACODepGraphs() {
        HashMap<String, HashMap<Ref, List<RefWithMeta>>> tacoDepGraphs = new HashMap<>();
        depGraphMap.forEach((sheetName, depGraph) -> {
            tacoDepGraphs.put(sheetName, ((DependencyGraphTACO)depGraph).getCompressedGraph());
        });
        return tacoDepGraphs;
    }

    public Ref getRefWithLongestDepPath() {
        return null;
    }

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
