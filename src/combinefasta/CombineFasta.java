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
    private static final String version = "0.0.6";
    private static final Logger log = Logger.getLogger(CombineFasta.class.getName());
    
    private static ArrayModeCmdLineParser PrepareCMDOptions(){
        String nl = System.lineSeparator();
        ArrayModeCmdLineParser cmd = new ArrayModeCmdLineParser("CombineFasta: a simple tool to join/merge fast[a/q] files" + nl +
                "Version: " + version + nl +
                "Usage: java -jar CombineFasta.jar [mode] [mode options]" + nl +
                "\tModes:" + nl +
                "\t\torder\tCombine and orient separate fasta files" + nl +
                "\t\tpair\tRestore jumbled paired end fastq files" + nl +
                "\t\tstandardize\tMake fasta lines standard in a file" + nl, 
        "order", "pair");
        
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
        
        cmd.AddMode("pair", 
                "CombineFasta pair:" + nl +
                        "Usage: java -jar CombineFasta.jar pair -f [input forward read fastq] -r [input reverse read fastq] -o [output base name]" + nl +
                        "\t-f\tInput forward read fastq file" + nl+
                        "\t-r\tInput reverse read fastq file" + nl +
                        "\t-o\tOutput base read name [reads are output with a '.1.fastq' and '.2.fastq' extension]" + nl, 
                "f:r:o:d|", 
                "fro", 
                "frod", 
                "forward", "reverse", "output", "debug");
        
        cmd.AddMode("validate", 
                "CombineFasta validate:" + nl +
                        "Usage: java -jar CombineFasta.jar validate -f [input forward read fastq] -r [input reverse read fastq] -o [OPTIONAL: print validation stats to file]" + nl +
                        "\t-f\tInput forward read fastq file" + nl+
                        "\t-r\tInput reverse read fastq file" + nl +
                        "\t-o\tOutput validation stats file [if not specified, validation stats are sent to STDOUT]" + nl, 
                "f:r:o:d|", 
                "fr", 
                "frod", 
                "forward", "reverse", "output", "debug");
        
        cmd.AddMode("standardize", 
                "CombineFasta standardize:" + nl +
                        "Usage: java -jar CombineFasta.jar standardize -f [input fasta file] -o [output fasta file] -r [OPTIONAL: remove this suffix from fasta file entries]" + nl +
                        "\t-f\tInput fasta file" + nl+
                        "\t-r\tSuffix to remove from fasta entries" + nl +
                        "\t-o\tOutput fasta file with corrected settings" + nl, 
                "f:r:o:d|", 
                "fo", 
                "frod", 
                "fasta", "format", "output", "debug");
        
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
            case "pair":
                log.log(Level.INFO, "Mode pair selected");
                Pair pair = new Pair(cmd.GetValue("forward"), cmd.GetValue("reverse"), cmd.GetValue("output"));
                pair.run();
                break;
            case "standardize":
                log.log(Level.INFO, "Mode standardize selected");
                Standardize standard = new Standardize(cmd);
                standard.run();
                break;
            default:
                log.log(Level.SEVERE, "Error! Must designate a valid mode to continue!");
                System.exit(-1);
        }
        
        log.log(Level.INFO, "CombineFasta mode finished: " + cmd.CurrentMode);
        System.exit(0);
    }
    
}
