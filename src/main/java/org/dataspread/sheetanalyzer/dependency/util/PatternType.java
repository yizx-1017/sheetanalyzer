package org.dataspread.sheetanalyzer.dependency.util;

public enum PatternType {
    TYPEZERO,  // RRChain - Long chain, special case of TypeOne
    TYPEONE,   // RR - Relative start, Relative end
    TYPETWO,   // RF - Relative start, Fixed end
    TYPETHREE, // FR - Fixed start, Relative end
    TYPEFOUR,  // FF - Fixed start, Fixed end
    TYPEFIVE,  // RRGapOne - Relative + Relative with gap 1
    TYPESIX,   // RRGapTwo - Relative + Relative with gap 2
    TYPESEVEN, // RRGapThree - Relative + Relative with gap 3
    TYPEEIGHT, // RRGapFour - Relative + Relative with gap 4
    TYPENINE,  // RRGapFive - Relative + Relative with gap 5
    TYPETEN,   // RRGapSix - Relative + Relative with gap 6
    TYPEELEVEN,// RRGapSeven - Relative + Relative with gap 7
    NOTYPE
}
