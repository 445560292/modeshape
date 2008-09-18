/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.mimetype;

import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * MIME-type detection libraries must provide thread-safe implementations of this interface to enable DNA to use the libraries to
 * return MIME-types for data sources.
 * 
 * @author jverhaeg
 */
@ThreadSafe
public interface MimeTypeDetector {

    /**
     * Returns the MIME-type of a data source, using its supplied content and/or its supplied name, depending upon the
     * implementation. If the MIME-type cannot be determined, either a "default" MIME-type or <code>null</code> may be returned,
     * where the former will prevent earlier registered MIME-type detectors from being consulted.
     * 
     * @param name The name of the data source; may be <code>null</code>.
     * @param content The content of the data source; may be <code>null</code>.
     * @return The MIME-type of the data source, or optionally <code>null</code> if the MIME-type could not be determined.
     * @throws IOException If an error occurs reading the supplied content.
     */
    String mimeTypeOf( String name,
                       InputStream content ) throws IOException;
}
