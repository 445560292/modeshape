/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.jcr.JcrRepository.Option;
import org.jboss.dna.repository.DnaEngine;
import org.jboss.dna.repository.RepositoryLibrary;
import org.jboss.dna.repository.RepositoryService;
import org.jboss.dna.repository.sequencer.SequencingService;

/**
 * The basic component that encapsulates the JBoss DNA services, including the {@link Repository} instances.
 */
public class JcrEngine {

    private final DnaEngine dnaEngine;
    private final Map<String, JcrRepository> repositories;
    private final Lock repositoriesLock;

    JcrEngine( DnaEngine dnaEngine ) {
        this.dnaEngine = dnaEngine;
        this.repositories = new HashMap<String, JcrRepository>();
        this.repositoriesLock = new ReentrantLock();
    }

    /**
     * Get the problems that were encountered when setting up this engine from the configuration.
     * 
     * @return the problems, which may be empty but will never be null
     */
    public Problems getProblems() {
        return dnaEngine.getProblems();
    }

    /**
     * Get the execution context for this engine. This context can be used to create additional (perhaps narrowed) contexts.
     * 
     * @return the engine's execution context; never null
     */
    public final ExecutionContext getExecutionContext() {
        return dnaEngine.getExecutionContext();
    }

    /**
     * Get the RepositorySource with the supplied name.
     * 
     * @param repositoryName the name of the repository (or repository source)
     * @return the named repository source, or null if there is no such repository
     */
    protected final RepositorySource getRepositorySource( String repositoryName ) {
        return dnaEngine.getRepositorySource(repositoryName);
    }

    protected final RepositoryConnectionFactory getRepositoryConnectionFactory() {
        return dnaEngine.getRepositoryConnectionFactory();
    }

    protected final RepositoryService getRepositoryService() {
        return dnaEngine.getRepositoryService();
    }

    protected final SequencingService getSequencingService() {
        return dnaEngine.getSequencingService();
    }

    /**
     * Returns a list of the names of all available JCR repositories.
     * <p>
     * In a {@code JcrEngine}, the available repositories are {@link RepositoryLibrary#getSourceNames() all repositories} except
     * for the {@link RepositoryService#getConfigurationSourceName() the configuration repository}.
     * </p>
     * 
     * @return a list of all repository names.
     */
    public final Collection<String> getJcrRepositoryNames() {
        List<String> jcrRepositories = new ArrayList<String>();
        jcrRepositories.addAll(getRepositoryService().getRepositoryLibrary().getSourceNames());
        
        jcrRepositories.remove(getRepositoryService().getConfigurationSourceName());
        
        return jcrRepositories;
    }

    /**
     * Get the {@link Repository} implementation for the named repository.
     * 
     * @param repositoryName the name of the repository, which corresponds to the name of a configured {@link RepositorySource}
     * @return the named repository instance
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws RepositoryException if there is no repository with the specified name
     */
    public final JcrRepository getRepository( String repositoryName ) throws RepositoryException {
        CheckArg.isNotEmpty(repositoryName, "repositoryName");
        try {
            repositoriesLock.lock();
            JcrRepository repository = repositories.get(repositoryName);
            if (repository == null) {
                if (getRepositorySource(repositoryName) == null) {
                    // The repository name is not a valid repository ...
                    String msg = JcrI18n.repositoryDoesNotExist.text(repositoryName);
                    throw new RepositoryException(msg);
                }
                repository = doCreateJcrRepository(repositoryName);
                repositories.put(repositoryName, repository);
            }
            return repository;
        } finally {
            repositoriesLock.unlock();
        }
    }

    protected JcrRepository doCreateJcrRepository( String repositoryName ) {
        RepositoryConnectionFactory connectionFactory = getRepositoryConnectionFactory();
        Map<String, String> descriptors = null;

        /*
         * Extract the JCR options from the configuration graph
         */
        String configurationName = dnaEngine.getRepositoryService().getConfigurationSourceName();
        Map<Option, String> options = new HashMap<Option, String>();

        PathFactory pathFactory = getExecutionContext().getValueFactories().getPathFactory();
        Graph configuration = Graph.create(connectionFactory.createConnection(configurationName), getExecutionContext());

        try {
            Node sources = configuration.getNodeAt(pathFactory.create(DnaLexicon.SOURCES));

            /*
             * Hopefully, this can all get cleaned up when the connector layer supports queries
             */
            for (Location childLocation : sources.getChildren()) {
                Node source = configuration.getNodeAt(childLocation);

                Property nameProperty = source.getProperty("name");
                if (nameProperty != null && nameProperty.getFirstValue().toString().equals(repositoryName)) {
                    for (Location optionsLocation : source.getChildren()) {
                        if (DnaLexicon.OPTIONS.equals(optionsLocation.getPath().getLastSegment().getName())) {
                            Node optionsNode = configuration.getNodeAt(optionsLocation);

                            for (Location optionLocation : optionsNode.getChildren()) {
                                Path.Segment segment = optionLocation.getPath().getLastSegment();
                                Node optionNode = configuration.getNodeAt(optionLocation);
                                Property valueProperty = optionNode.getProperty(DnaLexicon.VALUE);

                                options.put(Option.valueOf(segment.getName().getLocalName()), valueProperty.getFirstValue()
                                                                                                           .toString());

                            }

                        }
                    }
                }
            }
        } catch (PathNotFoundException pnfe) {
            // Must not be any configuration set up
        }

        return new JcrRepository(getExecutionContext(), connectionFactory, repositoryName, descriptors, options);
    }

    /*
     * Lifecycle methods
     */

    /**
     * Start this engine to make it available for use.
     * 
     * @throws IllegalStateException if this method is called when already shut down.
     * @see #shutdown()
     */
    public void start() {
        dnaEngine.start();
    }

    /**
     * Shutdown this engine to close all connections, terminate any ongoing background operations (such as sequencing), and
     * reclaim any resources that were acquired by this engine. This method may be called multiple times, but only the first time
     * has an effect.
     * 
     * @see #start()
     */
    public void shutdown() {
        dnaEngine.shutdown();
    }

    /**
     * Blocks until the shutdown has completed, or the timeout occurs, or the current thread is interrupted, whichever happens
     * first.
     * 
     * @param timeout the maximum time to wait for each component in this engine
     * @param unit the time unit of the timeout argument
     * @return <tt>true</tt> if this service complete shut down and <tt>false</tt> if the timeout elapsed before it was shut down
     *         completely
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination( long timeout,
                                     TimeUnit unit ) throws InterruptedException {
        return dnaEngine.awaitTermination(timeout, unit);
    }

}
