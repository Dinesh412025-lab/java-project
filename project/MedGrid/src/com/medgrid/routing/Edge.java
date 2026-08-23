package com.medgrid.routing;

public class Edge {
    private final Node source;
    private final Node target;
    private double weight; // e.g. time or distance

    public Edge(Node source, Node target, double weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    public Node getSource() { return source; }
    public Node getTarget() { return target; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}
