package org.dataspread.sheetanalyzer.data;

public class CellContent {
    final boolean isFormula;
    final String formula;
    final String value;
    final String formulaTemplate;

    public CellContent(CellContent content) {
        this.value = content.value;
        this.formula = content.formula;
        this.isFormula = content.isFormula;
        this.formulaTemplate = content.formulaTemplate;
    }

    public CellContent(String value, String formula,
                       String formulaTemplate, boolean isFormula) {
        this.value = value;
        this.formula = formula;
        this.formulaTemplate = formulaTemplate;
        this.isFormula = isFormula;
    }

    public String getValue() {
        return value;
    }

    public String getFormula() {
        return formula;
    }

    public String getFormulaTemplate() {
        return formulaTemplate;
    }

    public boolean isFormula() {
        return isFormula;
    }

    public static CellContent getNullCellContent() {
        return new CellContent("", "", "", false);
    }
}
