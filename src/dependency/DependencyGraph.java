package dependency;

import util.Pair;
import util.Ref;

import java.util.List;
import java.util.Set;

public interface DependencyGraph {
 Set<Ref> getDependents(Ref precedent);
 void add(Ref precedent, Ref dependent);
 void clearDependents(Ref dependent);
 void addBatch(List<Pair<Ref, Ref>> edgeBatch);
}
