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
package org.jboss.dna.graph.query;

import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.plan.PlanHints;

/**
 * An interface defining the ability to submit a query and obtain results.
 */
public interface Queryable {

    /**
     * Execute the supplied query by planning, optimizing, and then processing it.
     * 
     * @param context the context in which the query should be executed
     * @param query the query that is to be executed
     * @return the query results; never null
     * @throws IllegalArgumentException if the context or query references are null
     */
    QueryResults execute( ExecutionContext context,
                          QueryCommand query );

    /**
     * Execute the supplied query by planning, optimizing, and then processing it.
     * 
     * @param context the context in which the query should be executed
     * @param query the query that is to be executed
     * @param hints the hints for the execution; may be null if there are no hints
     * @return the query results; never null
     * @throws IllegalArgumentException if the context or query references are null
     */
    QueryResults execute( ExecutionContext context,
                          QueryCommand query,
                          PlanHints hints );
}
