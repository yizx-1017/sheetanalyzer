package org.dataspread.sheetanalyzer.clusterTest;

import org.dataspread.sheetanalyzer.util.SheetNotSupportedException;
import org.dataspread.sheetanalyzer.util.TestUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dataspread.sheetanalyzer.SheetAnalyzer;
import org.dataspread.sheetanalyzer.util.Ref;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Assertions;
import org.apache.poi.ss.usermodel.Row;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.io.File;

public class TestClustering {

  private static SheetAnalyzer sheetAnalyzer;
  private static final String sheetName = "sheet1";
  private static final int maxRows = 1000;

  private static File createTestSheet() throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet(sheetName);
      int colA = 0, colB = 1, colC = 2, colD = 3;
      for (int i = 0; i < maxRows; i++) {
        Row row = sheet.createRow(i);
        row.createCell(colA).setCellValue(i + 1);
        row.createCell(colB).setCellValue(10);
        row.createCell(colC).setCellFormula("SUM(A1:" + "B" + maxRows + ")");
        row.createCell(colD).setCellFormula("SUM(A1:" + "B" + maxRows + ")");
      }
      TestUtil.createAnEmptyRowWithTwoCols(sheet, maxRows, colA, colB);
      File xlsTempFile = TestUtil.createXlsTempFile();
      workbook.write(new FileOutputStream(xlsTempFile));
      return xlsTempFile;
    }
  }

  @BeforeAll
  public static void setup() throws IOException, SheetNotSupportedException {
    sheetAnalyzer = SheetAnalyzer.createSheetAnalyzer(createTestSheet().getAbsolutePath());
  }

  @Test
  public void testSimpleClustering() {

    Map<String, Map<String, List<Ref>>> mapping = sheetAnalyzer.getFormulaClusters();

    // There should only be one sheet name in the mapping
    Assertions.assertEquals(1, mapping.keySet().size());

    // The name of the sheet should be the same as our configuration
    Assertions.assertTrue(mapping.containsKey(TestClustering.sheetName));

    // Number of hashes / clusters should be 1 since all formulae only differ by
    // cell reference
    Assertions.assertEquals(1, mapping.get(TestClustering.sheetName).keySet().size());

    // The size of the cluster should equal 2 for column C and column D
    List<Ref> refs = mapping.get(TestClustering.sheetName).values().iterator().next();
    Assertions.assertEquals(2, refs.size());

    // The cell count of the Refs in the clusters should be equal to maxRows
    Assertions.assertEquals(maxRows, refs.get(0).getCellCount());
    Assertions.assertEquals(maxRows, refs.get(1).getCellCount());
  }

}
