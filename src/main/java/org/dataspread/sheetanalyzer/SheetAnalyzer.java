package org.dataspread.sheetanalyzer;

import org.dataspread.sheetanalyzer.dependency.DependencyGraph;
import org.dataspread.sheetanalyzer.dependency.DependencyGraphTACO;
import org.dataspread.sheetanalyzer.dependency.util.RefWithMeta;
import org.dataspread.sheetanalyzer.parser.POIParser;
import org.dataspread.sheetanalyzer.parser.SpreadsheetParser;
import org.dataspread.sheetanalyzer.util.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SheetAnalyzer {
    private final SpreadsheetParser parser;
    private final String fileName;
    private final HashMap<String, SheetData> sheetDataMap;
    private final HashMap<String, DependencyGraph> depGraphMap;
    private final boolean inRowCompression;
    private long numEdges = 0;

    public SheetAnalyzer(String filePath,
                         boolean inRowCompression) throws SheetNotSupportedException {
        parser = new POIParser(filePath);
        fileName = parser.getFileName();
        sheetDataMap = parser.getSheetData();

        this.inRowCompression = inRowCompression;
        depGraphMap = new HashMap<>();
        genDepGraphFromSheetData(depGraphMap);
    }

    private void genDepGraphFromSheetData(HashMap<String, DependencyGraph> inputDepGraphMap) {
        boolean isRowWise = false;
        sheetDataMap.forEach((sheetName, sheetData) -> {
            DependencyGraphTACO depGraph = new DependencyGraphTACO();
            depGraph.setInRowCompression(inRowCompression);
            sheetData.getSortedDepPairs(isRowWise).forEach(depPair -> {
                if (inRowCompression) {
                    boolean inRowOnly = isInRowOnly(depPair);
                    depGraph.setDoCompression(inRowOnly);
                }
                Ref dep = depPair.first;
                HashSet<Ref> precSet = depPair.second;
                precSet.forEach(prec -> {
                    depGraph.add(prec, dep);
                    numEdges += 1;
                });
            });
            depGraph.setDoCompression(true);
            inputDepGraphMap.put(sheetName, depGraph);
        });
    }

    private boolean isInRowOnly(Pair<Ref, HashSet<Ref>> depPair) {
        Ref dep = depPair.first;
        HashSet<Ref> precSet = depPair.second;
        int rowIndex = dep.getRow();
        AtomicBoolean isInRowOnly = new AtomicBoolean(true);
        precSet.forEach(prec -> {
            if (prec.getRow() != rowIndex || prec.getLastRow() != rowIndex)
                isInRowOnly.set(false);
        });
        return isInRowOnly.get();
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

    public HashMap<Integer, Integer> getRefDistribution() {
        HashMap<Integer, Integer> refDist = new HashMap<>();
        sheetDataMap.forEach((sheetName, sheetData) -> {
            sheetData.getDepSet().forEach(dep -> {
                Integer numRefs = sheetData.getNumRefs(dep);
                Integer existingCount = refDist.getOrDefault(numRefs, 0);
                refDist.put(numRefs, existingCount + 1);
            });
        });
        return refDist;
    }

    public long getNumCompEdges() {
        AtomicLong numOfCompEdges = new AtomicLong();
        depGraphMap.forEach((sheetName, depGraph) -> {
            numOfCompEdges.addAndGet(depGraph.getNumEdges());
        });
        return numOfCompEdges.get();
    }

    public long getNumEdges() {
        return numEdges;
    }

    public long getNumOfFormulae() {
        AtomicLong numOfFormulae = new AtomicLong();
        sheetDataMap.forEach((sheetName, sheetData) -> {
            numOfFormulae.addAndGet(sheetData.getDepSet().size());
        });
        return numOfFormulae.get();
    }

    /* Return the cell that has the longest org.dataspread.sheetanalyzer.dependency chain
    * */
    public Ref getRefWithLongestDepChain() {
        return null;
    }

    /* Return the cell that has the largest number of dependencies
     * */
    public Pair<Ref, Long> getRefWithMostDeps() {
        AtomicReference<Ref> retRef = new AtomicReference<>(null);
        AtomicLong maxNumDeps = new AtomicLong(0L);
        sheetDataMap.forEach((String sheetName, SheetData sheetData) -> {
            DependencyGraph depGraph = depGraphMap.get(sheetName);
            Set<Ref> valueOnlyPrecSet = sheetData.getValueOnlyPrecSet();
            valueOnlyPrecSet.forEach(cellRef -> {
                AtomicLong numDeps = new AtomicLong(0L);
                depGraph.getDependents(cellRef).forEach(depRef -> {
                    numDeps.addAndGet(depRef.getCellCount());
                });
                if (numDeps.get() > maxNumDeps.get()) {
                    cellRef.setSheetName(sheetName);
                    retRef.set(cellRef);
                    maxNumDeps.set(numDeps.get());
                }
            });
        });
        return new Pair<>(retRef.get(), maxNumDeps.get());
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
