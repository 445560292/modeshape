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
package org.jboss.dna.graph.connector.federation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.jboss.dna.common.statistic.Stopwatch;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * This {@link RepositoryConnection} implementation executes {@link Request requests} against the federated repository by
 * projecting them into requests against the underlying sources that are being federated.
 * <p>
 * One important design of the connector framework is that requests can be submitted in {@link CompositeRequest batch} to a
 * {@link RepositoryConnection connection}, which may perform them more efficiently than if each request was submitted one at a
 * time. This connector design maintains this feature by projecting the incoming requests into requests against each source, then
 * submitting the batch of projected requests to each source, and then transforming the results of the projected requests back
 * into original requests.
 * </p>
 * <p>
 * This is accomplished using a three-step process:
 * <ol>
 * <li><strong>Step 1:</strong> Process the incoming requests and for each generate the appropriate request(s) against the sources
 * (dictated by the {@link FederatedWorkspace workspace's} {@link FederatedWorkspace#getProjections() projections}). These
 * "projected requests" are then enqueued for each source.</li>
 * <li><strong>Step 2:</strong> Submit each batch of projected requests to the appropriate source, in parallel where possible.
 * Note that the requests are still ordered correctly for each source.</li>
 * <li><strong>Step 3:</strong> Accumulate the results for the incoming requests by post-processing the projected requests and
 * transforming the source-specific results back into the federated workspace (again, using the workspace's projections).</li>
 * </ol>
 * </p>
 * <p>
 * This process is a form of the <i>fork-join</i> divide-and-conquer algorithm, which involves splitting a problem into smaller
 * parts, forking new subtasks to execute each smaller part, joining on the subtasks (waiting until all have finished), and then
 * composing the results. Technically, Step 2 performs the fork and join operations, but this class uses {@link RequestProcessor}
 * implementations to do Step 1 and 3 (called {@link ForkRequestProcessor} and {@link JoinRequestProcessor}, respectively).
 * </p>
 */
class FederatedRepositoryConnection implements RepositoryConnection {

    private final FederatedRepository repository;
    private final Stopwatch stopwatch;
    private final Logger logger;

    FederatedRepositoryConnection( FederatedRepository repository ) {
        this.repository = repository;
        this.logger = Logger.getLogger(getClass());
        this.stopwatch = logger.isTraceEnabled() ? new Stopwatch() : null;
    }

    /**
     * The federated repository that created this connection.
     * 
     * @return repository
     */
    FederatedRepository getRepository() {
        return repository;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getDefaultCachePolicy()
     */
    public CachePolicy getDefaultCachePolicy() {
        return repository.getDefaultCachePolicy(); // may be null
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getSourceName()
     */
    public String getSourceName() {
        return repository.getSourceName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#getXAResource()
     */
    public XAResource getXAResource() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#ping(long, java.util.concurrent.TimeUnit)
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        return true;
    }

    protected boolean shouldProcessSynchronously( Request request ) {
        if (request instanceof CompositeRequest) {
            CompositeRequest composite = (CompositeRequest)request;
            if (composite.size() == 1) return true;
            return false;
        }
        // Otherwise, its just a single request ...
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.request.Request)
     */
    public void execute( ExecutionContext context,
                         final Request request ) throws RepositorySourceException {
        // Compute the current time ...
        DateTime nowInUtc = context.getValueFactories().getDateFactory().createUtc();

        // Figure out whether we should asynchronously do the forking ...
        boolean synchronousStep1 = shouldProcessSynchronously(request);
        final boolean awaitAllSubtasks = false;

        // Prepare for trace-level logging ...
        if (stopwatch != null) stopwatch.start();

        boolean abort = false;
        try {
            // ----------------------------------------------------------------------------------------------------
            // Step 1: Fork the submitted requests into source-specific requests...
            // ----------------------------------------------------------------------------------------------------
            // This forks a subtask for each source, as soon as the first source-specific request for a source
            // is generated. Each source's "execute(ExecutionContext,Request)" is called only once (a queue is
            // used so that the source can begin processing the requests before all the requests have been
            // computed and submitted to the subtask). Thus, it's possible (and likely) that this thread
            // and subtask threads are executed in parallel.
            final Queue<FederatedRequest> requests = awaitAllSubtasks ? new LinkedList<FederatedRequest>() : new LinkedBlockingQueue<FederatedRequest>();
            final ForkRequestProcessor fork = new ForkRequestProcessor(repository, context, nowInUtc, requests);
            if (synchronousStep1) {
                // Execute the forking process in this thread ...
                try {
                    fork.process(request);
                } finally {
                    fork.close();
                }
                if (!awaitAllSubtasks) requests.add(new NoMoreFederatedRequests());
                // At this point, all submitted requests have been processed/forked, so we can continue with
                // the join process, starting with the first submitted request. Note that the subtasks may
                // still be executing, but as the join process operates on a forked request, it will wait
                // until all forked requests have completed. Hopefully, in most situations, the subtasks
                // have enough of a lead that the join process never has to wait.
            } else {
                // Submit the forking process for execution in a separate thread ...
                repository.getExecutor().submit(new Runnable() {
                    public void run() {
                        try {
                            fork.process(request);
                        } finally {
                            fork.close();
                        }
                        if (!awaitAllSubtasks) requests.add(new NoMoreFederatedRequests());
                    }
                });

                // At this point, the forking process is being run by a thread owned by the Executor. We'll still
                // continue with the join process, starting with the first submitted request. Note that it is
                // likely that the subtasks are still running in threads owned by the Executor.
            }

            if (awaitAllSubtasks) {
                // Await until all subtasks have completed ...
                fork.await();
            }

            // ----------------------------------------------------------------------------------------------------
            // Step 2: Join the results of the source-specific (forked) requests back into the submitted requests
            // ----------------------------------------------------------------------------------------------------
            JoinRequestProcessor join = new JoinRequestProcessor(repository, context, nowInUtc);
            try {
                if (awaitAllSubtasks) {
                    join.process(requests);
                } else {
                    join.process((BlockingQueue<FederatedRequest>)requests);
                }
            } catch (RuntimeException e) {
                abort = true;
                throw e;
            } finally {
                join.close();
            }
        } catch (InterruptedException e) {
            abort = true;
            request.setError(e);
        } catch (ExecutionException e) {
            abort = true;
            request.setError(e);
        } catch (CancellationException e) {
            abort = true;
            request.cancel();
            // do nothing else
        } catch (RuntimeException e) {
            abort = true;
            throw e;
        } finally {
            if (stopwatch != null) stopwatch.stop();
            if (abort) {
                // Rollback the transaction (if there is one) ...
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#close()
     */
    public void close() {
        if (stopwatch != null) {
            logger.trace("Processing federated requests:\n" + stopwatch.getDetailedStatistics());
        }
        // do nothing else, since we don't currently hold any state
    }

}
