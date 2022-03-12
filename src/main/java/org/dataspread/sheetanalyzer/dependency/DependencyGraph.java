package org.dataspread.sheetanalyzer.dependency;

import org.dataspread.sheetanalyzer.dependency.util.RefWithMeta;
import org.dataspread.sheetanalyzer.util.Pair;
import org.dataspread.sheetanalyzer.util.Ref;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DependencyGraph {
  void addBatch(List<Pair<Ref, Ref>> edgeBatch);

  void add(Ref precedent, Ref dependent);

  Set<Ref> getDependents(Ref precedent);

  void clearDependents(Ref dependent);

  String getCompressInfo();

  long getNumVertices();

  long getNumEdges();

  Pair<Map<Ref, List<RefWithMeta>>, Map<Ref, List<RefWithMeta>>> getCompressedGraph();
}
