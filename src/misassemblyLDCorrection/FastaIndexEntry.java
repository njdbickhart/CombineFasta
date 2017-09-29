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
    public final long length;
    public final long startByte;
    public final long lineLen;
    public boolean isModified = false;
    
    public FastaIndexEntry(String line){
        line = line.trim();
        String[] segs = line.split("\t");
        
        name = segs[0];
        length = Long.parseLong(segs[1]);
        startByte = Long.parseLong(segs[2]);
        lineLen = Long.parseLong(segs[3]);
    }
    
    public String getName(){
        return this.name;
    }
}
