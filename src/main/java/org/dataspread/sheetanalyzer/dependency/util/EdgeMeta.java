package org.dataspread.sheetanalyzer.dependency.util;

import java.util.Objects;

public class EdgeMeta {

    public final PatternType patternType;
    public final Offset startOffset;
    public final Offset endOffset;

    public EdgeMeta(PatternType patternType, Offset startOffset, Offset endOffset) {
        this.patternType = patternType;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EdgeMeta)) {
            return false;
        }
        EdgeMeta edgeMeta = (EdgeMeta) o;
        return this.patternType == edgeMeta.patternType
                && Objects.equals(this.startOffset, edgeMeta.startOffset)
                && Objects.equals(this.endOffset, edgeMeta.endOffset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.patternType, this.startOffset, this.endOffset);
    }
}
