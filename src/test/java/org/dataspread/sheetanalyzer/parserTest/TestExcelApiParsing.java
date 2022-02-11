package org.dataspread.sheetanalyzer.parserTest;

import org.dataspread.sheetanalyzer.SheetAnalyzer;
import org.dataspread.sheetanalyzer.util.Ref;
import org.dataspread.sheetanalyzer.util.RefImpl;
import org.dataspread.sheetanalyzer.util.SheetNotSupportedException;
import org.dataspread.sheetanalyzer.util.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestExcelApiParsing {

    private static SheetAnalyzer sheetAnalyzer;
    private static final Map<String,
            String[][]> spreadsheetContent = new HashMap<>();
    private static final int maxRows = 1000;
    private static final int maxCols = 3;

    private static String[][] createTestSheet() {
        String[][] sheet = new String[maxRows][maxCols];
        for (int i = 0; i < maxRows; i++) {
            sheet[i][0] = String.valueOf(i + 1);
            sheet[i][1] = String.valueOf(10);
            sheet[i][2] = "=SUM(A" + (i + 1) + ":" + "B" + maxRows + ")";
        }
        return sheet;
    }

    @BeforeAll
    public static void setUp() throws SheetNotSupportedException {
        String testSheetName = "TempSheet";
        spreadsheetContent.put(testSheetName, createTestSheet());
        sheetAnalyzer = SheetAnalyzer.createSheetAnalyzer(spreadsheetContent);
    }

    @Test
    public void testRFPattern() {
        int queryRow = maxRows - 1, queryColumn = 0;
        Ref queryRef = new RefImpl(queryRow, queryColumn);
        Set<Ref> queryResult = sheetAnalyzer.getDependents("TempSheet",
                queryRef);

        Set<Ref> groundTruth = new HashSet<>();
        int firstRow = 0, firstColumn = 2;
        int lastRow = maxRows - 1, lastColumn = 2;
        groundTruth.add(new RefImpl(firstRow, firstColumn, lastRow,
                lastColumn));
        Assertions.assertTrue(TestUtil.hasSameRefs(groundTruth, queryResult));
    }
}
