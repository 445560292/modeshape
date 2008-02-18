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
package org.jboss.dna.maven.spi;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import org.jboss.dna.maven.ArtifactType;
import org.jboss.dna.maven.MavenId;
import org.jboss.dna.maven.MavenRepository;
import org.jboss.dna.maven.MavenRepositoryException;
import org.jboss.dna.maven.SignatureType;

/**
 * @author Randall Hauch
 */
public interface IMavenUrlProvider {

    /**
     * Configure this provider given the configuration properties. This method is intended to be called by the
     * {@link MavenRepository} that instantiates this provider, and only once immediately after instantiation and before any calls
     * to {@link #getUrl(MavenId, ArtifactType, SignatureType, boolean)}.
     * @param properties the configuration properties
     * @throws MavenRepositoryException if there is a problem connecting to or using the Maven repository, as configured
     */
    public void configure( Properties properties ) throws MavenRepositoryException;

    /**
     * Get the URL for the artifact with the specified type in the given Maven project. The resulting URL can be used to
     * {@link URL#openConnection() connect} to the repository to {@link URLConnection#getInputStream() read} or
     * {@link URLConnection#getOutputStream() write} the artifact's content.
     * @param mavenId the ID of the Maven project; may not be null
     * @param artifactType the type of artifact; may be null, but the URL will not be able to be read or written to
     * @param signatureType the type of signature; may be null if the signature file is not desired
     * @param createIfRequired true if the node structure should be created if any part of it does not exist; this always expects
     * that the path to the top of the repository tree exists.
     * @return the URL to this artifact, or null if the artifact does not exist
     * @throws MalformedURLException if the supplied information cannot be turned into a valid URL
     * @throws MavenRepositoryException if there is a problem connecting to or using the Maven repository, as configured
     */
    public URL getUrl( MavenId mavenId, ArtifactType artifactType, SignatureType signatureType, boolean createIfRequired ) throws MalformedURLException, MavenRepositoryException;

}
