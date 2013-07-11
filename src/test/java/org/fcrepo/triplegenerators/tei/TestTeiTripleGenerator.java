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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.fcrepo.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;


public class TestTeiTripleGenerator {

    private TeiTripleGenerator testTeiTripleGenerator;

    @Mock
    private Node mockUriNode;

    @Mock
    private GraphSubjects mockGraphSubjects;

    @Mock
    private Resource mockResource;

    @Mock
    private com.hp.hpl.jena.graph.Node mockNode;

    private static final Logger LOGGER = getLogger(TestTeiTripleGenerator.class);

    public TestTeiTripleGenerator() throws TransformerConfigurationException,
            TransformerFactoryConfigurationError, IOException {
        super();
    }

    @Before
    public void setUp() throws Exception {
        testTeiTripleGenerator = new TeiTripleGenerator();
        initMocks(this);
        when(mockUriNode.getPath()).thenReturn("/test");
        when(mockGraphSubjects.getGraphSubject(mockUriNode)).thenReturn(mockResource);
        when(mockResource.asNode()).thenReturn(mockNode);
        when(mockNode.getURI()).thenReturn("http://fedora");
    }

    @Test
    public void testExtraction() throws Exception {
        Model results;
        try (
            final InputStream teiStream =
                new FileInputStream(new File("target/test-classes/tei.xml"))) {
            results =
                testTeiTripleGenerator.getTriples(mockUriNode,
                        mockGraphSubjects, teiStream).getDefaultModel();
        }
        assertFalse("Got no triples!", results.isEmpty());
        for (final StmtIterator i = results.listStatements(); i.hasNext();) {
            LOGGER.debug("Retrieved triple: \n{}", i.next().asTriple());
        }
        assertTrue("Didn't find test triple!", results.contains(results
                .createResource("http://fedora"), results
                .createProperty("http://purl.org/dc/terms/publisher"), results
                .createResource("http://www.ancientwisdoms.ac.uk")));
        LOGGER.info("Found appropriate triple.");
    }

    @Test
    public void testExtractionWithProblems() throws Exception {
        Model results;
        try (
            final InputStream teiStream =
                new FileInputStream(new File("target/test-classes/bad-tei.xml"))) {
            results =
                testTeiTripleGenerator.getTriples(mockUriNode,
                        mockGraphSubjects, teiStream).getDefaultModel();
        }
        for (final StmtIterator i = results.listStatements(); i.hasNext();) {
            LOGGER.debug("Retrieved triple: \n{}", i.next().asTriple());
        }
        LOGGER.info("Found appropriate triple.");
    }

}
