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
package org.jboss.dna.repository;

import java.util.ArrayList;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.text.Inflector;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Reflection;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathExpression;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.basic.RootPath;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.jboss.dna.repository.sequencer.Sequencer;

/**
 * @param <BuilderType>
 */
public abstract class Configurator<BuilderType> {

    /**
     * Interface used to configure a sequencer.
     * 
     * @param <ReturnType> the type of interface to return after the sequencer's configuration is completed
     */
    public interface SequencerConfigurator<ReturnType> {

        /**
         * Add a new {@link StreamSequencer sequencer} to this configuration. The new sequencer will have the supplied name, and
         * if the name of an existing sequencer is used, this will replace the existing sequencer configuration.
         * 
         * @param id the identifier of the new sequencer
         * @return the interface for choosing the class, which returns the interface used to configure the sequencer; never null
         * @throws IllegalArgumentException if the sequencer name is null, empty, or otherwise invalid
         */
        public ChooseClass<StreamSequencer, SequencerDetails<ReturnType>> addSequencer( String id );

        /**
         * Get the details for the sequencer with the supplied identifier.
         * 
         * @param id the identifier of the sequencer that is to be added
         * @return the details for the repository source, or null if there is no such source in this configuration
         */
        public SequencerDetails<ReturnType> sequencer( String id );
    }

    /**
     * Interface used to initialize the configurator to use a specific repository containing configuration information.
     * 
     * @param <ReturnType> the configurator type returned after the configuration repository is defined
     */
    public interface Initializer<ReturnType> {
        /**
         * Specify that this configuration should use a particular {@link RepositorySource} for its configuration repository. By
         * default each configuration uses an internal transient repository for its configuration, but using this method will make
         * the configuration use a different repository (that is perhaps shared with other processes).
         * 
         * @return the interface for choosing the class, which returns the interface used to configure the source that will be
         *         used for the configuration repository; never null
         */
        public ChooseClass<RepositorySource, ConfigSourceDetails<ReturnType>> withConfigurationSource();

        /**
         * Get the details for the configuration repository source.
         * 
         * @return the details for the configuration's repository source; never null
         */
        public ConfigSourceDetails<ReturnType> configurationSource();
    }

    /**
     * Interface used to configure a repository source.
     * 
     * @param <ReturnType> the type of interface to return after the repository source's configuration is completed
     */
    public interface RepositorySourceConfigurator<ReturnType> {
        /**
         * Add a new {@link RepositorySource repository} for this configuration. The new repository will have the supplied name,
         * and if the name of an existing repository is used, this will replace the existing repository configuration.
         * 
         * @param id the id of the new repository that is to be added
         * @return the interface for choosing the class, which returns the interface used to configure the repository source;
         *         never null
         * @throws IllegalArgumentException if the repository name is null, empty, or otherwise invalid
         * @see #addSource(RepositorySource)
         */
        public ChooseClass<RepositorySource, RepositorySourceDetails<ReturnType>> addSource( final String id );

        /**
         * Add a new {@link RepositorySource repository} for this configuration. The new repository will have the supplied name,
         * and if the name of an existing repository is used, this will replace the existing repository configuration.
         * 
         * @param source the {@link RepositorySource} instance that should be used
         * @return this configuration object, for method-chaining purposes
         * @throws IllegalArgumentException if the repository source reference is null
         * @see #addSource(String)
         */
        public ReturnType addSource( RepositorySource source );

        /**
         * Get the details for the repository source with the supplied identifier.
         * 
         * @param id the identifier of the repository that is to be added
         * @return the details for the repository source, or null if there is no such source in this configuration
         */
        public RepositorySourceDetails<ReturnType> source( String id );
    }

    /**
     * Interface used to configure a MIME type detector.
     * 
     * @param <ReturnType> the type of interface to return after the detector's configuration is completed
     */
    public interface MimeDetectorConfigurator<ReturnType> {
        /**
         * Add a new {@link MimeTypeDetector MIME type detector} to this configuration. The new detector will have the supplied
         * name, and if the name of an existing detector is used, this will replace the existing detector configuration.
         * 
         * @param id the id of the new detector
         * @return the interface for choosing the class, which returns the interface used to configure the detector; never null
         * @throws IllegalArgumentException if the detector name is null, empty, or otherwise invalid
         */
        public ChooseClass<MimeTypeDetector, MimeTypeDetectorDetails<ReturnType>> addMimeTypeDetector( final String id );

        /**
         * Get the details for the MIME type detector with the supplied identifier.
         * 
         * @param id the identifier of the MIME type detector that is to be added
         * @return the details for the MIME type detector, or null if there is no such detector in this configuration
         */
        public MimeTypeDetectorDetails<ReturnType> mimeTypeDetector( String id );
    }

    /**
     * Interface used to build the configured component.
     * 
     * @param <ReturnType> the type of component that this configuration builds
     */
    public interface Builder<ReturnType> {
        /**
         * Complete this configuration and create the corresponding engine.
         * 
         * @return the new engine configured by this instance
         * @throws DnaConfigurationException if the engine cannot be created from this configuration.
         */
        public ReturnType build() throws DnaConfigurationException;
    }

    /**
     * Interface used to configure a {@link RepositorySource repository}.
     * 
     * @param <ReturnType>
     */
    public interface RepositorySourceDetails<ReturnType>
        extends SetName<RepositorySourceDetails<ReturnType>>, SetDescription<RepositorySourceDetails<ReturnType>>,
        SetProperties<RepositorySourceDetails<ReturnType>>, And<ReturnType> {
    }

    /**
     * Interface used to define the configuration repository.
     * 
     * @param <ReturnType>
     */
    public interface ConfigSourceDetails<ReturnType>
        extends SetDescription<ConfigSourceDetails<ReturnType>>, SetProperties<ConfigSourceDetails<ReturnType>>, And<ReturnType> {
        /**
         * Specify the path under which the configuration content is to be found. This path is assumed to be "/" by default.
         * 
         * @param path the path to the configuration content in the configuration source; may not be null
         * @return this instance for method chaining purposes; never null
         */
        public ConfigSourceDetails<ReturnType> under( String path );

        /**
         * Specify the path under which the configuration content is to be found. This path is assumed to be "/" by default.
         * 
         * @param workspace the name of the workspace with the configuration content in the configuration source; may not be null
         * @return this instance for method chaining purposes; never null
         */
        public ConfigSourceDetails<ReturnType> usingWorkspace( String workspace );
    }

    /**
     * Interface used to configure a {@link Sequencer sequencer}.
     * 
     * @param <ReturnType>
     */
    public interface SequencerDetails<ReturnType>
        extends SetName<SequencerDetails<ReturnType>>, SetDescription<SequencerDetails<ReturnType>>, And<ReturnType> {

        /**
         * Specify the input {@link PathExpression path expression} represented as a string, which determines when this sequencer
         * will be executed.
         * 
         * @param inputPathExpression the path expression for nodes that, when they change, will be passed as an input to the
         *        sequencer
         * @return the interface used to specify the output path expression; never null
         */
        PathExpressionOutput<ReturnType> sequencingFrom( String inputPathExpression );

        /**
         * Specify the input {@link PathExpression path expression}, which determines when this sequencer will be executed.
         * 
         * @param inputPathExpression the path expression for nodes that, when they change, will be passed as an input to the
         *        sequencer
         * @return the interface used to continue specifying the configuration of the sequencer
         */
        SequencerDetails<ReturnType> sequencingFrom( PathExpression inputPathExpression );
    }

    /**
     * Interface used to specify the output path expression for a
     * {@link Configurator.SequencerDetails#sequencingFrom(PathExpression) sequencer configuration}.
     * 
     * @param <ReturnType>
     */
    public interface PathExpressionOutput<ReturnType> {
        /**
         * Specify the output {@link PathExpression path expression}, which determines where this sequencer's output will be
         * placed.
         * 
         * @param outputExpression the path expression for the location(s) where output generated by the sequencer is to be placed
         * @return the interface used to continue specifying the configuration of the sequencer
         */
        SequencerDetails<ReturnType> andOutputtingTo( String outputExpression );
    }

    /**
     * Interface used to configure a {@link MimeTypeDetector MIME type detector}.
     * 
     * @param <ReturnType>
     */
    public interface MimeTypeDetectorDetails<ReturnType>
        extends SetName<MimeTypeDetectorDetails<ReturnType>>, SetDescription<MimeTypeDetectorDetails<ReturnType>>,
        SetProperties<MimeTypeDetectorDetails<ReturnType>>, And<ReturnType> {
    }

    /**
     * Interface for configuring the JavaBean-style properties of an object.
     * 
     * @param <ReturnType> the interface returned after the property has been set.
     * @author Randall Hauch
     */
    public interface SetProperties<ReturnType> {
        /**
         * Set the property value to an integer.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                int value );

        /**
         * Set the property value to a long number.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                long value );

        /**
         * Set the property value to a short.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                short value );

        /**
         * Set the property value to a boolean.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                boolean value );

        /**
         * Set the property value to a float.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                float value );

        /**
         * Set the property value to a double.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                double value );

        /**
         * Set the property value to a string.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                String value );

        /**
         * Set the property value to an object.
         * 
         * @param beanPropertyName the name of the JavaBean-style property (e.g., "retryLimit")
         * @param value the new value for the property
         * @return the next component to continue configuration; never null
         */
        ReturnType setProperty( String beanPropertyName,
                                Object value );
    }

    /**
     * The interface used to configure the class used for a component.
     * 
     * @param <ComponentClassType> the class or interface that the component is to implement
     * @param <ReturnType> the interface returned from these methods
     */
    public interface ChooseClass<ComponentClassType, ReturnType> {

        /**
         * Specify the name of the class that should be instantiated for the instance. The classpath information will need to be
         * defined using the returned interface.
         * 
         * @param classname the name of the class that should be instantiated
         * @return the interface used to define the classpath information; never null
         * @throws IllegalArgumentException if the class name is null, empty, blank, or not a valid class name
         */
        LoadedFrom<ReturnType> usingClass( String classname );

        /**
         * Specify the class that should be instantiated for the instance. Because the class is already available to this class
         * loader, there is no need to specify the classloader information.
         * 
         * @param clazz the class that should be instantiated
         * @return the next component to continue configuration; never null
         * @throws DnaConfigurationException if the class could not be accessed and instantiated (if needed)
         * @throws IllegalArgumentException if the class reference is null
         */
        ReturnType usingClass( Class<? extends ComponentClassType> clazz );
    }

    /**
     * The interface used to set a description on a component.
     * 
     * @param <ReturnType> the interface returned from these methods
     */
    public interface SetDescription<ReturnType> {
        /**
         * Specify the description of this component.
         * 
         * @param description the description; may be null or empty
         * @return the next component to continue configuration; never null
         */
        ReturnType describedAs( String description );
    }

    /**
     * The interface used to set a human readable name on a component.
     * 
     * @param <ReturnType> the interface returned from these methods
     */
    public interface SetName<ReturnType> {
        /**
         * Specify the human-readable name for this component.
         * 
         * @param name the name; may be null or empty
         * @return the next component to continue configuration; never null
         */
        ReturnType named( String name );
    }

    /**
     * Interface for specifying from where the component's class is to be loaded.
     * 
     * @param <ReturnType> the interface returned from these methods
     */
    public interface LoadedFrom<ReturnType> {
        /**
         * Specify the names of the classloaders that form the classpath for the component, from which the component's class (and
         * its dependencies) can be loaded. The names correspond to the names supplied to the
         * {@link ExecutionContext#getClassLoader(String...)} methods.
         * 
         * @param classPathNames the names for the classloaders, as passed to the {@link ClassLoaderFactory} implementation (e.g.,
         *        the {@link ExecutionContext}).
         * @return the next component to continue configuration; never null
         * @see #loadedFromClasspath()
         * @see ExecutionContext#getClassLoader(String...)
         */
        ReturnType loadedFrom( String... classPathNames );

        /**
         * Specify that the component (and its dependencies) will be found on the current (or
         * {@link Thread#getContextClassLoader() current context}) classloader.
         * 
         * @return the next component to continue configuration; never null
         * @see #loadedFrom(String...)
         * @see ExecutionContext#getClassLoader(String...)
         */
        ReturnType loadedFromClasspath();
    }

    /**
     * Continue with another aspect of configuration.
     * 
     * @param <ReturnType>
     */
    public interface And<ReturnType> {

        /**
         * Return a reference to the next configuration interface for additional operations.
         * 
         * @return a reference to the next configuration interface
         */
        ReturnType and();
    }

    protected final BuilderType builder;
    protected final ExecutionContext context;
    protected ConfigurationRepository configurationSource;
    private Graph graph;
    private Graph.Batch batch;
    private final Problems problems;

    /**
     * Specify a new {@link ExecutionContext} that should be used for this DNA instance.
     * 
     * @param context the new context, or null if a default-constructed execution context should be used
     * @param builder the builder
     * @throws IllegalArgumentException if the supplied context reference is null
     */
    protected Configurator( ExecutionContext context,
                            BuilderType builder ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(builder, "builder");
        this.context = context;
        this.builder = builder;
        this.problems = new SimpleProblems();

        // Set up the default configuration repository ...
        this.configurationSource = createDefaultConfigurationSource();
    }

    /**
     * @return problems
     */
    public Problems getProblems() {
        return problems;
    }

    /**
     * Method that is used to set up the default configuration repository source. By default, this method sets up the
     * {@link InMemoryRepositorySource} loaded from the classpath.
     * 
     * @return the default repository source
     */
    protected ConfigurationRepository createDefaultConfigurationSource() {
        InMemoryRepositorySource defaultSource = new InMemoryRepositorySource();
        defaultSource.setName("Configuration");
        ConfigurationRepository result = new ConfigurationRepository(defaultSource, "Configuration Repository", null, null);
        return result;
    }

    /**
     * Get the execution context used by this configurator.
     * 
     * @return the execution context; never null
     */
    public final ExecutionContext getExecutionContext() {
        return this.context;
    }

    protected final PathFactory pathFactory() {
        return getExecutionContext().getValueFactories().getPathFactory();
    }

    /**
     * Get the graph containing the configuration information.
     * 
     * @return the configuration repository graph; never null
     * @see #graph()
     */
    protected final Graph graph() {
        if (this.graph == null) {
            this.graph = Graph.create(configurationSource.getRepositorySource(), context);
        }
        return this.graph;
    }

    /**
     * Get the graph batch that can be used to change the configuration, where the changes are enqueued until {@link #save()
     * saved}.
     * 
     * @return the latest batch for changes to the configuration repository; never null
     * @see #graph()
     */
    protected final Graph.Batch configuration() {
        if (this.batch == null) {
            this.batch = graph().batch();
        }
        return this.batch;
    }

    /**
     * Save any changes that have been made so far to the configuration. This method does nothing if no changes have been made.
     * 
     * @return this configuration object for method chaining purposes; never null
     */
    public BuilderType save() {
        if (this.batch != null) {
            this.batch.execute();
            this.batch = this.graph.batch();
        }
        return this.builder;
    }

    protected abstract Name nameFor( String name );

    protected Path createOrReplaceNode( Path parentPath,
                                        String id,
                                        Name propertyName,
                                        Object value ) {
        Path path = pathFactory().create(parentPath, id);
        configuration().create(path).with(propertyName, value).and();
        return path;

    }

    protected Path createOrReplaceNode( Path parentPath,
                                        Name id,
                                        Name propertyName,
                                        Object value ) {
        Path path = pathFactory().create(parentPath, id);
        configuration().create(path).with(propertyName, value).and();
        return path;
    }

    protected Path createOrReplaceNode( Path parentPath,
                                        String id ) {
        Path path = pathFactory().create(parentPath, id);
        configuration().create(path).and();
        return path;

    }

    protected Path createOrReplaceNode( Path parentPath,
                                        Name id ) {
        Path path = pathFactory().create(parentPath, id);
        configuration().create(path).and();
        return path;
    }

    protected void recordBeanPropertiesInGraph( Path path,
                                                Object javaBean ) {
        Reflection reflector = new Reflection(javaBean.getClass());
        for (String propertyName : reflector.findGetterPropertyNames()) {
            Object value;
            try {
                value = reflector.invokeGetterMethodOnTarget(propertyName, javaBean);
                if (value == null) continue;
                propertyName = Inflector.getInstance().lowerCamelCase(propertyName);
                configuration().set(nameFor(propertyName)).to(value).on(path);
            } catch (ValueFormatException err) {
                throw err;
            } catch (Throwable err) {
                // Unable to call getter and set property
            }
        }
    }

    protected class ConfigurationRepositoryClassChooser<ReturnType>
        implements ChooseClass<RepositorySource, ConfigSourceDetails<ReturnType>> {

        private final ReturnType returnObject;

        protected ConfigurationRepositoryClassChooser( ReturnType returnObject ) {
            assert returnObject != null;
            this.returnObject = returnObject;
        }

        public LoadedFrom<ConfigSourceDetails<ReturnType>> usingClass( final String className ) {
            return new LoadedFrom<ConfigSourceDetails<ReturnType>>() {
                @SuppressWarnings( "unchecked" )
                public ConfigSourceDetails loadedFrom( String... classpath ) {
                    ClassLoader classLoader = getExecutionContext().getClassLoader(classpath);
                    Class<? extends RepositorySource> clazz = null;
                    try {
                        clazz = (Class<? extends RepositorySource>)classLoader.loadClass(className);
                    } catch (ClassNotFoundException err) {
                        throw new DnaConfigurationException(RepositoryI18n.unableToLoadClassUsingClasspath.text(className,
                                                                                                                classpath));
                    }
                    return usingClass(clazz);
                }

                @SuppressWarnings( "unchecked" )
                public ConfigSourceDetails loadedFromClasspath() {
                    Class<? extends RepositorySource> clazz = null;
                    try {
                        clazz = (Class<? extends RepositorySource>)Class.forName(className);
                    } catch (ClassNotFoundException err) {
                        throw new DnaConfigurationException(RepositoryI18n.unableToLoadClass.text(className));
                    }
                    return usingClass(clazz);
                }
            };
        }

        public ConfigSourceDetails<ReturnType> usingClass( Class<? extends RepositorySource> repositorySource ) {
            try {
                Configurator.this.configurationSource = new ConfigurationRepository(repositorySource.newInstance());
            } catch (InstantiationException err) {
                I18n msg = RepositoryI18n.errorCreatingInstanceOfClass;
                throw new DnaConfigurationException(msg.text(repositorySource.getName(), err.getLocalizedMessage()), err);
            } catch (IllegalAccessException err) {
                I18n msg = RepositoryI18n.errorCreatingInstanceOfClass;
                throw new DnaConfigurationException(msg.text(repositorySource.getName(), err.getLocalizedMessage()), err);
            }
            return new ConfigurationSourceDetails<ReturnType>(returnObject);
        }
    }

    protected class ConfigurationSourceDetails<ReturnType> implements ConfigSourceDetails<ReturnType> {
        private final ReturnType returnObject;

        protected ConfigurationSourceDetails( ReturnType returnObject ) {
            assert returnObject != null;
            this.returnObject = returnObject;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetDescription#describedAs(java.lang.String)
         */
        public ConfigSourceDetails<ReturnType> describedAs( String description ) {
            Configurator.this.configurationSource = Configurator.this.configurationSource.withDescription(description);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, boolean)
         */
        public ConfigSourceDetails<ReturnType> setProperty( String propertyName,
                                                            boolean value ) {
            return setProperty(propertyName, (Object)value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, int)
         */
        public ConfigSourceDetails<ReturnType> setProperty( String propertyName,
                                                            int value ) {
            return setProperty(propertyName, (Object)value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, short)
         */
        public ConfigSourceDetails<ReturnType> setProperty( String propertyName,
                                                            short value ) {
            return setProperty(propertyName, (Object)value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, long)
         */
        public ConfigSourceDetails<ReturnType> setProperty( String propertyName,
                                                            long value ) {
            return setProperty(propertyName, (Object)value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, float)
         */
        public ConfigSourceDetails<ReturnType> setProperty( String propertyName,
                                                            float value ) {
            return setProperty(propertyName, (Object)value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, double)
         */
        public ConfigSourceDetails<ReturnType> setProperty( String propertyName,
                                                            double value ) {
            return setProperty(propertyName, (Object)value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, java.lang.String)
         */
        public ConfigSourceDetails<ReturnType> setProperty( String propertyName,
                                                            String value ) {
            return setProperty(propertyName, (Object)value);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, java.lang.Object)
         */
        public ConfigSourceDetails<ReturnType> setProperty( String beanPropertyName,
                                                            Object value ) {
            // Set the JavaBean-style property on the RepositorySource instance ...
            Object javaBean = Configurator.this.configurationSource.getRepositorySource();
            Reflection reflection = new Reflection(javaBean.getClass());
            try {
                reflection.invokeSetterMethodOnTarget(beanPropertyName, javaBean, value);
            } catch (Throwable err) {
                I18n msg = RepositoryI18n.errorSettingJavaBeanPropertyOnInstanceOfClass;
                throw new DnaConfigurationException(msg.text(beanPropertyName, javaBean.getClass(), err.getMessage()), err);
            }
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.ConfigSourceDetails#usingWorkspace(java.lang.String)
         */
        public ConfigSourceDetails<ReturnType> usingWorkspace( String workspace ) {
            Configurator.this.configurationSource = Configurator.this.configurationSource.withWorkspace(workspace);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.ConfigSourceDetails#under(java.lang.String)
         */
        public ConfigSourceDetails<ReturnType> under( String path ) {
            CheckArg.isNotNull(path, "path");
            Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(path);
            Configurator.this.configurationSource = Configurator.this.configurationSource.with(newPath);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.And#and()
         */
        public ReturnType and() {
            return returnObject;
        }
    }

    /**
     * Reusable implementation of {@link Configurator.ChooseClass} that can be used to obtain the name of a class and how its
     * class loader is defined.
     * 
     * @param <ComponentClass> the type of the component that is being chosen
     * @param <ReturnType> the interface that should be returned when the class name and classpath have been chosen.
     */
    protected class ClassChooser<ComponentClass, ReturnType> implements Configurator.ChooseClass<ComponentClass, ReturnType> {
        protected final Path pathOfComponentNode;
        protected final ReturnType returnObject;

        public ClassChooser( Path pathOfComponentNode,
                             ReturnType returnObject ) {
            assert pathOfComponentNode != null;
            assert returnObject != null;
            this.pathOfComponentNode = pathOfComponentNode;
            this.returnObject = returnObject;
        }

        /**
         * {@inheritDoc}
         * 
         * @see Configurator.ChooseClass#usingClass(java.lang.String)
         */
        public Configurator.LoadedFrom<ReturnType> usingClass( final String classname ) {
            CheckArg.isNotEmpty(classname, "classname");
            configuration().set(DnaLexicon.CLASSNAME).to(classname).on(pathOfComponentNode);
            return new Configurator.LoadedFrom<ReturnType>() {
                public ReturnType loadedFromClasspath() {
                    return returnObject;
                }

                public ReturnType loadedFrom( String... classpath ) {
                    CheckArg.isNotEmpty(classpath, "classpath");
                    if (classpath.length == 1 && classpath[0] != null) {
                        configuration().set(DnaLexicon.CLASSPATH).to(classpath[0]).on(pathOfComponentNode);
                    } else {
                        Object[] remaining = new String[classpath.length - 1];
                        System.arraycopy(classpath, 1, remaining, 0, remaining.length);
                        configuration().set(DnaLexicon.CLASSPATH).to(classpath[0], remaining).on(pathOfComponentNode);
                    }
                    return returnObject;
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see Configurator.ChooseClass#usingClass(java.lang.Class)
         */
        public ReturnType usingClass( Class<? extends ComponentClass> clazz ) {
            CheckArg.isNotNull(clazz, "clazz");
            return usingClass(clazz.getName()).loadedFromClasspath();
        }
    }

    protected <ReturnType> GraphRepositorySourceDetails<ReturnType> createRepositoryDetails( Path path,
                                                                                             ReturnType returnObject ) {
        return new GraphRepositorySourceDetails<ReturnType>(path, returnObject);
    }

    protected class GraphRepositorySourceDetails<ReturnType> implements RepositorySourceDetails<ReturnType> {
        private final Path path;
        private final ReturnType returnObject;

        protected GraphRepositorySourceDetails( Path path,
                                                ReturnType returnObject ) {
            assert path != null;
            assert returnObject != null;
            this.path = path;
            this.returnObject = returnObject;
        }

        public Path path() {
            return this.path;
        }

        /**
         * Get the name used for this source.
         * 
         * @return the source's node name
         */
        public Name name() {
            return path().getLastSegment().getName();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetName#named(java.lang.String)
         */
        public RepositorySourceDetails<ReturnType> named( String name ) {
            configuration().set(DnaLexicon.READABLE_NAME).to(name).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetDescription#describedAs(java.lang.String)
         */
        public RepositorySourceDetails<ReturnType> describedAs( String description ) {
            configuration().set(DnaLexicon.DESCRIPTION).to(description).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, boolean)
         */
        public RepositorySourceDetails<ReturnType> setProperty( String propertyName,
                                                                boolean value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, int)
         */
        public RepositorySourceDetails<ReturnType> setProperty( String propertyName,
                                                                int value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, short)
         */
        public RepositorySourceDetails<ReturnType> setProperty( String propertyName,
                                                                short value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, long)
         */
        public RepositorySourceDetails<ReturnType> setProperty( String propertyName,
                                                                long value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, float)
         */
        public RepositorySourceDetails<ReturnType> setProperty( String propertyName,
                                                                float value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, double)
         */
        public RepositorySourceDetails<ReturnType> setProperty( String propertyName,
                                                                double value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, java.lang.String)
         */
        public RepositorySourceDetails<ReturnType> setProperty( String propertyName,
                                                                String value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, java.lang.Object)
         */
        public RepositorySourceDetails<ReturnType> setProperty( String propertyName,
                                                                Object value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.And#and()
         */
        public ReturnType and() {
            return returnObject;
        }
    }

    protected class GraphSequencerDetails<ReturnType> implements SequencerDetails<ReturnType> {
        private final Path path;
        private final List<String> compiledExpressions = new ArrayList<String>();
        private final ReturnType returnObject;

        protected GraphSequencerDetails( Path path,
                                         ReturnType returnObject ) {
            assert path != null;
            assert returnObject != null;
            this.path = path;
            this.returnObject = returnObject;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SequencerDetails#sequencingFrom(java.lang.String)
         */
        public PathExpressionOutput<ReturnType> sequencingFrom( final String from ) {
            CheckArg.isNotEmpty(from, "from");
            return new PathExpressionOutput<ReturnType>() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.repository.Configurator.PathExpressionOutput#andOutputtingTo(java.lang.String)
                 */
                public SequencerDetails<ReturnType> andOutputtingTo( String into ) {
                    CheckArg.isNotEmpty(into, "into");
                    return sequencingFrom(PathExpression.compile(from + " => " + into));
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetName#named(java.lang.String)
         */
        public SequencerDetails<ReturnType> named( String name ) {
            configuration().set(DnaLexicon.READABLE_NAME).to(name).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SequencerDetails#sequencingFrom(org.jboss.dna.graph.property.PathExpression)
         */
        public SequencerDetails<ReturnType> sequencingFrom( PathExpression expression ) {
            CheckArg.isNotNull(expression, "expression");
            String compiledExpression = expression.getExpression();
            if (!compiledExpressions.contains(compiledExpression)) compiledExpressions.add(compiledExpression);
            configuration().set(DnaLexicon.PATH_EXPRESSIONS).on(path).to(compiledExpressions);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetDescription#describedAs(java.lang.String)
         */
        public SequencerDetails<ReturnType> describedAs( String description ) {
            configuration().set(DnaLexicon.DESCRIPTION).to(description).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.And#and()
         */
        public ReturnType and() {
            return returnObject;
        }
    }

    protected class GraphMimeTypeDetectorDetails<ReturnType> implements MimeTypeDetectorDetails<ReturnType> {
        private final Path path;
        private final ReturnType returnObject;

        protected GraphMimeTypeDetectorDetails( Path path,
                                                ReturnType returnObject ) {
            assert path != null;
            assert returnObject != null;
            this.path = path;
            this.returnObject = returnObject;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetName#named(java.lang.String)
         */
        public MimeTypeDetectorDetails<ReturnType> named( String name ) {
            configuration().set(DnaLexicon.READABLE_NAME).to(name).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, boolean)
         */
        public MimeTypeDetectorDetails<ReturnType> setProperty( String propertyName,
                                                                boolean value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, int)
         */
        public MimeTypeDetectorDetails<ReturnType> setProperty( String propertyName,
                                                                int value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, short)
         */
        public MimeTypeDetectorDetails<ReturnType> setProperty( String propertyName,
                                                                short value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, long)
         */
        public MimeTypeDetectorDetails<ReturnType> setProperty( String propertyName,
                                                                long value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, float)
         */
        public MimeTypeDetectorDetails<ReturnType> setProperty( String propertyName,
                                                                float value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, double)
         */
        public MimeTypeDetectorDetails<ReturnType> setProperty( String propertyName,
                                                                double value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, java.lang.String)
         */
        public MimeTypeDetectorDetails<ReturnType> setProperty( String propertyName,
                                                                String value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetProperties#setProperty(java.lang.String, java.lang.Object)
         */
        public MimeTypeDetectorDetails<ReturnType> setProperty( String propertyName,
                                                                Object value ) {
            configuration().set(nameFor(propertyName)).to(value).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.SetDescription#describedAs(java.lang.String)
         */
        public MimeTypeDetectorDetails<ReturnType> describedAs( String description ) {
            configuration().set(DnaLexicon.DESCRIPTION).to(description).on(path);
            return this;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.Configurator.And#and()
         */
        public ReturnType and() {
            return returnObject;
        }
    }

    @Immutable
    public static class ConfigurationRepository {
        private final RepositorySource source;
        private final String description;
        private final Path path;
        private final String workspace;

        protected ConfigurationRepository( RepositorySource source ) {
            this(source, null, null, null);
        }

        protected ConfigurationRepository( RepositorySource source,
                                           String description,
                                           Path path,
                                           String workspace ) {
            this.source = source;
            this.description = description != null ? description : "";
            this.path = path != null ? path : RootPath.INSTANCE;
            this.workspace = workspace != null ? workspace : "";
        }

        /**
         * @return source
         */
        public RepositorySource getRepositorySource() {
            return source;
        }

        /**
         * @return description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return path
         */
        public Path getPath() {
            return path;
        }

        /**
         * @return workspace
         */
        public String getWorkspace() {
            return workspace;
        }

        public ConfigurationRepository withDescription( String description ) {
            return new ConfigurationRepository(source, description, path, workspace);
        }

        public ConfigurationRepository with( Path path ) {
            return new ConfigurationRepository(source, description, path, workspace);
        }

        public ConfigurationRepository withWorkspace( String workspace ) {
            return new ConfigurationRepository(source, description, path, workspace);
        }
    }
}
