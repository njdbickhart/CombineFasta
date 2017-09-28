/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misassemblyLDCorrection;

import combinefasta.BufferedFastaReaderWriter;
import file.BedSimple;
import htsjdk.samtools.reference.FastaSequenceFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import utils.MergerUtils;

/**
 * The main workhorse for identifying which chromosomes and segments need to be rearranged
 * Reads in an alignment file and generates segment "blocks" that are to be reordered
 * @author dbickhart
 */
public class RearrangementPlan {
    private static final Logger log = Logger.getLogger(RearrangementPlan.class.getName());
    private final Path samFile;
    private IndexedFastaReader origin; 
    //private BufferedFastaReaderWriter output;
    //protected static final Pattern readMarkerOrder = Pattern.compile(".+\\.(.+)\\.(.+)");
    private List<markerCoords> coords;
    private final Map<String, List<BedFastaPlan>> mergedPlan = new HashMap<>();
    //private static final MergerUtils util = new MergerUtils();
    
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
        
        log.log(Level.INFO, "Finished sam file reading and organization");
                
        // sort the list and process into a bed file
        // First, sort by new marker maps and link prior markers together
        coords.sort(Comparator.comparing(markerCoords::getNChr)
                .thenComparingInt(markerCoords::getNPos));
        log.log(Level.INFO, "Finished sort by origin fasta coordinates");
        
        // Fill in the linked list for each marker based on new position coordinates
        for(int x = 1; x < coords.size() - 2; x++){
            coords.get(x).setPrevMarker(coords.get(x - 1));
            coords.get(x).setNextMarker(coords.get(x + 1));
        }
        
        // Now sort by original coordinate order
        // Such a better syntax for comparisons here! 
        coords.sort(Comparator.comparing(markerCoords::getOChr)
                .thenComparingInt(markerCoords::getOPos));
        log.log(Level.INFO, "Finished sort by marker order coordinates");
        
        // Finally, merge into overlapping segments
        String lastChr = coords.get(0).oChr;
        this.mergedPlan.put(lastChr, new ArrayList<>());
        this.mergedPlan.get(lastChr).add(convertCoordToBed(coords.get(0)));
        
        for(int x = 1; x < coords.size() - 1; x++){
            markerCoords current = coords.get(x);
            if(this.mergedPlan.containsKey(current.oChr)){
                BedFastaPlan ref = this.mergedPlan.get(current.oChr)
                        .get(this.mergedPlan.get(current.oChr).size() - 1);
                BedFastaPlan query = convertCoordToBed(current);
                if(MergerUtils.checkOverlap(ref.Start(), query.Start(), ref.End(), query.Start())
                        && ref.Chr().equals(query.Chr())){
                    // Check if this is a rev comp join
                    if(MergerUtils.isCloserLeft(query.Start(), query.End(), ref.Start())){
                        // The merger is a rev comp join
                        ref.setStart(query.Start());
                        ref.isRev = true;
                    }else{
                        ref.setEnd(query.End());
                    }
                }else{
                    // No overlap, so we'll add this as a separate entry
                    this.mergedPlan.get(current.oChr).add(query);
                }
            }else{
                // Need to start a new chromosome
                this.mergedPlan.put(current.oChr, new ArrayList<>());
                this.mergedPlan.get(current.oChr).add(convertCoordToBed(current));
            }
        }
        log.log(Level.INFO, "Finished marker plan");
        
        // Generate current chromosome segment stats
        this.mergedPlan.entrySet().stream().forEach((s) -> {
            String chr = s.getKey();
            int num = s.getValue().size();
            long diffChrs = s.getValue().stream()
                    .map(BedFastaPlan::Chr)
                    .count();
            log.log(Level.INFO, "Original chr: " + chr + "\tsegments: " + num + "\tsegment chr counts: " + diffChrs);
        });
        
        //Ensuring that the list is cleared from memory
        this.coords = null;
        System.gc();
    }
    
    public void printOrderedListToFile(String outfile){
        try(BufferedWriter output = Files.newBufferedWriter(Paths.get(outfile), Charset.defaultCharset())){
            // Trying HTSJDK subsectioning test
            FastaSequenceFile reader = new FastaSequenceFile(this.origin.getPath(), true);
            
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error writing fasta entry to output file: " + outfile + "!", ex);
        }
    }
    
    private BedFastaPlan convertCoordToBed(markerCoords coord){
        BedFastaPlan value; int start = 1; int end = 1;
        if(coord.prevMarker == null || !coord.prevMarker.nChr.equals(coord.prevMarker.nChr) ){
            // Start of the chromosome
            start = 1;
        }else{
            // Average of the two coords  -1 to facilitate overlap
            start = (coord.prevMarker.nPos + coord.nPos / 2) - 1;
        }
        if(coord.nextMarker == null || !coord.nextMarker.nChr.equals(coord.nextMarker.nChr)){
            // end of the chromosome
            end = this.origin.getChrLen(coord.nChr);
        }else{
            // Average of the two coords + 1 to facilitate overlap
            end = (coord.nextMarker.nPos + coord.nPos / 2) + 1;
        }
        value = new BedFastaPlan(coord.nChr, start, end, coord.oChr, coord.oPos);
        return value;
    }
    
    protected class markerCoords{
        public final String oChr;
        public final String nChr;
        public final int oPos;
        public final int nPos;
        // linked list of previous and current markers to estimate breakpoints of new position coordinates
        public markerCoords prevMarker = null;
        public markerCoords nextMarker = null;
        
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
        
        public void setPrevMarker(markerCoords prev){
            this.prevMarker = prev;
        }
        
        public void setNextMarker(markerCoords next){
            this.nextMarker = next;
        }
        
        public String getNChr(){
            return this.nChr;
        }
        
        public int getNPos(){
            return this.nPos;
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
