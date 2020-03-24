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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private Map<String, List<AGPEntry>> AGPEntries;
    private final static String nl = System.lineSeparator();
    
    public AGPToFasta(Path fastaFile, Path agpFile){
        this.fastaFile = fastaFile;
        this.agpFile = agpFile;
    }
    
    public AGPToFasta(String fastaFile, String agpFile){
        this(Paths.get(fastaFile), Paths.get(agpFile));
    }
    
    @SuppressWarnings("deprecation")
    public void GenerateFastaFromAGP(String outputFasta){
        // Load all AGP entries into a mapped list
        log.log(Level.INFO, "Beginning AGP entry loading");
        try(BufferedReader input = Files.newBufferedReader(agpFile, Charset.defaultCharset())){
            List<AGPEntry> data = input.lines()
                    .map(s -> new AGPEntry(s)).collect(Collectors.toList());
            
            // Store the AGP entries into a map by chromosome
            this.AGPEntries = data.stream()
                    .collect(Collectors.groupingBy(AGPEntry::getAChr));
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error reading input AGP file!", ex);
        }
        
        log.log(Level.INFO, "Loaded: " + this.AGPEntries.keySet().size() + "AGP chromosomes");
        
        try(BufferedWriter output = Files.newBufferedWriter(Paths.get(outputFasta), Charset.defaultCharset())){
            FastaSequenceIndex index = new FastaSequenceIndex(new File(this.fastaFile + ".fai"));
            IndexedFastaSequenceFile reader = new IndexedFastaSequenceFile(this.fastaFile, index);
            
            IndexedFastaReader dictionary = new IndexedFastaReader(this.fastaFile);
            
            
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
                for(AGPEntry b : this.AGPEntries.get(s)){
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
                        if(b.End() > dictionary.getChrLen(b.Chr())){
                            b.setEnd((int)dictionary.getChrLen(b.Chr()));
                            log.log(Level.WARNING, "Chr lengths for " + b.Chr() + " exceeded expected length, set value to: " + b.End());
                        }
                        byte[] temp = reader.getSubsequenceAt(b.Chr(), b.Start(), b.End()).getBases();
                        log.log(Level.INFO, "[AGPSUB] Pulling subsection of chr " + s + "\t" + b.Chr() + ":" + b.Start() + "-" + b.End() 
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
        public final String aChr;
        public final int aStart;
        public final int aEnd;
        public final ORIENT orient;
        public final boolean isGap;
        public int gapLen;
        
        public AGPEntry(String agpLine){
            String[] segs = agpLine.split("\t");
            
            switch(segs[4]){
                case "D":
                case "A":
                    super.chr = segs[5];
                    super.start = Integer.parseInt(segs[6]);
                    super.end = Integer.parseInt(segs[7]);

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
                    this.aChr = segs[0];
                    this.aStart = Integer.parseInt(segs[1]);
                    this.aEnd = Integer.parseInt(segs[2]);
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
