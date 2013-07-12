/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.triplegenerators.tei;

import static com.google.common.io.Files.toByteArray;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.any23.extractor.ExtractionException;
import org.fcrepo.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;

public class TestTeiTripleGenerator extends TeiTripleGenerator {

    @Mock
    private Node mockContentNode;

    @Mock
    private GraphSubjects mockGraphSubjects;

    @Mock
    private Resource mockResource;

    @Mock
    private com.hp.hpl.jena.graph.Node mockNode;

    @Mock
    private Property mockProperty;

    @Mock
    private Binary mockBinary;

    private static final Statement testTriple = new StatementImpl(
            createResource("http://fedora"),
            createProperty("http://purl.org/dc/terms/publisher"),
            createResource("http://www.ancientwisdoms.ac.uk"));

    private static final Logger LOGGER =
        getLogger(TestTeiTripleGenerator.class);

    public TestTeiTripleGenerator() throws TransformerConfigurationException,
            TransformerFactoryConfigurationError, IOException {
        super();
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(mockContentNode.getPath()).thenReturn("/test");
        when(mockGraphSubjects.getGraphSubject(mockContentNode)).thenReturn(
                mockResource);
        when(mockResource.getURI()).thenReturn("http://fedora");
        when(mockContentNode.getNode(JCR_CONTENT)).thenReturn(mockContentNode);
        when(mockContentNode.getProperty(JCR_DATA)).thenReturn(mockProperty);
        when(mockProperty.getBinary()).thenReturn(mockBinary);
        when(mockBinary.getStream()).thenReturn(
                new FileInputStream(new File("target/test-classes/tei.xml")));
    }

    @Test
    public void testExtraction() throws Exception {
        Model results;
        try (
            final InputStream teiStream =
                new FileInputStream(new File("target/test-classes/tei.xml"))) {
            results =
                getProperties(mockContentNode, mockGraphSubjects)
                        .getDefaultModel();
        }
        assertFalse("Got no triples!", results.isEmpty());
        for (final StmtIterator i = results.listStatements(); i.hasNext();) {
            LOGGER.debug("Retrieved triple: \n{}", i.next().asTriple());
        }
        assertTrue("Didn't find test triple!", results.contains(testTriple));
        LOGGER.info("Found test triple: {}", testTriple.asTriple());
    }

    @Test(expected = ExtractionException.class)
    public void testextractTriplesWithBadRdfXml() throws Exception {
        final byte[] rdfXml =
            toByteArray(new File("target/test-classes/bad-rdf.xml"));
        extractTriples(rdfXml, "http://fedora");
    }

    @Test
    public void testExceptionRdf() {
        final Dataset ds = exceptionRdf("uri", new Exception("Bad news!"));
        assertTrue(ds.getNamedModel("problems").contains(null, null,
                "Bad news!"));
    }
}
