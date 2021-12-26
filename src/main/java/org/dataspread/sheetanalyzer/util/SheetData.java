package org.dataspread.sheetanalyzer.util;

import java.util.*;

public class SheetData {

    private final String sheetName;

    private int maxRows;
    private int maxCols;

    private final HashMap<Ref, HashSet<Ref>> sheetDeps = new HashMap<>();
    private final HashMap<Ref, Integer> formulaNumRefs = new HashMap<>();
    private final HashMap<Ref, CellContent> sheetContent = new HashMap<>();
    private final HashSet<Ref> accessAreaCache = new HashSet<>();

    public static final Ref rootRef = new RefImpl(-1, -1);

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

    public void addOneAccess(Ref areaRef) {
        accessAreaCache.add(areaRef);
    }

    public boolean areaAccessed(Ref areaRef) {
        return accessAreaCache.contains(areaRef);
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

    public Set<Ref> getValueOnlyPrecSet() {
        Set<Ref> valueOnlyPrecSet = new HashSet<>();
        Set<Ref> areaSet = new HashSet<>();

        sheetDeps.forEach((Ref dep, Set<Ref> precSet) -> {
            precSet.forEach(prec -> {
                if (!areaSet.contains(prec)) {
                    areaSet.add(prec);
                    valueOnlyPrecSet.addAll(toCellSet(prec));
                }
            });
        });

        return valueOnlyPrecSet;
    }

    private Set<Ref> toCellSet(Ref ref) {
        Set<Ref> cellSet = new HashSet<>();
        for (int row = ref.getRow(); row <= ref.getLastRow(); row++) {
            for (int col = ref.getColumn(); col <= ref.getLastColumn(); col++) {
                Ref cellRef = new RefImpl(row, col);
                CellContent cc = sheetContent.get(cellRef);
                if (!cc.isFormula  || (formulaNumRefs.get(cellRef) == 0))
                    cellSet.add(cellRef);
            }
        }
        return cellSet;
    }

    public Pair<HashMap<Ref, Set<Ref>>, HashMap<Ref, Set<Ref>>> genCellWiseDepGraph(long maxRange) {
        Set<Ref> valueCells = new HashSet<>();

        HashMap<Ref, Set<Ref>> precToDeps = new HashMap<>();
        HashMap<Ref, Set<Ref>> depToPrecs = new HashMap<>();

        sheetDeps.forEach((dep, precSet) -> {
            precSet.forEach(precRange -> {
                int numCells = 0;
                for (int row = precRange.getRow(); row <= precRange.getLastRow(); row++) {
                    for (int col = precRange.getColumn(); col <= precRange.getLastColumn(); col++) {
                        Ref prec = new RefImpl(row, col);
                        CellContent cc = sheetContent.get(prec);
                        if (!cc.isFormula || (formulaNumRefs.get(prec) == 0))
                            valueCells.add(prec);

                        // add to precToDeps
                        Set<Ref> deps = precToDeps.getOrDefault(prec, new HashSet<>());
                        deps.add(dep);
                        precToDeps.putIfAbsent(prec, deps);

                        // add to depToPrecs
                        Set<Ref> precs = depToPrecs.getOrDefault(dep, new HashSet<>());
                        precs.add(prec);
                        depToPrecs.putIfAbsent(dep, precs);

                        numCells += 1;
                    }
                    if (numCells >= maxRange) break;
                }
            });
        });

        precToDeps.put(rootRef, valueCells);
        valueCells.forEach(valueCell -> {
            Set<Ref> precSet = new HashSet<>();
            precSet.add(rootRef);
            depToPrecs.put(valueCell, precSet);
        });

        return new Pair<>(precToDeps, depToPrecs);
    }

    public Pair<HashMap<Ref, Set<Ref>>, HashMap<Ref, Set<Ref>>> replicateGraph(Pair<HashMap<Ref, Set<Ref>>, HashMap<Ref, Set<Ref>>> cellWiseGraph) {
        HashMap<Ref, Set<Ref>> newPrecToDeps = new HashMap<>();
        HashMap<Ref, Set<Ref>> newDepToPrecs = new HashMap<>();

        HashMap<Ref, Set<Ref>> oldPrecToDeps = cellWiseGraph.first;
        HashMap<Ref, Set<Ref>> oldDepToPrecs = cellWiseGraph.second;

        oldPrecToDeps.forEach((prec, depSet) -> {
            HashSet<Ref> newDepSet = new HashSet<>();
            newDepSet.addAll(depSet);
            newPrecToDeps.put(prec, newDepSet);
        });

        oldDepToPrecs.forEach((dep, precSet) -> {
            HashSet<Ref> newPrecSet = new HashSet<>();
            newPrecSet.addAll(precSet);
            newDepToPrecs.put(dep, newPrecSet);
        });

        return new Pair<>(newPrecToDeps, newDepToPrecs);
    }

    public List<Ref> getSortedRefsByTopology(Pair<HashMap<Ref, Set<Ref>>, HashMap<Ref, Set<Ref>>> cellWiseGraph) {
        HashMap<Ref, Set<Ref>> precToDeps = cellWiseGraph.first;
        HashMap<Ref, Set<Ref>> depToPrecs = cellWiseGraph.second;

        List<Ref> sortedCells = new LinkedList<>();
        List<Ref> rootCells = new LinkedList<>();
        rootCells.add(rootRef);

        while (!rootCells.isEmpty()) {
            Ref rootCell = rootCells.remove(0);
            Set<Ref> depSet = precToDeps.remove(rootCell);
            if (depSet != null) {
                depSet.forEach(dep -> {
                    Set<Ref> precSet = depToPrecs.get(dep);
                    precSet.remove(rootCell);
                    if (precSet.isEmpty()) {
                        depToPrecs.remove(dep);
                        rootCells.add(dep);
                    }
                });
            }
            sortedCells.add(rootCell);
        }

        return sortedCells;
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
