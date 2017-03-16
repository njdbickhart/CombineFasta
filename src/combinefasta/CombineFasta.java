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
    private static final String version = "0.0.3";
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
                        "Usage: java -jar CombineFasta.jar order -i [tab delim input] -o [output fasta] -p [padding bases]" + nl +
                "\t-i\tInput fasta files in tab delimited format with orientations in second column" + nl +  
                        "\t-o\tOutput fasta file name" + nl +
                        "\t-p\tNumber of N bases to pad fasta entries" + nl, 
                "i:o:p:d|", 
                "io", 
                "iopd", 
                "input", "output", "padding", "debug");
        
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
                int padding = -1;
                if(cmd.HasOpt("padding"))
                    padding = Integer.parseInt(cmd.GetValue("padding"));
                Order order = new Order(cmd.GetValue("input"), cmd.GetValue("output"), padding);
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
