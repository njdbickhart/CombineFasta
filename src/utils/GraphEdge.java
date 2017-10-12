/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

/**
 * This is a modification of the Princeton lectures on MST algorithms
 * http://algs4.cs.princeton.edu/43mst/Edge.java.html
 * @author dbickhart
 */
public class GraphEdge implements Comparable<GraphEdge>{
    private final int v;
    private final int w;
    private final double weight;
    
    public GraphEdge(int v, int w, double weight){
        assert(v > 0 && w > 0 && !Double.isNaN(weight));
        this.v = v;
        this.w = w;
        this.weight = weight;
    }
    
    public double weight(){
        return weight;
    }
    
    public int either(){
        return v;
    }

    public int other(int vertex){
        assert(vertex == v || vertex == w);
        if(vertex == v) 
            return w;
        else 
            return v;
    }
    
    @Override
    public int compareTo(GraphEdge t) {
        return Double.compare(this.weight, t.weight);
    }
    
}
