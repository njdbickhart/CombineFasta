/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import GetCmdOpt.ArrayModeCmdLineParser;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dbickhart
 */
public class Standardize {
    private final Logger log = Logger.getLogger(Standardize.class.getName());
    private BufferedWriter output;
    private final String fasta;
    private final boolean changeName;
    private final String format;
    
    public Standardize(ArrayModeCmdLineParser cmd){
        this.output = null;
        try{
            this.output = Files.newBufferedWriter(Paths.get(cmd.GetValue("output")), Charset.defaultCharset());
        }catch(IOException ex){
            log.log(Level.SEVERE, "Fatal error writing to output fasta file: " + cmd.GetValue("output") + "!", ex);
        }
        
        this.fasta = cmd.GetValue("fasta");
        if(cmd.HasOpt("format")){
            this.format = cmd.GetValue("format");
            this.changeName = true;
        }else{
            this.format = "";
            this.changeName = false;
        }
    }
    
    public void run(){
        int counter = 0;
        try{
            BufferedFastaReaderWriter workhorse = new BufferedFastaReaderWriter(fasta, this.changeName);
            int retVal = 0;
            while((retVal = workhorse.readToNextChr(output, format)) != -1){
                counter++;
            }
            workhorse.close();
            output.close();
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error reading input fasta file!", ex);
        }
        
        log.log(Level.INFO, "Processed: " + counter + " Fasta entries.");
    }
}
