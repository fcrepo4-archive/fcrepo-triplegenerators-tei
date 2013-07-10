package org.fcrepo.triplegenerators.tei;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;


public class TestTeiTripleGenerator {

    private TeiTripleGenerator testTeiTripleGenerator;

    private static final Logger LOGGER = getLogger(TestTeiTripleGenerator.class);

    public TestTeiTripleGenerator() throws TransformerConfigurationException,
            TransformerFactoryConfigurationError, IOException {
        super();
    }

    @Before
    public void setUp() throws Exception {
        testTeiTripleGenerator = new TeiTripleGenerator();
    }

    @Test
    public void testExtraction() throws Exception {
        Model results;
        try (
            final InputStream teiStream =
                new FileInputStream(new File("target/test-classes/tei.xml"))) {
            results = testTeiTripleGenerator.getTriples(teiStream).getDefaultModel();
        }
        assertFalse("Got no triples!", results.isEmpty());
        for (final StmtIterator i = results.listStatements(); i.hasNext();) {
            LOGGER.debug("Retrieved triple: \n{}", i.next().asTriple());
        }
        assertTrue("Didn't find test triple!", results.contains(results
                .createResource("http://fcrepo.org/MSH"), results
                .createProperty("http://purl.org/dc/terms/publisher"), results
                .createResource("http://www.ancientwisdoms.ac.uk")));
        LOGGER.info("Found appropriate triple.");
    }

}
