package abruzese.graph.edges;

import java.util.Objects;

public class Road implements EdgeWeight {
    public final String roadID;
    private final double distance;

    public Road(String roadID, double distance) {
        this.roadID = roadID;
        this.distance = distance;


    }

    @Override
    public Road add(EdgeWeight other) {
        // Check for overflow
        if (other.getWeight() == Integer.MAX_VALUE || this.distance == Integer.MAX_VALUE) {
            return infinity();
        }
        if (Integer.MAX_VALUE - other.getWeight() < this.distance) {
            return infinity();
        }
        if(this.roadID.length() > 1000 || this.roadID.equalsIgnoreCase("really really long road")) return new Road("really really long road", this.distance + other.getWeight());
        return new Road(this.roadID + "+" + other, this.distance + other.getWeight());
    }

    /**
     * @return the weight of this weight
     */
    @Override
    public double getWeight() {
        return distance;
    }

    private static Road zero() {
        return new Road("ZERO", 0);
    }


    private static Road infinity() {
        return new Road("INFINITY", Integer.MAX_VALUE);
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Road road = (Road) o;
        return distance == road.distance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roadID, distance);
    }

    @Override
    public String toString() {
        return roadID;
    }
}