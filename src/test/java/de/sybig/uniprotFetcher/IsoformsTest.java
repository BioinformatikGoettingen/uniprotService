package de.sybig.uniprotFetcher;

import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author juergen.doenitz@bioinf.med.uni-goettingen.de
 */
public class IsoformsTest {

    private Isoforms instance;

    public IsoformsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        UniProtConfiguration config = new UniProtConfiguration();
        config.setDataDir("testData");
        instance = new Isoforms(config);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getAlignmentPos method, of class Isoforms.
     */
    @Test
    public void testGetAlignmentPos() throws Exception {
        System.out.println("getAlignmentPos");
        String uniprotID = "test2sameSubs";

        List<AlignedSequence> result = instance.getAlignmentPos(uniprotID);

        AlignedSequence iso1 = result.get(0);
        AlignedSequence iso2 = result.get(1);
        AlignedSequence iso3 = result.get(2);
        assertEquals(iso1.getSequence().length(), iso2.getSequence().length());
        assertEquals(iso1.getSequence().length(), iso3.getSequence().length());
    }

}
