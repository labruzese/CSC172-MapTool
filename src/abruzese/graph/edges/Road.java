package abruzese.graph.edges;

public class Road implements EdgeWeight<Road> {
    public final String roadID;
    private final int distance;

    public Road(String roadID, Integer distance) {
        this.roadID = roadID;
        this.distance = distance;
    }

    @Override
    public Road add(Road other) {
        // Check for overflow
        if (other.distance == Integer.MAX_VALUE || this.distance == Integer.MAX_VALUE) {
            return infinity();
        }
        if (Integer.MAX_VALUE - other.distance < this.distance) {
            return infinity();
        }
        return new Road(this.roadID, this.distance + other.distance);
    }

    @Override
    public Road zero() {
        return new Road("ZERO", 0);
    }

    @Override
    public Road infinity() {
        return new Road("INFINITY", Integer.MAX_VALUE);
    }

    @Override
    public int compareTo(Road other) {
        return Integer.compare(this.distance, other.distance);
    }

    public int getDistance() {
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
        return Integer.hashCode(distance);
    }

    @Override
    public String toString() {
        return roadID + ": " + distance;
    }
}