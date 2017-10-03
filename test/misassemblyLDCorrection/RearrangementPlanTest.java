/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misassemblyLDCorrection;

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
public class RearrangementPlanTest {
    private final Path samFile = Paths.get("test").resolve("misassemblyLDCorrection").resolve("test.probes.sam");
    private final Path fastaFile = Paths.get("test").resolve("misassemblyLDCorrection").resolve("test.chr1.fasta");
    
    public RearrangementPlanTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of CreateMarkerPlan method, of class RearrangementPlan.
     */
    @Test
    public void testCreateMarkerPlan() {
        System.out.println("CreateMarkerPlan");
        RearrangementPlan instance = new RearrangementPlan(this.samFile.toString(), this.fastaFile.toString());
        instance.CreateMarkerPlan();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of refineEdges method, of class RearrangementPlan.
     */
    @Test
    public void testRefineEdges() {
        System.out.println("refineEdges");
        String JellyfishDb = "";
        RearrangementPlan instance = null;
        instance.refineEdges(JellyfishDb);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of printOrderedListToAGP method, of class RearrangementPlan.
     */
    @Test
    public void testPrintOrderedListToAGP() {
        System.out.println("printOrderedListToAGP");
        String outfile = "";
        RearrangementPlan instance = null;
        instance.printOrderedListToAGP(outfile);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
