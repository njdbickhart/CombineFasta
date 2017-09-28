/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misassemblyLDCorrection;

import file.BedSimple;

/**
 *
 * @author dbickhart
 */
public class BedFastaPlan extends BedSimple{
    public boolean isRev;
    public final String aChr;
    public int aStart;
    
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
}
