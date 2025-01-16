package abruzese.graph.edges;

/**
 * Represents a weight/distance that can be summed with other weights and compared.
 * Used to represent edge weights in a graph.
 */
public interface EdgeWeight<T extends EdgeWeight<T>> extends Comparable<T> {
    /**
     * Adds this weight to another weight
     * @param other the weight to add to this one
     * @return the sum of the weights
     */
    T add(T other);

    /**
     * @return the zero/identity value for this type of weight
     */
    T zero();

    /**
     * @return the maximum possible value for this type of weight
     */
    T infinity();
}
