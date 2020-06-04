/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import file.BedAbstract;
import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import misassemblyLDCorrection.FastaIndexEntry;
import misassemblyLDCorrection.IndexedFastaReader;

/**
 *
 * @author dbickhart
 */
public class AGPToFasta {
    private final Logger log = Logger.getLogger(AGPToFasta.class.getName());
    private final Path fastaFile;
    private final Path agpFile;
    private int interval;
    private Map<String, List<AGPEntry>> AGPEntries;
    private final static String nl = System.lineSeparator();
    
    public AGPToFasta(Path fastaFile, Path agpFile){
        this.fastaFile = fastaFile;
        this.agpFile = agpFile;
    }
    
    public AGPToFasta(String fastaFile, String agpFile){
        this(Paths.get(fastaFile), Paths.get(agpFile));
    }
    
    // Bed file input constructor
    public AGPToFasta(String fastaFile, String bedFile, int interval){
        this(Paths.get(fastaFile), Paths.get(bedFile));
        this.interval = interval;
    }
    
    public void GenerateFastaFromBed(String outputFasta){
        // Load all bed entries into a mapped list, with intervening gaps
        log.log(Level.INFO, "Beginning Bed entry loading");
        try(BufferedReader input = Files.newBufferedReader(agpFile, Charset.defaultCharset())){
            List<AGPEntry> data = input.lines()
                    .map(s -> new AGPEntry(s, this.interval, false))
                    .collect(Collectors.toList());
            
            // sort by Chromosome so we can interleave gaps
            Map<String, List<AGPEntry>> temp = data.stream()
                    .collect(Collectors.groupingBy(AGPEntry::Chr));
            this.AGPEntries = new ConcurrentHashMap<>();
            
            for(String k : temp.keySet()){
                // Because the user did not add gaps, we need to add them and update
                // the order of the entry
                this.AGPEntries.put(k, new ArrayList<>());
                List<AGPEntry> t = temp.get(k);
                Collections.sort(t);
                int offset = 0;
                for(int x = 0; x < t.size() - 1; x++){
                    StringBuilder l = new StringBuilder(3);
                    int cstart = t.get(x).Start();
                    AGPEntry j = t.get(x);
                    j.setStart(cstart + offset);
                    j.setEnd(cstart + offset);
                    l.append(k).append("\t").append(cstart + offset + 1).append("\n");
                    // Normal, updated entry
                    this.AGPEntries.get(k).add(j);
                    // Gap entry
                    this.AGPEntries.get(k).add(new AGPEntry(l.toString(),this.interval, true));
                    offset++;
                }
                AGPEntry j = t.get(t.size() - 1);
                int cstart = j.Start() + 0;
                j.setStart(cstart + offset);
                j.setEnd(cstart + offset);
                this.AGPEntries.get(k).add(j);
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error reading input bed file!", ex);
        }
        
        log.log(Level.INFO, "Loaded: " + this.AGPEntries.keySet().size() + "AGP chromosomes");
        
        writeFasta(outputFasta);
        log.log(Level.INFO, "Finished writing fasta file from AGP!");
    }
    
    
    public void GenerateFastaFromAGP(String outputFasta){
        // Load all AGP entries into a mapped list
        log.log(Level.INFO, "Beginning AGP entry loading");
        try(BufferedReader input = Files.newBufferedReader(agpFile, Charset.defaultCharset())){
            List<AGPEntry> data = input.lines()
                    .map(s -> new AGPEntry(s)).collect(Collectors.toList());
            
            // Store the AGP entries into a map by chromosome
            this.AGPEntries = data.stream()
                    .collect(Collectors.groupingBy(AGPEntry::Chr));
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error reading input AGP file!", ex);
        }
        
        log.log(Level.INFO, "Loaded: " + this.AGPEntries.keySet().size() + "AGP chromosomes");
        
        writeFasta(outputFasta);
        log.log(Level.INFO, "Finished writing fasta file from AGP!");
    }

    @SuppressWarnings("deprecation")
    private void writeFasta(String outputFasta) {
        try(BufferedWriter output = Files.newBufferedWriter(Paths.get(outputFasta), Charset.defaultCharset())){
            File fastaIndex = new File(this.fastaFile + ".fai");
            if(! fastaIndex.exists()){
                log.log(Level.SEVERE, "Could not find the .fai index for file: " + this.fastaFile + "! Did you forget to samtools faidx it?");
                System.exit(-1);
            }
            FastaSequenceIndex index = new FastaSequenceIndex(new File(this.fastaFile + ".fai"));
            IndexedFastaSequenceFile reader = new IndexedFastaSequenceFile(this.fastaFile, index);
            
            final IndexedFastaReader dictionary = new IndexedFastaReader(this.fastaFile);
            
            // Make sure that chromosomes are in the fasta file!
            Set<String> missingChrs = this.AGPEntries.values().stream()
                    .flatMap(s -> s.stream())
                    .filter(s -> ! s.isGap)
                    .map(s -> s.aChr)
                    .filter(s -> ! dictionary.getChrNames().contains(s))
                    .collect(Collectors.toSet());
            
            if(missingChrs.size() > 0){
                log.log(Level.SEVERE, "Found chromosomes in your fasta plan that aren't in your fasta! Exiting!");
                log.log(Level.SEVERE, "Here are the offenders:");
                for(String s : missingChrs)
                    log.log(Level.SEVERE, s);
                System.exit(-1);
            }
            
            // Extract the ordered list of chromosomes (numerically)
            List<String> orderedChrs = this.AGPEntries.keySet().stream().collect(Collectors.toList());
            orderedChrs.sort((s, s1) -> {
                if(utils.MergerUtils.isNumeric(s) && utils.MergerUtils.isNumeric(s1)){
                    return Integer.compare(Integer.parseInt(s), Integer.parseInt(s1));
                }
                return s.compareTo(s1);
            });
            
            orderedChrs.stream().forEach((s) -> {
                List<Byte> seq = new ArrayList<>();
                List<AGPEntry> entries = this.AGPEntries.get(s);
                Collections.sort(entries);
                for(AGPEntry b : entries){
                    if(b.isGap){
                        // It's super messy and really silly, but the HTSJDK guys use it
                        byte[] gap = new byte[b.getGapLen()];
                        StringBuilder sb = new StringBuilder();
                        for(int x = 0; x < b.getGapLen(); x++){
                            sb.append("N");
                        }
                        sb.toString().getBytes(0, gap.length, gap, 0);
                        List<Byte> temp = new ArrayList<>(gap.length);
                        for(byte t : gap)
                            temp.add(t);
                        seq.addAll(temp);
                    }else{
                        if(b.aEnd > dictionary.getChrLen(b.aChr)){
                            b.setEnd((int)dictionary.getChrLen(b.aChr));
                            log.log(Level.WARNING, "Chr lengths for " + b.aChr + " exceeded expected length, set value to: " + b.aEnd);
                        }
                        byte[] temp = reader.getSubsequenceAt(b.aChr, b.aStart, b.aEnd).getBases();
                        log.log(Level.FINE, "[AGPSUB] Pulling subsection of chr " + s + "\t" + b.aChr + ":" + b.aStart + "-" + b.aEnd 
                                + " at current byte count: " + seq.size());
                        if(b.orient == ORIENT.REV){
                            temp = getRevComp(temp);
                        }
                        
                        for(byte t : temp)
                            seq.add(t);
                    }
                }
                // Now that the whole chromosome is in memory, print it out in an ordered fashion
                StringBuilder builder = new StringBuilder(101);
                try{
                    output.write(">" + s + nl);
                    for(int x = 0; x < seq.size(); x++){
                        builder.append(Character.toUpperCase((char)(seq.get(x) & 0xff)));
                        if(x != 0 && (x + 1) % 100 == 0){
                            builder.append(nl);
                            output.write(builder.toString());
                            builder = new StringBuilder(101);
                        }
                    }
                    if(builder.length() > 0){
                        builder.append(nl);
                        output.write(builder.toString());
                    }
                    log.log(Level.INFO, "[AGPWRITE] Wrote chromosome " + s + " size of: " + seq.size());
                }catch(IOException ex){
                    log.log(Level.SEVERE, "Error writing to chromosome segment: " + s, ex);
                }
            });
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error writing to output fasta file!", ex);
        }
    }
    
    private byte[] getRevComp(byte[] array){
        byte[] comp = new byte[array.length];
        int current = 0;
        for(int x = array.length -1; x >= 0; x--){
            char c = (char)(array[x] & 0xff);
            switch(c){
                case 'T':
                case 't':
                    comp[current++] = 'A';
                    break;
                case 'A':
                case 'a':
                    comp[current++] = 'T';
                    break;
                case 'G':
                case 'g':
                    comp[current++] = 'C';
                    break;
                case 'C':
                case 'c':
                    comp[current++] = 'G';
                    break;
                default:
                    comp[current++] = 'N';
            }
        }
        return comp;
    }
    
    public enum ORIENT{
        FWD, REV
    }
    
    
    
    private class AGPEntry extends BedAbstract{
        // The BedAbstract entries are from the original fasta file
        // Refactored -- the Super Chr, Start and End are for the final scaffold
        // The aChr, aStart and aEnd are for the fasta file that will be modified
        public final String aChr;
        public final int aStart;
        public final int aEnd;
        public final ORIENT orient;
        public final boolean isGap;
        public int gapLen;
        
        public AGPEntry(String bedLine, int interval, boolean isGap){
            String[] segs = bedLine.trim().split("\t");
            if(isGap){
                // gaps are a string: "chr\torder"
                super.chr = segs[0];
                super.start = Integer.parseInt(segs[1]);
                super.end = Integer.parseInt(segs[1]);
                this.orient = ORIENT.FWD;
                this.aChr = this.chr;
                this.aStart = this.start;
                this.aEnd = this.end;
                this.isGap = true;
                this.gapLen = interval;
            }else{
                // bed lines are a string: "contig\tstart\tend\tscaffold\torder\torient"
                super.chr = segs[3];
                super.start = Integer.parseInt(segs[4]);
                super.end = Integer.parseInt(segs[4]);
                switch(segs[5]){
                    case "+":
                        this.orient = ORIENT.FWD;
                        break;
                    case "-":
                        this.orient = ORIENT.REV;
                        break;
                    default:
                        this.orient = ORIENT.FWD;
                }
                this.aChr = segs[0];
                this.aStart = Integer.parseInt(segs[1]);
                this.aEnd = Integer.parseInt(segs[2]);
                this.isGap = false;
            }
        }
        
        public AGPEntry(String agpLine){
            String[] segs = agpLine.trim().split("\t");
            
            switch(segs[4]){
                case "D":
                case "A":
                case "W":
                    super.chr = segs[0];
                    super.start = Integer.parseInt(segs[1]);
                    super.end = Integer.parseInt(segs[2]);

                    switch(segs[8]){
                        case "+":
                            this.orient = ORIENT.FWD;
                            break;
                        case "-":
                            this.orient = ORIENT.REV;
                            break;
                        default:
                            this.orient = ORIENT.FWD;
                    }
                    this.aChr = segs[5];
                    this.aStart = Integer.parseInt(segs[6]);
                    this.aEnd = Integer.parseInt(segs[7]);
                    this.isGap = false;
                    break;
                default:
                    super.chr = segs[0];
                    super.start = Integer.parseInt(segs[1]);
                    super.end = Integer.parseInt(segs[2]);
                    this.orient = ORIENT.FWD;
                    this.aChr = this.chr;
                    this.aStart = this.start;
                    this.aEnd = this.end;
                    this.isGap = true;
                    this.gapLen = Integer.parseInt(segs[5]);
            }
        }
        
        public String getAChr(){
            return this.aChr;
        }
        
        @Override
        public int compareTo(BedAbstract t) {
            return Comparator.comparing(BedAbstract::Chr)
                    .thenComparingInt(BedAbstract::Start)
                    .compare(this, t);
        }
        
        public int getGapLen(){
            return (isGap)? this.gapLen : 0;
        }
    }
    
}
