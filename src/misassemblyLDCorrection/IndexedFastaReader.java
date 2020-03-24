/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misassemblyLDCorrection;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author dbickhart
 */
public class IndexedFastaReader {
    private final Path Input;
    private String CurHead;
    private String NextHead;
    private final List<Byte> seq = new ArrayList<>();
    private Map<String, FastaIndexEntry> indexMap;
    // A = 0, T = 1, G = 2, C =3, N = 4
    private static final char[] codes = {'A', 'T', 'G', 'C', 'N'};
    private boolean started = false;
    private static final Logger log = Logger.getLogger(IndexedFastaReader.class.getName());
    
    public IndexedFastaReader(Path Input){
        this.Input = Input;
        File index = new File(Input.toString() + ".fai");
        if(!this.Input.toFile().canRead()){
            log.log(Level.SEVERE, "Could not find input fasta file! Exiting...");
            System.exit(-1);
        }
        if(!index.canRead()){
            log.log(Level.SEVERE, "Could not find fasta index file for: " + Input.toString() + "!");
            System.exit(-1);
        }
        
        try(BufferedReader input = Files.newBufferedReader(Paths.get(index.toURI()), Charset.defaultCharset())){
            indexMap = input.lines().map((s) -> new FastaIndexEntry(s))
                    .collect(Collectors.toMap(x -> x.getName(),x -> x));
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error opening fasta index file!", ex);
            System.exit(-1);
        }
    }
    
    private enum FaiState{
        START, // Default state
        CHRINTERNAL, // Has a contig name
        CHRCOUNT, // Need to count sequence lines
        PREVSPACE; // error flag to ensure that there are no double-spaces in the fasta
    }
    
    public int GenerateIndex(Path Input, File index){
        long len = 0, lineLen = -1, lineBpLen = -1; 
        long offset = 0;
        String contig = null;
        FaiState state = FaiState.START;
        final String nl = System.lineSeparator();
        try(BufferedReader input = Files.newBufferedReader(Input, Charset.defaultCharset())){
            String line = null;
            while((line = input.readLine()) != null){
                if(line.equals(nl)){
                    // An empty line is only tolerated in the middle of
                    if(state == FaiState.START)
                        IndexFailure("Error! The fasta file is not allowed to start with an empty line!");
                    else if(state == FaiState.PREVSPACE)
                        IndexFailure("Error! Identified two empty lines in a row! Please check to see if the file is not malformed.");
                    else{
                        state = FaiState.PREVSPACE;
                        offset += nl.length();
                    }
                }
                
                if(line.startsWith(">")){
                    if(state == FaiState.CHRINTERNAL){
                        FastaIndexEntry temp = new FastaIndexEntry(contig, len, offset, lineBpLen, lineLen);
                        this.indexMap.put(contig, temp);
                        log.log(Level.FINE, "Generated index entry: " + temp.toString());
                    }
                    offset += len + line.length();
                    String[] segs = line.trim().split("\\s+");
                    contig = segs[0].replaceFirst(">", "");
                    state = FaiState.CHRINTERNAL;
                }else if(state == FaiState.START)
                    IndexFailure("Error! Did not encounter a valid fasta header sequence! The first line consisted of: " + line);
                
                
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error in reading fasta file during index generation!", ex);
        }
        return 0;
    }
    
    private void IndexFailure(String message){
        log.log(Level.SEVERE, message);
        System.exit(-1);
    }
    
    public void LoadEntry(String chr, int start, int end){
        try(RandomAccessFile fasta = new RandomAccessFile(this.Input.toFile(), "r")){
            FastaIndexEntry entry = this.indexMap.get(chr);
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error subsectioning fasta entry on coordinates: " + chr + ":" + start + "-" + end, ex);
            System.exit(-1);
        }
    }
    
    public void LoadEntry(String chr){
        try(RandomAccessFile fasta = new RandomAccessFile(this.Input.toFile(), "r")){
            FastaIndexEntry entry = this.indexMap.get(chr);
            int newlines = (int)(entry.length / 60); 
            byte[] len = new byte[newlines + (int)entry.length];
                        
            int readBytes = fasta.read(len, (int)entry.startByte, newlines + (int)entry.length);
            if(readBytes != newlines + entry.length)
                throw new IOException("Error accessing fasta entry via chr loading!");
            
            // bypass newlines and convert bytes to byte codes
            for(int x = 0; x < len.length; x+= entry.lineLen + 1){
                for(int y = x; y < entry.lineLen + x; y++){
                    this.seq.add(convertNucToCode((char)len[y]));
                }
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error accessing fasta file for reading chromosome: " + chr + "!", ex);
            System.exit(-1);
        }
    }
    public void LoadEntry(){
        try(BufferedReader input = Files.newBufferedReader(Input, Charset.defaultCharset())){
            String line = null;
            if(!started){
                line = input.readLine();
                line = line.trim().replace(">", "");
                this.CurHead = line;
                started = true;
            }else{
                this.CurHead = this.NextHead;
            }
            
            while((line = input.readLine()) != null){
                line = line.trim();
                if(line.startsWith(">")){
                    this.NextHead = line.replace(">", "");
                    break;
                }
                for(int x = 0; x < line.length(); x++)
                    seq.add(convertNucToCode(line.charAt(x)));
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error reading fasta file!", ex);
        }
    }
    
    private byte convertNucToCode(char c){
        switch(c){
            case 'a':
            case 'A':
                return (byte) 0;
            case 't':
            case 'T':
                return (byte) 1;
            case 'g':
            case 'G':
                return (byte) 2;
            case 'c':
            case 'C':
                return (byte) 3;
            default:
                return (byte) 4;
        }
    }
        
    public long getChrLen(String chr){
        return this.indexMap.get(chr).length;
    }
    
    public Set<String> getChrNames(){
        return this.indexMap.keySet();
    }
    
    public String getHead(){
        return this.CurHead;
    }
    
    public Path getPath(){
        return this.Input;
    }
}
