package abruzese.console;

import abruzese.graph.edges.Road;
import abruzese.graph.vertices.Intersection;

public class DirectionStep {
    public final Intersection from;
    public final Intersection to;
    public final Road road;
    public final String instruction;
    public final double distance;
    public final int stepNumber;

    DirectionStep(Intersection from, Intersection to, Road road, String instruction, double distance, int stepNumber) {
        this.from = from;
        this.to = to;
        this.road = road;
        this.instruction = instruction;
        this.distance = distance;
        this.stepNumber = stepNumber;
    }
}