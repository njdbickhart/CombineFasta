/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misassemblyLDCorrection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author dbickhart
 */
public class KmerRepeatClassifier {
    private static final Logger log = Logger.getLogger(KmerRepeatClassifier.class.getName());
    private final String jellyfishDB;
    
    public KmerRepeatClassifier(String jellydb){
        this.jellyfishDB = jellydb;
    }
    
    //Read in kmers and separate into separate random access files to prevent memory overload
    
    public Integer RefineStartCoord(String seq){
        List<Integer> coords = jellyfishWrapper(generateSectionedKmers(seq));
        int idx = -1;
        for(int x = 0; x < coords.size(); x++){
            if(coords.get(x) > 10){
                idx = x;
                break;
            }
        }
        return idx;
    }
    
    public Integer RefineEndCoord(String seq){
        List<Integer> coords = jellyfishWrapper(generateSectionedKmers(seq));
        int idx = -1;
        for(int x = coords.size() -1; x >= 0; x--){
            if(coords.get(x) > 10){
                idx = x;
                break;
            }
        }
        return idx;
    }
    
    private List<Integer> jellyfishWrapper(String query){
        ProcessBuilder builder = new ProcessBuilder();
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
        if(isWindows){
            builder.command("cmd.exe", "jellyfish", "query", this.jellyfishDB, query);
        }else{
            builder.command("sh", "jellyfish", "query", this.jellyfishDB, query);
        }
        List<Integer> values = null;
        
        try{
           Process process = builder.start(); 
           KmerConsumer consumer = new KmerConsumer(process.getInputStream());
           Future<List<Integer>> rets = Executors.newSingleThreadExecutor().submit(consumer);
           values = rets.get();
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error running jellyfish executable!", ex);
        }catch(InterruptedException|ExecutionException zx){
            log.log(Level.SEVERE, "Error retrieving returned integer values from consumer!", zx);
        }
        
        return values;
    }
    
    private String generateSectionedKmers(String seq){
        String[] segs = seq.split("(?<=\\G.{21})");
        return StrUtils.StrArray.Join(segs, " ");
    }
    
    private static class KmerConsumer implements Callable<List<Integer>>{
        private static final Logger log = Logger.getLogger(KmerConsumer.class.getName());
        private InputStream inputStream;
        
        public KmerConsumer(InputStream inputstream){
            this.inputStream = inputstream;
        }
        
        @Override
        public List<Integer> call() throws Exception {
            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .filter(s -> !s.startsWith("Invalid"))
                    .map((s) -> {
                        String segs[] = s.split(" ");
                        return Integer.parseInt(segs[1]);
                    })
                    .collect(Collectors.toList());
        }
    }
}
