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

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createModelForGraph;
import static java.lang.String.format;
import static javax.xml.transform.TransformerFactory.newInstance;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import javax.ws.rs.HeaderParam;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.extractor.Extractor;
import org.apache.any23.extractor.IssueReport.Issue;
import org.apache.any23.source.ByteArrayDocumentSource;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.writer.TripleHandlerException;
import org.apache.any23.Any23;
import org.apache.any23.ExtractionReport;
import org.fcrepo.triplegenerators.tei.xslt.LoggingErrorListener;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetImpl;


/**
 * @author ajs6f
 * @date Jul 10, 2013
 */
public class TeiTripleGenerator {

    private static Transformer addIdsXform;

    private static Transformer tei2RdfXform;

    private static Any23 any23 = new Any23();

    private static final Logger LOGGER = getLogger(TeiTripleGenerator.class);

    private static final Node PROBLEM_PREDICATE = createURI("info:fedora/hasProblem");

    //TODO replace this with a constructed URI derived from barmintor's contract
    private Node uri;

    /**
     * @throws TransformerConfigurationException
     * @throws TransformerFactoryConfigurationError
     * @throws IOException
     */
    public TeiTripleGenerator() throws TransformerConfigurationException,
            TransformerFactoryConfigurationError, IOException {
        // initialize XSLT
        final TransformerFactory tf =
            newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        tf.setErrorListener(new LoggingErrorListener());

        try (
            final InputStream sourceStream =
                this.getClass().getResourceAsStream("/xslt/add-ids.xslt")) {
            addIdsXform = tf.newTransformer(new StreamSource(sourceStream));
        }

        try (
            final InputStream sourceStream =
                this.getClass().getResourceAsStream("/xslt/tei2rdf.xslt")) {
            tei2RdfXform = tf.newTransformer(new StreamSource(sourceStream));
        }
    }

    /**
     * @param teiLocation
     * @return A {@link Dataset} with extracted triples.
     * @throws IOException
     * @throws TransformerConfigurationException
     * @throws TransformerException
     * @throws ExtractionException
     * @throws TripleHandlerException
     */
    public Dataset getTriples(@HeaderParam("Content-Location")
    final URL teiLocation) throws IOException,
    TransformerConfigurationException, TransformerException,
    ExtractionException, TripleHandlerException {
        // TODO redo this brainless way of retrieving the resource
        try (final InputStream teiStream = teiLocation.openStream()) {
            return getTriples(teiStream);
        }
    }

    /**
     * @param resource
     * @return A {@link Dataset} with extracted triples.
     * @throws TransformerConfigurationException
     * @throws IOException
     * @throws TransformerException
     * @throws ExtractionException
     * @throws TripleHandlerException
     */
    public Dataset getTriples(final InputStream resource)
        throws TransformerConfigurationException, IOException,
        TransformerException, ExtractionException, TripleHandlerException {

        final StreamResult rdfXml = createRDFXML(resource);
        // TODO when Any23 supports it, use a streaming transfer here
        final byte[] rdfXmlBytes = rdfXml.getWriter().toString().getBytes();
        final DocumentSource source =
            new ByteArrayDocumentSource(rdfXmlBytes,
                    "http://dummy.absolute.url", "application/rdf+xml");
        try (final ModelTripleHandler handler = new ModelTripleHandler()) {
            final ExtractionReport report = any23.extract(source, handler);
            final Dataset results = new DatasetImpl(handler.getModel());
            final Graph problems = new GraphMem();
            for (final Extractor<?> extractor : report.getMatchingExtractors()) {
                for (final Issue issue : report.getExtractorIssues(extractor
                        .getDescription().getExtractorName())) {
                    final String mesg = format("Extraction issue: ({},{}): {}\n", issue
                            .getCol(), issue.getRow(), issue.getMessage());
                    problems.add(new Triple(uri, PROBLEM_PREDICATE, createLiteral(mesg)));
                }
            }
            if (problems.size() > 0) {
                results.addNamedModel("problems", createModelForGraph(problems));
            }

            return results;
        }
    }

    /**
     * @param resource
     * @return A {@link Result} of RDF/XML
     * @throws IOException
     * @throws TransformerConfigurationException
     * @throws TransformerException
     */
    private StreamResult createRDFXML(final InputStream resource)
        throws IOException, TransformerConfigurationException,
        TransformerException {
        final Source resourceSource = new StreamSource(resource);
        try (final Writer addIdsResultWriter = new StringWriter()) {
            final Result addIdsResult = new StreamResult(addIdsResultWriter);
            addIdsXform.transform(resourceSource, addIdsResult);
            final String teiWithIds = addIdsResultWriter.toString();
            LOGGER.debug("Added XML IDs to TEI: \n{}", teiWithIds);
            try (
                final InputStream tei2RdfSourceStream =
                    new ByteArrayInputStream(teiWithIds.getBytes())) {
                final Source tei2RdfSource =
                    new StreamSource(tei2RdfSourceStream);
                final StreamResult tei2RdfResult =
                    new StreamResult(new StringWriter());
                tei2RdfXform.transform(tei2RdfSource, tei2RdfResult);
                LOGGER.debug("Created RDF/XML from TEI: \n{}", tei2RdfResult
                        .getWriter().toString());
                return tei2RdfResult;
            }
        }
    }
}
