package abruzese.util;

import abruzese.graph.ALGraph;
import abruzese.graph.Graph;
import abruzese.graph.edges.Road;
import abruzese.graph.vertices.Intersection;
import abruzese.hashtable.HashTable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Builds a graph from a file, following the format for this project.
 */
public class MapBuilder {
    /**
     * Builds a graph from a file, following the format for this project.
     */
    public static Graph<Intersection, Road> buildFromFile(String filename) throws IOException {
        Graph<Intersection, Road> graph = new ALGraph<>();
        LinkedList<Intersection> intersectionList = new LinkedList<>();
        LinkedList<Object[]> edges = new LinkedList<>();
        HashTable<String, Intersection> intersectionMap = new HashTable<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");

                if (parts.length < 2) {
                    continue;
                }

                switch (parts[0]) {
                    case "i":
                        if (parts.length >= 4) {
                            processIntersection(parts, intersectionList, intersectionMap);
                        }
                        break;
                    case "r":
                        if (parts.length >= 4) {
                            processRoad(parts, edges, intersectionMap);
                        }
                        break;
                }
            }
            graph.addAll(intersectionList);
            for(Object[] edge : edges) {
                graph.set((Intersection) edge[0], (Intersection) edge[1], (Road) edge[2]);
            }
        }

        return graph;
    }

    /**
     * Parses and adds intersection to the list and map
     */
    private static void processIntersection(String[] parts, LinkedList<Intersection> intersectionList,
                                            HashTable<String, Intersection> intersectionMap) {
        String id = parts[1];
        double latitude = Double.parseDouble(parts[2]);
        double longitude = Double.parseDouble(parts[3]);

        Intersection intersection = new Intersection(id, longitude, latitude);
        intersectionMap.put(id, intersection);

        intersectionList.add(intersection);
    }

    /**
     * Parses and adds road to the list and map
     */
    private static void processRoad(String[] parts, LinkedList<Object[]> edges,
                                    HashTable<String, Intersection> intersectionMap) {
        String roadId = parts[1];
        Intersection intersection1 = intersectionMap.get(parts[2]);
        Intersection intersection2 = intersectionMap.get(parts[3]);

        if (intersection1 == null || intersection2 == null) {
            throw new IllegalArgumentException("Road references non-existent intersection(s)");
        }

        // Calculate distance between intersections
        int distance = (int) Helpers.estimateDistance(intersection1, intersection2);

        // Add road in both directions since the graph is undirected
        Road road = new Road(roadId, distance);
        edges.add(new Object[]{intersection1, intersection2, road});
        edges.add(new Object[]{intersection2, intersection1, road});
    }
}