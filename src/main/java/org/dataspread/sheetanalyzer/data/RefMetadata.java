package org.dataspread.sheetanalyzer.data;

import org.dataspread.sheetanalyzer.util.Ref;
import java.util.ArrayList;
import java.util.List;

public class RefMetadata {

    private List<Ref> dependents = new ArrayList<>();
    private CellContent content = CellContent.getNullCellContent();
    private int numFormulaRefs = 0;

    public RefMetadata(List<Ref> dependents) {
        this(dependents, CellContent.getNullCellContent(), 0);
    }

    public RefMetadata(CellContent content) {
        this(new ArrayList<>(), content, 0);
    }

    public RefMetadata(int numFormulaRefs) {
        this(new ArrayList<>(), CellContent.getNullCellContent(), numFormulaRefs);
    }

    public RefMetadata(RefMetadata metadata) {
        this.dependents = metadata.getDependents();
        this.content = metadata.getContent();
        this.numFormulaRefs = metadata.getNumFormulaRefs();
    }

    public RefMetadata(List<Ref> dependents, CellContent content, int numFormulaRefs) {
        this.dependents = dependents;
        this.content = content;
        this.numFormulaRefs = numFormulaRefs;
    }

    public List<Ref> getDependents() {
        return this.dependents;
    }

    public RefMetadata setDependents(List<Ref> dependents) {
        this.dependents = dependents;
        return this;
    }

    public CellContent getContent() {
        return this.content;
    }

    public RefMetadata setContent(CellContent content) {
        this.content = content;
        return this;
    }

    public int getNumFormulaRefs() {
        return this.numFormulaRefs;
    }

    public RefMetadata setNumFormulaRefs(int numFormulaRefs) {
        this.numFormulaRefs = numFormulaRefs;
        return this;
    }
}