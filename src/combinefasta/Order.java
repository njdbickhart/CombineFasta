/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package combinefasta;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dbickhart
 */
public class Order {
    private final List<Path> fastaPaths = new ArrayList<>();
    private final List<String> fastaOrients = new ArrayList<>();
    private final Path Output;
    private static final Logger log = Logger.getLogger(Order.class.getName());
    private final List<String> seq = new ArrayList<>();
    private boolean hasPadding = false;
    private int paddingBP;
    
    public Order(String fastaStr, String orientStr, String output, int padding){
        String[] inputs = fastaStr.split(",");
        String[] orients = orientStr.split(",");
        this.Output = Paths.get(output);
        
        if(inputs.length != orients.length){
            log.log(Level.SEVERE, "Error! Number of inputs did not match number of orientations!");
            System.exit(-1);
        }
        
        for(int x = 0; x < inputs.length; x++){
            this.fastaPaths.add(Paths.get(inputs[x]));
            if(!orients[x].equals("+") && !orients[x].equals("-")){
                log.log(Level.SEVERE, "Error! Expected either a plus or minus in orientation field!");
                System.exit(-1);
            }
            this.fastaOrients.add(orients[x]);
        }       
        
        if(!(padding < 1)){
            this.hasPadding = true;
            this.paddingBP = padding;
        }
    }
    
    public void GenerateFasta(){
        for(int x = 0; x < fastaPaths.size(); x++){
            FastaReader reader = new FastaReader(fastaPaths.get(x));
            reader.LoadEntry();
            switch(fastaOrients.get(x)){
                case "+":
                    this.seq.addAll(reader.getSeq());
                    break;
                case "-":
                    this.seq.addAll(reader.getRevComp());
                    break;
            }
            if(this.hasPadding && x + 1 < fastaPaths.size()){
                // Add padding bases in between fasta entries
                this.AddPaddingBP();
            }
            log.log(Level.INFO, "Loaded fasta entry: " + reader.getHead());
        }
        
        String nl = System.lineSeparator();
        
        try(BufferedWriter output = Files.newBufferedWriter(Output, Charset.defaultCharset())){
            output.write("> merged" + nl);
            
            StringBuilder builder = new StringBuilder(this.seq.size() + (this.seq.size() / 60) + 1);
            for(int x = 0; x < this.seq.size(); x++){
                builder.append(this.seq.get(x));
                if(x != 0 && (x + 1) % 60 == 0){
                    builder.append(nl);
                }
            }
            output.write(builder.toString() + nl);
        }catch(IOException ex){
            log.log(Level.SEVERE, "Error writing to output!", ex);
        }
    }
    
    private void AddPaddingBP(){
        for(int x = 0; x < this.paddingBP; x++){
            this.seq.add("N");
        }
    }
    
}
