package org.dataspread.sheetanalyzer.parser;

import org.dataspread.sheetanalyzer.data.SheetData;

import java.util.Map;

public interface SpreadsheetParser {
    public String getFileName();
    public Map<String, SheetData> getSheetData();
    public boolean skipParsing(int threshold);
}
