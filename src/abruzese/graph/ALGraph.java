package abruzese.graph;

import abruzese.graph.edges.EdgeWeight;
import abruzese.hashtable.HashTable;
import abruzese.priorityQueue.IndexedPriorityQueue;

import java.util.*;

/**
 * AdjacencyListGraph is a graph implementation that uses an adjacency list
 * for efficient storage and operations. It supports Dijkstra's algorithm
 * for shortest path calculations.
 *
 * @param <E> The type of elements in the graph
 * @param <W> The type of weight used for edges
 */
public class ALGraph<E, W extends EdgeWeight> extends Graph<E, W> {
    private final HashTable<E, HashTable<E, W>> adjacencyList = new HashTable<>();

    @Override
    public W get(E from, E to) {
        Objects.requireNonNull(from, "Source vertex cannot be null");
        Objects.requireNonNull(to, "Destination vertex cannot be null");

        HashTable<E, W> edges = adjacencyList.get(from);
        if (edges == null) {
            throw new NoSuchElementException("Source vertex does not exist: " + from);
        }
        return edges.get(to);
    }

    @Override
    public W set(E from, E to, W weight) {
        Objects.requireNonNull(from, "Source vertex cannot be null");
        Objects.requireNonNull(to, "Destination vertex cannot be null");
        Objects.requireNonNull(weight, "Edge weight cannot be null");

        adjacencyList.putIfAbsent(from, new HashTable<>());
        adjacencyList.putIfAbsent(to, new HashTable<>());

        return adjacencyList.get(from).put(to, weight);
    }

    @Override
    public Set<E> getVertices() {
        return adjacencyList.keySet();
    }

    @Override
    public Set<HashTable.Entry<E, E>> getEdges() {
        Set<HashTable.Entry<E, E>> edges = new HashSet<>();
        for (HashTable.Entry<E, HashTable<E, W>> entry : adjacencyList.entrySet()) {
            E from = entry.getKey();
            for (E to : entry.getValue().keySet()) {
                edges.add(new HashTable.Entry<>(from, to));
            }
        }
        return edges;
    }

    @Override
    public Collection<E> addAll(Collection<E> vertices) {
        List<E> alreadyPresent = new ArrayList<>();
        for (E vertex : vertices) {
            if (!adjacencyList.containsKey(vertex)) {
                adjacencyList.put(vertex, new HashTable<>());
            } else {
                alreadyPresent.add(vertex);
            }
        }
        return alreadyPresent;
    }

    @Override
    public Collection<E> removeAll(Collection<E> vertices) {
        List<E> notPresent = new ArrayList<>();
        for (E vertex : vertices) {
            if (adjacencyList.containsKey(vertex)) {
                adjacencyList.remove(vertex);
                for (HashTable<E, W> neighbors : adjacencyList.values()) {
                    neighbors.remove(vertex);
                }
            } else {
                notPresent.add(vertex);
            }
        }
        return notPresent;
    }

    @Override
    public W removeEdge(E from, E to) {
        Objects.requireNonNull(from, "Source vertex cannot be null");
        Objects.requireNonNull(to, "Destination vertex cannot be null");

        HashTable<E, W> edges = adjacencyList.get(from);
        if (edges == null) {
            throw new IllegalArgumentException("Source vertex does not exist: " + from);
        }
        return edges.remove(to);
    }

    @Override
    public boolean contains(E vertex) {
        return adjacencyList.containsKey(vertex);
    }

    @Override
    public int countEdgesBetween(E v1, E v2) {
        int count = 0;
        if (adjacencyList.getOrDefault(v1, new HashTable<>()).containsKey(v2)) count++;
        if (adjacencyList.getOrDefault(v2, new HashTable<>()).containsKey(v1)) count++;
        return count;
    }

    @Override
    public Collection<E> getConnected(E source) {
        HashTable<E, W> neighbors = adjacencyList.get(source);
        if (neighbors == null) {
            throw new IllegalArgumentException("Source vertex does not exist: " + source);
        }
        return neighbors.keySet();
    }

    @Override
    public Graph<E, W> subgraph(Collection<E> vertices) {
        ALGraph<E, W> subgraph = new ALGraph<>();
        for (E vertex : vertices) {
            if (!adjacencyList.containsKey(vertex)) {
                throw new IllegalArgumentException("Vertex does not exist: " + vertex);
            }
            subgraph.add(vertex);
        }
        for (E from : vertices) {
            for (E to : getConnected(from)) {
                if (vertices.contains(to)) {
                    subgraph.set(from, to, get(from, to));
                }
            }
        }
        return subgraph;
    }

    @Override
    public List<E> path(E from, E to) {
        List<E> path = new ArrayList<>();
        distance(from, to); // Side effect: populate shortestPathMap
        E current = to;

        while (current != null && !current.equals(from)) {
            path.addFirst(current);
            current = previous.get(current);
        }
        if (current != null) path.addFirst(from);
        return path;
    }

    private final HashTable<E, EdgeWeight> distances = new HashTable<>();
    private final HashTable<E, E> previous = new HashTable<>();

    @Override
    public EdgeWeight distance(E from, E to) {
        Objects.requireNonNull(from, "Source vertex cannot be null");
        Objects.requireNonNull(to, "Destination vertex cannot be null");

        IndexedPriorityQueue<E> pq = new IndexedPriorityQueue<>(Comparator.comparing(v ->
                distances.getOrDefault(v, EdgeWeight.infinity())));
        distances.clear();
        previous.clear();

        distances.put(from, EdgeWeight.zero());
        pq.add(from);

        while (!pq.isEmpty()) {
            E current = pq.poll();
            if (current.equals(to)) break;

            EdgeWeight currentDistance = distances.get(current);

            for (HashTable.Entry<E, W> edge : adjacencyList.getOrDefault(current, new HashTable<>()).entrySet()) {
                E neighbor = edge.getKey();
                EdgeWeight newDist = currentDistance.add(edge.getValue());
                EdgeWeight prevDist = distances.getOrDefault(neighbor, EdgeWeight.infinity());

                if (newDist.compareTo(prevDist) < 0) {
                    distances.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    if (pq.contains(neighbor)) {
                        pq.decreaseKey(neighbor);
                    } else {
                        pq.add(neighbor);
                    }
                }
            }
        }

        return distances.getOrDefault(to, EdgeWeight.infinity());
    }

    //coordinates is a map from vertex to [latitude, longitude]
    public List<E> pathAStar(E from, E to, HashTable<E, double[]> coordinates) {
        Objects.requireNonNull(from, "Source vertex cannot be null");
        Objects.requireNonNull(to, "Destination vertex cannot be null");
        Objects.requireNonNull(coordinates, "Coordinates map cannot be null");

        if (!coordinates.containsKey(from) || !coordinates.containsKey(to)) {
            throw new IllegalArgumentException("Missing coordinates for source or destination vertex");
        }

        // Calculate Haversine distance for heuristic
        double[] destCoord = coordinates.get(to);

        distances.clear();
        previous.clear();

        HashTable<E, EdgeWeight> fScores = new HashTable<>();
        distances.put(from, EdgeWeight.zero());

        // Calculate initial heuristic
        double[] startCoord = coordinates.get(from);
        double initialH = haversineDistance(
                startCoord[0], startCoord[1],
                destCoord[0], destCoord[1]
        );
        fScores.put(from, EdgeWeight.fromDouble(initialH));

        IndexedPriorityQueue<E> openSet = new IndexedPriorityQueue<>(
                Comparator.comparing(v -> fScores.getOrDefault(v, EdgeWeight.infinity()))
        );
        openSet.add(from);

        while (!openSet.isEmpty()) {
            E current = openSet.poll();

            if (current.equals(to)) {
                break;
            }

            EdgeWeight gScore = distances.get(current);

            for (HashTable.Entry<E, W> edge : adjacencyList.getOrDefault(current, new HashTable<>()).entrySet()) {
                E neighbor = edge.getKey();
                EdgeWeight tentativeGScore = gScore.add(edge.getValue());

                if (tentativeGScore.compareTo(distances.getOrDefault(neighbor, EdgeWeight.infinity())) < 0) {
                    previous.put(neighbor, current);
                    distances.put(neighbor, tentativeGScore);

                    // Calculate f-score = g-score + heuristic
                    double[] neighborCoord = coordinates.get(neighbor);
                    double h = haversineDistance(
                            neighborCoord[0], neighborCoord[1],
                            destCoord[0], destCoord[1]
                    );
                    fScores.put(neighbor, tentativeGScore.add(EdgeWeight.fromDouble(h)));

                    if (openSet.contains(neighbor)) {
                        openSet.decreaseKey(neighbor);
                    } else {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        List<E> path = new ArrayList<>();
        E current = to;

        while (current != null && !current.equals(from)) {
            path.addFirst(current);
            current = previous.get(current);
        }

        if (current != null) {
            path.addFirst(from);
        }

        return path;
    }

    /**
     * Calculates the Haversine distance between two points on Earth
     * @return Distance in kilometers
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers

        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}
