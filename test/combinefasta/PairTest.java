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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dbickhart
 */
public class PairTest {
    private final static String[] firstFile = {
        "@NS500206:355:HFL5LBGXY:3:11403:22552:1019 1:N:0:ATTCCT",
        "NTTGAATGACAAGAGTCTCTGGTAAATTCATGAAGCATTTCATGTATGAACAACT",
        "+",
        "#AAAAEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE",
        "@NS500206:355:HFL5LBGXY:3:11403:7604:1020 1:N:0:ATTCCT",
        "NTTGAATGACAAGAGTCTCTGGTAAATTCATGAAGCATTTCATGTATGAACAACT",
        "+",
        "#AAAAEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE",
    };
    
    private final static String[] secondFile = {
        "@NS500206:355:HFL5LBGXY:3:11403:7604:1020 2:N:0:ATTCCT",
        "NTTGAATGACAAGAGTCTCTGGTAAATTCATGAAGCATTTCATGTATGAACAACT",
        "+",
        "#AAAAEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE",
        "@NS500206:355:HFL5LBGXY:3:11403:7604:1021 2:N:0:ATTCCT",
        "NTTGAATGACAAGAGTCTCTGGTAAATTCATGAAGCATTTCATGTATGAACAACT",
        "+",
        "#AAAAEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE",
        "@NS500206:355:HFL5LBGXY:3:11403:22552:1019 2:N:0:ATTCCT",
        "NTTGAATGACAAGAGTCTCTGGTAAATTCATGAAGCATTTCATGTATGAACAACT",
        "+",
        "#AAAAEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE",
    };
    
    private final static String forwardFile = "test.1.fq";
    private final static String reverseFile = "test.2.fq";
    private final static String outBase = "test.sorted";
    private final static String expectedFor = "test.sorted.1.fq";
    private final static String expectedRev = "test.sorted.2.fq";
    
    private static void WriteOut(String[] data, String fileName){
        Path output = Paths.get(fileName);
        output.toFile().deleteOnExit();
        try(BufferedWriter writer = Files.newBufferedWriter(output, Charset.defaultCharset())){
            for(String d : data){
                writer.write(d);
                writer.write(System.lineSeparator());
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
    public PairTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        WriteOut(firstFile, forwardFile);
        WriteOut(secondFile, reverseFile);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of run method, of class Pair.
     */
    @Test
    public void testRun() {
        Pair pair = new Pair(forwardFile, reverseFile, outBase);
        pair.run();
        assertTrue(Paths.get(expectedFor).toFile().canRead());
        assertTrue(Paths.get(expectedRev).toFile().canRead());
    }
    
}
