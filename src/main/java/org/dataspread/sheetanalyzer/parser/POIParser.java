package org.dataspread.sheetanalyzer.parser;

import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.dataspread.sheetanalyzer.data.CellContent;
import org.dataspread.sheetanalyzer.data.SheetData;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaType;
import org.dataspread.sheetanalyzer.util.*;
import org.apache.poi.ss.formula.ptg.*;
import org.apache.poi.ss.usermodel.*;

import java.util.LinkedList;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

public class POIParser implements SpreadsheetParser {

    private final Map<String, SheetData> sheetDataMap = new HashMap<>();
    private final FormulaParsingWorkbook evalbook;
    private Workbook workbook;
    private String filename;

    public POIParser(String filePath) throws SheetNotSupportedException {
        File file = new File(filePath);
        this.filename = file.getName();
        try (Workbook wb = WorkbookFactory.create(file)) {
            this.workbook = wb;
            System.out.println(this.workbook.toString());
            if (workbook instanceof HSSFWorkbook) {
                this.evalbook = HSSFEvaluationWorkbook.create((HSSFWorkbook) workbook);
                System.out.println("HSSF");
                System.out.println(this.evalbook.toString());
            } else if (workbook instanceof XSSFWorkbook) {
                this.evalbook = XSSFEvaluationWorkbook.create((XSSFWorkbook) workbook);
                System.out.println("XSSF");
                System.out.println(this.evalbook.toString());
            } else {
                throw new SheetNotSupportedException();
            }
            parseSpreadsheet();
        } catch (IOException err) {
            err.printStackTrace();
            throw new SheetNotSupportedException("Could not load workbook " + this.filename);
        } catch (SheetNotSupportedException err) {
            throw new SheetNotSupportedException("Parsing " + filePath + " failed");
        }
    }

    public POIParser(Map<String, String[][]> sheetContent) throws SheetNotSupportedException {
        try {
            this.filename = "TempWorkbook";
            this.workbook = new XSSFWorkbook();
            this.evalbook = XSSFEvaluationWorkbook.create((XSSFWorkbook) workbook);
            parseSheetContentToWorkbook(sheetContent);
            parseSpreadsheet();
        } catch (Exception e) {
            throw new SheetNotSupportedException("Parsing formulae failed");
        }
    }

    public String getFileName() {
        return this.filename;
    }

    public Map<String, SheetData> getSheetData() {
        return this.sheetDataMap;
    }

    public boolean skipParsing(int threshold) {
        int totalRows = 0;
        for (Sheet sheet : workbook) {
            totalRows += sheet.getPhysicalNumberOfRows();
        }
        return totalRows <= threshold;
    }

    /**
     * Parses formula matrix (String[][]) from Excel's JavaScript API into
     * workbook.
     */
    private void parseCellsToWorkbook(String sheetName, String[][] cells) {
        Sheet sheet = workbook.createSheet(sheetName);
        for (int i = 0; i < cells.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < cells[0].length; j++) {
                String cellString = cells[i][j];
                boolean isFormula = cellString.startsWith("=");
                CellType cellType = isFormula ? CellType.FORMULA : CellType.STRING;
                Cell cell = row.createCell(j, cellType);
                if (isFormula) {
                    cell.setCellFormula(cellString.substring(1));
                } else if (cellString.length() < 1) {
                    cell.setBlank();
                } else {
                    cell.setCellValue(cellString);
                }
            }
        }
    }

    private void parseSheetContentToWorkbook(Map<String, String[][]> sheetContent) {
        for (Map.Entry<String, String[][]> sheet : sheetContent.entrySet()) {
            String sheetName = sheet.getKey();
            String[][] sheetCells = sheet.getValue();
            parseCellsToWorkbook(sheetName, sheetCells);
        }
    }

    private void parseSpreadsheet() throws SheetNotSupportedException {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            SheetData sheetData = parseOneSheet(workbook.getSheetAt(i));
            this.sheetDataMap.put(workbook.getSheetAt(i).getSheetName().replace(',', '-'), sheetData);
        }
    }

    private SheetData parseOneSheet(Sheet sheet) throws SheetNotSupportedException {
        SheetData sheetData = new SheetData(sheet.getSheetName());
        int maxRows = 0;
        int maxCols = 0;
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell != null) {
                    if (cell.getCellType() == CellType.FORMULA) {
                        parseOneFormulaCell(sheetData, cell);
                    } else {
                        Ref dep = new RefImpl(cell.getRowIndex(), cell.getColumnIndex());
                        CellContent cellContent = new CellContent(getCellContentString(cell), "", false);
                        sheetData.addContent(dep, cellContent);
                    }
                }
                if (cell.getColumnIndex() > maxCols) {
                    maxCols = cell.getColumnIndex();
                }
            }
            if (row.getRowNum() > maxRows) {
                maxRows = row.getRowNum();
            }
        }
        return sheetData;
    }

    private String getCellContentString(Cell cell) {
        switch (cell.getCellType()) {
            case ERROR:
                return String.valueOf(cell.getErrorCellValue());
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            default:
                return "";
        }
    }

    private void parseOneFormulaCell(SheetData sheetData, Cell cell) throws SheetNotSupportedException {
        Ref dep = new RefImpl(cell.getRowIndex(), cell.getColumnIndex());
        Ptg[] tokens = this.getTokens(cell);
        List<Ref> precList = new LinkedList<>();
        int numRefs = 0;
        if (tokens != null) {
            for (Ptg token : tokens) {
                if (token instanceof OperandPtg) {
                    Ref prec = parseOneToken(cell, (OperandPtg) token,
                            sheetData);
                    if (prec != null) {
                        numRefs += 1;
                        precList.add(prec);
                    }
                }
            }
        }

        if (!precList.isEmpty()) {
            sheetData.addDeps(dep, precList);
        }
        sheetData.addFormulaNumRef(dep, numRefs);
        CellContent cellContent = new CellContent("", cell.getCellFormula(), true);
        sheetData.addContent(dep, cellContent);
    }

    private Ref parseOneToken(Cell cell, OperandPtg token,
            SheetData sheetData) throws SheetNotSupportedException {
        Sheet sheet = this.getDependentSheet(cell, token);
        if (sheet != null) {
            if (token instanceof Area2DPtgBase) {
                Area2DPtgBase ptg = (Area2DPtgBase) token;
                int rowStart = ptg.getFirstRow();
                int colStart = ptg.getFirstColumn();
                int rowEnd = ptg.getLastRow();
                int colEnd = ptg.getLastColumn();
                Ref areaRef = new RefImpl(rowStart, colStart, rowEnd, colEnd);
                if (!sheetData.areaAccessed(areaRef)) {
                    sheetData.addOneAccess(areaRef);
                    for (int r = ptg.getFirstRow(); r <= ptg.getLastRow(); r++) {
                        for (int c = ptg.getFirstColumn(); c <= ptg.getLastColumn(); c++) {
                            Cell dep = this.getCellAt(sheet, r, c);
                            if (dep == null) {
                                Ref cellRef = new RefImpl(r, c);
                                if (sheetData.getCellContent(cellRef) == null) {
                                    sheetData.addContent(cellRef,
                                            CellContent.getNullCellContent());
                                }
                            }
                        }
                    }
                }
                return areaRef;
            } else if (token instanceof RefPtg) {
                RefPtg ptg = (RefPtg) token;
                int row = ptg.getRow();
                int col = ptg.getColumn();
                Cell dep = this.getCellAt(sheet, row, col);
                if (dep == null) {
                    sheetData.addContent(new RefImpl(row, col),
                            CellContent.getNullCellContent());
                }
                return new RefImpl(row, col, row, col);
            } else if (token instanceof Area3DPtg ||
                    token instanceof Area3DPxg ||
                    token instanceof Ref3DPtg ||
                    token instanceof Ref3DPxg) {
                throw new SheetNotSupportedException();
            }
        }

        return null;
    }

    private Sheet getDependentSheet(Cell src, OperandPtg opPtg) throws SheetNotSupportedException {
        Sheet sheet = null;
        if (opPtg instanceof RefPtg) {
            sheet = src.getSheet();
        } else if (opPtg instanceof Area2DPtgBase) {
            sheet = src.getSheet();
        } else {
            throw new SheetNotSupportedException();
        }

        // else if (opPtg instanceof Ref3DPtg) {
        // sheet = this.workbook.getSheet(this.getSheetNameFrom3DRef(
        // (Ref3DPtg) opPtg));
        // } else if (opPtg instanceof Area3DPtg) {
        // sheet = this.workbook.getSheet(this.getSheetNameFrom3DRef(
        // (Area3DPtg) opPtg));
        // }
        return sheet;
    }

    private Cell getCellAt(Sheet sheet, int rowIdx, int colIdx) {
        try {
            return sheet.getRow(rowIdx).getCell(colIdx);
        } catch (NullPointerException e) {
            return null;
        }
    }

    private Ptg[] getTokens(Cell cell) {
        try {
            return FormulaParser.parse(
                    cell.getCellFormula(),
                    this.evalbook,
                    FormulaType.CELL,
                    this.workbook.getSheetIndex(cell.getSheet()),
                    cell.getRowIndex());
        } catch (Exception e) {
            return null;
        }
    }
}
