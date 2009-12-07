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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.GraphNamespaceRegistry;
import org.jboss.dna.jcr.JcrRepository.Option;
import org.jboss.dna.repository.DnaConfiguration;
import org.jboss.dna.repository.DnaEngine;

/**
 * The basic component that encapsulates the JBoss DNA services, including the {@link Repository} instances.
 */
@ThreadSafe
public class JcrEngine extends DnaEngine {

    final static int LOCK_SWEEP_INTERVAL_IN_MILLIS = 30000;
    final static int LOCK_EXTENSION_INTERVAL_IN_MILLIS = LOCK_SWEEP_INTERVAL_IN_MILLIS * 2;

    private final Logger log = Logger.getLogger(DnaEngine.class);

    private final Map<String, JcrRepository> repositories;
    private final Lock repositoriesLock;

    /**
     * Provides the ability to schedule lock clean-up
     */
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);

    JcrEngine( ExecutionContext context,
               DnaConfiguration.ConfigurationDefinition configuration ) {
        super(context, configuration);
        this.repositories = new HashMap<String, JcrRepository>();
        this.repositoriesLock = new ReentrantLock();
    }

    /**
     * Clean up session-scoped locks created by session that are no longer active by iterating over the {@link JcrRepository
     * repositories} and calling their {@link JcrRepository#cleanUpLocks() clean-up method}.
     * <p>
     * It should not be possible for a session to be terminated without cleaning up its locks, but this method will help clean-up
     * dangling locks should a session terminate abnormally.
     * </p>
     */
    void cleanUpLocks() {
        Collection<JcrRepository> repos;

        try {
            // Make a copy of the repositories to minimize the time that the lock needs to be held
            repositoriesLock.lock();
            repos = new ArrayList<JcrRepository>(repositories.values());
        } finally {
            repositoriesLock.unlock();
        }

        for (JcrRepository repository : repos) {
            try {
                repository.cleanUpLocks();
            } catch (Throwable t) {
                log.error(t, JcrI18n.errorCleaningUpLocks, repository.getRepositorySourceName());
            }
        }
    }

    @Override
    public void shutdown() {
        scheduler.shutdown();

        super.shutdown();
    }

    @Override
    public boolean awaitTermination( long timeout,
                                     TimeUnit unit ) throws InterruptedException {
        if (!scheduler.awaitTermination(timeout, unit)) return false;

        return super.awaitTermination(timeout, unit);
    }

    @Override
    public void start() {
        super.start();

        final JcrEngine engine = this;
        Runnable cleanUpTask = new Runnable() {

            public void run() {
                engine.cleanUpLocks();
            }

        };
        scheduler.scheduleAtFixedRate(cleanUpTask, 0, LOCK_SWEEP_INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Get the {@link Repository} implementation for the named repository.
     * 
     * @param repositoryName the name of the repository, which corresponds to the name of a configured {@link RepositorySource}
     * @return the named repository instance
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws RepositoryException if there is no repository with the specified name
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public final JcrRepository getRepository( String repositoryName ) throws RepositoryException {
        CheckArg.isNotEmpty(repositoryName, "repositoryName");
        checkRunning();
        try {
            repositoriesLock.lock();
            JcrRepository repository = repositories.get(repositoryName);
            if (repository == null) {
                try {
                    repository = doCreateJcrRepository(repositoryName);
                } catch (PathNotFoundException e) {
                    // The repository name is not a valid repository ...
                    String msg = JcrI18n.repositoryDoesNotExist.text(repositoryName);
                    throw new RepositoryException(msg);
                }
                repositories.put(repositoryName, repository);
            }
            return repository;
        } finally {
            repositoriesLock.unlock();
        }
    }

    /**
     * Get the names of each of the JCR repositories.
     * 
     * @return the immutable names of the repositories that exist at the time this method is called
     */
    public Set<String> getRepositoryNames() {
        checkRunning();
        Set<String> results = new HashSet<String>();
        // Read the names of the JCR repositories from the configuration (not from the Repository objects used so far) ...
        PathFactory pathFactory = getExecutionContext().getValueFactories().getPathFactory();
        Path repositoriesPath = pathFactory.create(configuration.getPath(), DnaLexicon.REPOSITORIES);
        Graph configuration = getConfigurationGraph();
        for (Location child : configuration.getChildren().of(repositoriesPath)) {
            Name repositoryName = child.getPath().getLastSegment().getName();
            results.add(readable(repositoryName));
        }
        return Collections.unmodifiableSet(results);
    }

    protected JcrRepository doCreateJcrRepository( String repositoryName ) throws RepositoryException, PathNotFoundException {
        RepositoryConnectionFactory connectionFactory = getRepositoryConnectionFactory();
        Map<String, String> descriptors = null;
        Map<Option, String> options = new HashMap<Option, String>();

        // Read the subgraph that represents the repository ...
        PathFactory pathFactory = getExecutionContext().getValueFactories().getPathFactory();
        Path repositoriesPath = pathFactory.create(configuration.getPath(), DnaLexicon.REPOSITORIES);
        Path repositoryPath = pathFactory.create(repositoriesPath, repositoryName);
        Graph configuration = getConfigurationGraph();
        Subgraph subgraph = configuration.getSubgraphOfDepth(6).at(repositoryPath);

        // Read the options ...
        Node optionsNode = subgraph.getNode(DnaLexicon.OPTIONS);
        if (optionsNode != null) {
            for (Location optionLocation : optionsNode.getChildren()) {
                Node optionNode = configuration.getNodeAt(optionLocation);
                Path.Segment segment = optionLocation.getPath().getLastSegment();
                Property valueProperty = optionNode.getProperty(DnaLexicon.VALUE);
                if (valueProperty == null) continue;
                Option option = Option.findOption(segment.getName().getLocalName());
                if (option == null) continue;
                options.put(option, valueProperty.getFirstValue().toString());
            }
        }

        // Read the namespaces ...
        ExecutionContext context = getExecutionContext();
        Node namespacesNode = subgraph.getNode(DnaLexicon.NAMESPACES);
        if (namespacesNode != null) {
            GraphNamespaceRegistry registry = new GraphNamespaceRegistry(configuration, namespacesNode.getLocation().getPath(),
                                                                         DnaLexicon.NAMESPACE_URI);
            context = context.with(registry);
        }

        // Get the name of the source ...
        Property property = subgraph.getRoot().getProperty(DnaLexicon.SOURCE_NAME);
        if (property == null || property.isEmpty()) {
            String readableName = readable(DnaLexicon.SOURCE_NAME);
            String readablePath = readable(subgraph.getLocation());
            String msg = JcrI18n.propertyNotFoundOnNode.text(readableName, readablePath, configuration.getCurrentWorkspaceName());
            throw new RepositoryException(msg);
        }
        String sourceName = context.getValueFactories().getStringFactory().create(property.getFirstValue());

        // Create the repository ...
        JcrRepository repository = new JcrRepository(context, connectionFactory, sourceName,
                                                     getRepositoryService().getRepositoryLibrary(), descriptors, options);

        // Register all the the node types ...
        Node nodeTypesNode = subgraph.getNode(JcrLexicon.NODE_TYPES);
        if (nodeTypesNode != null) {
            repository.getRepositoryTypeManager().registerNodeTypes(subgraph, nodeTypesNode.getLocation());// throws exception
        }

        return repository;
    }

    protected final String readable( Name name ) {
        return name.getString(context.getNamespaceRegistry());
    }

    protected final String readable( Path path ) {
        return path.getString(context.getNamespaceRegistry());
    }

    protected final String readable( Location location ) {
        return location.getString(context.getNamespaceRegistry());
    }
}
