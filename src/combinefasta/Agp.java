/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import GetCmdOpt.ArrayModeCmdLineParser;
import utils.AGPToFasta;

/**
 *
 * @author dbickhart
 */
public class Agp {
    private final String fasta;
    private final String agp;
    private final String output;
    public Agp(ArrayModeCmdLineParser cmd){
        this.fasta = cmd.GetValue("fasta");
        this.agp = cmd.GetValue("agp");
        this.output = cmd.GetValue("output");
    }
    
    public void run(){
        AGPToFasta workhorse = new AGPToFasta(this.fasta, this.agp);
        
        workhorse.GenerateFastaFromAGP(output);
    }
}
