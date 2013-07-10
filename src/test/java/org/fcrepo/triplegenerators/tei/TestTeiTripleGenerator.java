package org.fcrepo.triplegenerators.tei;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.writer.TripleHandlerException;
import org.junit.Test;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;


public class TestTeiTripleGenerator extends TeiTripleGenerator {

    private static final Logger LOGGER = getLogger(TestTeiTripleGenerator.class);

    public TestTeiTripleGenerator() throws TransformerConfigurationException,
            TransformerFactoryConfigurationError, IOException {
        super();
    }



    @Test
    public void testExtraction() throws FileNotFoundException, IOException, TransformerConfigurationException, TransformerException, ExtractionException, TripleHandlerException {
        try (
            final InputStream teiStream =
                new FileInputStream(new File("target/test-classes/tei.xml"))) {
            final Model results = getTriples(teiStream);
            final StmtIterator i = results.listStatements();
            while (i.hasNext()) {
                LOGGER.debug("Retrieved triple: \n{}", i.next().asTriple());
            }
        }
    }

}
