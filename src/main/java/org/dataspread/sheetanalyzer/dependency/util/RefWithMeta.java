package org.dataspread.sheetanalyzer.dependency.util;

import org.dataspread.sheetanalyzer.util.Ref;

import java.util.Objects;

public class RefWithMeta {

    private final EdgeMeta edgeMeta;
    private final Ref ref;

    public RefWithMeta(Ref ref, EdgeMeta edgeMeta) {
        this.ref = ref;
        this.edgeMeta = edgeMeta;
    }

    public Ref getRef() {
        return this.ref;
    }

    public EdgeMeta getEdgeMeta() {
        return this.edgeMeta;
    }

    public PatternType getPatternType() {
        return this.edgeMeta.patternType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RefWithMeta)) {
            return false;
        }
        RefWithMeta that = (RefWithMeta) o;
        return Objects.equals(this.ref, that.ref)
                && Objects.equals(this.edgeMeta, that.edgeMeta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.ref, this.edgeMeta);
    }
}
