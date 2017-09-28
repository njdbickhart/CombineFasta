/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misassemblyLDCorrection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author dbickhart
 */
public class SubStringFastaCode {
    private static final Logger log = Logger.getLogger(SubStringFastaCode.class.getName());
    private static final char[] codes = {'A', 'T', 'G', 'C', 'N'};
    private static final char[] revcodes = {'T', 'A', 'C', 'G', 'N'};
    private final byte[] seqCode;
    private final String chr;
    private final int start;
    private final int end;
    private boolean isRev = false;
    
    public SubStringFastaCode(byte[] seqCode, String chr, int start, int end){
        this.seqCode = seqCode;
        this.chr = chr;
        this.start = start;
        this.end = end;
    }
    
    public void setRevComp(){
        this.isRev = true;
    }
    
    public String getChr(){
        return this.chr;
    }
    
    public int getStart(){
        return this.start;
    }
    
    public int getEnd(){
        return this.end;
    }
    
    public boolean getRevComp(){
        return this.isRev;
    }
    
    public List<Character> getFwdSeq(){
        List<Character> output = new ArrayList<>(this.seqCode.length);
        for(int x = 0; x < seqCode.length; x++)
            output.add(codes[this.seqCode[x]]);
        return output;
    }
    
    public List<Character> getRevSeq(){
        List<Character> output = new ArrayList<>(this.seqCode.length);
        for(int x = seqCode.length -1; x >= 0; x--)
            output.add(revcodes[this.seqCode[x]]);
        return output;
    }
}
