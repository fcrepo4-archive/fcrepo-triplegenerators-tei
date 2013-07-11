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

import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createModelForGraph;
import static java.lang.String.format;
import static javax.xml.transform.TransformerFactory.newInstance;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import javax.jcr.RepositoryException;
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
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.triplegenerators.tei.xslt.LoggingErrorListener;
import org.slf4j.Logger;

import com.google.common.io.FileBackedOutputStream;
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

    private static final Node PROBLEM_PREDICATE =
        createURI("info:fedora/hasProblemWithTeiRdfExtraction");

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
     * @param uri
     * @param gs
     * @param teiLocation
     * @return A {@link Dataset} with extracted triples.
     */
    public Dataset getTriples(final javax.jcr.Node uri, final GraphSubjects gs,
    final URL teiLocation) {
        // TODO redo this brainless way of retrieving the resource
        try (final InputStream teiStream = teiLocation.openStream()) {
            return getTriples(uri, gs, teiStream);
        } catch (final IOException e) {
            try {
                return exceptionRdf(gs.getGraphSubject(uri).getURI(), e);
            } catch (final RepositoryException ee) {
                return exceptionRdf("unknown", e);
            }
        }
    }

    /**
     * @param uri
     * @param gs
     * @param resource
     * @return A {@link Dataset} with extracted triples.
     */
    public Dataset getTriples(final javax.jcr.Node uri, final GraphSubjects gs,
        final InputStream resource) {
        String baseUri = "unknown";
        try {
            baseUri = gs.getGraphSubject(uri).getURI();
            final byte[] rdfXml = createRDFXML(resource);
            // TODO when Any23 supports it, use a streaming transfer between
            // these two steps
            return extractTriples(rdfXml, baseUri);
        } catch (
            TripleHandlerException | IOException | TransformerException |
            ExtractionException | RepositoryException e) {
            return exceptionRdf(baseUri, e);
        }
    }

    /**
     * @param rdfXml
     * @param baseUri
     * @return A {@link Dataset} with extracted triples.
     * @throws TripleHandlerException
     * @throws IOException
     * @throws ExtractionException
     */
    protected Dataset extractTriples(final byte[] rdfXml, final String baseUri)
        throws TripleHandlerException, IOException, ExtractionException {

        final DocumentSource source =
            new ByteArrayDocumentSource(rdfXml, baseUri, "application/rdf+xml");
        final Graph problems = new GraphMem();
        try (final ModelTripleHandler handler = new ModelTripleHandler()) {
            final ExtractionReport report = any23.extract(source, handler);
            final Dataset results = new DatasetImpl(handler.getModel());
            for (final Extractor<?> extractor : report.getMatchingExtractors()) {
                for (final Issue issue : report.getExtractorIssues(extractor
                        .getDescription().getExtractorName())) {
                    final String mesg =
                        format("Extraction issue: ({},{}): {}\n", issue
                                .getCol(), issue.getRow(), issue.getMessage());
                    problems.add(new Triple(createURI(baseUri),
                            PROBLEM_PREDICATE, createLiteral(mesg)));
                }
            }
            if (problems.size() > 0) {
                results.addNamedModel("problems", createModelForGraph(problems));
            }
            return results;
        }
    }

    /**
     * @param resource An {@link InputStream} with TEI XML.
     * @return A {@code byte[]} of RDF/XML.
     * @throws IOException
     * @throws TransformerException
     */
    private byte[] createRDFXML(final InputStream resource)
        throws IOException, TransformerException {
        final Source resourceSource = new StreamSource(resource);
        try (
            final FileBackedOutputStream addIdsResultStream =
                new FileBackedOutputStream(1024 * 1024)) {
            final Result addIdsResult = new StreamResult(addIdsResultStream);
            addIdsXform.transform(resourceSource, addIdsResult);
            LOGGER.debug("Added XML IDs to TEI.");
            try (
                final InputStream tei2RdfSourceStream =
                    addIdsResultStream.getSupplier().getInput()) {
                final Source tei2RdfSource =
                    new StreamSource(tei2RdfSourceStream);
                final StreamResult tei2RdfResult =
                    new StreamResult(new StringWriter());
                tei2RdfXform.transform(tei2RdfSource, tei2RdfResult);
                LOGGER.debug("Created RDF/XML from TEI: \n{}", tei2RdfResult
                        .getWriter().toString());
                return tei2RdfResult.getWriter().toString().getBytes();
            }
        }
    }

    /**
     * @param baseUri
     * @param e
     * @return A {@link Dataset} of RDF containing the exception
     */
    protected Dataset exceptionRdf(final String baseUri, final Exception... es) {
        final Graph problems = new GraphMem();
        final Dataset sadResults = new DatasetImpl(createDefaultModel());
        for (final Exception e : newArrayList(es)) {
            problems.add(new Triple(createURI(baseUri), PROBLEM_PREDICATE,
                    createLiteral(e.getMessage())));
        }
        sadResults.addNamedModel("problems", createModelForGraph(problems));
        return sadResults;
    }
}
