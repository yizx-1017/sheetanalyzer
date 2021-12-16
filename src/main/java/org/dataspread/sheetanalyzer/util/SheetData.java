package org.dataspread.sheetanalyzer.util;

import java.util.*;

public class SheetData {

    private final String sheetName;

    private int maxRows;
    private int maxCols;

    private final HashMap<Ref, HashSet<Ref>> sheetDeps = new HashMap<>();
    private final HashMap<Ref, Integer> formulaNumRefs = new HashMap<>();
    private final HashMap<Ref, CellContent> sheetContent = new HashMap<>();

    public SheetData(String sheetName) {
        this.sheetName = sheetName;
    }

    public static Comparator<Pair<Ref, HashSet<Ref>>> rowWiseComp =
            (pairA, pairB) -> {
                Ref refA = pairA.first;
                Ref refB = pairB.first;

                int rowResult = Integer.compare(refA.getRow(), refB.getRow());
                if (rowResult == 0) return Integer.compare(refA.getColumn(), refB.getColumn());
                else return rowResult;
            };

    public static Comparator<Pair<Ref, HashSet<Ref>>> colWiseComp =
            (pairA, pairB) -> {
                Ref refA = pairA.first;
                Ref refB = pairB.first;

                int colResult = Integer.compare(refA.getColumn(), refB.getColumn());
                if (colResult == 0) return Integer.compare(refA.getRow(), refB.getRow());
                else return colResult;
            };

    public void addDeps(Ref dep, HashSet<Ref> precSet) {
        sheetDeps.put(dep, precSet);
    }

    public void addFormulaNumRef(Ref dep, int numRefs) {
        formulaNumRefs.put(dep, numRefs);
    }

    public void addContent(Ref cellRef, CellContent cellContent) {
        sheetContent.put(cellRef, cellContent);
    }

    public List<Pair<Ref, HashSet<Ref>>> getSortedDepPairs(boolean rowWise) {
        LinkedList<Pair<Ref, HashSet<Ref>>> depPairList = new LinkedList<>();
        sheetDeps.forEach((Ref dep, HashSet<Ref> precSet) -> {
            depPairList.add(new Pair<>(dep, precSet));
        });
        if (rowWise) depPairList.sort(rowWiseComp);
        else depPairList.sort(colWiseComp);
        return depPairList;
    }

    public Set<Ref> getDepSet() {
        return formulaNumRefs.keySet();
    }

    public void setMaxRowsCols(int maxRows, int maxCols) {
        this.maxRows = maxRows;
        this.maxCols = maxCols;
    }

    public int getMaxRows() {return maxRows;}
    public int getMaxCols() {return maxCols;}

    public HashSet<Ref> getPrecSet(Ref dep) {
        return sheetDeps.get(dep);
    }

    public int getNumRefs(Ref dep) {
        return formulaNumRefs.get(dep);
    }

    public CellContent getCellContent(Ref ref) {
        return sheetContent.get(ref);
    }

    public String getSheetName() {
        return sheetName;
    }
}
