/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import GetCmdOpt.ArrayModeCmdLineParser;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dbickhart
 */
public class CombineFasta {
    private static final String version = "0.0.1";
    private static final Logger log = Logger.getLogger(CombineFasta.class.getName());
    
    private static ArrayModeCmdLineParser PrepareCMDOptions(){
        String nl = System.lineSeparator();
        ArrayModeCmdLineParser cmd = new ArrayModeCmdLineParser("CombineFasta: a simple tool to join/merge fasta files" + nl +
                "Version: " + version + nl +
                "Usage: java -jar CombineFasta.jar [mode] [mode options]" + nl +
                "\tModes:" + nl +
                "\t\torder\tCombine and orient separate fasta files" + nl, 
        "order");
        
        cmd.AddMode("order", 
                "CombineFasta order:" + nl +
                        "Usage: java -jar CombineFasta.jar order -i [comma sep input] -d [comma sep orient] -o [output fasta]" + nl +
                "\t-i\tInput fasta files, separated by commas" + nl + 
                        "\t-d\tFasta file orientations, separated by commas" + nl + 
                        "\t-o\tOutput fasta file name" + nl, 
                "i:d:o:", 
                "ido", 
                "ido", 
                "input", "direction", "output");
        
        return cmd;        
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ArrayModeCmdLineParser cmd = PrepareCMDOptions();
        cmd.GetAndCheckMode(args);
        log.setLevel(Level.INFO);
        
        log.log(Level.INFO, "CombineFasta version: " + version);
        
        // identify and run  mode
        switch(cmd.CurrentMode){
            case "order":
                log.log(Level.INFO, "Mode order selected");
                Order order = new Order(cmd.GetValue("input"), cmd.GetValue("direction"), cmd.GetValue("output"));
                order.GenerateFasta();
                break;
            default:
                log.log(Level.SEVERE, "Error! Must designate a valid mode to continue!");
                System.exit(-1);
        }
        
        log.log(Level.INFO, "CombineFasta mode finished: " + cmd.CurrentMode);
        System.exit(0);
    }
    
}
