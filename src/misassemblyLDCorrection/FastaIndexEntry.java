/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misassemblyLDCorrection;

/**
 *
 * @author dbickhart
 */
public class FastaIndexEntry {
    public final String name;
    public final int length;
    public final int startByte;
    public final int lineLen;
    public boolean isModified = false;
    
    public FastaIndexEntry(String line){
        line = line.trim();
        String[] segs = line.split("\t");
        
        name = segs[0];
        length = Integer.parseInt(segs[1]);
        startByte = Integer.parseInt(segs[2]);
        lineLen = Integer.parseInt(segs[3]);
    }
    
    public String getName(){
        return this.name;
    }
}
