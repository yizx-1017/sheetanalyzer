package org.dataspread.sheetanalyzer;

import org.dataspread.sheetanalyzer.util.SheetNotSupportedException;
import org.dataspread.sheetanalyzer.dependency.util.RefWithMeta;
import org.dataspread.sheetanalyzer.analyzer.SheetAnalyzerImpl;
import org.dataspread.sheetanalyzer.data.CellContent;
import org.dataspread.sheetanalyzer.util.Pair;
import org.dataspread.sheetanalyzer.util.Ref;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SheetAnalyzer {

    /**
     * Creating a SheetAnalyzer from a Spreadsheet File
     *
     * @param filePath
     * @return
     * @throws SheetNotSupportedException
     */
    public static SheetAnalyzer createSheetAnalyzer(String filePath) throws SheetNotSupportedException {
        return new SheetAnalyzerImpl(filePath);
    }

    /**
     * Creating a SheetAnalyzer from a map between sheetName and associated
     * cells (String[][]). Used to handle output from Excel's JavaScript API.
     * {@link CellContent}
     *
     * @param spreadsheetContent
     * @return
     * @throws SheetNotSupportedException
     */
    public static SheetAnalyzer createSheetAnalyzer(Map<String, String[][]> spreadsheetContent)
            throws SheetNotSupportedException {
        return new SheetAnalyzerImpl(spreadsheetContent);
    }

    /**
     * @return fileName
     */
    public abstract String getFileName();

    /**
     * @return a set of sheetnames
     */
    public abstract Set<String> getSheetNames();

    /**
     * Get the number of sheets in the spreadsheet file
     *
     * @return
     */
    public abstract int getNumSheets();

    /**
     * @return a map between sheetnames and the string recording compression
     *         information
     */
    public abstract Map<String, String> getCompressInfo();

    /**
     * Get the dependents of a reference {@link Ref}
     *
     * @param sheetName
     * @param ref
     * @return
     */
    public abstract Set<Ref> getDependents(String sheetName, Ref ref);

    /**
     * Get the full information of a TACO graph
     *
     * @return
     */
    public abstract Map<String, Pair<Map<Ref, List<RefWithMeta>>,
            Map<Ref, List<RefWithMeta>>>>  getTACODepGraphs();

    /**
     * Get the formula clusters
     * 
     * @return
     */
    public abstract Map<String, Map<String, List<Ref>>> getFormulaClusters();

    /**
     * Get the distribution of references a formula has
     *
     * @return
     */
    public abstract Map<Integer, Integer> getRefDistribution();

    /**
     * Get the number of compressed edges of TACO
     *
     * @return
     */
    public abstract long getNumCompEdges();

    /**
     * Get the number of compressed vertices of TACO
     *
     * @return
     */
    public abstract long getNumCompVertices();

    /**
     * Get the number of edges without compression
     *
     * @return
     */
    public abstract long getNumEdges();

    /**
     * Get the number of vertices without compression
     *
     * @return
     */
    public abstract long getNumVertices();

    /**
     * Get the number of formulae
     *
     * @return
     */
    public abstract long getNumOfFormulae();

    /**
     * Get the {@link Ref} that has the longest dependency chain
     *
     * @return
     */
    public abstract Pair<Ref, Long> getRefWithLongestDepChain();

    /**
     * Get the length of the longest path of a reference {@link Ref}
     *
     * @param startRef
     * @return
     */
    public abstract long getLongestPathLength(Ref startRef);

    /**
     * Get the {@link Ref} that has the most dependents
     *
     * @return
     */
    public abstract Pair<Ref, Long> getRefWithMostDeps();

    /**
     * Check whether this spreadsheet only includes derived column
     *
     * @return
     */
    public abstract boolean includeDerivedColumnOnly();

    /**
     * Return the derived columns in compressed format by TACO
     *
     * @return
     */
    public abstract List<RefWithMeta> extractDerivedColumns();

    /**
     * Check whether this spreadsheet is a table
     *
     * @return
     */
    public abstract boolean isTabularSheet() throws IOException, SheetNotSupportedException;

    /**
     * Check whether this spreadsheet is a taco supported table
     *
     * @return
     */
    public abstract boolean isTACOSheet() throws SheetNotSupportedException;
}
