/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import GetCmdOpt.ArrayModeCmdLineParser;
import java.util.logging.Level;
import java.util.logging.Logger;
import misassemblyLDCorrection.RearrangementPlan;

/**
 *
 * @author dbickhart
 */
public class Missassembly {
    private static final Logger log = Logger.getLogger(Missassembly.class.getName());
    private final String samFile;
    private final String fastaFile;
    private final String jellyDB;
    private final String outbase;
    //"sam", "fasta", "jellydb", "output",
    public Missassembly(ArrayModeCmdLineParser cmd){
        this.samFile = cmd.GetValue("sam");
        this.fastaFile = cmd.GetValue("fasta");
        this.jellyDB = cmd.GetValue("jellydb");
        this.outbase = cmd.GetValue("output");
    }
    
    public void Run(){
        RearrangementPlan rearrange = new RearrangementPlan(this.samFile, this.fastaFile);
        
        log.log(Level.INFO, "Beginning marker plan mapping");
        rearrange.CreateMarkerPlan();
        
        //log.log(Level.INFO, "Starting refinement routine");
        //rearrange.refineEdges(jellyDB);
        
        log.log(Level.INFO, "Printing out AGP file");
        rearrange.printOrderedListToAGP(this.outbase + ".agp");
    }
}
