package org.dataspread.sheetanalyzer.util;

import java.util.Set;

public interface Ref {
	RefType getType();

	String getBookName();

	void setSheetName(String sheetName);

	String getSheetName();

	String getLastSheetName();

	int getRow();

	int getColumn();

	int getLastRow();

	int getLastColumn();

	// ZSS-815
	// since 3.7.0
	int getSheetIndex();

	// ZSS-815
	// since 3.7.0
	int getLastSheetIndex();

	Set<Ref> getPrecedents();

	Ref getBoundingBox(Ref target);

	int getCellCount();

	Ref getOverlap(Ref target);

	Set<Ref> getNonOverlap(Ref target);

	void addPrecedent(Ref precedent);

	void clearDependent();

	/**
	 * @since 3.5.0
	 */
	enum RefType {
		CELL, AREA, SHEET, BOOK, NAME, OBJECT, INDIRECT, TABLE,
	}
}
