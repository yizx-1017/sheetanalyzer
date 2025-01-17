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

import java.util.*;
import java.io.IOException;
import java.io.File;

public class POIParser implements SpreadsheetParser {

    private final Map<String, SheetData> sheetDataMap = new HashMap<>();
    private final FormulaParsingWorkbook evalbook;
    private Workbook workbook;
    private String filename;
    private Node root;

    public POIParser(String filePath) throws SheetNotSupportedException {
        File file = new File(filePath);
        this.filename = file.getName();
        try (Workbook wb = WorkbookFactory.create(file)) {
            this.workbook = wb;
            if (workbook instanceof HSSFWorkbook) {
                this.evalbook = HSSFEvaluationWorkbook.create((HSSFWorkbook) workbook);
            } else if (workbook instanceof XSSFWorkbook) {
                this.evalbook = XSSFEvaluationWorkbook.create((XSSFWorkbook) workbook);
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

    public Node getFormulaTree() {return this.root; }

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
//            this.sheetDataMap.put(workbook.getSheetAt(i).getSheetName().replace(',', '-'), sheetData);
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
                        CellContent cellContent = new CellContent(getCellContentString(cell), "",
                                " ", false);
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

    private String extractFormulaTemplate(Ptg[] ptgs) {
        StringBuilder cleanedFormula = new StringBuilder();
        for (Ptg ptg : ptgs) {
            if (ptg instanceof OperationPtg) {
                // Include mathematical operators in the cleaned formula
                OperationPtg tok = (OperationPtg) ptg;
                String[] operands = new String[tok.getNumberOfOperands()];
                Arrays.fill(operands, "");
                cleanedFormula.append(tok.toFormulaString(operands));
            } else if (ptg instanceof OperandPtg) {
                // Only exclude the references in the cleaned formula
                continue;
            } else {
                // Include ArrayPtg, UnknownPtg, and ControlPtg in the cleaned formula
                cleanedFormula.append(ptg.toFormulaString());
            }
        }
        return cleanedFormula.toString();
    }

    private Node buildFormulaTree(Ptg[] ptgs) {
        Stack<Node> stack = new Stack<>();

        for (Ptg token : ptgs) {
            if (token instanceof OperationPtg) {
                String val;
                if (token instanceof AddPtg) {
                    val = "+";
                } else if (token instanceof SubtractPtg) {
                    val = "-";
                } else if (token instanceof MultiplyPtg) {
                    val = "*";
                } else if (token instanceof DividePtg) {
                    val = "/";
                } else {
                    val = token.toFormulaString();
                }
                OperationPtg tok = (OperationPtg) token;
                int num = tok.getNumberOfOperands();
                List<Node> children = new ArrayList<>(num);
                for (int i = 0; i < num; i++) {
                    children.add(stack.pop());
                }
                Collections.reverse(children);
                Node node = new OperatorNode(val, children);
                stack.push(node);
            } else if (token instanceof OperandPtg) {
                if (token instanceof Area2DPtgBase) {
                    Area2DPtgBase ptg = (Area2DPtgBase) token;
                    int rowStart = ptg.getFirstRow();
                    int colStart = ptg.getFirstColumn();
                    int rowEnd = ptg.getLastRow();
                    int colEnd = ptg.getLastColumn();
                    boolean startRelative = ptg.isFirstRowRelative();
                    boolean endRelative = ptg.isLastRowRelative();
                    Node node = new RefNode(rowStart, colStart, rowEnd, colEnd, startRelative, endRelative);
                    stack.push(node);
                } else if (token instanceof RefPtg) {
                    RefPtg ptg = (RefPtg) token;
                    int row = ptg.getRow();
                    int col = ptg.getColumn();
                    boolean relative = ptg.isRowRelative();
                    Node node = new RefNode(row, col, row, col, relative, relative);
                    stack.push(node);
                } else if (token instanceof Area3DPtg ||
                        token instanceof Area3DPxg ||
                        token instanceof Ref3DPtg ||
                        token instanceof Ref3DPxg) {
                    // Not supported
                }
            } else if (token instanceof ScalarConstantPtg) {
                // Literal Value
                double value;
                if (token instanceof IntPtg) {
                    IntPtg ptg = (IntPtg) token;
                    value = ptg.getValue();
                    LiteralNode node = new LiteralNode(value);
                    stack.push(node);
                } else if (token instanceof NumberPtg) {
                    NumberPtg ptg = (NumberPtg) token;
                    value = ptg.getValue();
                    LiteralNode node = new LiteralNode(value);
                    stack.push(node);
                }
            }
            else {
                // ArrayPtg, UnknownPtg, and ControlPtg
                if (token instanceof AttrPtg && ((AttrPtg) token).isSum()) {
                    // special treatment for SUM
                    List<Node> children = new ArrayList<>(1);
                    children.add(stack.pop());
                    Collections.reverse(children);
                    Node node = new OperatorNode("SUM", children);
                    stack.push(node);
                }
            }
        }

        return stack.pop();
    }

    private void parseOneFormulaCell(SheetData sheetData, Cell cell) throws SheetNotSupportedException {
        Ptg[] tokens = this.getTokens(cell);
        this.root = buildFormulaTree(tokens);
//        Ref dep = new RefImpl(cell.getRowIndex(), cell.getColumnIndex());
//        List<Ref> precList = new LinkedList<>();
//        int numRefs = 0;
//        if (tokens != null) {
//            for (Ptg token : tokens) {
//                if (token instanceof OperandPtg) {
//                    Ref prec = parseOneToken(cell, (OperandPtg) token,
//                            sheetData);
//                    if (prec != null) {
//                        numRefs += 1;
//                        precList.add(prec);
//                    }
//                }
//            }
//        }
//
//        if (!precList.isEmpty()) {
//            sheetData.addDeps(dep, precList);
//        }
//        sheetData.addFormulaNumRef(dep, numRefs);
//        String formulaTemplate = extractFormulaTemplate(tokens);
//        CellContent cellContent = new CellContent("", cell.getCellFormula(),
//                formulaTemplate, true);
//        sheetData.addContent(dep, cellContent);
    }

//    private Ref parseOneToken(Cell cell, OperandPtg token,
//            SheetData sheetData) throws SheetNotSupportedException {
//        Sheet sheet = this.getDependentSheet(cell, token);
//        if (sheet != null) {
//            if (token instanceof Area2DPtgBase) {
//                Area2DPtgBase ptg = (Area2DPtgBase) token;
//                int rowStart = ptg.getFirstRow();
//                int colStart = ptg.getFirstColumn();
//                int rowEnd = ptg.getLastRow();
//                int colEnd = ptg.getLastColumn();
//                Ref areaRef = new RefImpl(rowStart, colStart, rowEnd, colEnd);
//                if (!sheetData.areaAccessed(areaRef)) {
//                    sheetData.addOneAccess(areaRef);
//                    for (int r = ptg.getFirstRow(); r <= ptg.getLastRow(); r++) {
//                        for (int c = ptg.getFirstColumn(); c <= ptg.getLastColumn(); c++) {
//                            Cell dep = this.getCellAt(sheet, r, c);
//                            if (dep == null) {
//                                Ref cellRef = new RefImpl(r, c);
//                                if (sheetData.getCellContent(cellRef) == null) {
//                                    sheetData.addContent(cellRef,
//                                            CellContent.getNullCellContent());
//                                }
//                            }
//                        }
//                    }
//                }
//                return areaRef;
//            } else if (token instanceof RefPtg) {
//                RefPtg ptg = (RefPtg) token;
//                int row = ptg.getRow();
//                int col = ptg.getColumn();
//                Cell dep = this.getCellAt(sheet, row, col);
//                if (dep == null) {
//                    sheetData.addContent(new RefImpl(row, col),
//                            CellContent.getNullCellContent());
//                }
//                return new RefImpl(row, col, row, col);
//            } else if (token instanceof Area3DPtg ||
//                    token instanceof Area3DPxg ||
//                    token instanceof Ref3DPtg ||
//                    token instanceof Ref3DPxg) {
//                throw new SheetNotSupportedException();
//            }
//        }
//
//        return null;
//    }

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
