/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import gziputils.ReaderReturn;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import utils.BloomFilter;
import utils.FastqFileQuickSort;
import utils.FastqFileQuickSort.QuickSortOutput;

/**
 *
 * @author dbickhart
 */
public class Pair {
    private static final Logger log = Logger.getLogger(Pair.class.getName());
    private final Path forwardFile;
    private final Path reverseFile;
    private final String outbase;
    private final String NL = System.lineSeparator();
    
    public Pair(String forward, String reverse, String outbase){
        this.forwardFile = Paths.get(forward);
        this.reverseFile = Paths.get(reverse);
        
        if(!this.forwardFile.toFile().canRead())
            log.log(Level.SEVERE, "Could not read forward fastq file! Does it exist?");
        if(!this.reverseFile.toFile().canRead())
            log.log(Level.SEVERE, "Could not read reverse fastq file! Does it exist?");
        
        this.outbase = outbase;
    }
    
    public void run(){
        // Print the entries to temp files on single lines for easier sorting
        /*File tempFor = new File(this.outbase + ".f.temp");
        File tempRev = new File(this.outbase + ".r.temp");
        
        tempFor.deleteOnExit();
        tempRev.deleteOnExit();
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        executor.execute(new GenerateTempFiles(this.forwardFile, tempFor));
        executor.execute(new GenerateTempFiles(this.reverseFile, tempRev));
        
        executor.shutdown();
        try {
        executor.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException ex) {
        log.log(Level.SEVERE, "Error terminating temp file thread generation pool!", ex);
        }
        log.log(Level.INFO, "Completed initial temporary file generation");*/
        
        // Sort the files in-place and add read names to Bloom filter
        Map<String, Path> preSortFiles = new HashMap<>();

        preSortFiles.put("for", this.forwardFile);
        preSortFiles.put("rev", this.reverseFile);
        
        Map<String, QuickSortOutput> postSortOutput = preSortFiles.entrySet().parallelStream().map((f) -> {
                FastqFileQuickSort t = new FastqFileQuickSort("\t", new int[]{0}, f.getValue().toString());
                try{
                    t.splitChunks(f.getValue(), f.getKey());
                    t.mergeChunks();
                }catch(IOException ex){
                    log.log(Level.SEVERE, "Could not split text file: " + f.getValue().toString(), ex);
                }
                return t.getOutput();
            }).collect(Collectors.toMap(p -> p.id, Function.identity()));
        
        log.log(Level.INFO, "Completed sort routine");
        
        // Output reads to proper fastq format files
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        executor.execute(new FilterSortedTextFile(postSortOutput.get("for").output, 
                this.outbase + ".1.fastq", 
                postSortOutput.get("rev").bloom));
        executor.execute(new FilterSortedTextFile(postSortOutput.get("rev").output, 
                this.outbase + ".2.fastq", 
                postSortOutput.get("for").bloom));
        executor.shutdown();
        try {
            executor.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException ex) {
            log.log(Level.SEVERE, "Error terminating fastq file output generation pool!", ex);
        }
        
        log.log(Level.INFO, "Completed fastq output routine");

        // Parity check read names from the files to ensure everything matches
        File first = new File(this.outbase + ".1.fastq");
        File second = new File(this.outbase + ".2.fastq");
        
        if(!first.canRead())
            log.log(Level.SEVERE, "Error! Cannot read forward output fastq file!");
        if(!second.canRead())
            log.log(Level.SEVERE, "Error! Cannot read reverse output fastq file!");
        
        
    }
    
    protected class FilterSortedTextFile implements Runnable{
        private final Path inputSortFile;
        private final Path outputFastq;
        private final BloomFilter<String> bloom;
        
        public FilterSortedTextFile(Path inputSortFile, String outbase, BloomFilter bloom){
            this.inputSortFile = inputSortFile;
            this.outputFastq = Paths.get(outbase);
            this.bloom = bloom;
        }

        @Override
        public void run() {
            BufferedWriter output = null;
            long linesFiltered = 0, linesWritten = 0;
            try(BufferedReader input = Files.newBufferedReader(inputSortFile, Charset.defaultCharset())){
                output = Files.newBufferedWriter(outputFastq, Charset.defaultCharset());
                String line;
                while((line = input.readLine()) != null){
                    line = line.trim();
                    String[] segs = line.split(" ");
                    if(bloom.contains(segs[0])){
                        String[] tsegs = line.split("\t");
                        output.write(StrUtils.StrArray.Join(tsegs, NL));
                        output.write(NL);
                        linesWritten++;
                    }else{
                        linesFiltered++;
                    }
                }
            }catch(IOException ex){
                log.log(Level.SEVERE, "Error reading from sorted temp file!", ex);
            }finally{
                try{
                    output.close();
                }catch(IOException ex){
                    log.log(Level.SEVERE, "Error closing output file!", ex);
                }
            }
            log.log(Level.INFO, "Finished writing file: " 
                    + inputSortFile.toString() + ". Wrote: " + linesWritten 
                    + " lines and filtered: " + linesFiltered + " one-sided reads");
        }
    }

    protected class GenerateTempFiles implements Runnable{
        private final Path inputFile;
        private final File tempFor;
    
        public GenerateTempFiles(Path inputFile, File tempFor){
            this.inputFile = inputFile;
            this.tempFor = tempFor;
        }

        @Override
        public void run() {
            BufferedWriter temp = null;
            BufferedReader input = null;
            try{
                temp = Files.newBufferedWriter(tempFor.toPath(), Charset.defaultCharset());
                input = ReaderReturn.openFile(inputFile.toFile());
                String head, seq, plus, qual;
                while((head = input.readLine())!= null){
                    seq = input.readLine();
                    plus = input.readLine();
                    qual = input.readLine();

                    head = head.trim();
                    seq = seq.trim();
                    plus = plus.trim();
                    qual = qual.trim();

                    temp.write(head + "\t" + seq + "\t" + plus + "\t" + qual + NL);
                }
            }catch(IOException ex){
                log.log(Level.SEVERE, "Error reading from" + inputFile.toString() + " file to temporary file!", ex);
            }finally{
                try{
                    temp.close();
                    input.close();
                }catch(IOException ex){
                    log.log(Level.SEVERE, "Error closing files!", ex);
                }            
            }
        }
        
    }
}
