package org.fcrepo.triplegenerators.tei;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URISyntaxException;
import org.apache.any23.extractor.ExtractionContext;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Utility class to produce a {@link Model} from a stream of triples produced
 * by Any23 parsing. Most of {@link TripleHandler}'s methods are unimplemented,
 * because this class expects to produce nothing other than a {@link Model} of the
 * triples emitted into it.
 *
 * @author ajs6f
 */

public class ModelTripleHandler implements TripleHandler, AutoCloseable {

    /**
     * A {@link Model} of {@link Triple}s we are collecting.
     */
    protected final Model model = createDefaultModel();

    private static final Logger LOGGER = getLogger(ModelTripleHandler.class);

    /*
     * (non-Javadoc)
     * @see
     * org.apache.any23.writer.TripleHandler#receiveTriple(org.openrdf.model
     * .Resource, org.openrdf.model.URI, org.openrdf.model.Value,
     * org.openrdf.model.URI, org.apache.any23.extractor.ExtractionContext)
     */
    @Override
    public void receiveTriple(final Resource s, final URI p, final Value o,
        final URI g, final ExtractionContext ec)
        throws TripleHandlerException {
        final Statement triple =
            model.createStatement(model.createResource(s.stringValue()),
                model.createProperty(p.getNamespace(), p.getLocalName()),
                    objectNode(o));
        model.add(triple);
        LOGGER.debug("Added triple: {}", triple.asTriple().toString());
    }

    /**
     * @param v A {@link Value}
     * @return An {@link RDFNode}
     */
    private RDFNode objectNode(final Value value) {
        final String v = value.stringValue();
        try {
            final java.net.URI uri = new java.net.URI(v);
            if (uri.isAbsolute()) {
                return model.createResource(uri.toString());
            } else {
                return model.createLiteral(v);
            }
        } catch (final URISyntaxException e) {
            return model.createLiteral(v);
        }
    }

    /**
     * @return The {@link Model} that has been accumulated via calls to
     *         {@link #receiveTriple(Resource, URI, Value, URI, ExtractionContext)}
     */
    public Model getModel() {
        return model;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.any23.writer.TripleHandler#close()
     */
    @Override
    public void close() throws TripleHandlerException {
        model.removeAll();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.any23.writer.TripleHandler#startDocument(org.openrdf.model
     * .URI)
     */
    @Override
    public void startDocument(final URI u) throws TripleHandlerException {
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.any23.writer.TripleHandler#endDocument(org.openrdf.model.URI)
     */
    @Override
    public void endDocument(final URI u) throws TripleHandlerException {
    }

    /*
     * (non-Javadoc)
     * @see org.apache.any23.writer.TripleHandler#setContentLength(long)
     */
    @Override
    public void setContentLength(final long l) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.any23.writer.TripleHandler#openContext(org.apache.any23.extractor
     * .ExtractionContext)
     */
    @Override
    public void openContext(final ExtractionContext ec)
        throws TripleHandlerException {
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.any23.writer.TripleHandler#receiveNamespace(java.lang.String,
     * java.lang.String, org.apache.any23.extractor.ExtractionContext)
     */
    @Override
    public void receiveNamespace(final String prefix, final String uri,
        final ExtractionContext ec) throws TripleHandlerException {
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.any23.writer.TripleHandler#closeContext(org.apache.any23.extractor
     * .ExtractionContext)
     */
    @Override
    public void closeContext(final ExtractionContext ec)
        throws TripleHandlerException {
    }

    public void reset() {
        model.removeAll();
    }
}
