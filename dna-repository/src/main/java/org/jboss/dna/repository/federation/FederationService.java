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
package org.jboss.dna.repository.federation;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.Reflection;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.AdministeredService;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicCompositeCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetChildrenCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetNodeCommand;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionFactory;
import org.jboss.dna.spi.graph.connection.RepositorySource;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class FederationService implements AdministeredService {

    protected static final String CLASSNAME_PROPERTY_NAME = "dna:classname";
    protected static final String CLASSPATH_PROPERTY_NAME = "dna:classpath";

    /**
     * The administrative component for this service.
     * 
     * @author Randall Hauch
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(RepositoryI18n.federationServiceName, State.STARTED);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doStart( State fromState ) {
            super.doStart(fromState);
            startService();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean doCheckIsTerminated() {
            return FederationService.this.isTerminated();
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) throws InterruptedException {
            return FederationService.this.awaitTermination(timeout, unit);
        }

    }

    private final ClassLoaderFactory classLoaderFactory;
    private final ExecutionEnvironment env;
    private final RepositorySourceManager sources;
    private final FederatedRegion configurationRegion;
    private final Administrator administrator = new Administrator();
    private final ConcurrentMap<String, FederatedRepository> repositories = new ConcurrentHashMap<String, FederatedRepository>();
    private RepositoryConnection configurationConnection;

    /**
     * Create a federation service instance
     * 
     * @param sources the source manager
     * @param configurationRegion the repository region defining where the service can find configuration information for the
     *        different repositories that it is to manage
     * @param env the execution environment in which this service should run
     * @param classLoaderFactory the class loader factory used to instantiate {@link RepositorySource} instances; may be null if
     *        this instance should use a default factory that attempts to load classes first from the
     *        {@link Thread#getContextClassLoader() thread's current context class loader} and then from the class loader that
     *        loaded this class.
     * @throws IllegalArgumentException if the bootstrap source is null or the execution context is null
     */
    public FederationService( RepositorySourceManager sources,
                              FederatedRegion configurationRegion,
                              ExecutionEnvironment env,
                              ClassLoaderFactory classLoaderFactory ) {
        ArgCheck.isNotNull(configurationRegion, "configurationRegion");
        ArgCheck.isNotNull(sources, "sources");
        ArgCheck.isNotNull(env, "env");
        this.sources = sources;
        this.configurationRegion = configurationRegion;
        this.env = env;
        this.classLoaderFactory = classLoaderFactory != null ? classLoaderFactory : new StandardClassLoaderFactory();
    }

    /**
     * {@inheritDoc}
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * @return configurationRegion
     */
    public FederatedRegion getConfigurationRegion() {
        return configurationRegion;
    }

    /**
     * @return sources
     */
    public RepositorySourceManager getRepositorySourceManager() {
        return sources;
    }

    /**
     * @return env
     */
    public ExecutionEnvironment getExecutionEnvironment() {
        return env;
    }

    /**
     * @return classLoaderFactory
     */
    public ClassLoaderFactory getClassLoaderFactory() {
        return this.classLoaderFactory;
    }

    public String getJndiName() {
        // TODO
        return null;
    }

    protected synchronized void startService() {
        if (this.configurationConnection == null) {
            Problems problems = new SimpleProblems();

            // ------------------------------------------------------------------------------------
            // Establish a connection to the configuration source ...
            // ------------------------------------------------------------------------------------
            final String configurationSourceName = configurationRegion.getSourceName();
            RepositoryConnectionFactory factory = sources.getConnectionFactory(configurationSourceName);
            if (factory == null) {
                I18n msg = RepositoryI18n.unableToFindRepositorySourceByName;
                throw new FederationException(msg.text(configurationSourceName));
            }
            try {
                this.configurationConnection = factory.getConnection();
            } catch (InterruptedException err) {
                I18n msg = RepositoryI18n.interruptedWhileConnectingToFederationConfigurationRepository;
                throw new FederationException(msg.text(configurationSourceName));
            }

            // ------------------------------------------------------------------------------------
            // Read the configuration ...
            // ------------------------------------------------------------------------------------
            ValueFactories valueFactories = env.getValueFactories();
            PathFactory pathFactory = valueFactories.getPathFactory();

            // Read the configuration and the repository sources, located as child nodes/branches under "/dna:sources",
            // and then instantiate and register each in the "sources" manager
            try {
                Path sourcesNode = pathFactory.create("/dna:sources");
                BasicGetChildrenCommand getSources = new BasicGetChildrenCommand(sourcesNode);
                configurationConnection.execute(env, getSources);

                // Build the commands to get each of the children ...
                List<Path.Segment> children = getSources.getChildren();
                if (children.isEmpty()) {
                    BasicCompositeCommand commands = new BasicCompositeCommand();
                    for (Path.Segment child : getSources.getChildren()) {
                        final Path pathToSource = pathFactory.create(sourcesNode, child);
                        commands.add(new BasicGetNodeCommand(pathToSource));
                    }
                    configurationConnection.execute(env, commands);

                    // Iterate over each source node obtained ...
                    for (GraphCommand command : commands) {
                        BasicGetNodeCommand getSourceCommand = (BasicGetNodeCommand)command;
                        RepositorySource source = createRepositorySource(getSourceCommand.getPath(),
                                                                         getSourceCommand.getProperties(),
                                                                         problems);
                        if (source != null) sources.addSource(source);
                    }
                }
            } catch (InterruptedException err) {
                I18n msg = RepositoryI18n.interruptedWhileUsingFederationConfigurationRepository;
                throw new FederationException(msg.text(configurationSourceName));
            }

        }
    }

    /**
     * Instantiate the {@link RepositorySource} described by the supplied properties.
     * 
     * @param path the path to the node where these properties were found; never null
     * @param properties the properties; never null
     * @param problems the problems container in which any problems should be reported; never null
     * @return the repository source instance, or null if it could not be created
     */
    @SuppressWarnings( "null" )
    protected RepositorySource createRepositorySource( Path path,
                                                       Map<Name, Property> properties,
                                                       Problems problems ) {
        ValueFactories valueFactories = env.getValueFactories();
        NameFactory nameFactory = valueFactories.getNameFactory();
        ValueFactory<String> stringFactory = valueFactories.getStringFactory();

        // Get the classname and classpath ...
        Property classnameProperty = properties.get(nameFactory.create(CLASSNAME_PROPERTY_NAME));
        Property classpathProperty = properties.get(nameFactory.create(CLASSPATH_PROPERTY_NAME));
        if (classnameProperty == null) {
            problems.addError(RepositoryI18n.requiredPropertyIsMissingFromNode, CLASSNAME_PROPERTY_NAME, path);
        }
        if (classpathProperty == null) {
            problems.addError(RepositoryI18n.requiredPropertyIsMissingFromNode, CLASSPATH_PROPERTY_NAME, path);
        }
        if (problems.hasErrors()) return null;

        // Create the instance ...
        String classname = stringFactory.create(classnameProperty.getValues().next());
        String[] classpath = stringFactory.create(classpathProperty.getValuesAsArray());
        ClassLoader classLoader = this.classLoaderFactory.getClassLoader(classpath);
        RepositorySource source = null;
        try {
            Class<?> sourceClass = classLoader.loadClass(classname);
            source = (RepositorySource)sourceClass.newInstance();
        } catch (ClassNotFoundException err) {
            problems.addError(err, RepositoryI18n.unableToLoadClassUsingClasspath, classname, classpath);
        } catch (IllegalAccessException err) {
            problems.addError(err, RepositoryI18n.unableToAccessClassUsingClasspath, classname, classpath);
        } catch (Throwable err) {
            problems.addError(err, RepositoryI18n.unableToInstantiateClassUsingClasspath, classname, classpath);
        }

        // Now set all the properties that we can, ignoring any property that doesn't fit pattern ...
        Reflection reflection = new Reflection(source.getClass());
        for (Map.Entry<Name, Property> entry : properties.entrySet()) {
            Name propertyName = entry.getKey();
            Property property = entry.getValue();
            String javaPropertyName = propertyName.getLocalName();
            if (property.isEmpty()) continue;
            Object value = null;
            if (property.isSingle()) {
                value = property.getValues().next();
            } else if (property.isMultiple()) {
                value = property.getValuesAsArray();
            }
            try {
                reflection.invokeSetterMethodOnTarget(javaPropertyName, source, value);
            } catch (SecurityException err) {
                // Do nothing ... assume not a JavaBean property
            } catch (NoSuchMethodException err) {
                // Do nothing ... assume not a JavaBean property
            } catch (IllegalArgumentException err) {
                // Do nothing ... assume not a JavaBean property
            } catch (IllegalAccessException err) {
                // Do nothing ... assume not a JavaBean property
            } catch (InvocationTargetException err) {
                // Do nothing ... assume not a JavaBean property
            }
        }
        return source;
    }

    /**
     * Get the federated repository object with the given name. The resulting repository will be started and ready to use.
     * 
     * @param name the name of the repository
     * @return the repository instance
     */
    protected FederatedRepository getRepository( String name ) {
        if (this.configurationConnection == null) startService();

        // Look for an existing repository ...
        FederatedRepository repository = this.repositories.get(name);
        if (repository == null) {
            // Look up the node representing the repository in the configuration ...

            // // New up a repository and configure it ...
            // Repository
            repository = new FederatedRepository(name, env, sources);
            // Now register it, being careful to not overwrite any added since "get" call above ..
            FederatedRepository existingRepository = this.repositories.putIfAbsent(name, repository);
            if (existingRepository != null) repository = existingRepository;
        }
        // Make sure it's started. By doing this here, whoever finds it in the map will start it.
        repository.getAdministrator().start();
        return repository;
    }

    protected synchronized void shutdownService() {
        if (this.configurationConnection != null) {
            try {
                this.configurationConnection.close();
            } catch (InterruptedException err) {
                I18n msg = RepositoryI18n.interruptedWhileClosingConnectionToFederationConfigurationRepository;
                throw new FederationException(msg.text(configurationRegion.getSourceName()));
            }
            // Now shut down all repositories ...
            for (String repositoryName : this.repositories.keySet()) {
                FederatedRepository repository = this.repositories.get(repositoryName);
                repository.getAdministrator().shutdown();
            }
        }
    }

    protected boolean isTerminated() {
        // Now shut down all repositories ...
        for (String repositoryName : this.repositories.keySet()) {
            FederatedRepository repository = this.repositories.get(repositoryName);
            if (!repository.getAdministrator().isTerminated()) return false;
        }
        return true;
    }

    protected boolean awaitTermination( long timeout,
                                        TimeUnit unit ) throws InterruptedException {
        // Now shut down all repositories ...
        for (String repositoryName : this.repositories.keySet()) {
            FederatedRepository repository = this.repositories.get(repositoryName);
            if (repository.getAdministrator().awaitTermination(timeout, unit)) return false;
        }
        return true;
    }

    /**
     * Create a {@link RepositorySource} that can be used to establish connections to the federated repository with the supplied
     * name.
     * 
     * @param repositoryName the name of the federated repository
     * @return the source that can be configured and used to establish connection to the repository
     */
    public RepositorySource createRepositorySource( String repositoryName ) {
        FederatedRepositorySource source = new FederatedRepositorySource(this, repositoryName);
        return source;
    }

    /**
     * Get the current set of repository names.
     * 
     * @return the unmodifiable names of the repository.
     */
    public Set<String> getRepositoryNames() {
        return Collections.unmodifiableSet(this.repositories.keySet());
    }

    protected void removeRepository( FederatedRepository repository ) {
        this.repositories.remove(repository.getName(), repository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        return false;
    }
}
