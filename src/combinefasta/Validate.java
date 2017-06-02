/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import GetCmdOpt.ArrayModeCmdLineParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import utils.BloomFilter;

/**
 *
 * @author dbickhart
 */
public class Validate {
    private static final Logger log = Logger.getLogger(Validate.class.getName());
    private final Path forwardFile;
    private Path reverseFile;
    private final Boolean printFile;
    private final TestType category;
    private Path outputFile;
    private final Map<TestSet, Boolean> tests = new HashMap<>();
    private final Map<FileCat, Path> files = new HashMap<>();
    
    private Map<FileCat, Boolean> multFour = new ConcurrentHashMap<>();
    private Map<FileCat, statCounts> lineCount;
    private final Map<FileCat, BloomFilter> bloom = new ConcurrentHashMap<>();
    private String comment; 
    
    public Validate(ArrayModeCmdLineParser cmd){
        this.forwardFile = Paths.get(cmd.GetValue("forward"));
        
        // Check test category
        if(cmd.HasOpt("reverse")){
            this.reverseFile = Paths.get(cmd.GetValue("reverse"));
            this.category = TestType.pair;
        }else{
            this.category = TestType.single;
        }
        
        this.printFile = cmd.HasOpt("output");
        if(this.printFile){
            
        }
        
        // initialize test array
        for(TestSet t : TestSet.values()){
            this.tests.put(t, Boolean.FALSE);
        }
        
        switch(this.category){
            case pair:
                log.log(Level.INFO, "Paired end validation module start.");
                files.put(FileCat.forward, this.forwardFile);
                files.put(FileCat.reverse, reverseFile);
                ValidatePair();
                break;
            case single:
                log.log(Level.INFO, "Single end validation module start.");
                ValidateSingle();
                break;
        }
            
    }
    
    private void ValidateSingle(){
        
    }
    private void ValidatePair(){
        // Test if files exist
        if(this.forwardFile.toFile().canRead() && this.reverseFile.toFile().canRead()){
            this.tests.put(TestSet.filesExist, Boolean.TRUE);
        }else{
            this.comment = "FAILED. One or more files did not exist.";
            PrintResults();
        }
        
        // Count first file lines and bases
        this.lineCount = this.files.entrySet().parallelStream()
                .map(p -> countLines(p.getValue(), p.getKey()))
                .collect(Collectors.toMap(statCounts::getType, p -> p));
        
        // Check if we had any unexpected file terminations
        boolean terminated = this.multFour.values().contains(false);
        
        if(terminated){
            boolean notBoth = this.multFour.values().contains(true);
            if(notBoth){
                this.comment = "FAILED. One file ended unexpectedly.";
                this.multFour.entrySet().forEach(p -> {
                    if(p.getValue()){
                        switch(p.getKey()){
                            case forward:
                                this.tests.put(TestSet.forwardNoJunk, true);
                                break;
                            case reverse:
                                this.tests.put(TestSet.reverseNoJunk, true);
                                break;
                        }
                    }                        
                });
                PrintResults();
            }else{
                this.comment = "FAILED. Both files ended unexpectedly.";
                PrintResults();
            }
        }
        
        // Checking to see if the file read names match
    }
    
    private void PrintResults(){
        
    }
    
    private boolean readNamesMatch(Path forward, Path reverse){
        try(BufferedReader forFile = gziputils.ReaderReturn.openFile(forward.toFile())){
            BufferedReader revFile = gziputils.ReaderReturn.openFile(reverse.toFile());
            
            String head1, head2, seq, plus, qual;
            while((head1 = forFile.readLine())!= null){
                head2 = revFile.readLine();
                
                head1 = head1.trim();
                head2 = head2.trim();
                
                
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error accessing files to check read name synchronization!", ex);
        }
        return true;
    }
    
    private statCounts countLines(Path file, FileCat cat){
        Long tLineCount = 0l;
        Long tBaseCount = 0l;
        
        // Set test for four count lines
        this.multFour.put(cat, Boolean.TRUE);
        
        statCounts vals = new statCounts(cat);
        
        String head, seq, plus, qual;
        try(BufferedReader input = gziputils.ReaderReturn.openFile(file.toFile())){
            while((head = input.readLine()) != null){
                seq = input.readLine();
                plus = input.readLine();
                qual = input.readLine();
                
                tLineCount++;

                if(seq == null || plus == null || qual == null){
                    this.multFour.put(cat, Boolean.FALSE);
                    log.log(Level.WARNING, "Unexpected end of fastq file, for file: " + file.toString());
                    vals.baseCount = tBaseCount;
                    vals.lineCount = tLineCount;
                    return vals;
                }
                
                seq = seq.trim();
                tBaseCount += seq.length();
            }
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error counting lines in file: " + file.toString() + " which was the " + cat.toString() + " file.", ex);
        }
        
        log.log(Level.INFO, "Finished counting lines in file: " + file.toString() + " which was the " + cat.toString() + "file.");
        
        vals.baseCount = tBaseCount;
        vals.lineCount = tLineCount;
        return vals;
    }
    
    private class statCounts{
        public final FileCat type;
        public long lineCount = 0l;
        public long baseCount = 0l;
        
        public statCounts(FileCat cat){
            this.type = cat;
        }
        
        public FileCat getType(){
            return type;
        }
    }
    
    private enum FileCat{
        forward, reverse
    }
    
    private enum TestType{
        single, pair
    }
    
    private enum TestSet{
        filesExist, lineCountComplete, fileLineCountsMatch, readsMatch, 
        forwardNoJunk, reverseNoJunk
    }
}
