/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author dbickhart
 */
public class PrimMST {
    private static final double EPSILON = 1E-12;
    private static final Logger log = Logger.getLogger(PrimMST.class.getName());
    
    private GraphEdge[] edgeTo;
    private double[] distTo;
    private boolean[] used;
    private IndexedPriorityQueue<Double> pq;
    
    public PrimMST(EdgeWeightedGraph G){
        edgeTo = new GraphEdge[G.MaxV()];
        distTo = new double[G.MaxV()];
        used = new boolean[G.MaxV()];
        pq = new IndexedPriorityQueue<>(G.MaxV());
        
        for (int v = 0; v < G.MaxV(); v++)
            distTo[v] = Double.POSITIVE_INFINITY;
    }
    
    public void runPrim(EdgeWeightedGraph G){
        for(int v = 0; v < G.MaxV(); v++){
            if(!used[v]){
                distTo[v] = 0.0;
                pq.insert(v, distTo[v]);
                while(!pq.isEmpty()){
                    int s = pq.delMin();
                    this.scanVertex(G, s);
                }
            }
        }
    }
    
    private void scanVertex(EdgeWeightedGraph G, int v){
        used[v] = true;
        for(GraphEdge e : G.Adjacent(v)){
            int w = e.other(v);
            if(used[w])
                continue;
            if(e.weight() < distTo[w]){
                distTo[w] = e.weight();
                edgeTo[w] = e;
                if(pq.contains(w))
                    pq.decreaseKey(w, distTo[w]);
                else
                    pq.insert(w, distTo[w]);
            }
        }
    }
    
    public List<LinkedList<GraphEdge>> edges(){
        List<LinkedList<GraphEdge>> queue = new ArrayList<>();
        int idx = 0;
        queue.add(new LinkedList<>());
        for(int v = 0; v < edgeTo.length; v++){
            GraphEdge e = edgeTo[v];
            if(e != null)
                queue.get(idx).push(e);
            else{
                // New forest
                queue.add(new LinkedList<>());
                idx++;
            }
        }
        return queue;
    }
    
    public double sumWeight(){
        return this.edges().parallelStream()
                .flatMap(List::stream)
                .reduce(0.0, (sum, p) -> sum += p.weight(), (sum1, sum2) -> sum1 + sum2);
    }
}
