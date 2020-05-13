import javafx.util.Pair;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import java.util.*;

class GraphIso {
    // Constructor
    public GraphIso(Graph<String, DefaultEdge> theSG, Graph<String, DefaultEdge> theG){
        G = theG;
        SG = theSG;
        found = false;
        endOfBranch = false;
        Mapping = new Vector<>();
    }

    public boolean checkSGI() {
        Set<String> verticesSG = SG.vertexSet();
        Set<String> verticesG = G.vertexSet();

        Iterator<String> A = verticesSG.iterator();
        Iterator<String> B = verticesG.iterator();
        String U = A.next();
        while (B.hasNext()) {
            String V = B.next();
            if (findMapping(U, V)) {
                found = true;
                return true;
            }
        }
        return false;
    }

    boolean findMapping(String U, String V) {
        endOfBranch = false;
        if (Mapping.size() == SG.vertexSet().size()) {
            found = true;
            return true;
        }
        if (compatibleNodes(U, V)) {
            Vector<Pair<String, String>> Pairs = getCompatibleNeighbors(U, V);
            if (Pairs == null) return false;
            else {
                Pair<String, String> A = new Pair<>(U,V);
                Mapping.add(A);
                for (Pair<String, String> pair : Pairs) {
                    if (found) break;
                    if (endOfBranch) {
                        endOfBranch = false;
                        continue;
                    }
                    findMapping(pair.getKey(), pair.getValue());
                }
                if (Mapping.size() == SG.vertexSet().size()) {
                    Mapping.remove(Mapping.size() - 1);
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    Vector<Pair<String, String>> getCompatibleNeighbors(Object U, Object V) {
        Vector<Pair<String, String>> compatiblePairs = new Vector<>();
        List<String> u = Graphs.neighborListOf(SG, U.toString());
        List<String> v = Graphs.neighborListOf(G, V.toString());

        for (String uNeighbor : u) {
            for (String vNeighbor : v) {
                if (compatibleNodes(uNeighbor, vNeighbor)) {
                    if (!Mapping.contains(uNeighbor) || !Mapping.contains(vNeighbor)) {
                        Pair<String, String> A = new Pair<>(uNeighbor, vNeighbor);
                        compatiblePairs.add(A);
                    }
                    if (SG.vertexSet().size() == Mapping.size()) {
                        endOfBranch = true;
                        return compatiblePairs;
                    }
                }
            }
        }
        return compatiblePairs;
    }

    boolean compatibleNodes(String U, String V) {
        return U.split(" ")[0].equals(V.split(" ")[0]) &&
                SG.degreeOf(U) <= G.degreeOf(V);
    }

    public boolean found;
    public boolean endOfBranch;
    public Vector<Pair<String, String>> Mapping;
    public Graph<String, DefaultEdge> G;
    public Graph<String, DefaultEdge> SG;

}