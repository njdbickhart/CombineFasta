/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author dbickhart
 */
public class FastaReader {
    private final Path Input;
    private String CurHead;
    private String NextHead;
    private final List<String> seq = new ArrayList<>();
    private boolean started = false;
    private static final Logger log = Logger.getLogger(FastaReader.class.getName());
    
    public FastaReader(Path Input){
        this.Input = Input;
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
                seq.addAll(Arrays.asList(line.split("")));
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error reading fasta file!", ex);
        }
    }
    
    public List<String> getSeq(){
        return this.seq;
    }
    
    public List<String> getRevComp(){
        List<String> comp = this.seq.stream().sequential().map(d -> {
            switch(d){
                case "a":
                case "A":
                    return "T";
                case "t":
                case "T":
                    return "A";
                case "c":
                case "C":
                    return "G";
                case "g":
                case "G":
                    return "C";
                default:
                    return "N";                
            }
        }).collect(Collectors.toList());
        Collections.reverse(comp);
        return comp;
    }
    
    public String getHead(){
        return this.CurHead;
    }
}
