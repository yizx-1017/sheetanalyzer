package org.dataspread.sheetanalyzer.data;

import org.dataspread.sheetanalyzer.util.Pair;
import org.dataspread.sheetanalyzer.util.Ref;

import java.util.*;

public class SheetData {

    private final String sheetName;

    private final HashMap<Ref, List<Ref>> sheetDeps = new HashMap<>();
    private final HashMap<Ref, Integer> formulaNumRefs = new HashMap<>();
    private final HashMap<Ref, CellContent> sheetContent = new HashMap<>();
    private final HashSet<Ref> accessAreaCache = new HashSet<>();

    public SheetData(String sheetName) {
        this.sheetName = sheetName;
    }

    public static Comparator<Pair<Ref, List<Ref>>> rowWiseComp =
            (pairA, pairB) -> {
                Ref refA = pairA.first;
                Ref refB = pairB.first;

                int rowResult = Integer.compare(refA.getRow(), refB.getRow());
                if (rowResult == 0) return Integer.compare(refA.getColumn(), refB.getColumn());
                else return rowResult;
            };

    public static Comparator<Pair<Ref, List<Ref>>> colWiseComp =
            (pairA, pairB) -> {
                Ref refA = pairA.first;
                Ref refB = pairB.first;

                int colResult = Integer.compare(refA.getColumn(), refB.getColumn());
                if (colResult == 0) return Integer.compare(refA.getRow(), refB.getRow());
                else return colResult;
            };

    public void addDeps(Ref dep, List<Ref> precList) {
        sheetDeps.put(dep, precList);
    }

    public void addFormulaNumRef(Ref dep, int numRefs) {
        formulaNumRefs.put(dep, numRefs);
    }

    public void addContent(Ref cellRef, CellContent cellContent) {
        sheetContent.put(cellRef, cellContent);
    }

    public void addOneAccess(Ref areaRef) {
        accessAreaCache.add(areaRef);
    }

    public boolean areaAccessed(Ref areaRef) {
        return accessAreaCache.contains(areaRef);
    }

    public List<Pair<Ref, List<Ref>>> getSortedDepPairs(boolean rowWise) {
        LinkedList<Pair<Ref, List<Ref>>> depPairList = new LinkedList<>();
        sheetDeps.forEach((Ref dep, List<Ref> precList) -> {
            depPairList.add(new Pair<>(dep, precList));
        });
        if (rowWise) depPairList.sort(rowWiseComp);
        else depPairList.sort(colWiseComp);
        return depPairList;
    }

    public Set<Ref> getDepSet() {
        return formulaNumRefs.keySet();
    }

    public List<Ref> getPrecList(Ref dep) {
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
