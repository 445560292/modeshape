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
package org.jboss.dna.graph.connectors;

/**
 * An interface for a factory of {@link RepositoryConnection}s using the names of the {@link RepositorySource} from which the
 * connections are to be obtained. This interface need not be implemented by a {@link RepositorySource}, as implementations are
 * usually provided by the system.
 * 
 * @author Randall Hauch
 */
public interface RepositoryConnectionFactory {

    /**
     * Create a {@link RepositoryConnection} using the given {@link RepositorySource} name.
     * 
     * @param sourceName the name of the source from which a connection should be obtained
     * @return the source, or null if no such source could be found (or created)
     * @throws RepositorySourceException if there was an error obtaining the new connection
     */
    RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException;

}
