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
