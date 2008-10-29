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
package org.jboss.dna.repository.sequencers;

import java.util.Set;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.component.Component;
import org.jboss.dna.common.monitor.ActivityMonitor;
import org.jboss.dna.repository.observation.NodeChange;
import org.jboss.dna.repository.observation.NodeChangeListener;
import org.jboss.dna.repository.observation.NodeChanges;
import org.jboss.dna.repository.observation.ObservationService;
import org.jboss.dna.repository.util.JcrExecutionContext;
import org.jboss.dna.repository.util.RepositoryNodePath;

/**
 * The interface for a DNA sequencer, which sequences nodes and their content to extract additional information from the
 * information.
 * <p>
 * Implementations must provide a no-argument constructor.
 * </p>
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@ThreadSafe
public interface Sequencer extends Component<SequencerConfig> {

    /**
     * Execute the sequencing operation on the supplied node, which has recently been created or changed. The implementation of
     * this method is responsible for {@link JcrExecutionContext#getSessionFactory() getting sessions}, modifying the appropriate
     * nodes, {@link Session#save() saving} any changes made by this sequencer, and {@link Session#logout() closing} all sessions
     * (and any other acquired resources), even in the case of {@link ActivityMonitor#isCancelled() cancellation} or exceptions.
     * <p>
     * The {@link SequencingService} determines the sequencers that should be executed by monitoring the changes to one or more
     * workspaces (it is a {@link NodeChangeListener} registered with the {@link ObservationService}). Changes in those workspaces
     * are aggregated for each transaction, and organized into {@link NodeChanges changes for each node}. The SequencingService
     * then determines for each {@link NodeChange set of changes to a node} the set of full paths to the properties that have
     * changed and whether those paths {@link SequencerPathExpression#matcher(String) match} the sequencer's
     * {@link SequencerConfig#getPathExpressions() path expressions}. Each path expression produces the path to the output node,
     * and these output paths are accumulated and (with the original node that changed, the node change summary, and other
     * information) supplied to the sequencer via this method.
     * <p>
     * It is possible that a sequencer is configured to apply to multiple properties on a node. So, in cases where multiple
     * properties are changed on a single node (within a single repository transaction), the sequencer will only be executed once.
     * Also, in such cases the sequencer's configuration may imply multiple output nodes, so it is left to the sequencer to define
     * the behavior in such cases.
     * </p>
     * <p>
     * This operation should report progress to the supplied {@link ActivityMonitor}. At the beginning of the operation, call
     * {@link ActivityMonitor#beginTask(double, org.jboss.dna.common.i18n.I18n, Object...)} with a meaningful message describing
     * the operation and a total for the amount of work that will be done by this sequencer. Then perform the sequencing work,
     * periodically reporting work by specifying the {@link ActivityMonitor#worked(double) amount of work} that has was just
     * completed or by {@link ActivityMonitor#createSubtask(double) creating a subtask} and reporting work against that subtask
     * monitor.
     * </p>
     * <p>
     * The implementation should also periodically check whether the operation has been {@link ActivityMonitor#isCancelled()
     * cancelled}. If this method returns true, the implementation should abort all work as soon as possible and close any
     * resources that were acquired or opened.
     * </p>
     * <p>
     * Finally, the implementation should call {@link ActivityMonitor#done()} when the operation has finished.
     * </p>
     * 
     * @param input the node that has recently been created or changed; never null
     * @param sequencedPropertyName the name of the property that caused this sequencer to be executed; never null and never empty
     * @param changes the immutable summary of changes that occurred on the <code>input</code> node within the transaction; never
     *        null
     * @param outputPaths the paths to the nodes where the sequencing content should be placed; never null and never empty, but
     *        the set may contain paths for non-existant nodes or may reference the <code>input</code> node
     * @param context the context in which this sequencer is executing; never null
     * @param activityMonitor the activity monitor that should be kept updated with the sequencer's progress and that should be
     *        frequently consulted as to whether this operation has been {@link ActivityMonitor#isCancelled() cancelled}.
     * @throws RepositoryException if there is a problem while working with the repository
     * @throws SequencerException if there is an error in this sequencer
     */
    void execute( Node input,
                  String sequencedPropertyName,
                  NodeChange changes,
                  Set<RepositoryNodePath> outputPaths,
                  JcrExecutionContext context,
                  ActivityMonitor activityMonitor ) throws RepositoryException, SequencerException;

}
