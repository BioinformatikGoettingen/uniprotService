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
        String uniprotID = "test2sameSubs";

        List<AlignedSequence> result = instance.getAlignmentPos(uniprotID);

        AlignedSequence iso1 = result.get(0);
        AlignedSequence iso2 = result.get(1);
        AlignedSequence iso3 = result.get(2);
        assertEquals(iso1.getSequence().length(), iso2.getSequence().length());
        assertEquals(iso1.getSequence().length(), iso3.getSequence().length());
        assertEquals(2, iso1.getFeatures().size());
        assertEquals(1, iso2.getFeatures().size());
        assertEquals(1, iso3.getFeatures().size());
        assertEquals("ABCD-----EFGHIJKLMNOPQRSTUVWXYZ", iso1.getSequence());
        assertEquals("ABCD12345EFGHIJKLMNOPQRSTUVWXYZ", iso2.getSequence());

    }

    @Test
    public void oneIsoformWithDeletion() throws Exception {
        List<AlignedSequence> result = instance.getAlignmentPos("1isoWithDeletion");
        AlignedSequence iso1 = result.get(0);
        AlignedSequence iso2 = result.get(1);
        assertEquals(iso1.getSequence().length(), iso2.getSequence().length());
        assertEquals(1, iso1.getFeatures().size());
        assertEquals(2, iso2.getFeatures().size());
        assertEquals("ABCDE-----KLMNOPQRSTUVWXYZ", iso2.getSequence());

    }

    @Test
    public void mismatchWithSameLength() throws Exception {
        List<AlignedSequence> result = instance.getAlignmentPos("testSameLengthMismatch");
        AlignedSequence iso1 = result.get(0);
        AlignedSequence iso2 = result.get(1);
        assertEquals(iso1.getSequence().length(), iso2.getSequence().length());
        assertEquals(1, iso1.getFeatures().size()); // only the orig sequence, no modifications
        assertEquals(2, iso2.getFeatures().size());
        assertEquals("mismatch", iso2.getFeatures().get(1).getType());
    }

    @Test
    public void smallerOtherReplacment() throws Exception {
        List<AlignedSequence> result = instance.getAlignmentPos("testSmallerOtherReplacement");
        AlignedSequence iso1 = result.get(0);
        AlignedSequence iso2 = result.get(1);
    }
    
    @Test
    public void smallerMismatch() throws Exception {
        List<AlignedSequence> result = instance.getAlignmentPos("smallerMismatch");
        AlignedSequence iso1 = result.get(0);
        AlignedSequence iso2 = result.get(1);
        assertEquals(iso1.getSequence().length(), iso2.getSequence().length());
        assertEquals(1, iso1.getFeatures().size());
        assertEquals(3, iso2.getFeatures().size());
        assertEquals("mismatch", iso2.getFeatures().get(1).getType());
                assertEquals("gapD", iso2.getFeatures().get(2).getType());
    }
}
