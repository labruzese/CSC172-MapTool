package abruzese.graph;

import abruzese.graph.edges.EdgeWeight;
import abruzese.hashtable.HashTable;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a mutable graph data structure. A graph is directed and uses
 * generic edge weights. The graph may contain cycles. The graph permits any
 * non-null elements as vertices.
 *
 * @param <E> The type of elements in the graph
 * @param <W> The type of weight used for edges
 */
public abstract class Graph<E, W extends EdgeWeight> implements Iterable<E>, Serializable {

    /**
     * @return The number of vertices/elements in the graph.
     */
    public int size() {
        return getVertices().size();
    }

    /**
     * The edge getter for graphs.
     *
     * @param from The source of the edge
     * @param to The destination of the edge
     * @return The weight of the directed edge between the two vertices, or
     *         null if no edge exists
     * @throws NoSuchElementException if one or more elements don't exist in the graph
     */
    public abstract W get(E from, E to);

    /**
     * Sets the weight of the given edge.
     *
     * @param from The vertex to start from
     * @param to The vertex to end at
     * @param weight The weight to set on the edge between the two vertices
     * @return The previous weight of the edge between the two vertices, or
     *         null if no edge existed
     * @throws NoSuchElementException if from or to aren't in this graph
     */
    public abstract W set(E from, E to, W weight);

    /**
     * @return a set of the vertices in the graph.
     */
    public abstract Set<E> getVertices();

    /**
     * @return a set of the directed Edges (source to destination) in the graph.
     */
    public abstract Set<HashTable.Entry<E, E>> getEdges();

    /**
     * @return An iterator over the vertices in the graph. No order is guaranteed.
     */
    @Override
    public Iterator<E> iterator() {
        return getVertices().iterator();
    }

    /**
     * Adds a new, unconnected vertex to the graph.
     *
     * @param vertices The vertex to add to the graph
     * @return true if the vertex is successfully added to the graph and false
     *         if the vertex was already present in the graph
     */
    @SafeVarargs
    public final boolean add(E... vertices) {
        return !addAll(Arrays.asList(vertices)).isEmpty();
    }

    /**
     * Adds a collection of vertices to the graph.
     *
     * @param vertices The collection of vertices to add to the graph
     * @return a collection of all elements that already existed in this graph
     */
    public abstract Collection<E> addAll(Collection<E> vertices);

    /**
     * Removes a vertex from the graph and removes all edges it was a part of.
     *
     * @param vertices The vertices to remove from the graph
     * @return true if the vertex is successfully removed from the graph and
     *         false if the vertex is not present in the graph
     */
    @SafeVarargs
    public final boolean remove(E... vertices) {
        return !removeAll(Arrays.asList(vertices)).isEmpty();
    }

    /**
     * Removes all vertices in a collection from the graph and removes all
     * edges they were a part of.
     *
     * @param vertices the collection of vertices to remove from the graph
     * @return a collection of the vertices that were already not present in
     *         the graph
     */
    public abstract Collection<E> removeAll(Collection<E> vertices);

    /**
     * Removes an edge from the given vertices.
     *
     * @param from the source of the edge
     * @param to the destination of the edge
     * @return the weight of the edge removed, null if edge already didn't exist
     * @throws IllegalArgumentException if from or to don't exist in this graph
     */
    public abstract W removeEdge(E from, E to);

    /**
     * Removes all edges between the given vertices.
     *
     * @param v1 one end of the edges
     * @param v2 the other end of the edges
     * @return the number of edges removed
     */
    public int disconnect(E v1, E v2) {
        int numDisconnected = 0;
        if (removeEdge(v1, v2) != null) numDisconnected++;
        if (removeEdge(v2, v1) != null) numDisconnected++;
        return numDisconnected;
    }

    /**
     * Removes all edges in this graph.
     */
    public void clearEdges() {
        for (E from : this) {
            for (E to : this) {
                removeEdge(from, to);
            }
        }
    }

    /**
     * Checks if the given vertex is already in the graph
     *
     * @param vertex the vertex to check
     * @return true if the graph contains this vertex false if the graph
     *         doesn't contain this vertex
     */
    public abstract boolean contains(E vertex);

    /**
     * Retrieves all the vertices that source has an outbound connection to
     *
     * @param source the source vertex of the neighbors
     * @return a collection of vertices that are connected to the given vertex
     * @throws NoSuchElementException if source is not in this graph
     */
    public Collection<E> neighbors(E source) {
        List<E> neighbors = new ArrayList<>();
        Set<E> vertices = getVertices();
        for (E vert : vertices) {
            if (get(source, vert) != null) neighbors.add(vert);
        }
        return neighbors;
    }

    /**
     * @return the amount of connection between 2 vertices. Always between 0 and 2.
     * @throws NoSuchElementException if v1 or v2 doesn't exist in this graph
     */
    public abstract int countEdgesBetween(E v1, E v2);

    /**
     * Retrieve the vertices reachable from source.
     *
     * @param source the source that the returned vertices are reachable from
     * @return a collection of every vertex reachable from source
     * @throws IllegalArgumentException if source doesn't exist in this graph
     */
    public abstract Collection<E> getConnected(E source);

    /**
     * Note that this does not perform a deep copy and the objects
     * themselves are not copied. References will still point to the same object.
     *
     * @return a new graph identical to this one.
     */
    public Graph<E, W> copy() {
        return subgraph(getVertices());
    }

    /**
     * Returns a graph containing only a subset of vertices.
     *
     * @param vertices the subset of vertices the subgraph should contain
     * @return a new graph containing only the specified vertices
     * @throws IllegalArgumentException if any vertices do not exist in this graph
     */
    public abstract Graph<E, W> subgraph(Collection<E> vertices);

    /**
     * Joins other onto this graph including edges.
     *
     * @param other the graph to add to this graph
     */
    public void union(Graph<E, W> other) {
        addAll(other.getVertices());
        for (HashTable.Entry<E, E> edge : other.getEdges()) {
            W weight = other.get(edge.getKey(), edge.getValue());
            if (weight != null) {
                set(edge.getKey(), edge.getValue(), weight);
            }
        }
    }

    /**
     * Finds the shortest path between two vertices using Dijkstra's algorithm.
     *
     * @param from The vertex to start from
     * @param to The vertex to end at
     * @return A list of vertices representing the shortest path between the
     *         two vertices. Empty list if no path exists
     */
    public abstract List<E> path(E from, E to);

    /**
     * Finds a path between two vertices using Depth First Search.
     *
     * @param source The vertex to start from
     * @param destination The vertex to find/end at
     * @return A list of vertices representing the discovered path between the
     *         two vertices. Empty list if no path exists
     */
    public List<E> depthFirstSearch(E source, E destination) {
        return search(true, source, destination);
    }

    /**
     * Finds a path between two vertices using Breadth First Search.
     *
     * @param source The vertex to start from
     * @param destination The vertex to find/end at
     * @return A list of vertices representing the BFS path between the
     *         two vertices. Empty list if no path exists
     */
    public List<E> breadthFirstSearch(E source, E destination) {
        return search(false, source, destination);
    }

    protected List<E> search(boolean depth, E source, E destination) {
        LinkedList<E> q = new LinkedList<>();
        HashTable<E, E> prev = new HashTable<>(size());

        q.addFirst(source);
        prev.put(source, source);

        while (!q.isEmpty()) {
            E curPath = q.pop();

            for (E outboundVertex : getConnected(curPath)) {
                if (!prev.containsKey(outboundVertex) && !outboundVertex.equals(curPath)) {
                    if (depth) {
                        q.addFirst(outboundVertex);
                    } else {
                        q.addLast(outboundVertex);
                    }
                    prev.put(outboundVertex, curPath);
                    if (outboundVertex.equals(destination)) break;
                }
            }
        }

        LinkedList<E> path = new LinkedList<>();
        if (!prev.containsKey(destination)) return path;

        E next = prev.get(destination);
        while (!next.equals(source)) {
            path.addFirst(next);
            next = prev.get(next);
            if (next == null) throw new IllegalStateException("Destination found but no path exists");
        }
        path.addFirst(source);
        return path;
    }

    /**
     * Finds the shortest distance between two vertices using Dijkstra's algorithm.
     *
     * @param from The vertex to start from
     * @param to The vertex to end at
     * @return The distance between the two vertices, or infinity if no path exists
     */
    public abstract EdgeWeight distance(E from, E to);

    @Override
    public int hashCode() {
        return Objects.hash(getVertices(), getEdges());
    }

    @Override
    // Auto-generated by IntelliJ
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Graph<?, ?> other)) return false;
        return getVertices().equals(other.getVertices()) &&
                getEdges().equals(other.getEdges());
    }

    @Override
    public String toString() {
        return getEdges().toString();
    }
}