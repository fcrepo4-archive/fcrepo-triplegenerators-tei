
package org.fcrepo.triplegenerators.tei;

import static javax.xml.transform.TransformerFactory.newInstance;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import org.apache.any23.source.ByteArrayDocumentSource;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.writer.TripleHandlerException;
import org.apache.any23.Any23;
import org.fcrepo.triplegenerators.tei.xslt.LoggingErrorListener;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

@Path("/")
public class TeiTripleGenerator {

    private static Transformer addIdsXform;

    private static Transformer tei2RdfXform;

    private static Any23 any23 = new Any23();

    private static final Logger LOGGER = getLogger(TeiTripleGenerator.class);

    public TeiTripleGenerator() throws TransformerConfigurationException,
            TransformerFactoryConfigurationError, IOException {
        // initialize XSLT

        final TransformerFactory tf =
            newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        tf.setErrorListener(new LoggingErrorListener());

        try (
            final InputStream sourceStream =
                this.getClass().getResourceAsStream("/xslt/add-ids.xslt")) {
            assert sourceStream != null : "Couldn't find ID-generating XSLT!";
            addIdsXform = tf.newTransformer(new StreamSource(sourceStream));
        }

        try (
            final InputStream sourceStream =
                this.getClass().getResourceAsStream("/xslt/tei2rdf.xslt")) {
            assert sourceStream != null : "Couldn't find RDF-generating XSLT!";
            tei2RdfXform = tf.newTransformer(new StreamSource(sourceStream));
        }
    }

    @GET
    public Model getTriples(@HeaderParam("Content-Location")
    final URL teiLocation) throws IOException,
    TransformerConfigurationException, TransformerException,
    ExtractionException, TripleHandlerException {
        // TODO redo this brainless way of retrieving the resource
        try (final InputStream teiStream = teiLocation.openStream()) {
            return getTriples(teiStream);
        }
    }

    @POST
    public Model getTriples(final InputStream resource)
        throws TransformerConfigurationException, IOException,
        TransformerException, ExtractionException, TripleHandlerException {

        final StreamResult rdfXml = createRDFXML(resource);
        // TODO when Any23 supports it, use a streaming transfer here
        final byte[] rdfXmlBytes = rdfXml.getWriter().toString().getBytes();
        final DocumentSource source =
            new ByteArrayDocumentSource(rdfXmlBytes,
                    "http://dummy.absolute.url", "application/rdf+xml");
        try (final ModelTripleHandler triples = new ModelTripleHandler()) {
            any23.extract(source, triples);
            return triples.getModel();
        }
    }

    protected StreamResult createRDFXML(final InputStream resource)
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
