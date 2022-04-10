package org.dataspread.sheetanalyzer.analyzer;

import org.apache.poi.ss.usermodel.*;
import org.dataspread.sheetanalyzer.data.SheetData;
import org.dataspread.sheetanalyzer.dependency.util.PatternType;
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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;

import static java.lang.Integer.max;

public class SheetAnalyzerImpl extends SheetAnalyzer {

    private final Map<String, DependencyGraph> depGraphMap = new HashMap<>();
    private final SpreadsheetParser parser;
    private long numVertices = 0;
    private long numEdges = 0;
    private String filePath;
    private int firstRowNum, lastRowNum;
    boolean isTACOSheet;

    public SheetAnalyzerImpl(String filePath) throws SheetNotSupportedException {
        this.filePath = filePath;
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
    public Map<String, Pair<Map<Ref, List<RefWithMeta>>,
            Map<Ref, List<RefWithMeta>>>> getTACODepGraphs() {
        Map<String, Pair<Map<Ref, List<RefWithMeta>>, Map<Ref, List<RefWithMeta>>>> tacoDepGraphs = new HashMap<>();
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
            Map<String, List<Ref>> cluster = new HashMap<>();
            sheetData.getDepSet().forEach(dep -> {
                CellContent cellContent = sheetData.getCellContent(dep);
                if (cellContent.isFormula()) {String formulaTemplate = cellContent.getFormulaTemplate();
                    if (!cluster.containsKey(formulaTemplate)) {
                        cluster.put(formulaTemplate, new ArrayList<>());
                    }
                    cluster.get(formulaTemplate).add(dep);
                }
            });
            formulaClusters.put(sheetName, cluster);
        });
        return formulaClusters;
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
    public boolean isTabularSheet() throws SheetNotSupportedException {
        File file = new File(filePath);
        try (Workbook wb = WorkbookFactory.create(file)) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                this.firstRowNum = sheet.getFirstRowNum();
                this.lastRowNum = sheet.getLastRowNum();
                int rowIndex = -1, startCol = -1, endCol = -1;
                Map<Integer, Boolean> isFormulaColumn = new HashMap<>();
                this.isTACOSheet = true;
                for (Row row : sheet) {
                    if (rowIndex != -1 && row.getRowNum() != rowIndex + 1) {
                        return false;
                    }
                    rowIndex = row.getRowNum();
                    int columnIndex = -1, minCol = -1, maxCol = -1;
                    for (Cell cell : row) {
                        if (cell.getCellType() != CellType.BLANK) {
                            int col = cell.getColumnIndex();
                            if (columnIndex != -1 && col != columnIndex + 1) {
                                return false;
                            }
                            if (minCol == -1) {
                                if (startCol != -1 && startCol != col) return false;
                                startCol = minCol = col;
                            }
                            maxCol = max(col, maxCol);
                            columnIndex = col;
//                            System.out.println(cell.getRowIndex()+ " "+cell.getColumnIndex());
                            boolean isFormula = (cell.getCellType() == CellType.FORMULA);
                            if(isFormulaColumn.get(col) == null) {
                                isFormulaColumn.put(col, isFormula);
                            } else if (isFormulaColumn.get(col) != isFormula) {
                                this.isTACOSheet = false;
                            }
                        }
                    }
                    if (endCol != -1 && endCol != maxCol) return false;
                    endCol = maxCol;
                }
            }
            return true;
        } catch (IOException err) {
            err.printStackTrace();
            throw new SheetNotSupportedException("Could not load workbook " + this.filePath);
        }
    }

    @Override
    public boolean isTACOSheet() throws SheetNotSupportedException {
        if (!isTabularSheet()) this.isTACOSheet = false;
        return isTACOSheet;
    }

    @Override
    public boolean isTACOSheetWithSameFormulaPattern() throws SheetNotSupportedException {
        if (isTACOSheet()) {
            for (Map.Entry<String, SheetData> sheetEntry: this.parser.getSheetData().entrySet()) {
                SheetData sheetData = sheetEntry.getValue();
                Map<Integer, String> colTemplate = new HashMap<>();
                for (Ref dep: sheetData.getDepSet()) {
                    CellContent cellContent = sheetData.getCellContent(dep);
                    int col = dep.getColumn();
//                    System.out.println(dep.getRow() +" " +col);
                    if (cellContent.isFormula()) {
                        String formulaTemplate = cellContent.getFormulaTemplate();
                        if (!colTemplate.containsKey(col)) {
                            colTemplate.put(col, formulaTemplate);
                        } else if (!Objects.equals(colTemplate.get(col), formulaTemplate)) {
                            return false;
                        }
                    } else {
                        if (!colTemplate.containsKey(col)) {
                            colTemplate.put(col, "N/A");
                        } else if (!Objects.equals(colTemplate.get(col), "N/A")) {
                            return false;
                        }
                    }
                }
//                System.out.println(colTemplate);
            }
            for (Map.Entry<String, DependencyGraph> entry: this.depGraphMap.entrySet()) {
//                System.out.println("-----------------------------");
                Map<Ref, List<RefWithMeta>> depToPrecList = entry.getValue().getCompressedGraph().second;
                for (Map.Entry<Ref, List<RefWithMeta>> e : depToPrecList.entrySet()) {
//                    System.out.println(e.getKey().toString());
                    if (this.firstRowNum != e.getKey().getRow() ||
                        this.lastRowNum != e.getKey().getLastRow()) {
                        return false;
                    }
                    for (RefWithMeta r : e.getValue()) {
                        PatternType p = r.getEdgeMeta().patternType;
                        if (p != PatternType.TYPEONE &&
                            p != PatternType.TYPETWO &&
                            p != PatternType.TYPETHREE &&
                            p != PatternType.TYPEFOUR) {
                            // the column doesn't belong to the four type that we want
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

}
