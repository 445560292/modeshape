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
package org.jboss.dna.services.sequencers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.ComponentLibrary;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.monitor.LoggingProgressMonitor;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.monitor.SimpleProgressMonitor;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.services.ExecutionContext;
import org.jboss.dna.services.ManagedService;
import org.jboss.dna.services.SessionFactory;
import org.jboss.dna.services.observation.NodeChange;
import org.jboss.dna.services.observation.NodeChangeListener;

/**
 * A sequencing system is used to monitor changes in the content of {@link Repository JCR repositories} and to sequence the
 * content to extract or to generate structured information.
 * @author Randall Hauch
 */
public class SequencingService extends ManagedService implements NodeChangeListener {

    /**
     * Interface used to select the set of {@link Sequencer} instances that should be run.
     * @author Randall Hauch
     */
    public static interface Selector {

        /**
         * Select the sequencers that should be used to sequence the supplied node.
         * @param sequencers the list of all sequencers available at the moment; never null
         * @param node the node to be sequenced; never null
         * @return the list of sequencers that should be used; may not be null
         */
        List<Sequencer> selectSequencers( List<Sequencer> sequencers, Node node );
    }

    /**
     * The default {@link Selector} implementation that selects every sequencer every time it's called, regardless of the node (or
     * logger) supplied.
     * @author Randall Hauch
     */
    protected static class DefaultSelector implements Selector {

        public List<Sequencer> selectSequencers( List<Sequencer> sequencers, Node node ) {
            return sequencers;
        }
    }

    /**
     * Interface used to determine whether a {@link NodeChange} should be processed.
     * @author Randall Hauch
     */
    public static interface NodeFilter {

        /**
         * Determine whether the node represented by the supplied change should be submitted for sequencing.
         * @param nodeChange the node change event
         * @return true if the node should be submitted for sequencing, or false if the change should be ignored
         */
        boolean accept( NodeChange nodeChange );
    }

    /**
     * The default filter implementation, which accepts only new nodes or nodes that have new or changed properties.
     * @author Randall Hauch
     */
    protected static class DefaultNodeFilter implements NodeFilter {

        public boolean accept( NodeChange nodeChange ) {
            // Only care about new nodes or nodes that have new/changed properies ...
            return nodeChange.includesEventTypes(Event.NODE_ADDED, Event.PROPERTY_ADDED, Event.PROPERTY_CHANGED);
        }
    }

    /**
     * The default {@link Selector} that considers every {@link Sequencer} to be used for every node.
     * @see SequencingService#setSequencerSelector(org.jboss.dna.services.sequencers.SequencingService.Selector)
     */
    public static final Selector DEFAULT_SEQUENCER_SELECTOR = new DefaultSelector();
    /**
     * The default {@link NodeFilter} that accepts new nodes or nodes that have new/changed properties.
     * @see SequencingService#setSequencerSelector(org.jboss.dna.services.sequencers.SequencingService.Selector)
     */
    public static final NodeFilter DEFAULT_NODE_FILTER = new DefaultNodeFilter();

    /**
     * Class loader factory instance that always returns the
     * {@link Thread#getContextClassLoader() current thread's context class loader} (if not null) or component library's class
     * loader.
     */
    protected static final ClassLoaderFactory DEFAULT_CLASSLOADER_FACTORY = new StandardClassLoaderFactory(SequencingService.class.getClassLoader());

    private SessionFactory sessionFactory;
    private ComponentLibrary<Sequencer, SequencerConfig> sequencerLibrary = new ComponentLibrary<Sequencer, SequencerConfig>();
    private Selector sequencerSelector = DEFAULT_SEQUENCER_SELECTOR;
    private NodeFilter nodeFilter = DEFAULT_NODE_FILTER;
    private ExecutorService executorService;
    private Logger logger = Logger.getLogger(this.getClass());
    private final Statistics statistics = new Statistics();

    /**
     * Create a new sequencing system, configured with no sequencers and not monitoring any workspaces. Upon construction, the
     * system is {@link #isPaused() paused} and must be configured and then {@link #start() started}.
     */
    public SequencingService() {
        super(State.PAUSED);
        this.sequencerLibrary.setClassLoaderFactory(DEFAULT_CLASSLOADER_FACTORY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String serviceName() {
        return "SequencingService";
    }

    /**
     * Get the statistics for this system.
     * @return statistics
     */
    public Statistics getStatistics() {
        return this.statistics;
    }

    /**
     * Get the class loader factory that should be used to load sequencers. By default, this service uses a factory that will
     * return either the {@link Thread#getContextClassLoader() current thread's context class loader} (if not null) or the class
     * loader that loaded this class.
     * @return the class loader factory; never null
     * @see #setClassLoaderFactory(ClassLoaderFactory)
     */
    public ClassLoaderFactory getClassLoaderFactory() {
        return this.sequencerLibrary.getClassLoaderFactory();
    }

    /**
     * Set the Maven Repository that should be used to load the sequencer classes. By default, this service uses a class loader
     * factory that will return either the {@link Thread#getContextClassLoader() current thread's context class loader} (if not
     * null) or the class loader that loaded this class.
     * @param classLoaderFactory the class loader factory reference, or null if the default class loader factory should be used.
     * @see #getClassLoaderFactory()
     */
    public void setClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
        this.sequencerLibrary.setClassLoaderFactory(classLoaderFactory != null ? classLoaderFactory : DEFAULT_CLASSLOADER_FACTORY);
    }

    /**
     * Add the configuration for a sequencer, or update any existing one that represents the
     * {@link SequencerConfig#equals(Object) same configuration}
     * @param config the new configuration
     * @return true if the sequencer was added, or false if there already was an existing and
     * {@link SequencerConfig#hasChanged(SequencerConfig) unchanged} sequencer configuration
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #updateSequencer(SequencerConfig)
     * @see #removeSequencer(SequencerConfig)
     */
    public boolean addSequencer( SequencerConfig config ) {
        return this.sequencerLibrary.add(config);
    }

    /**
     * Update the configuration for a sequencer, or add it if there is no
     * {@link SequencerConfig#equals(Object) matching configuration}.
     * @param config the updated (or new) configuration
     * @return true if the sequencer was updated, or false if there already was an existing and
     * {@link SequencerConfig#hasChanged(SequencerConfig) unchanged} sequencer configuration
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #addSequencer(SequencerConfig)
     * @see #removeSequencer(SequencerConfig)
     */
    public boolean updateSequencer( SequencerConfig config ) {
        return this.sequencerLibrary.update(config);
    }

    /**
     * Remove the configuration for a sequencer.
     * @param config the configuration to be removed
     * @return true if the sequencer was removed, or false if there was no existing sequencer
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #addSequencer(SequencerConfig)
     * @see #updateSequencer(SequencerConfig)
     */
    public boolean removeSequencer( SequencerConfig config ) {
        return this.sequencerLibrary.remove(config);
    }

    /**
     * Get the logger for this system
     * @return the logger
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Set the logger for this system.
     * @param logger the logger, or null if the standard logging should be used
     */
    public void setLogger( Logger logger ) {
        this.logger = logger != null ? logger : Logger.getLogger(this.getClass());
    }

    /**
     * @return sessionFactory
     */
    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    /**
     * @param sessionFactory Sets sessionFactory to the specified value.
     */
    public void setSessionFactory( SessionFactory sessionFactory ) {
        if (sessionFactory == null) {
            throw new IllegalArgumentException("The session factory parameter may not be null");
        }
        if (this.isStarted()) {
            throw new IllegalStateException("Unable to change the session factory while running");
        }
        this.sessionFactory = sessionFactory;
    }

    /**
     * Get the executor service used to run the sequencers.
     * @return the executor service
     * @see #setExecutorService(ExecutorService)
     */
    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    /**
     * Set the executor service that should be used by this system. By default, the system is set up with a
     * {@link Executors#newSingleThreadExecutor() executor that uses a single thread}.
     * @param executorService the executor service
     * @see #getExecutorService()
     * @see Executors#newCachedThreadPool()
     * @see Executors#newCachedThreadPool(java.util.concurrent.ThreadFactory)
     * @see Executors#newFixedThreadPool(int)
     * @see Executors#newFixedThreadPool(int, java.util.concurrent.ThreadFactory)
     * @see Executors#newScheduledThreadPool(int)
     * @see Executors#newScheduledThreadPool(int, java.util.concurrent.ThreadFactory)
     * @see Executors#newSingleThreadExecutor()
     * @see Executors#newSingleThreadExecutor(java.util.concurrent.ThreadFactory)
     * @see Executors#newSingleThreadScheduledExecutor()
     * @see Executors#newSingleThreadScheduledExecutor(java.util.concurrent.ThreadFactory)
     */
    public void setExecutorService( ExecutorService executorService ) {
        if (sequencerLibrary == null) {
            throw new IllegalArgumentException("The executor service parameter may not be null");
        }
        if (this.isStarted()) {
            throw new IllegalStateException("Unable to change the executor service while running");
        }
        this.executorService = executorService;
    }

    /**
     * Override this method to creates a different kind of default executor service. This method is called when the system is
     * {@link #start() started} without an executor service being {@link #setExecutorService(ExecutorService) set}.
     * <p>
     * This method creates a {@link Executors#newSingleThreadExecutor() single-threaded executor}.
     * </p>
     * @return
     */
    protected ExecutorService createDefaultExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doStart( org.jboss.dna.services.ManagedService.State fromState ) {
        super.doStart(fromState);
        if (this.getSessionFactory() == null) {
            throw new IllegalStateException("Unable to start the sequencing system without a session factory");
        }
        if (this.executorService == null) {
            this.executorService = createDefaultExecutorService();
        }
        assert this.executorService != null;
        assert this.sequencerSelector != null;
        assert this.nodeFilter != null;
        assert this.sequencerLibrary != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doShutdown( org.jboss.dna.services.ManagedService.State fromState ) {
        super.doShutdown(fromState);
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
    }

    /**
     * Get the sequencing selector used by this system.
     * @return the sequencing selector
     */
    public Selector getSequencerSelector() {
        return this.sequencerSelector;
    }

    /**
     * Set the sequencer selector, or null if the {@link #DEFAULT_SEQUENCER_SELECTOR default sequencer selector} should be used.
     * @param sequencerSelector the selector
     */
    public void setSequencerSelector( Selector sequencerSelector ) {
        this.sequencerSelector = sequencerSelector != null ? sequencerSelector : DEFAULT_SEQUENCER_SELECTOR;
    }

    /**
     * Get the node filter used by this system.
     * @return the node filter
     */
    public NodeFilter getNodeFilter() {
        return this.nodeFilter;
    }

    /**
     * Set the filter that checks which nodes are to be sequenced, or null if the {@link #DEFAULT_NODE_FILTER default node filter}
     * should be used.
     * @param nodeFilter the new node filter
     */
    public void setNodeFilter( NodeFilter nodeFilter ) {
        this.nodeFilter = nodeFilter != null ? nodeFilter : DEFAULT_NODE_FILTER;
    }

    /**
     * {@inheritDoc}
     */
    public void onNodeChanges( Iterable<NodeChange> changes ) {
        NodeFilter filter = this.getNodeFilter();
        for (final NodeChange changedNode : changes) {
            // Only care about new nodes or nodes that have new/changed properies ...
            if (filter.accept(changedNode)) {
                this.executorService.execute(new Runnable() {

                    public void run() {
                        processChangedNode(changedNode);
                    }
                });
            }
        }
    }

    /**
     * Do the work of processing by sequencing the node. This method is called by the {@link #executorService executor service}
     * when it performs it's work on the enqueued {@link ChangedNode ChangedNode runnable objects}.
     * @param node the node to be processed.
     */
    protected void processChangedNode( NodeChange changedNode ) {
        try {
            // Create a session that we'll use for all sequencing ...
            final Session session = this.getSessionFactory().createSession(changedNode.getRepositoryWorkspaceName());

            try {
                // Find the changed node ...
                String relPath = changedNode.getAbsolutePath().replaceAll("^/+", "");
                Node node = session.getRootNode().getNode(relPath);

                // Figure out which sequencers should run ...
                List<Sequencer> sequencers = this.sequencerLibrary.getInstances();
                sequencers = this.sequencerSelector.selectSequencers(sequencers, node);
                // Run each of those sequencers ...
                if (sequencers.isEmpty()) {
                    this.statistics.recordNodeSkipped();
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Skipping '{}': no sequencers matched this condition", changedNode);
                    }
                } else {
                    String activityName = StringUtil.createString("Sequencing {1}", changedNode);
                    ProgressMonitor progressMonitor = new SimpleProgressMonitor(activityName);
                    if (this.logger.isTraceEnabled()) {
                        progressMonitor = new LoggingProgressMonitor(progressMonitor, this.logger, Logger.Level.TRACE);
                    }
                    try {
                        progressMonitor.beginTask(activityName, sequencers.size());
                        for (Sequencer sequencer : sequencers) {
                            final SequencerConfig config = sequencer.getConfiguration();
                            final String sequencerName = config != null ? config.getName() : sequencer.getClass().getName();

                            // Create a new execution context for each sequencer
                            final Context executionContext = new Context();
                            final ProgressMonitor sequenceMonitor = progressMonitor.createSubtask(1);
                            try {
                                String subtaskName = StringUtil.createString("running {}", sequencerName);
                                sequenceMonitor.beginTask(subtaskName, 100);
                                sequencer.execute(node, node, executionContext, sequenceMonitor.createSubtask(80)); // 80%
                            } finally {
                                try {
                                    // Save the changes made by the sequencer ...
                                    session.save();
                                    sequenceMonitor.worked(10); // 90% of sequenceMonitor
                                } finally {
                                    try {
                                        // And always close the context.
                                        // This closes all sessions that may have been created by the sequencer.
                                        executionContext.close();
                                    } finally {
                                        sequenceMonitor.done(); // 100% of sequenceMonitor
                                    }
                                }
                            }
                        }
                        this.statistics.recordNodeSequenced();
                    } finally {
                        progressMonitor.done();
                    }
                }
            } finally {
                session.logout();
            }
        } catch (RepositoryException e) {
            String msg = "Error while sequencing {}";
            this.logger.error(e, msg, changedNode);
        } catch (Exception e) {
            String msg = "Error while finding sequencers to run against {}";
            this.logger.error(e, msg, changedNode);
        }
    }

    protected class Context implements ExecutionContext {

        protected final SessionFactory factory;
        private final Set<Session> sessions = new HashSet<Session>();
        protected final AtomicBoolean closed = new AtomicBoolean(false);

        protected Context() {
            final SessionFactory delegate = SequencingService.this.getSessionFactory();
            this.factory = new SessionFactory() {

                public Session createSession( String name ) throws RepositoryException {
                    if (closed.get()) throw new IllegalStateException("This execution context has been closed and may not be used to create another session");
                    Session session = delegate.createSession(name);
                    recordSession(session);
                    return session;
                }
            };
        }

        protected synchronized void recordSession( Session session ) {
            if (session != null) sessions.add(session);
        }

        /**
         * {@inheritDoc}
         */
        public SessionFactory getSessionFactory() {
            return this.factory;
        }

        public synchronized void close() {
            if (this.closed.get()) return;
            this.closed.set(true);
            for (Session session : sessions) {
                if (session != null) session.logout();
            }
        }
    }

    /**
     * The statistics for the system. Each sequencing system has an instance of this class that is updated.
     * @author Randall Hauch
     */
    @ThreadSafe
    public class Statistics {

        private final AtomicLong numberOfNodesSequenced = new AtomicLong(0);
        private final AtomicLong numberOfNodesSkipped = new AtomicLong(0);
        private final AtomicLong startTime;

        protected Statistics() {
            startTime = new AtomicLong(System.currentTimeMillis());
        }

        public Statistics reset() {
            this.startTime.set(System.currentTimeMillis());
            this.numberOfNodesSequenced.set(0);
            this.numberOfNodesSkipped.set(0);
            return this;
        }

        /**
         * @return the system time when the statistics were started
         */
        public long getStartTime() {
            return this.startTime.get();
        }

        /**
         * @return the number of nodes that were sequenced
         */
        public long getNumberOfNodesSequenced() {
            return this.numberOfNodesSequenced.get();
        }

        /**
         * @return the number of nodes that were skipped because no sequencers applied
         */
        public long getNumberOfNodesSkipped() {
            return this.numberOfNodesSkipped.get();
        }

        protected void recordNodeSequenced() {
            this.numberOfNodesSequenced.incrementAndGet();
        }

        protected void recordNodeSkipped() {
            this.numberOfNodesSkipped.incrementAndGet();
        }
    }
}
