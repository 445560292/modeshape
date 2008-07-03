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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionFactories;
import org.jboss.dna.spi.graph.connection.RepositorySource;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;

/**
 * The component in the {@link FederationService} that represents a single federated repository. The federated repository uses a
 * set of {@link RepositorySource federated sources} as designated by name through the {@link #getConfiguration() configuration},
 * and provides the logic of interacting with those sources and presenting a single unified graph.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class FederatedRepository {

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
            FederatedRepository.this.startRepository();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
            FederatedRepository.this.shutdownRepository();
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) throws InterruptedException {
            return FederatedRepository.this.awaitTermination(timeout, unit);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean doCheckIsTerminated() {
            return FederatedRepository.this.isTerminated();
        }

    }

    private final ServiceAdministrator administrator = new Administrator();
    private final String name;
    private final ExecutionEnvironment env;
    private final RepositoryConnectionFactories connectionFactories;
    private FederatedRepositoryConfig config;
    private final AtomicInteger openConnections = new AtomicInteger(0);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<RepositorySourceListener> listeners = new CopyOnWriteArrayList<RepositorySourceListener>();

    /**
     * Create a federated repository instance, as managed by the supplied {@link FederationService}.
     * 
     * @param repositoryName the name of the repository
     * @param env the execution environment
     * @param connectionFactories the set of connection factories that should be used
     * @param config the configuration for this repository
     * @throws IllegalArgumentException if any of the parameters are null, or if the name is blank
     */
    public FederatedRepository( String repositoryName,
                                ExecutionEnvironment env,
                                RepositoryConnectionFactories connectionFactories,
                                FederatedRepositoryConfig config ) {
        ArgCheck.isNotNull(connectionFactories, "connectionFactories");
        ArgCheck.isNotNull(env, "env");
        ArgCheck.isNotNull(config, "config");
        ArgCheck.isNotEmpty(repositoryName, "repositoryName");
        this.name = repositoryName;
        this.env = env;
        this.connectionFactories = connectionFactories;
        this.config = config;
    }

    /**
     * Get the name of this repository
     * 
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return administrator
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * @return the execution environment
     */
    public ExecutionEnvironment getExecutionEnvironment() {
        return env;
    }

    /**
     * @return connectionFactories
     */
    protected RepositoryConnectionFactories getConnectionFactories() {
        return connectionFactories;
    }

    /**
     * Utility method called by the administrator.
     */
    protected synchronized void startRepository() {
        // Do not establish connections to the sources; these will be established as needed
    }

    /**
     * Utility method called by the administrator.
     */
    protected synchronized void shutdownRepository() {
        this.shutdownRequested.set(true);
        if (this.openConnections.get() <= 0) shutdownLatch.countDown();
    }

    /**
     * Utility method called by the administrator.
     * 
     * @param timeout
     * @param unit
     * @return true if all connections open at the time this method is called were {@link RepositoryConnection#close() closed} in
     *         the supplied time, or false if the timeout occurred before all the connections were closed
     * @throws InterruptedException
     */
    protected boolean awaitTermination( long timeout,
                                        TimeUnit unit ) throws InterruptedException {
        // Await until all connections have been closed, or until the timeout occurs
        return shutdownLatch.await(timeout, unit);
    }

    /**
     * Return true if this federated repository has completed its termination and no longer has any open connections.
     * 
     * @return true if terminated, or false otherwise
     */
    protected boolean isTerminated() {
        return this.openConnections.get() != 0;
    }

    /**
     * Add a listener that is to receive notifications to changes to content within this repository. This method does nothing if
     * the supplied listener is null.
     * 
     * @param listener the new listener
     * @return true if the listener was added, or false if the listener was not added (if reference is null, or if non-null
     *         listener is already an existing listener)
     */
    public boolean addListener( RepositorySourceListener listener ) {
        if (listener == null) return false;
        return this.listeners.addIfAbsent(listener);
    }

    /**
     * Remove the supplied listener. This method does nothing if the supplied listener is null.
     * <p>
     * This method can safely be called while the federation repository is in use.
     * </p>
     * 
     * @param listener the listener to remove
     * @return true if the listener was removed, or false if the listener was not registered
     */
    public boolean removeListener( RepositorySourceListener listener ) {
        if (listener == null) return false;
        return this.listeners.remove(listener);
    }

    /**
     * Get the list of listeners, which is the actual list used by the repository.
     * 
     * @return the listeners
     */
    public List<RepositorySourceListener> getListeners() {
        return this.listeners;
    }

    /**
     * Authenticate the supplied username with the supplied credentials, and return whether authentication was successful.
     * 
     * @param source the {@link RepositorySource} that should be affiliated with the resulting connection
     * @param username the username
     * @param credentials the credentials
     * @return the repository connection if authentication succeeded, or null otherwise
     */
    public RepositoryConnection createConnection( RepositorySource source,
                                                  String username,
                                                  Object credentials ) {
        return new FederatedRepositoryConnection(this, source.getName());
    }

    /**
     * Get the configuration of this repository. This configuration is immutable and may be
     * {@link #setConfiguration(FederatedRepositoryConfig) changed} as needed. Therefore, when using a configuration and needing a
     * consistent configuration, maintain a reference to the configuration during that time (as the actual configuration may be
     * replaced at any time).
     * 
     * @return the repository's configuration at the time this method is called.
     */
    public FederatedRepositoryConfig getConfiguration() {
        return config;
    }

    /**
     * Set the configuration for this repository. The configuration is immutable and therefore may be replaced using this method.
     * All interaction with the configuration is done in a thread-safe and concurrent manner, and as such only valid
     * configurations should be used.
     * 
     * @param config the new configuration
     * @throws IllegalArgumentException if the configuration is null
     */
    public void setConfiguration( FederatedRepositoryConfig config ) {
        ArgCheck.isNotNull(config, "config");
        this.config = config;
    }

    /**
     * Called by {@link FederatedRepositoryConnection#FederatedRepositoryConnection(FederatedRepository, String)
     * FederatedRepositoryConnection constructor}.
     * 
     * @param federatedRepositoryConnection
     */
    /*package*/void register( FederatedRepositoryConnection federatedRepositoryConnection ) {
        openConnections.incrementAndGet();
    }

    /**
     * Called by {@link FederatedRepositoryConnection#close()}.
     * 
     * @param federatedRepositoryConnection
     */
    /*package*/void unregister( FederatedRepositoryConnection federatedRepositoryConnection ) {
        if (openConnections.decrementAndGet() <= 0 && shutdownRequested.get()) {
            // Last connection, so turn out the lights ...
            shutdownLatch.countDown();
        }
    }

}
