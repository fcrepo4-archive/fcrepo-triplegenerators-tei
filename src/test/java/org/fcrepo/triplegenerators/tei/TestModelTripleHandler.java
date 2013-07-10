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


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;

import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.FileDocumentSource;
import org.apache.any23.writer.TripleHandlerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Tests for {@link SetTripleHandler}
 *
 * @author ajs6f
 *
 */
public class TestModelTripleHandler extends ModelTripleHandler {

    private static final Logger LOGGER = getLogger(TestModelTripleHandler.class);

    private final Any23 any23 = new Any23();

    private DocumentSource rdfXmlSource;

    /**
     * Loads a sample RDF/XML document.
     */
    @Before
    public void setUp() {
        rdfXmlSource = new FileDocumentSource(new File(
                "target/test-classes/rdf.xml"));
    }

    /**
     * "Zeros-out" the {@link Model} being accumulated.
     */
    @After
    public void resetModel() {
        reset();
    }

    /**
     * Checks that a triple with all URI nodes from the sample RDF appears in
     * the accumulated triples after extraction.
     *
     * @throws IOException
     * @throws ExtractionException
     * @throws TripleHandlerException
     */
    @Test
    public void testOneTriple() throws IOException, ExtractionException,
            TripleHandlerException {
        LOGGER.info("Running testOneTriple()...");
        any23.extract(rdfXmlSource, this);
        assertTrue(
                "Didn't find appropriate triple!",
                getModel()
                        .contains(
                                model.createResource("info:fedora/uva-lib:1038847"),
                                model.createProperty("http://fedora.lib.virginia.edu/relationships#testPredicate"),
                                model.createResource("info:test/resource")));
        LOGGER.info("Found appropriate triple.");
        close();
    }

    /**
     * Checks that a triple with URI nodes for subject and predicate and literal
     * node for object from the sample RDF appears in the accumulated triples
     * after extraction.
     *
     * @throws IOException
     * @throws ExtractionException
     * @throws TripleHandlerException
     */
    @Test
    public void testOneTripleWithLiteral() throws IOException,
            ExtractionException, TripleHandlerException {
        LOGGER.info("Running testOneTripleWithLiteral()...");
        any23.extract(rdfXmlSource, this);
        assertTrue(
                "Didn't find appropriate triple!",
                getModel()
                        .contains(
                                model.createResource("info:fedora/uva-lib:1038847"),
                                model.createProperty("http://fedora.lib.virginia.edu/relationships#testPredicateWithLiteral"),
                                model.createLiteral("literal value")));
        LOGGER.info("Found appropriate triple.");
        close();
    }

    /**
     * Checks that a triple with absolute URI nodes for subject and predicate
     * and relative URI node for object from the sample RDF appears in the
     * accumulated triples after extraction. The literal node has the form of a
     * relative URI, and Fedora's Resource Index does not contemplate such URIs,
     * so we treat it as a literal.
     *
     * @throws IOException
     * @throws ExtractionException
     * @throws TripleHandlerException
     */
    @Test
    public void testOneTripleWithRelativeUri() throws IOException,
            ExtractionException, TripleHandlerException {
        LOGGER.info("Running testOneTripleWithRelativeUri()...");
        any23.extract(rdfXmlSource, this);
        assertTrue(
                "Didn't find appropriate triple!",
                getModel()
                        .contains(
                                model.createResource("info:fedora/uva-lib:1038847"),
                                model.createProperty("http://fedora.lib.virginia.edu/relationships#testPredicateWithLiteral"),
                                model.createLiteral("/relative/uri/")));
        LOGGER.info("Found appropriate triple.");
        close();
    }

    /**
     * We do not implement setContentLength() because there is no need for it in
     * the Fedora context.
     */
    @Test
    public void testSetContentLength() {
        LOGGER.info("Running testSetContentLength()...");
        try {
            setContentLength(0);
            fail("setContentLength() didn't throw an UnsupportedOperationException!");
        } catch (final UnsupportedOperationException e) {
            LOGGER.info("Found correct behavior for setContentLength().");
        }
    }



}
