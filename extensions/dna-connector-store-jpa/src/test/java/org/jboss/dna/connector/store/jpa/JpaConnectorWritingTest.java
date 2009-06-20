/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.connector.store.jpa;

import org.jboss.dna.common.statistic.Stopwatch;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.WritableConnectorTest;
import org.jboss.dna.graph.property.ReferentialIntegrityException;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class JpaConnectorWritingTest extends WritableConnectorTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        // Set the connection properties to be an in-memory HSQL database ...
        JpaSource source = new JpaSource();
        source.setName("Test Repository");
        source.setDialect("org.hibernate.dialect.HSQLDialect");
        source.setDriverClassName("org.hsqldb.jdbcDriver");
        source.setUsername("sa");
        source.setPassword("");
        source.setUrl("jdbc:hsqldb:.");
        source.setMaximumConnectionsInPool(1);
        source.setMinimumConnectionsInPool(0);
        source.setNumberOfConnectionsToAcquireAsNeeded(1);
        source.setMaximumSizeOfStatementCache(100);
        source.setMaximumConnectionIdleTimeInSeconds(0);
        source.setReferentialIntegrityEnforced(true);
        source.setLargeValueSizeInBytes(150);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
        graph.createWorkspace().named("default");
    }

    @Test( expected = ReferentialIntegrityException.class )
    public void shouldNotCopyChildrenBetweenWorkspacesAndRemoveExistingNodesWithSameUuidIfSpecifiedIfReferentialIntegrityIsViolated()
        throws Exception {
        String defaultWorkspaceName = graph.getCurrentWorkspaceName();
        String workspaceName = "copyChildrenSource";

        tryCreatingAWorkspaceNamed(workspaceName);

        graph.useWorkspace(workspaceName);
        String initialPath = "";
        int depth = 3;
        int numChildrenPerNode = 3;
        int numPropertiesPerNode = 3;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);

        graph.useWorkspace(defaultWorkspaceName);

        graph.create("/newUuids");
        // Copy once to get the UUID into the default workspace
        graph.copy("/node1").replacingExistingNodesWithSameUuids().fromWorkspace(workspaceName).to("/newUuids/node1");

        // Create a new child node that in the target workspace that has no corresponding node in the source workspace
        graph.create("/newUuids/node1/shouldBeRemoved");
        graph.create("/refererringNode");
        graph.set("refProp").on("/refererringNode").to(graph.getNodeAt("/newUuids/node1/shouldBeRemoved"));

        // Now create a reference to this new node

        // Copy again to test the behavior now that the UUIDs are already in the default workspace
        // This should fail because /newUuids/node1/shouldBeRemoved must be removed by the copy, but can't be removed
        // because there is a reference to it.
        graph.copy("/node1").replacingExistingNodesWithSameUuids().fromWorkspace(workspaceName).to("/newUuids/otherNode");
    }
}
