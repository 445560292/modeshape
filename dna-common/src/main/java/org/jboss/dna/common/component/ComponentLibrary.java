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

package org.jboss.dna.common.component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.util.ArgCheck;

/**
 * Maintains the list of component instances for the system. This class does not actively update the component configurations, but
 * is designed to properly maintain the sequencer instances when those configurations are changed by other callers. If the
 * components are subclasses of {@link Component}, then they will be
 * {@link Component#setConfiguration(ComponentConfig) configured} with the appropriate configuration.
 * <p>
 * Therefore, this library does guarantee that the {@link #getInstances() instances} at the time they are
 * {@link #getInstances() obtained} are always reflected by the configurations.
 * </p>
 * 
 * @author Randall Hauch
 * @param <ComponentType> the type of component being managed, which may be a subclass of {@link Component}
 * @param <ConfigType> the configuration type describing the components
 */
@ThreadSafe
public class ComponentLibrary<ComponentType, ConfigType extends ComponentConfig> {

    /**
     * Class loader factory instance that always returns the
     * {@link Thread#getContextClassLoader() current thread's context class loader} (if not null) or component library's class
     * loader.
     */
    public static final ClassLoaderFactory DEFAULT = new StandardClassLoaderFactory(ComponentLibrary.class.getClassLoader());

    /**
     * The class loader factory
     */
    private final AtomicReference<ClassLoaderFactory> classLoaderFactory = new AtomicReference<ClassLoaderFactory>(DEFAULT);

    /**
     * The list of component instances. The index of each component instance matches the corresponding configuration instance in
     * {@link #configs}
     */
    @GuardedBy( value = "lock" )
    private final List<ComponentType> instances = new CopyOnWriteArrayList<ComponentType>();
    private final List<ConfigType> configs = new CopyOnWriteArrayList<ConfigType>();
    private final List<ComponentType> unmodifiableInstances = Collections.unmodifiableList(instances);
    private final Lock lock = new ReentrantLock();

    /**
     * Create a new library of components.
     */
    public ComponentLibrary() {
    }

    /**
     * Get the class loader factory that should be used to load the component classes. Unless changed, the library uses the
     * {@link #DEFAULT default} class loader factory, which uses the
     * {@link Thread#getContextClassLoader() current thread's context class loader} if not null or the class loader that loaded
     * the library class.
     * 
     * @return the class loader factory; never null
     * @see #setClassLoaderFactory(ClassLoaderFactory)
     */
    public ClassLoaderFactory getClassLoaderFactory() {
        return this.classLoaderFactory.get();
    }

    /**
     * Set the Maven Repository that should be used to load the sequencer classes. Unless changed, the library uses the
     * {@link #DEFAULT default} class loader factory, which uses the
     * {@link Thread#getContextClassLoader() current thread's context class loader} if not null or the class loader that loaded
     * the library class.
     * 
     * @param classLoaderFactory the class loader factory reference, or null if the {@link #DEFAULT default class loader factory}
     *        should be used
     * @see #getClassLoaderFactory()
     */
    public void setClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
        this.classLoaderFactory.set(classLoaderFactory != null ? classLoaderFactory : DEFAULT);
    }

    /**
     * Add the configuration for a sequencer, or update any existing one that represents the
     * {@link ConfigType#equals(Object) same configuration}
     * 
     * @param config the new configuration
     * @return true if the component was added, or false if there already was an existing and
     *         {@link ComponentConfig#hasChanged(ComponentConfig) unchanged} component configuration
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #update(ComponentConfig)
     * @see #remove(ComponentConfig)
     */
    public boolean add( ConfigType config ) {
        ArgCheck.isNotNull(config, "component configuration");
        try {
            this.lock.lock();
            // Find an existing configuration that matches ...
            int index = findIndexOfMatchingConfiguration(config);
            if (index >= 0) {
                // See if the matching configuration has changed ...
                ConfigType existingConfig = this.configs.get(index);
                if (existingConfig.hasChanged(config)) {
                    // It has changed, so we need to replace it ...
                    this.configs.set(index, config);
                    this.instances.set(index, newInstance(config));
                }
                return false;
            }
            // Didn't find one, so add it ...
            this.configs.add(config);
            this.instances.add(newInstance(config));
            return true;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Update the configuration for a sequencer, or add it if there is no {@link ConfigType#equals(Object) matching configuration}.
     * 
     * @param config the updated (or new) configuration
     * @return true if the component was updated, or false if there already was an existing and
     *         {@link ComponentConfig#hasChanged(ComponentConfig) unchanged} component configuration
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #add(ComponentConfig)
     * @see #remove(ComponentConfig)
     */
    public boolean update( ConfigType config ) {
        return add(config);
    }

    /**
     * Remove the configuration for a sequencer.
     * 
     * @param config the configuration to be removed
     * @return true if the component was remove, or false if there was no existing configuration
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #add(ComponentConfig)
     * @see #update(ComponentConfig)
     */
    public boolean remove( ConfigType config ) {
        ArgCheck.isNotNull(config, "component configuration");
        try {
            this.lock.lock();
            // Find an existing configuration that matches ...
            int index = findIndexOfMatchingConfiguration(config);
            if (index >= 0) {
                // Remove the configuration and the sequencer instance ...
                this.configs.remove(index);
                this.instances.remove(index);
                return true;
            }
            return false;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Return the list of sequencers.
     * 
     * @return the unmodifiable list of sequencers; never null
     */
    public List<ComponentType> getInstances() {
        return this.unmodifiableInstances;
    }

    /**
     * Instantiate, configure and return a new sequencer described by the supplied configuration. This method does not manage the
     * returned instance.
     * 
     * @param config the configuration describing the sequencer
     * @return the new sequencer, or null if the sequencer could not be successfully configured
     * @throws IllegalArgumentException if the sequencer could not be configured properly
     */
    @SuppressWarnings( "unchecked" )
    protected ComponentType newInstance( ConfigType config ) {
        String[] classpath = config.getComponentClasspathArray();
        final ClassLoader classLoader = this.getClassLoaderFactory().getClassLoader(classpath);
        assert classLoader != null;
        ComponentType newInstance = null;
        try {
            // Don't use ClassLoader.loadClass(String), as it doesn't properly initialize the class
            // (specifically static initializers may not be called)
            Class<?> componentClass = Class.forName(config.getComponentClassname(), true, classLoader);
            newInstance = doCreateInstance(componentClass);
            if (newInstance instanceof Component) {
                ((Component<ConfigType>)newInstance).setConfiguration(config);
            }
        } catch (Throwable e) {
            throw new SystemFailureException(e);
        }
        if (newInstance instanceof Component && ((Component<ConfigType>)newInstance).getConfiguration() == null) {
            throw new SystemFailureException(CommonI18n.componentNotConfigured.text(config.getName()));
        }
        return newInstance;
    }

    /**
     * Method that instantiates the supplied class. This method can be overridden by subclasses that may need to wrap or adapt the
     * instance to be a ComponentType.
     * 
     * @param componentClass
     * @return the new ComponentType instance
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings( "unchecked" )
    protected ComponentType doCreateInstance( Class<?> componentClass ) throws InstantiationException, IllegalAccessException {
        return (ComponentType)componentClass.newInstance();
    }

    /**
     * Find the index for the matching {@link #configs configuration} and {@link #instances sequencer}.
     * 
     * @param config the configuration; may not be null
     * @return the index, or -1 if not found
     */
    @GuardedBy( value = "lock" )
    protected int findIndexOfMatchingConfiguration( ConfigType config ) {
        // Iterate through the configurations and look for an existing one that matches
        for (int i = 0, length = this.configs.size(); i != length; i++) {
            ConfigType existingConfig = this.configs.get(i);
            assert existingConfig != null;
            if (existingConfig.equals(config)) {
                return i;
            }
        }
        return -1;
    }

}
