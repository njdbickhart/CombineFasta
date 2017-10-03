/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misassemblyLDCorrection;

import file.BedSimple;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author dbickhart
 */
public class BedFastaPlan extends BedSimple{
    public boolean isRev;
    public final String aChr;
    public int aStart;
    public int counter = 0;
    
    public BedFastaPlan(String c, int s, int e, String ac, int as) {
        super(c, s, e, null);
        this.aChr = ac;
        this.aStart = as;
    }
        
    public String getActualChr(){
        return this.aChr;
    }
    
    public int getActualStart(){
        return this.aStart;
    }
    
    public void SetActualStart(int start){
        this.aStart = start;
    }
    
    public void incCounter(){
        this.counter++;
    }
    
    @Override
    public String toString(){
        // returns in this format:
        // ochr:ostart-oend;mchr:mpos
        StringBuilder output = new StringBuilder();
        output.append(this.Chr()).append(":").append(this.start).append("-").append(this.end);
        output.append(";").append(this.aChr).append(":").append(this.aStart);
        return output.toString();
    }
    
    public Map<Integer, String> toAGP(int prevend, int counter, boolean addGap){
        // Returns AGP 2.0 tab-delimited string
        StringBuilder output = new StringBuilder();
        int len = this.end - this.start;
        output.append(this.aChr).append("\t").append(prevend).append("\t")
                .append(prevend + len).append("\t").append(counter).append("\t")
                .append("D\t").append(chr).append("\t").append(start).append("\t")
                .append(end).append("\t").append((isRev)? "-" : "+").append("\t")
                .append(System.lineSeparator());
        if(addGap)
            output.append(this.aChr).append("\t").append(prevend+this.end).append("\t")
                    .append(prevend+len + 100).append("\t").append(counter +1).append("\t")
                    .append("N\t").append(100).append("\t").append("scaffold").append("\t")
                    .append("yes").append("\t").append("map").append(System.lineSeparator());
                
        int newEnd = (addGap)? len + 100 + prevend : len + prevend;
        Map<Integer, String> m = new HashMap<>();
        m.put(newEnd, output.toString());
        return m;
    }
}
