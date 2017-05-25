/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import StrUtils.StrArray;
import gziputils.ReaderReturn;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that can be used to sort a large file by splitting said file into several temporary sorted files and 
 * merging those files.
 * Changed to allow line segment sorting
 * ##ADDED## new bloom filter feature
 * @author Greg Cope
 *
 */
public class FastqFileQuickSort {
        private final Comparator<String[]> sorter;
	private int maxChunkSize = 1000000000;
	private List<File> outputs = new ArrayList<>();
        // Changed temp dir to current working directory
	private String tempDirectory = Paths.get("").toAbsolutePath().toString(); 
        private final String delimiter;
        private final int[] colOrder;
        private boolean hasData = false;
        
        private static final Logger log = Logger.getLogger(FastqFileQuickSort.class.getName());
        private Path tempFile;
        private long lineCount = 0;
        private BloomFilter<String> bloom;
        private final double fp = 1.0E-15;
        private String identifier;

        public FastqFileQuickSort(String delimiter, int[] colOrder, String tmpoutbase){
		this.sorter = new ComparatorDelegate(colOrder);
                this.delimiter = delimiter;
                this.colOrder = colOrder;
                this.createTemp(Paths.get(tmpoutbase));
	}
        
	public FastqFileQuickSort(Comparator<String[]> sorter, String delimiter, int[] colOrder, String tmpoutbase){
		this.sorter = sorter;
                this.delimiter = delimiter;
                this.colOrder = colOrder;
                this.createTemp(Paths.get(tmpoutbase));
	}

	/**
	 * Sets the temporary directory
	 * @param temp
	 */
	public void setTempDirectory(String temp){
		tempDirectory = temp;
		File file = new File(tempDirectory);
		if ( !file.exists() || !file.isDirectory() ){
			throw new IllegalArgumentException("Parameter director is not a directory or does not exist");
		}
	}

	/**
	 * Sets the chunk size for temporary files
	 * @param size
	 */
	public void setMaximumChunkSize(int size){
		this.maxChunkSize = size;
	}	

	/**
	 * Reads the input io stream and splits it into sorted chunks which are written to temporary files. 
	 * @param in
         * @param identifier
	 * @throws IOException
	 */
	public void splitChunks(Path inFile, String identifier){
            outputs.clear();
            this.identifier = identifier;
            BufferedReader br = null;
            List<String[]> lines = new ArrayList<>(maxChunkSize);
            log.log(Level.INFO, "[TXTFILESORT] Beginning sort routine for bin: " + identifier);
            try{
                br = ReaderReturn.openFile(inFile.toFile());
                //String line = null;
                int currChunkSize = 0;
                String head, seq, plus, qual;
                while ((head = br.readLine() ) != null ){
                    seq = br.readLine();
                    plus = br.readLine();
                    qual = br.readLine();
                    
                    if(seq == null || plus == null || qual == null){
                        log.log(Level.INFO, "[TXTFILESORT] Premature fastq file end for file: " + inFile.toString());
                        break;
                    }

                    head = head.trim();
                    seq = seq.trim();
                    plus = plus.trim();
                    qual = qual.trim();

                    this.hasData = true;
                    this.lineCount++;
                    lines.add(new String[]{head, seq, plus, qual});
                    currChunkSize += head.length() + seq.length() + plus.length() + qual.length() + 1;
                    
                    if ( currChunkSize >= maxChunkSize ){
                        currChunkSize = 0;
                        Collections.sort(lines, sorter);
                        double rand = Math.random();
                        String tmpfile = tempDirectory + "/tempsplit" + System.currentTimeMillis() + "." + rand;
                        File file = new File(tmpfile);
                        file.deleteOnExit();
                        log.log(Level.INFO, "[TXTFILESORT] Created new chunk temp file: " + tmpfile + " for bin: " + identifier);
                        outputs.add(file);
                        writeOut(lines, new FileOutputStream(file));
                        lines.clear();
                    }
                }
                //write out the remaining chunk
                Collections.sort(lines, sorter);
                double rand = Math.random();
                String tmpfile = tempDirectory + "/tempsplit" + System.currentTimeMillis() + "." + rand;
                File file = new File(tmpfile);
                file.deleteOnExit();
                log.log(Level.FINE, "[TXTFILESORT] Created new chunk temp file: " + tmpfile + " for bin: " + identifier);
                outputs.add(file);
                writeOut(lines, new FileOutputStream(file));
                log.log(Level.FINE, "[TXTFILESORT] Finished split chunk routine. Had files? " + this.hasData);
                lines.clear();
            }catch(IOException io){
                log.log(Level.SEVERE, "[TXTFILESORT] Error reading from inputstream: " + inFile.toString(), io);
            }finally{
                if ( br != null )try{br.close();}catch(Exception e){
                    log.log(Level.SEVERE, "[TXTFILESORT] Error closing inputstream: " + inFile.toString(), e);
                }
            }
	}

	/**
	 * Writes the list of lines out to the output stream, append new lines after each line.
	 * @param list
	 * @param os
	 * @throws IOException
	 */
	private void writeOut(List<String[]> list, OutputStream os) throws IOException{
            BufferedWriter writer = null;
            try{
                writer = new BufferedWriter(new OutputStreamWriter(os));
                for ( String[] s : list ){
                        writer.write(StrArray.Join(s, delimiter));
                        writer.write(System.lineSeparator());
                }
                writer.flush();
            }catch(IOException io){
                log.log(Level.SEVERE, "[TXTFILESORT] Error writing to outputstream: " + os.toString(), io);
            }finally{
                if ( writer != null ){
                    try{writer.close();}catch(Exception e){
                        log.log(Level.SEVERE, "[TXTFILESORT] Error closing output stream!", e);
                    }
                }
            }
	}

	/**
	 * Reads the temporary files created by splitChunks method and merges them in a sorted manner into the output stream.
	 * @param os
	 * @throws IOException
	 */
	public void mergeChunks() throws IOException{
            OutputStream os = new FileOutputStream(this.tempFile.toFile());
            int lineC = this.convertLongToInt(this.lineCount);
            this.bloom = new BloomFilter<>(fp, lineC);
            
		Map<StringWrapper, BufferedReader> map = new HashMap<>();
		List<BufferedReader> readers = new ArrayList<>();
		BufferedWriter writer = null;
		//ComparatorDelegate delegate = new ComparatorDelegate();
		try{
			writer = new BufferedWriter(new OutputStreamWriter(os));
			for ( int i = 0; i < outputs.size(); i++ ){
				BufferedReader reader = new BufferedReader(new FileReader(outputs.get(i)));
                                
				String line = reader.readLine();
				if ( line != null ){
                                        readers.add(reader);
					map.put(new StringWrapper(line.split(delimiter), colOrder), readers.get(readers.size() - 1));
				}
			}

			///continue to loop until no more lines lefts
			List<StringWrapper> sorted = new LinkedList<>(map.keySet());
			while ( map.size() > 0 ){
				Collections.sort(sorted);
				StringWrapper line = sorted.remove(0);
                                
                                // sample readname
                                String[] substr = line.string[0].split(" ");
                                this.bloom.add(substr[0]);
                                
                                String outstr = StrArray.Join(line.string, delimiter);
				writer.write(outstr);
				writer.write(System.lineSeparator());
				BufferedReader reader = map.remove(line);
				String nextLine = reader.readLine();
				if ( nextLine != null ){
					StringWrapper sw = new StringWrapper(nextLine.split(delimiter), colOrder);
					map.put(sw,  reader);
					sorted.add(sw);
				}
			}
			writer.flush();
		}catch(IOException io){
			log.log(Level.SEVERE, "[TXTFILESORT] Error merging " + readers.size() + " files to: " + os.toString(), io);
		}finally{
			for ( int i = 0; i < readers.size(); i++ ){
				try{readers.get(i).close();}catch(Exception e){
                                    log.log(Level.SEVERE, "[TXTFILESORT] Could not close buffered reader merger: " + readers.get(i).toString());
                                }
			}
			for ( int i = 0; i < outputs.size(); i++ ){
				outputs.get(i).delete();
			}
                        
			try{writer.close();}catch(Exception e){
                            log.log(Level.SEVERE, "[TXTFILESORT] Erorr closing merge output writer!", e);
                        }
		}
                os.close();
	}
        
        private int convertLongToInt(long items){
            if(items > Integer.MAX_VALUE){
                return Integer.MAX_VALUE;
            }
            else return (int) items;
        }
        
        private double calcNumBitsPerItem(int items){
            return Math.ceil((items * Math.log(fp)) / Math.log(1.0d / (Math.pow(2.0d, Math.log(2.0d)))));
        }
        
        private int calcNumHashFunc(int items, double numBits){
            return (int) Math.round(Math.log(2.0d) * numBits / items);
        } 
        
        public QuickSortOutput getOutput(){
            return new QuickSortOutput(this.bloom, this.tempFile, this.identifier);
        }
        
        /*
        * return the temporary merged file
        */
        public Path getTemp(){
            return this.tempFile;
        }
        
        private void createTemp(Path path){
        
            this.tempFile = Paths.get(path.getFileName().toString() + ".sort.tmp");
            
            try {
                this.tempFile.toFile().createNewFile();
            } catch (IOException ex) {
                log.log(Level.SEVERE, "[TXTFILESORT] ERROR! could not create output merged temp file: " + this.tempFile.toString());
            }
            
            this.tempFile.toFile().deleteOnExit();

            log.log(Level.INFO, "[TXTFILESORT] Private temp file: " + this.tempFile.toString());
            
        }
        
        public class QuickSortOutput{
            public BloomFilter bloom;
            public Path output;
            public String id;
            public QuickSortOutput(BloomFilter bloom, Path output, String identifier){
                this.bloom = bloom;
                this.output = output;
                this.id = identifier;
            }
        }

	/**
	 * Delegate comparator to be able to sort String arrays. Delegates its behavior to 
	 * the sorter field.
	 * @author Greg Cope
	 *
	 */
	public class ComparatorDelegate implements Comparator<String[]>{
            private final int[] cols;
            public ComparatorDelegate(int[] cols){
                this.cols = cols;
            }

            @Override
            public int compare(String[] t, String[] t1) {
                for(int i : cols){
                    if(t[i].equals(t1[i]));
                    else
                        return t[i].compareTo(t1[i]);
                }
                return t[0].compareTo(t1[0]);
            }
            
	}

	/**
	 * Class which is a wrapper class for a String. This is necessary for String duplicates, which may cause equals/hashCode
	 * conflicts within the HashMap used in the file merge.
	 * @author Greg Cope
	 *
	 */
	private class StringWrapper implements Comparable<StringWrapper>{		

            public final String[] string;
            private final int[] cols;

            public StringWrapper(String[] line, int[] cols){
                    this.string = line;
                    this.cols = cols;
            }		

            @Override
            public int compareTo(StringWrapper t) {
                for(int i : cols){
                    if(this.string[i].equals(t.string[i]));
                    else
                        return this.string[i].compareTo(t.string[i]);
                }
                return this.string[0].compareTo(t.string[0]);
            }
	}

}
