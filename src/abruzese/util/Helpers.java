package abruzese.util;

import abruzese.graph.vertices.Intersection;

public class Helpers {
    // Estimate distance between two intersections using haversine formula
    // https://en.wikipedia.org/wiki/Haversine_formula
    public static double estimateDistance(Intersection i1, Intersection i2) {
        final double R = 6371000.0; // Earth's radius in meters

        double lat1 = Math.toRadians(i1.latitude());
        double lat2 = Math.toRadians(i2.latitude());
        double lon1 = Math.toRadians(i1.longitude());
        double lon2 = Math.toRadians(i2.longitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (R * c);
    }
}