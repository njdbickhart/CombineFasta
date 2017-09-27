/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misassemblyLDCorrection;

import combinefasta.BufferedFastaReaderWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The main workhorse for identifying which chromosomes and segments need to be rearranged
 * Reads in an alignment file and generates segment "blocks" that are to be reordered
 * @author dbickhart
 */
public class RearrangementPlan {
    private static final Logger log = Logger.getLogger(RearrangementPlan.class.getName());
    private Path samFile;
    private IndexedFastaReader origin; 
    private BufferedFastaReaderWriter output;
    //protected static final Pattern readMarkerOrder = Pattern.compile(".+\\.(.+)\\.(.+)");
    private List<markerCoords> coords;
    
    public RearrangementPlan(String samFile, String fasta){
        this.samFile = Paths.get(samFile);
        if(!this.samFile.toFile().canRead()){
            log.log(Level.SEVERE, "Cannot read input sam file! terminating...");
            System.exit(-1);
        }
        
        this.origin = new IndexedFastaReader(Paths.get(fasta));
    }
    
    public void CreateMarkerPlan(){
        // Read and process the sam file into a list        
        try(BufferedReader input = Files.newBufferedReader(this.samFile, Charset.defaultCharset())){
            this.coords = input.lines()
                    .filter((s) -> (! s.startsWith("@")))
                    .map((l) -> {return new markerCoords(l);})
                    .collect(Collectors.toList());
        }catch(IOException ex){
            log.log(Level.SEVERE, "Encountered error reading sam file!", ex);
        }
        
        // sort the list and process into a bed file
        // Such a better syntax for comparisons here! 
        coords.sort(Comparator.comparing(markerCoords::getOChr)
                .thenComparing(markerCoords::getOPos));
        
        
    }
    
    protected class markerCoords{
        public final String oChr;
        public final String nChr;
        public final int oPos;
        public final int nPos;
        
        public markerCoords(String line){
            line = line.trim();
            String[] segs = line.split("\t");
            String[] oSegs = ProcessReadName(segs[0]);
            
            this.oChr = oSegs[0];
            this.oPos = Integer.parseInt(oSegs[1]);
            
            this.nChr = segs[2];
            this.nPos = Integer.parseInt(segs[3]);
        }
        
        private String[] ProcessReadName(String name){
            String[] elements = name.split("\\.");
            return Arrays.copyOfRange(elements, 1, 2);
        }
        
        public String getOChr(){
            return this.oChr;
        }
        
        public int getOPos(){
            return this.oPos;
        }
        
        public boolean isUnmapped(){
            return this.nChr.startsWith("*");
        }
    }
}
