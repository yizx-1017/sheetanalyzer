package org.dataspread.sheetanalyzer.data;

import org.dataspread.sheetanalyzer.util.Ref;
import java.util.ArrayList;
import java.util.List;

public class CellWithMeta {

    private List<Ref> dependents = new ArrayList<>();
    private CellContent content = CellContent.getNullCellContent();
    private int numFormulaRefs = 0;

    public CellWithMeta(List<Ref> dependents) {
        this(dependents, CellContent.getNullCellContent(), 0);
    }

    public CellWithMeta(CellContent content) {
        this(new ArrayList<>(), content, 0);
    }

    public CellWithMeta(int numFormulaRefs) {
        this(new ArrayList<>(), CellContent.getNullCellContent(), numFormulaRefs);
    }

    public CellWithMeta(CellWithMeta metadata) {
        this.dependents = metadata.getDependents();
        this.content = metadata.getContent();
        this.numFormulaRefs = metadata.getNumFormulaRefs();
    }

    public CellWithMeta(List<Ref> dependents, CellContent content, int numFormulaRefs) {
        this.dependents = dependents;
        this.content = content;
        this.numFormulaRefs = numFormulaRefs;
    }

    public List<Ref> getDependents() {
        return this.dependents;
    }

    public CellWithMeta setDependents(List<Ref> dependents) {
        this.dependents = dependents;
        return this;
    }

    public CellContent getContent() {
        return this.content;
    }

    public CellWithMeta setContent(CellContent content) {
        this.content = content;
        return this;
    }

    public int getNumFormulaRefs() {
        return this.numFormulaRefs;
    }

    public CellWithMeta setNumFormulaRefs(int numFormulaRefs) {
        this.numFormulaRefs = numFormulaRefs;
        return this;
    }
}