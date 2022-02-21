package org.dataspread.sheetanalyzer.data;

import org.dataspread.sheetanalyzer.util.Pair;
import org.dataspread.sheetanalyzer.util.Ref;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class SheetData {

    private final Map<Ref, RefMetadata> refMetadata = new HashMap<>();
    private final Set<Ref> accessAreaCache = new HashSet<>();
    private final String sheetName;

    public SheetData(String sheetName) {
        this.sheetName = sheetName;
    }

    public static Comparator<Pair<Ref, List<Ref>>> rowWiseComp = (pairA, pairB) -> {
        Ref refA = pairA.first;
        Ref refB = pairB.first;
        int rowResult = Integer.compare(refA.getRow(), refB.getRow());
        if (rowResult == 0) {
            return Integer.compare(refA.getColumn(), refB.getColumn());
        } else {
            return rowResult;
        }
    };

    public static Comparator<Pair<Ref, List<Ref>>> colWiseComp = (pairA, pairB) -> {
        Ref refA = pairA.first;
        Ref refB = pairB.first;

        int colResult = Integer.compare(refA.getColumn(), refB.getColumn());
        if (colResult == 0) {
            return Integer.compare(refA.getRow(), refB.getRow());

        } else {
            return colResult;
        }
    };

    public List<Pair<Ref, List<Ref>>> getSortedDepPairs(boolean rowWise) {
        List<Pair<Ref, List<Ref>>> depPairList = new LinkedList<>();
        this.refMetadata.entrySet().forEach((Map.Entry<Ref, RefMetadata> entry) -> {
            depPairList.add(new Pair<>(entry.getKey(), entry.getValue().getDependents()));
        });
        if (rowWise) {
            depPairList.sort(rowWiseComp);

        } else {
            depPairList.sort(colWiseComp);
        }
        return depPairList;
    }

    public void addDeps(Ref ref, List<Ref> precList) {
        RefMetadata metadata = this.refMetadata.get(ref);
        if (metadata == null) {
            this.refMetadata.put(ref, new RefMetadata(precList));
        } else {
            metadata.setDependents(precList);
        }
    }

    public void addFormulaNumRef(Ref ref, int numRefs) {
        RefMetadata metadata = this.refMetadata.get(ref);
        if (metadata == null) {
            this.refMetadata.put(ref, new RefMetadata(numRefs));
        } else {
            metadata.setNumFormulaRefs(numRefs);
        }
    }

    public void addContent(Ref ref, CellContent content) {
        RefMetadata metadata = this.refMetadata.get(ref);
        if (metadata == null) {
            this.refMetadata.put(ref, new RefMetadata(content));
        } else {
            metadata.setContent(content);
        }
    }

    public void addOneAccess(Ref areaRef) {
        accessAreaCache.add(areaRef);
    }

    public boolean areaAccessed(Ref areaRef) {
        return accessAreaCache.contains(areaRef);
    }

    public String getSheetName() {
        return sheetName;
    }

    // Notice that in the following methods we return a copy
    // of the underlying metadata so that the caller doesn't 
    // accidently re-assign any of the values.

    public Set<Ref> getDepSet() {
        return this.refMetadata.keySet();
    }

    public RefMetadata getRefMetadata(Ref dep) {
        return new RefMetadata(this.refMetadata.get(dep));
    }

    // NOTE: now the following methods are no longer needed
    // since we have getRefMetadata. The only reason these 
    // are not deleted is because we want to maintain backwards 
    // compatibility.

    public List<Ref> getPrecList(Ref dep) {
        return new ArrayList<>(this.refMetadata.get(dep).getDependents());
    }

    public int getNumRefs(Ref dep) {
        return this.refMetadata.get(dep).getNumFormulaRefs();
    }

    public CellContent getCellContent(Ref ref) {
        return new CellContent(this.refMetadata.get(ref).getContent());
    }

}
