package org.dataspread.sheetanalyzer.dependency.util;

public enum PatternType {
    TYPEZERO,  // Long chain, special case of TypeOne
    TYPEONE,   // Relative start, Relative end
    TYPETWO,   // Relative start, Absolute end
    TYPETHREE, // Absolute start, Relative end
    TYPEFOUR,  // Absolute start, Absolute end
    TYPEFIVE,  // Relative + Relative with gap 1
    TYPESIX,  // Relative + Relative with gap 2
    TYPESEVEN,  // Relative + Relative with gap 3
    TYPEEIGHT,  // Relative + Relative with gap 4
    TYPENINE,  // Relative + Relative with gap 5
    TYPETEN,  // Relative + Relative with gap 6
    TYPEELEVEN,  // Relative + Relative with gap 7
    NOTYPE
}
