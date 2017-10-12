/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author dbickhart
 */
public class EdgeWeightedGraph {
    private final int MaxVertex;
    private int NumEdges;
    private final List<List<GraphEdge>> adjacent;
    
    public EdgeWeightedGraph(int vertices){
        assert(vertices > 0);
        this.MaxVertex = vertices;
        this.NumEdges = 0;
        this.adjacent = new ArrayList<>();
        for(int v = 0; v < MaxVertex; v++){
            this.adjacent.add(new LinkedList<>());
        }
    }
    
    public EdgeWeightedGraph(int vertices, int edges){
        this(vertices);
        assert(edges >= 0);       
        
    }
    
    public int MaxV(){
        return MaxVertex;
    }
    
    public int EdgeCount(){
        return NumEdges;
    }
    
    public Iterable<GraphEdge> Adjacent(int v){
        assert(v > 0 && v <= MaxVertex);
        return this.adjacent.get(v);
    }
    
    public int GetDegree(int v){
        assert(v > 0 && v <= MaxVertex);
        return this.adjacent.get(v).size();
    }
    
    public Iterable<GraphEdge> IterateThroughAll(){
        LinkedList<GraphEdge> list = new LinkedList<>();
        for(int v = 0; v < MaxVertex; v++){
            int selfLoops = 0;
            for(GraphEdge e : this.adjacent.get(v)){
                if(e.other(v) > v){
                    list.add(e);
                }else if(e.other(v) == v){
                    if(selfLoops % 2 == 0)
                        list.add(e);
                    selfLoops++;
                }                
            }
        }
        return list;
    }
}
