package org.dataspread.sheetanalyzer.analyzer;

import org.apache.commons.codec.digest.DigestUtils;
import org.dataspread.sheetanalyzer.util.SheetNotSupportedException;
import org.dataspread.sheetanalyzer.util.APINotImplementedException;
import org.dataspread.sheetanalyzer.dependency.DependencyGraphTACO;
import org.dataspread.sheetanalyzer.dependency.util.RefWithMeta;
import org.dataspread.sheetanalyzer.dependency.DependencyGraph;
import org.dataspread.sheetanalyzer.parser.SpreadsheetParser;
import org.dataspread.sheetanalyzer.parser.POIParser;
import org.dataspread.sheetanalyzer.SheetAnalyzer;
import org.dataspread.sheetanalyzer.data.CellContent;
import org.dataspread.sheetanalyzer.util.Pair;
import org.dataspread.sheetanalyzer.util.Ref;
import org.dataspread.sheetanalyzer.util.RefImpl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.*;

public class SheetAnalyzerImpl extends SheetAnalyzer {

    private final Map<String, DependencyGraph> depGraphMap = new HashMap<>();
    private final SpreadsheetParser parser;
    private long numVertices = 0;
    private long numEdges = 0;

    public SheetAnalyzerImpl(String filePath) throws SheetNotSupportedException {
        this.parser = new POIParser(filePath);
        genDepGraphFromSheetData(this.depGraphMap);
    }

    public SheetAnalyzerImpl(Map<String, String[][]> sheetContent) throws SheetNotSupportedException {
        this.parser = new POIParser(sheetContent);
        genDepGraphFromSheetData(this.depGraphMap);
    }

    private void genDepGraphFromSheetData(Map<String, DependencyGraph> inputDepGraphMap) {
        boolean isRowWise = false;
        this.parser.getSheetData().forEach((sheetName, sheetData) -> {
            DependencyGraphTACO depGraph = new DependencyGraphTACO();
            HashSet<Ref> refSet = new HashSet<>();
            sheetData.getSortedDepPairs(isRowWise).forEach(depPair -> {
                Ref dep = depPair.first;
                List<Ref> precList = depPair.second;
                precList.forEach(prec -> {
                    depGraph.add(prec, dep);
                    this.numEdges += 1;
                });
                refSet.add(dep);
                refSet.addAll(precList);
            });
            inputDepGraphMap.put(sheetName, depGraph);
            this.numVertices += refSet.size();
        });
    }

    @Override
    public String getFileName() {
        return this.parser.getFileName();
    }

    @Override
    public Set<String> getSheetNames() {
        return this.depGraphMap.keySet();
    }

    @Override
    public int getNumSheets() {
        return this.parser.getSheetData().size();
    }

    @Override
    public Map<String, String> getCompressInfo() {
        Map<String, String> compressInfoMap = new HashMap<>();
        this.depGraphMap.forEach((sheetName, depGraph) -> {
            compressInfoMap.put(sheetName, depGraph.getCompressInfo());
        });
        return compressInfoMap;
    }

    @Override
    public Set<Ref> getDependents(String sheetName, Ref ref) {
        return this.depGraphMap.get(sheetName).getDependents(ref);
    }

    @Override
    public Map<String, Map<Ref, List<RefWithMeta>>> getTACODepGraphs() {
        Map<String, Map<Ref, List<RefWithMeta>>> tacoDepGraphs = new HashMap<>();
        this.depGraphMap.forEach((sheetName, depGraph) -> {
            tacoDepGraphs.put(sheetName,
                    ((DependencyGraphTACO) depGraph).getCompressedGraph());
        });
        return tacoDepGraphs;
    }

    /**
     * Returns a map where each key is a sheet name and
     * each value is another map. In the nested map, each
     * key is a formula cluster hash and each value is a
     * list of refs belonging to the cluster.
     */
    @Override
    public Map<String, Map<String, List<Ref>>> getFormulaClusters() {
        Map<String, Map<String, List<Ref>>> formulaClusters = new HashMap<>();
        this.parser.getSheetData().forEach((sheetName, sheetData) -> {
            Map<String, List<Ref>> clusters = new HashMap<>();
            sheetData.getDepSet().forEach(dep -> {
                CellContent cellContent = sheetData.getCellContent(dep);
                if (cellContent.isFormula()) {
                    String formula = DigestUtils.md5Hex(cellContent.getFormula()).toUpperCase();
                    clusters.getOrDefault(formula, new ArrayList<>()).add(dep);
                }
            });
            for (Map.Entry<String, List<Ref>> cluster : clusters.entrySet()) {
                clusters.replace(cluster.getKey(), compressRefList(cluster.getValue()));
            }
            formulaClusters.put(sheetName, clusters);
        });
        return formulaClusters;
    }

    /**
     * Helper to compress a list of Refs based on column.
     * For example, if we have a list with references C1:C3 and C4:C7, this will
     * return a list with a single entry C1:C7.
     */
    private List<Ref> compressRefList(List<Ref> refs) {
        sortRefList(refs);
        int start = 0, end = 1;
        List<Ref> compressedRefs = new ArrayList<>();
        while (end < refs.size()) {
            boolean sameColumn = refs.get(start).getColumn() == refs.get(end).getColumn();
            boolean incrementing = refs.get(end - 1).getLastRow() == refs.get(end).getRow() - 1;
            if (!sameColumn || !incrementing) {
                addCompressedRef(compressedRefs, refs, start, end);
                start = end;
            }
            end += 1;
        }
        if (start != end) {
            addCompressedRef(compressedRefs, refs, start, end);
        }
        return compressedRefs;
    }

    /**
     * Sorts a list of Refs by row, then by column.
     */
    private void sortRefList(List<Ref> refs) {
        refs.sort((Ref ref1, Ref ref2) -> ref1.getRow() - ref2.getRow());
        refs.sort((Ref ref1, Ref ref2) -> ref1.getColumn() - ref2.getColumn());
    }

    /**
     * Adds a compressed ref to compressedRefs based on the current ref.
     */
    private void addCompressedRef(List<Ref> compressedRefs, List<Ref> refs, int start, int end) {
        int row = refs.get(start).getRow();
        int diff = end - start - 1;
        compressedRefs.add(copyRefNewLastRow(refs.get(start), row + diff));
    }

    /**
     * Return a copy of a Ref object but changes with a different last row field.
     */
    private Ref copyRefNewLastRow(Ref ref, int lastRow) {
        return new RefImpl(ref.getBookName(), ref.getSheetName(), ref.getLastSheetName(),
                ref.getRow(), ref.getColumn(), lastRow, ref.getLastColumn());
    }

    @Override
    public Map<Integer, Integer> getRefDistribution() {
        Map<Integer, Integer> refDist = new HashMap<>();
        this.parser.getSheetData().forEach((sheetName, sheetData) -> {
            sheetData.getDepSet().forEach(dep -> {
                Integer numRefs = sheetData.getNumRefs(dep);
                Integer existingCount = refDist.getOrDefault(numRefs, 0);
                refDist.put(numRefs, existingCount + 1);
            });
        });
        return refDist;
    }

    @Override
    public long getNumCompEdges() {
        AtomicLong numOfCompEdges = new AtomicLong();
        this.depGraphMap.forEach((sheetName, depGraph) -> {
            numOfCompEdges.addAndGet(depGraph.getNumEdges());
        });
        return numOfCompEdges.get();
    }

    @Override
    public long getNumCompVertices() {
        AtomicLong numOfCompVertices = new AtomicLong();
        this.depGraphMap.forEach((sheetName, depGraph) -> {
            numOfCompVertices.addAndGet(depGraph.getNumVertices());
        });
        return numOfCompVertices.get();
    }

    @Override
    public long getNumEdges() {
        return this.numEdges;
    }

    @Override
    public long getNumVertices() {
        return this.numVertices;
    }

    @Override
    public long getNumOfFormulae() {
        AtomicLong numOfFormulae = new AtomicLong();
        this.parser.getSheetData().forEach((sheetName, sheetData) -> {
            numOfFormulae.addAndGet(sheetData.getDepSet().size());
        });
        return numOfFormulae.get();
    }

    @Override
    public Pair<Ref, Long> getRefWithLongestDepChain() {
        throw new APINotImplementedException();
    }

    @Override
    public long getLongestPathLength(Ref startRef) {
        throw new APINotImplementedException();
    }

    @Override
    public Pair<Ref, Long> getRefWithMostDeps() {
        throw new APINotImplementedException();
    }

    @Override
    public boolean includeDerivedColumnOnly() {
        throw new APINotImplementedException();
    }

    @Override
    public List<RefWithMeta> extractDerivedColumns() {
        throw new APINotImplementedException();
    }

    @Override
    public boolean isTabularSheet() {
        throw new APINotImplementedException();
    }

}
