/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readsplit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import readsplit.kmerIndex.KOrder;
import utils.KmerIntersection.ByteString;

/**
 *
 * @author derek.bickhart-adm
 */
public class sparseMatrix {
    private static final Logger log = Logger.getLogger(sparseMatrix.class.getName());
    private final kmerIndex index;
    private final int[] dimensions;
    private final Map<Integer, Map<Integer, KCounter>> matrix;
    
    public sparseMatrix(int eCount, kmerIndex index){
        this.dimensions = new int[]{eCount, eCount};
        this.matrix = new ConcurrentHashMap<>();
        this.index = index;
        
        // Preloading matrix with hashkeys
        for(int x = 1; x <= eCount; x++)
            this.matrix.put(x, new ConcurrentHashMap<>());
    }
    
    public synchronized void updateCounter(List<ByteString> kmers, kmerIndex index){
        List<Integer> indices = kmers.stream().map(s -> index.getKIndex(s)).collect(Collectors.toList());
        List<KOrder> orders = kmers.stream().map(s -> index.getKOrder(s)).collect(Collectors.toList());
        
        for(int x = 0; x < indices.size(); x++){
            for(int y = 0; y < indices.size(); y++){
                if(x == y)
                    continue; // avoid self counting
                if(! this.matrix.get(indices.get(x)).containsKey(indices.get(y)))
                    this.matrix.get(indices.get(x)).put(indices.get(y), new KCounter());
                this.matrix.get(indices.get(x)).get(indices.get(y)).updateCount(orders.get(x), orders.get(y));
            }
        }
    }
    
    public class KCounter{
        // Using simplest data structure for speed and memory concerns
        // Index structure: AA = 0, AB = 1, BA = 2, BB = 3
        public final int[] counts = new int[]{0,0,0,0};
        
        public void updateCount(KOrder one, KOrder two){
            if(one == KOrder.A){
                if(two == KOrder.B)
                    this.counts[1]++;
                else
                    this.counts[0]++;
            }else{
                if(two == KOrder.B)
                    this.counts[3]++;
                else
                    this.counts[2]++;
            }
        }
        
        public int getCombined(){
            return Arrays.stream(counts).sum();
        }
        
        public double getStdev(){
            final double avg = this.getAvg();
            double var = Arrays.stream(counts).mapToDouble(s -> s / 1.0d)
                    .map(s -> s - avg)
                    .map(s -> s*s)
                    .average()
                    .getAsDouble();
            return Math.sqrt(var);
        }
        
        public double getAvg(){
            return this.getCombined() / 4d;
        }
        
        public boolean isHet(double hetDev){
            int[] sorted = this.counts.clone();
            Arrays.sort(sorted);
            // Test if sums fit model of het (25% +/- hetDev)
            int top = sorted[0] + sorted[1];
            int bottom = sorted[2] + sorted[3];
            boolean equal = (sorted[2] - (sorted[2] * hetDev) <= sorted[3] 
                    && sorted[2] + (sorted[2] * hetDev) >= sorted[3]);
            if(top < bottom / 2 && equal)
                return true;
            else
                return false;
        }
    }
}
