package abruzese.graph.edges;

/**
 * Represents a weight/distance that can be summed with other weights and compared.
 * Used to represent edge weights in a graph.
 */
public interface EdgeWeight extends Comparable<EdgeWeight> {
    /**
     * @return the weight of this weight
     */
    double getWeight();

    /**
     * Adds this weight to another weight
     * @param other the weight to add to this one
     * @return the sum of the weights
     */
    EdgeWeight add(EdgeWeight other);

    /**
     * @return the zero/identity value for this type of weight
     */
    static EdgeWeight zero() {
        return new Road("ZERO", 0);
    }

    /**
     * @return the maximum possible value for this type of weight
     */
    static EdgeWeight infinity() {
        return new Road("INFINITY", Double.MAX_VALUE);
    }

    @Override
    default int compareTo(EdgeWeight other) {
        return Double.compare(this.getWeight(), other.getWeight());
    }

    static EdgeWeight fromDouble(double weight) {
        return new Road("unnamed", weight);
    }
}
