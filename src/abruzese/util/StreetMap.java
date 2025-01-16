package abruzese;

import abruzese.graph.*;
import abruzese.graph.edges.Road;
import abruzese.graph.vertices.Intersection;
import abruzese.gui.MapPanel;
import abruzese.hashtable.HashTable;
import abruzese.util.Helpers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * Stores a streetmap graph and allows for pathfinding
 */
public class StreetMap {
    private final Graph<Intersection, Road> streetGraph;

    public Graph<Intersection, Road> getStreetGraph() {
        return streetGraph;
    }

    public StreetMap(String filename) throws IOException {
        this.streetGraph = MapBuilder.buildFromFile(filename);
    }

    /**
     * Returns the shortest path between two intersections given as strings
     */
    public List<Intersection> findPath(String start, String end) {
        Intersection startIntersection = null;
        Intersection endIntersection = null;

        // Find the intersections matching the given IDs
        for (Intersection intersection : streetGraph) {
            if (intersection.intersectionID().equals(start)) {
                startIntersection = intersection;
            }
            if (intersection.intersectionID().equals(end)) {
                endIntersection = intersection;
            }
        }

        if (startIntersection == null || endIntersection == null) {
            throw new IllegalArgumentException("Start or end intersection not found");
        }

        //This is optional, but doing dijkstra's on paths over 200km can have noticeable delay and I wanted to try my hand at A*
        if(Helpers.estimateDistance(startIntersection, endIntersection) > 200000) {
            System.out.println("Switching to A* for directions greater than 200km: " + (int)(Helpers.estimateDistance(startIntersection, endIntersection)/1000) + "km");
//            System.out.print("Print searching statistics while finding path (it could take awhile) (y/n)\n> ");
//            String input = new java.util.Scanner(System.in).nextLine();
//            if(input.equalsIgnoreCase("y")) {
//                return Helpers.aStar(streetGraph, startIntersection, endIntersection);
//            }
            HashTable<Intersection, double[]> coordTable = new HashTable<>();
            for (Intersection intersection : streetGraph) {
                coordTable.put(intersection, new double[]{intersection.latitude(), intersection.longitude()});
            }

            return ((ALGraph<Intersection, Road>)streetGraph).pathAStar(startIntersection, endIntersection, coordTable);
        }

        return streetGraph.path(startIntersection, endIntersection);
    }
}