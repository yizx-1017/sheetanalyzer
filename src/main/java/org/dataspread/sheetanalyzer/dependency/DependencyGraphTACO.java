package org.dataspread.sheetanalyzer.dependency;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.dataspread.sheetanalyzer.dependency.util.*;
import org.dataspread.sheetanalyzer.util.Pair;
import org.dataspread.sheetanalyzer.util.Ref;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.dataspread.sheetanalyzer.dependency.util.PatternTools.*;

public class DependencyGraphTACO implements DependencyGraph {

    /** Map<dependant, precedent> */
    protected HashMap<Ref, List<RefWithMeta>> precToDepList = new HashMap<>();
    protected HashMap<Ref, List<RefWithMeta>> depToPrecList = new HashMap<>();
    private RTree<Ref, Rectangle> _rectToRef = RTree.create();

    private boolean doCompression = true;
    private boolean inRowCompression = false;

    private final CompressInfoComparator compressInfoComparator = new CompressInfoComparator();

    public HashMap<Ref, List<RefWithMeta>> getCompressedGraph() {
        return precToDepList;
    }

    public Set<Ref> getDependents(Ref precedent) {
        final boolean isDirectDep = false;
        LinkedHashSet<Ref> result = new LinkedHashSet<>();

        if (RefUtils.isValidRef(precedent)) getDependentsInternal(precedent, result, isDirectDep);
        return result;
    }

    private void getDependentsInternal(Ref precUpdate,
                                       LinkedHashSet<Ref> result,
                                       boolean isDirectDep) {
        Queue<Ref> updateQueue = new LinkedList<>();
        updateQueue.add(precUpdate);
        while (!updateQueue.isEmpty()) {
            Ref updateRef = updateQueue.remove();
            Iterator<Ref> refIter = findOverlappingRefs(updateRef);
            while (refIter.hasNext()) {
                Ref precRef = refIter.next();

                Ref realUpdateRef = updateRef.getOverlap(precRef);
                findDeps(precRef).forEach(depRefWithMeta -> {
                    Ref depUpdateRef = findUpdateDepRef(precRef, depRefWithMeta.getRef(),
                            depRefWithMeta.getEdgeMeta(), realUpdateRef);
                    if (!isContained(result, depUpdateRef)) {
                        result.add(depUpdateRef);
                        if (!isDirectDep) updateQueue.add(depUpdateRef);
                    }
                });
            }
        }
    }

    private boolean isContained(LinkedHashSet<Ref> result, Ref input) {
        return result.stream().anyMatch(ref -> isSubsume(ref, input));
    }

    public long getNumEdges() {
        AtomicLong numEdges = new AtomicLong(0);
        depToPrecList.forEach((dep, precSet) -> {
            numEdges.addAndGet(precSet.size());
        });
        return numEdges.get();
    }

    public void add(Ref precedent, Ref dependent) {
        LinkedList<CompressInfo> compressInfoList = new LinkedList<>();
        if (doCompression)
            compressInfoList = findCompressInfo(precedent, dependent);
        if (compressInfoList.isEmpty()) {
            insertMemEntry(precedent, dependent,
                    new EdgeMeta(PatternType.NOTYPE, Offset.noOffset, Offset.noOffset));
        } else {
            CompressInfo selectedInfo =
                    Collections.min(compressInfoList, compressInfoComparator);
            updateOneCompressEntry(selectedInfo);
        }
    }

    public void clearDependents(Ref delDep) {
        assert (delDep.getRow() == delDep.getLastRow() &&
                delDep.getColumn() == delDep.getLastColumn());

        findOverlappingRefs(delDep).forEachRemaining(depRange -> {
            findPrecs(depRange).forEach(precRangeWithMeta -> {
                Ref precRange = precRangeWithMeta.getRef();
                EdgeMeta edgeMeta = precRangeWithMeta.getEdgeMeta();
                List<Pair<Ref, RefWithMeta>> newEdges =
                        deleteOneCell(precRange, depRange, edgeMeta, delDep);
                deleteMemEntry(precRange, depRange, edgeMeta);
                newEdges.forEach(pair -> {
                    Ref newPrec = pair.first;
                    Ref newDep = pair.second.getRef();
                    EdgeMeta newEdgeMeta = pair.second.getEdgeMeta();
                    if (newDep.getType() == Ref.RefType.CELL) add(newPrec, newDep);
                    else insertMemEntry(newPrec, newDep, newEdgeMeta);
                });
            });
        });
    }

    public void addBatch(List<Pair<Ref, Ref>> edgeBatch) {
        edgeBatch.forEach(oneEdge -> {
            Ref prec = oneEdge.first;
            Ref dep = oneEdge.second;
            add(prec, dep);
        });
    }

    public void setInRowCompression(boolean inRowCompression) {
        this.inRowCompression = inRowCompression;
    }

    public void setDoCompression(boolean doCompression) {
        this.doCompression = doCompression;
    }

    private void updateOneCompressEntry(CompressInfo selectedInfo) {
        if (selectedInfo.isDuplicate) return;
        deleteMemEntry(selectedInfo.candPrec, selectedInfo.candDep, selectedInfo.edgeMeta);

        Ref newPrec = selectedInfo.prec.getBoundingBox(selectedInfo.candPrec);
        Ref newDep = selectedInfo.dep.getBoundingBox(selectedInfo.candDep);
        Pair<Offset, Offset> offsetPair = computeOffset(newPrec, newDep, selectedInfo.compType);
        insertMemEntry(newPrec, newDep, new EdgeMeta(selectedInfo.compType, offsetPair.first, offsetPair.second));
    }

    private void insertMemEntry(Ref prec,
                                Ref dep,
                                EdgeMeta edgeMeta) {
        List<RefWithMeta> depList = precToDepList.getOrDefault(prec, new LinkedList<>());
        depList.add(new RefWithMeta(dep, edgeMeta));
        precToDepList.put(prec, depList);

        List<RefWithMeta> precList = depToPrecList.getOrDefault(dep, new LinkedList<>());
        precList.add(new RefWithMeta(prec, edgeMeta));
        depToPrecList.put(dep, precList);

        _rectToRef = _rectToRef.add(prec, RefUtils.refToRect(prec));
        _rectToRef = _rectToRef.add(dep, RefUtils.refToRect(dep));
    }

    private void deleteMemEntry(Ref prec,
                                Ref dep,
                                EdgeMeta edgeMeta) {
        List<RefWithMeta> depList = precToDepList.get(prec);
        if (depList != null) {
            depList.remove(new RefWithMeta(dep, edgeMeta));
            if (depList.isEmpty()) precToDepList.remove(prec);
        }

        List<RefWithMeta> precList = depToPrecList.get(dep);
        if (precList != null) {
            precList.remove(new RefWithMeta(prec, edgeMeta));
            if (precList.isEmpty()) depToPrecList.remove(dep);
        }

        _rectToRef = _rectToRef.delete(prec, RefUtils.refToRect(prec));
        _rectToRef = _rectToRef.delete(dep, RefUtils.refToRect(dep));
    }

    private Iterator<Ref> findOverlappingRefs(Ref updateRef) {
        Iterator<Ref> refIter = null;

        if (updateRef == null) {
            return new Iterator<Ref>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Ref next() {
                    return null;
                }
            };
        }

        Iterator<Entry<Ref, Rectangle>> entryIter =
                _rectToRef.search(getRectangleFromRef(updateRef))
                        .toBlocking().getIterator();
        refIter = Iterators.transform(entryIter, new Function<Entry<Ref, Rectangle>, Ref>() {
            @Override
            public @Nullable Ref apply(@Nullable Entry<Ref, Rectangle> refRectangleEntry) {
                return refRectangleEntry.value();
            }
        });

        return refIter;
    }

    private Iterable<RefWithMeta> findPrecs(Ref dep) {
        List<RefWithMeta> precIter;
        precIter = depToPrecList.getOrDefault(dep, new LinkedList<>());
        return precIter;
    }

    private Iterable<RefWithMeta> findDeps(Ref prec) {
        List<RefWithMeta> depIter = null;
        depIter = precToDepList.getOrDefault(prec, new LinkedList<>());
        return depIter;
    }

    private List<Pair<Ref, RefWithMeta>> deleteOneCell(Ref prec, Ref dep,
                                                       EdgeMeta edgeMeta,
                                                       Ref delDep) {
        List<Pair<Ref, RefWithMeta>> ret = new LinkedList<>();
        boolean isDirectPrec = true;
        splitRangeByOneCell(dep, delDep).forEach(splitDep -> {
            Ref splitPrec = findUpdatePrecRef(prec, dep, edgeMeta, splitDep, isDirectPrec);
            ret.add(new Pair<>(splitPrec, new RefWithMeta(splitDep, edgeMeta)));
        });
        return ret;
    }

    private List<Ref> splitRangeByOneCell(Ref dep, Ref delDep) {
        int firstRow = dep.getRow();
        int firstCol = dep.getColumn();
        int lastRow = dep.getLastRow();
        int lastCol = dep.getLastColumn();

        int delRow = delDep.getRow();
        int delCol = delDep.getColumn();

        assert(firstRow == lastRow || firstCol == lastCol);
        List<Ref> refList = new LinkedList<>();

        // This range is actually a cell
        if (firstRow == lastRow && firstCol == lastCol) return refList;

        if (firstRow == lastRow) { // Row range
            if (delCol != firstCol)
                refList.add(RefUtils.coordToRef(dep, firstRow, firstCol, lastRow, delCol - 1));
            if (delCol != lastCol)
                refList.add(RefUtils.coordToRef(dep, firstRow, delCol + 1, lastRow, lastCol));
        } else { // Column range
            if (delRow != firstRow)
                refList.add(RefUtils.coordToRef(dep, firstRow, firstCol, delRow - 1, lastCol));
            if (delRow != lastRow)
                refList.add(RefUtils.coordToRef(dep, delRow + 1, firstCol, lastRow, lastCol));
        }

        return refList;
    }

    private Pair<Offset, Offset> computeOffset(Ref prec,
                                               Ref dep,
                                               PatternType compType) {
        Offset startOffset;
        Offset endOffset;

        assert(compType != PatternType.NOTYPE);
        switch (compType) {
            case TYPEZERO:
            case TYPEONE:
            case TYPEFIVE:
            case TYPESIX:
            case TYPESEVEN:
            case TYPEEIGHT:
            case TYPENINE:
            case TYPETEN:
            case TYPEELEVEN:
                startOffset = RefUtils.refToOffset(prec, dep, true);
                endOffset = RefUtils.refToOffset(prec, dep, false);
                break;
            case TYPETWO:
                startOffset = RefUtils.refToOffset(prec, dep, true);
                endOffset = Offset.noOffset;
                break;
            case TYPETHREE:
                startOffset = Offset.noOffset;
                endOffset = RefUtils.refToOffset(prec, dep, false);
                break;
            default: // TYPEFOUR
                startOffset = Offset.noOffset;
                endOffset = Offset.noOffset;
        }

        return new Pair<>(startOffset, endOffset);
    }

    private CompressInfo findCompressionPatternWithGap(Ref prec, Ref dep,
                                                       Ref candPrec, Ref candDep, EdgeMeta metaData,
                                                       int gapSize, PatternType patternType) {
        if (dep.getColumn() == candDep.getColumn() && candDep.getLastRow() - dep.getRow() == -(gapSize + 1)) {
            if (metaData.patternType == PatternType.NOTYPE) {
                Offset offsetStartA = RefUtils.refToOffset(prec, dep, true);
                Offset offsetStartB = RefUtils.refToOffset(candPrec, candDep, true);

                Offset offsetEndA = RefUtils.refToOffset(prec, dep, false);
                Offset offsetEndB = RefUtils.refToOffset(candPrec, candDep, false);

                if (offsetStartA.equals(offsetStartB) &&
                        offsetEndA.equals(offsetEndB)) {
                    return new CompressInfo(false, Direction.TODOWN, patternType,
                            prec, dep, candPrec, candDep, metaData);
                }
            } else if (metaData.patternType == patternType) {
                Offset offsetStartA = RefUtils.refToOffset(prec, dep, true);
                Offset offsetEndA = RefUtils.refToOffset(prec, dep, false);

                if (offsetStartA.equals(metaData.startOffset) &&
                        offsetEndA.equals(metaData.endOffset)) {
                    return new CompressInfo(false, Direction.TODOWN, patternType,
                            prec, dep, candPrec, candDep, metaData);
                }
            }
        } else if (dep.getRow() == candDep.getRow() && candDep.getLastColumn() - dep.getColumn() == -(gapSize + 1)) {
            if (metaData.patternType == PatternType.NOTYPE) {
                Offset offsetStartA = RefUtils.refToOffset(prec, dep, true);
                Offset offsetStartB = RefUtils.refToOffset(candPrec, candDep, true);

                Offset offsetEndA = RefUtils.refToOffset(prec, dep, false);
                Offset offsetEndB = RefUtils.refToOffset(candPrec, candDep, false);

                if (offsetStartA.equals(offsetStartB) &&
                        offsetEndA.equals(offsetEndB)) {
                    return new CompressInfo(false, Direction.TORIGHT, patternType,
                            prec, dep, candPrec, candDep, metaData);
                }
            } else if (metaData.patternType == patternType) {
                Offset offsetStartA = RefUtils.refToOffset(prec, dep, true);
                Offset offsetEndA = RefUtils.refToOffset(prec, dep, false);

                if (offsetStartA.equals(metaData.startOffset) &&
                        offsetEndA.equals(metaData.endOffset)) {
                    return new CompressInfo(false, Direction.TORIGHT, patternType,
                            prec, dep, candPrec, candDep, metaData);
                }
            }
        }
        return new CompressInfo(false, Direction.NODIRECTION, PatternType.NOTYPE,
                prec, dep, candPrec, candDep, metaData);
    }

    private LinkedList<CompressInfo> findCompressInfo(Ref prec, Ref dep) {
        LinkedList<CompressInfo> compressInfoList = new LinkedList<>();
        findOverlapAndAdjacency(dep).forEach(candDep -> {
            findPrecs(candDep).forEach(candPrecWithMeta -> {
                CompressInfo compRes = findCompressionPattern(prec, dep,
                        candPrecWithMeta.getRef(), candDep, candPrecWithMeta.getEdgeMeta());
                addToCompressionInfoList(compressInfoList, compRes);
            });
        });

        if (!inRowCompression) {
            for (int i = 0; i < PatternType.NOTYPE.ordinal()
                    - PatternType.TYPEFIVE.ordinal(); i++) {
                int gapSize = i + 1;
                PatternType patternType =
                        PatternType.values()[PatternType.TYPEFIVE.ordinal() + i];
                if (compressInfoList.isEmpty()) {
                    findOverlapAndAdjacency(dep).forEach(candDep -> {
                        findPrecs(candDep).forEach(candPrecWithMeta -> {
                            CompressInfo compRes = findCompressionPatternWithGap(prec, dep,
                                    candPrecWithMeta.getRef(), candDep,
                                    candPrecWithMeta.getEdgeMeta(), gapSize, patternType);
                            addToCompressionInfoList(compressInfoList, compRes);
                        });
                    });
                }
            }
        }

        return compressInfoList;
    }

    private void addToCompressionInfoList(LinkedList<CompressInfo> compressInfoList,
                                          CompressInfo compRes) {
        Boolean isDuplicate = compRes.isDuplicate;
        PatternType compType = compRes.compType;
        if (isDuplicate || compType != PatternType.NOTYPE) {
            compressInfoList.add(compRes);
        }
    }

    private CompressInfo findCompressionPattern(Ref prec, Ref dep,
                                                Ref candPrec, Ref candDep, EdgeMeta metaData) {
        PatternType curCompType = metaData.patternType;

        // Check the duplicate edge
        if (isSubsume(candPrec, prec) && isSubsume(candDep, dep))
            return new CompressInfo(true, Direction.NODIRECTION, curCompType,
                    prec, dep, candPrec, candDep, metaData);

        // Otherwise, find the compression type
        // Guarantee the adjacency
        Direction direction = findAdjacencyDirection(dep, candDep);
        if (direction == Direction.NODIRECTION ||
                (inRowCompression &&
                        (direction == Direction.TOLEFT ||
                                direction == Direction.TORIGHT))) {
            return new CompressInfo(false, Direction.NODIRECTION, PatternType.NOTYPE,
                    prec, dep, candPrec, candDep, metaData);
        }

        Ref lastCandPrec = findLastPrec(candPrec, candDep, metaData, direction);
        PatternType compressType =
                findCompPatternHelper(direction, prec, dep, candPrec, candDep, lastCandPrec);
        PatternType retCompType = PatternType.NOTYPE;
        if (curCompType == PatternType.NOTYPE) retCompType = compressType;
        else if (curCompType == compressType) retCompType = compressType;

        return new CompressInfo(false, direction, retCompType,
                prec, dep, candPrec, candDep, metaData);
    }

    private PatternType findCompPatternHelper(Direction direction,
                                              Ref prec, Ref dep,
                                              Ref candPrec, Ref candDep,
                                              Ref lastCandPrec) {
        PatternType compressType = PatternType.NOTYPE;
        if (isCompressibleTypeOne(lastCandPrec, prec, direction)) {
            compressType = PatternType.TYPEONE;
            if (isCompressibleTypeZero(prec, dep, lastCandPrec))
                compressType = PatternType.TYPEZERO;
        } else if (isCompressibleTypeTwo(lastCandPrec, prec, direction))
            compressType = PatternType.TYPETWO;
        else if (isCompressibleTypeThree(lastCandPrec, prec, direction))
            compressType = PatternType.TYPETHREE;
        else if (isCompressibleTypeFour(lastCandPrec, prec))
            compressType = PatternType.TYPEFOUR;

        return compressType;
    }

    private boolean isSubsume(Ref large, Ref small) {
        if (large.getOverlap(small) == null) return false;
        return large.getOverlap(small).equals(small);
    }

    private Iterable<Ref> findOverlapAndAdjacency(Ref ref) {
        LinkedList<Ref> res = new LinkedList<>();

        findOverlappingRefs(ref).forEachRemaining(res::addLast);
        Arrays.stream(Direction.values()).filter(direction -> direction != Direction.NODIRECTION)
                .forEach(direction ->
                        findOverlappingRefs(shiftRef(ref, direction))
                                .forEachRemaining(adjRef -> {
                                    if (isValidAdjacency(adjRef, ref)) res.addLast(adjRef); // valid adjacency
                                })
                );

        return res;
    }

    private class CompressInfoComparator implements Comparator<CompressInfo> {

        @Override
        public int compare(CompressInfo infoA, CompressInfo infoB) {
            if (infoA.isDuplicate) return -1;
            else if (infoB.isDuplicate) return 1;
            else {
                int directionResult = infoA.direction.compareTo(infoB.direction);
                if (directionResult != 0) return directionResult;
                else {
                    return infoA.compType.compareTo(infoB.compType);
                }
            }
        }
    }

    private class CompressInfo {
        Boolean isDuplicate;
        Direction direction;
        PatternType compType;
        Ref prec;
        Ref dep;
        Ref candPrec;
        Ref candDep;
        EdgeMeta edgeMeta;

        CompressInfo(Boolean isDuplicate,
                     Direction direction,
                     PatternType compType,
                     Ref prec, Ref dep,
                     Ref candPrec, Ref candDep, EdgeMeta edgeMeta) {
            this.isDuplicate = isDuplicate;
            this.direction = direction;
            this.compType = compType;
            this.prec = prec;
            this.dep = dep;
            this.candPrec = candPrec;
            this.candDep = candDep;
            this.edgeMeta = edgeMeta;
        }
    }

    private class EdgeUpdate {
        Ref oldPrec;
        Ref oldDep;
        EdgeMeta oldEdgeMeta;

        Ref newPrec;
        Ref newDep;
        EdgeMeta newEdgeMeta;

        EdgeUpdate(Ref oldPrec,
                   Ref oldDep,
                   EdgeMeta oldEdgeMeta,
                   Ref newPrec,
                   Ref newDep,
                   EdgeMeta newEdgeMeta) {
            this.oldPrec = oldPrec;
            this.oldDep = oldDep;
            this.oldEdgeMeta = oldEdgeMeta;
            updateEdge(newPrec, newDep, newEdgeMeta);
        }

        void updateEdge(Ref newPrec,
                        Ref newDep,
                        EdgeMeta newEdgeMeta) {
            this.newPrec = newPrec;
            this.newDep = newDep;
            this.newEdgeMeta = newEdgeMeta;
        }

        boolean hasUpdate() {
            return !(oldPrec.equals(newPrec) &&
                    oldDep.equals(oldPrec) &&
                    oldEdgeMeta.equals(newEdgeMeta));
        }

    }
}
