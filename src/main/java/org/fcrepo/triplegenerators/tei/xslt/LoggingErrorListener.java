
package org.fcrepo.triplegenerators.tei.xslt;

import static org.slf4j.LoggerFactory.getLogger;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;

public class LoggingErrorListener implements ErrorListener {

    private static final Logger LOGGER = getLogger(LoggingErrorListener.class);

    private static final String FATAL_ERROR_MSG = "Fatal error from XSLT: ";

    private static final String ERROR_MSG = "Error from XSLT: ";

    private static final String WARNING_MSG = "Warning from XSLT: ";

    @Override
    public void warning(final TransformerException e)
        throws TransformerException {
        LOGGER.warn(WARNING_MSG, e);

    }

    @Override
    public void error(final TransformerException e)
        throws TransformerException {
        LOGGER.warn(ERROR_MSG, e);

    }

    @Override
    public void fatalError(final TransformerException e)
        throws TransformerException {
        LOGGER.warn(FATAL_ERROR_MSG, e);

    }

}
