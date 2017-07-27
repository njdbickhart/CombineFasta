/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Derek.Bickhart
 */
public class BufferedFastaReaderWriter {
    private final Logger log = Logger.getLogger(BufferedFastaReaderWriter.class.getName());
    private final FileReader fasta;
    // Buffer will handle 5 megabases at a time for now.
    private final char[] buffer = new char[5000000];
    // This is an index to the last section of the buffer for the next chromosome
    private int lastIdx = 0;
    private int lastLen = 0;
    //private int lastCurPos = 0;
    private String curChr = "N/A";
    private final char[] outBuffer = new char[60];
    private final boolean changeName;
    
    public BufferedFastaReaderWriter(String fastaFile, boolean changeName) throws FileNotFoundException{
        this.fasta = new FileReader(fastaFile);
        this.changeName = changeName;
    }
    
    public int readToNextChr(BufferedWriter output, String format) throws IOException{
        char[] chrname = new char[256]; 
        int chrnamebuff = 0, currentpos = 0, currentrun = 0, charRead = 0;
        
        /*if(eof(fasta)){
        return -1;
        }*/
        
        // Since the file entries are not bounded exclusively, we have to determine if we've read this file before or not
        if(lastIdx > 0 && lastLen > 0){
            getChrName(chrname, chrnamebuff, format);
            lastIdx++;
            
            log.log(Level.INFO, "Standardizing chr: " + this.curChr);
            output.write(">" + this.curChr + System.lineSeparator());
            
            int[] ret = processBufferedChunk(lastIdx, lastLen, 0, 0, output);
            if(ret[0] == 0){
                // The end of the chromosome was reached! set the lengths
                // necessary for chromosomes that fit within the buffer
                this.lastIdx = ret[1];
                return 0;
            }else{
                currentpos = ret[0];
                currentrun = ret[1];
            }
        }else{
            // We're starting fresh            
            if((charRead = fasta.read(buffer)) != -1){
                // We need to get the chromosome name!
                getChrName(chrname, chrnamebuff, format);
                this.lastIdx++;
                
                log.log(Level.INFO, "Standardizing chr: " + this.curChr);
                output.write(">" + this.curChr + System.lineSeparator());
                
                int[] ret = processBufferedChunk(this.lastIdx, charRead, currentpos, currentrun, output);
                if(charRead == -1){
                    // reached the end of the file!
                    return -1;
                }else if(ret[0] == 0){
                    this.lastIdx = ret[1];
                    this.lastLen = charRead;
                    
                    // reached the end of the chromosome! Storing the remainder in the buffer!
                    return 0;
                }else{
                    currentpos = ret[0];
                    currentrun = ret[1];
                }
            }else{
                throw new IOException("Unexpected end of file!");
            }

        }
        
        while(true){
            if((charRead = fasta.read(buffer)) != -1){
                int[] ret = processBufferedChunk(0, charRead, currentpos, currentrun, output);
                if(charRead == -1)
                    // reached the end of the file!
                    return -1;
                if(ret[0] == 0){
                    this.lastIdx = ret[1];
                    this.lastLen = charRead;
                    
                    break; // reached end of chromosome!
                }else{
                    currentpos = ret[0];
                    currentrun = ret[1];
                }
            }else{
                return -1;
            }
        }
        return 0;
    }
    
    private boolean eof (FileReader r) throws IOException {
        r.mark(1);
        int i = r.read();
        r.reset();
        return i < 0;
    }
    
    public void close() throws IOException{
        this.fasta.close();
    }

    // The format string removes undesired suffixes from fasta names
    private void getChrName(char[] chrname, int chrnamebuff, String format) {
        // We last read part of the previous chromosome
        while(true){
            if(lastIdx >= buffer.length)
                break;
            // get the chromosome name
            if(buffer[lastIdx] == '>'){
                this.lastIdx++;
                continue;
            }else if(buffer[lastIdx] == '\n')
                break;
            else
                chrname[chrnamebuff] = buffer[lastIdx];
            
            chrnamebuff++;
            lastIdx++;
        }
        if(this.changeName)
            this.curChr = String.copyValueOf(chrname).replaceAll(format, "").trim();
        else
            this.curChr = String.copyValueOf(chrname).trim();
    }
    
    // Returns three integers, first integer is the currentposition, second is the current run of N's, the third integer is only used if the first integer is zero
    // If the current position returned is 0, then the end of the chr was reached, and the second int is the last index value and the third is the current position
    private int[] processBufferedChunk(int idx, int len, int currentPos, int currentRun, BufferedWriter output) throws IOException{
        
        final String nl = System.lineSeparator();
        int TempOutBufferAdds = 0;
        StringBuilder TempOutBuffer = new StringBuilder();
        TempOutBuffer.ensureCapacity(61000);
        for(int x = idx; x < len; x++){
            if(buffer[x] != '\n' && buffer[x] != '\r' && buffer[x] != ' ' && buffer[x] != '>'){
                outBuffer[currentRun] = buffer[x];
                currentRun++; 
            }else if (buffer[x] == '\n' || buffer[x] == '\r' || buffer[x] == ' '){
                // whitespace found! not counting
                continue;
            }else if (buffer[x] == '>'){
                // Reached the end of the chromosome!
                if(currentRun > 0 && currentRun <= 59){
                    char[] tempBuff = Arrays.copyOfRange(outBuffer, 0, currentRun);
                    TempOutBuffer.append(String.copyValueOf(tempBuff)).append(nl);
                    //output.write(String.copyValueOf(tempBuff) + nl);
                    output.write(TempOutBuffer.toString());
                    currentRun = 0;
                }
                int[] ret = {0, x, currentPos};
                return ret;
            }
            
            if(currentRun > 59){
                //output.write(String.copyValueOf(outBuffer) + nl);
                TempOutBuffer.append(outBuffer).append(nl);
                TempOutBufferAdds++;
                if(TempOutBufferAdds > 1000){
                    output.write(TempOutBuffer.toString());
                    TempOutBuffer = new StringBuilder();
                    TempOutBuffer.ensureCapacity(61000);
                    TempOutBufferAdds = 0;
                }
                currentRun = 0;
            }
            currentPos++;
        }
        
        if(TempOutBufferAdds > 0){
            output.write(TempOutBuffer.toString());
            TempOutBufferAdds = 0;
        }
        int[] ret = {currentPos, currentRun, 0};
        return ret;
    }
}
