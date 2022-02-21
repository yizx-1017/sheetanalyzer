package org.dataspread.sheetanalyzer.data;

public class CellContent {
    final boolean isFormula;
    final String formula;
    final String value;

    public CellContent (CellContent content) {
        this.value = content.value;
        this.formula = content.formula;
        this.isFormula = content.isFormula;
    }

    public CellContent (String value, String formula, boolean isFormula) {
        this.value = value;
        this.formula = formula;
        this.isFormula = isFormula;
    }

    public String getValue() {
        return value;
    }

    public String getFormula() {
        return formula;
    }

    public boolean isFormula() {
        return isFormula;
    }

    public static CellContent getNullCellContent() {
        return new CellContent("", "", false);
    }
}
