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
package org.jboss.dna.connector.store.jpa.model.simple;

import org.jboss.dna.connector.store.jpa.JpaSource;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.WritableConnectorTest;
import org.jboss.dna.graph.observe.Observer;

public class SimpleJpaConnectorWritableTest extends WritableConnectorTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = new JpaSource();

        source.setModel(JpaSource.Models.SIMPLE.getName());
        source.setName("SimpleJpaSource");
        source.setDialect("org.hibernate.dialect.HSQLDialect");
        source.setDriverClassName("org.hsqldb.jdbcDriver");
        source.setUsername("sa");
        source.setPassword("");
        source.setUrl("jdbc:hsqldb:mem:test");
        source.setShowSql(false);
        source.setAutoGenerateSchema("create");
        source.setReferentialIntegrityEnforced(false);

        source.initialize(new RepositoryContext() {

            private final ExecutionContext context = new ExecutionContext();

            public Subgraph getConfiguration( int depth ) {
                return null;
            }

            public ExecutionContext getExecutionContext() {
                return context;
            }

            public Observer getObserver() {
                return null;
            }

            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return null;
            }

        });

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
    }

}
