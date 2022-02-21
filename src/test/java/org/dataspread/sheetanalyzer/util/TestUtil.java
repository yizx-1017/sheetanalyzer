package org.dataspread.sheetanalyzer.util;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.io.File;

public class TestUtil {

    private static final String prefix = "DSTest";
    private static final String xlsSurfix = "xls";
    private static final String xlsxSurfix = "xlsx";

    public static File createXlsTempFile() throws IOException {
        File file = File.createTempFile(prefix, xlsSurfix);
        file.deleteOnExit();
        return file;
    }

    public static File createXlsxTempFile () throws IOException {
        File file = File.createTempFile(prefix, xlsxSurfix);
        file.deleteOnExit();
        return file;
    }

    public static boolean hasSameDependencies(Map<Ref, Set<Ref>> mapA,
                                              Map<Ref, Set<Ref>> mapB) {
        return containsMapAll(mapA, mapB) && containsMapAll(mapB, mapA);
    }

    private static boolean containsMapAll(Map<Ref, Set<Ref>> mapA,
                                          Map<Ref, Set<Ref>> mapB) {
        for (Ref refB: mapB.keySet()) {
            if (!mapA.containsKey(refB)) return false;
            Set<Ref> refSetA = mapA.get(refB);
            Set<Ref> refSetB = mapB.get(refB);
            if (!hasSameRefs(refSetA, refSetB)) return false;
        }

        return true;
    }

    public static boolean hasSameRefs(Set<Ref> setA, Set<Ref> setB) {
        return setA.containsAll(setB) && setB.containsAll(setA);
    }

    public static void createAnEmptyRowWithTwoCols(Sheet sheet, int rowNum,
                                                   int colA, int colB) {
        Row row = sheet.createRow(rowNum);
        row.createCell(colA);
        row.createCell(colB);
    }
}
