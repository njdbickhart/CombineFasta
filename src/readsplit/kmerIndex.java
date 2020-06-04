/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package readsplit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import utils.KmerIntersection.ByteString;

/**
 *
 * @author derek.bickhart-adm
 */
public class kmerIndex {
    // This class subsamples het-kmers from a file and stores information on their pairing
    private static final Logger log = Logger.getLogger(kmerIndex.class.getName());
    private final String FileName;
    private final Integer subSample;
    private final Integer merSize;
    // Mapping numerical relationship of byte string
    private Map<ByteString, Integer> kmerToIndex = new ConcurrentHashMap<>();
    // Mapping pair order to byte string
    private Map<ByteString, KOrder> kmerToOrder = new ConcurrentHashMap<>();
    // Mapping byte string to numerical relationship and pairing
    private Map<Integer, KPair<ByteString>> indexToKmer = new ConcurrentHashMap<>();
    
    public kmerIndex(int subSample, String fileName, int mer){
        this.subSample = subSample;
        this.FileName = fileName;
        this.merSize = mer;
    }
    
    public void buildIndex(){
        final Path fpath = Paths.get(FileName);
        Long flength = 0L;
        // Count file lines
        try(BufferedReader input = Files.newBufferedReader(fpath, Charset.defaultCharset())){
            String l;
            while((l = input.readLine()) != null)
                flength++;
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error estimating file length!", ex);
        }
        
        log.log(Level.FINE, "Estimated kmer pair file length: " + flength);
        
        double ratio = this.subSample / flength;
        Random rand = new Random();
        // Now read the file and build the index variables
        try(BufferedReader input = Files.newBufferedReader(fpath, Charset.defaultCharset())){
            String l;
            int sampled = 0;
            while((l = input.readLine()) != null){
                // if the kmer pair is in our subset
                if(rand.nextDouble() < ratio){
                    sampled++;
                    // Create the kmer bytestrings
                    String[] s = l.trim().split("\t");
                    List<ByteString> seqs = Arrays.stream(s)
                            .map(d -> new ByteString(this.merSize))
                            .collect(Collectors.toList());

                    for(int x = 0; x < s.length; x++)
                        seqs.get(x).calculateHash(s[x].getBytes(), 0);
                    
                    // Fill the table
                    this.indexToKmer.put(sampled, new KPair(seqs.get(0), seqs.get(1)));
                    this.kmerToIndex.put(seqs.get(0), sampled);
                    this.kmerToOrder.put(seqs.get(0), KOrder.A);
                    this.kmerToIndex.put(seqs.get(1), sampled);
                    this.kmerToOrder.put(seqs.get(0), KOrder.B);
                }
            }
            log.log(Level.INFO, "Subsampled " + sampled + " Kmer pairs for analysis");
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error building kmer index from file!", ex);
        }
    }
    
    // Getters and Setters
    public Integer getKIndex(ByteString b){
        if(this.kmerToIndex.containsKey(b))
            return this.kmerToIndex.get(b);
        else
            return -1;  // Value to show that the kmer isn't in the list
    }
    
    public KPair getKPair(Integer i){
        if(this.indexToKmer.containsKey(i))
            return this.indexToKmer.get(i);
        else
            return null;
    }
    
    public KOrder getKOrder(ByteString b){
        if(this.kmerToOrder.containsKey(b))
            return this.kmerToOrder.get(b);
        else
            return null;
    }
    
    // Utility classes
    public enum KOrder { A, B}
    
    public class KPair <T extends ByteString>{
        public final ByteString A;
        public final ByteString B;
        public final Map<ByteString, KOrder> kindex = new HashMap<>();
        
        public KPair(ByteString a, ByteString b){
            this.A = a;
            this.B = b;
            kindex.put(a, KOrder.A);
            kindex.put(b, KOrder.B);
        }
        
        public KOrder getKOrder(ByteString a){
            return this.kindex.get(a);
        }
    }
}
