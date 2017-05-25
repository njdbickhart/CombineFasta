/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import GetCmdOpt.ArrayModeCmdLineParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author dbickhart
 */
public class Validate {
    private static final Logger log = Logger.getLogger(Validate.class.getName());
    private final Path forwardFile;
    private final Path reverseFile;
    private final Boolean printFile;
    private Path outputFile;
    private final Map<TestSet, Boolean> tests = new HashMap<>();
    
    public Validate(ArrayModeCmdLineParser cmd){
        this.forwardFile = Paths.get(cmd.GetValue("forward"));
        this.reverseFile = Paths.get(cmd.GetValue("reverse"));
        this.printFile = cmd.HasOpt("output");
        if(this.printFile){
            
        }
        
        // initialize test array
        for(TestSet t : TestSet.values()){
            this.tests.put(t, Boolean.TRUE);
        }
        
        // Test if files exist
        if(!this.forwardFile.toFile().canRead()){
            
        }
            
    }
    
    private void PrintResults(){
        
    }
    
    private enum TestSet{
        filesExist, fileLineCountsMatch, forwardReadsMatch, reverseReadsMatch, 
        forwardNoJunk, reverseNoJunk
    }
}
