/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import GetCmdOpt.ArrayModeCmdLineParser;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.AGPToFasta;

/**
 *
 * @author dbickhart
 */
public class Agp {
    private final Logger log = Logger.getLogger(Agp.class.getName());
    private final String fasta;
    private String agp;
    private final String output;
    private final boolean isBed;
    private String bed;
    private int interval;
    
    public Agp(ArrayModeCmdLineParser cmd){
        this.fasta = cmd.GetValue("fasta");
        this.output = cmd.GetValue("output");
        if(cmd.HasOpt("agp")){
            this.agp = cmd.GetValue("agp");
            this.isBed = false;
        }else{
            if(! cmd.HasOpt("bed")){
                log.log(Level.SEVERE, "Must specify either bed (-b) or agp (-a) input! Exiting...");
                System.exit(-1);
            }else{
                this.bed = cmd.GetValue("bed");
                this.interval = (cmd.HasOpt("interval"))? Integer.parseInt(cmd.GetValue("interval")) : 100;
            }
            this.isBed = true;
        }
    }
    
    public void run(){
        if(this.isBed){
            AGPToFasta workhorse = new AGPToFasta(this.fasta, this.bed, this.interval);
            
            workhorse.GenerateFastaFromBed(output);
        }else{
            AGPToFasta workhorse = new AGPToFasta(this.fasta, this.agp);

            workhorse.GenerateFastaFromAGP(output);
        }
    }
}
