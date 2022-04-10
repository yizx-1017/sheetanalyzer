package org.dataspread.sheetanalyzer;

import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dataspread.sheetanalyzer.data.SheetData;
import org.dataspread.sheetanalyzer.parser.POIParser;
import org.dataspread.sheetanalyzer.parser.SpreadsheetParser;
import org.dataspread.sheetanalyzer.util.SheetNotSupportedException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class TestDemo {

    @Test
    public void testFileParser() throws SheetNotSupportedException, IOException {
        String filePath = "/Users/eve/Desktop/S/6/Project/experiment/demo_excel.xlsx";
        SheetAnalyzer sheetAnalyzer = SheetAnalyzer.createSheetAnalyzer(filePath);
        System.out.println(sheetAnalyzer.isTabularSheet());
        System.out.println(sheetAnalyzer.isTACOSheetWithSameFormulaPattern());
    }
}


