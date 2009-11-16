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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.query.model.BindVariableName;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.validate.Schemata;

/**
 * An immutable context in which queries are to be executed. Each query context defines the information that is available during
 * query execution.
 */
@Immutable
public class QueryContext {
    private final ExecutionContext context;
    private final PlanHints hints;
    private final Schemata schemata;
    private final Problems problems;
    private final Map<String, Object> variables;

    /**
     * Create a new context for query execution.
     * 
     * @param context the execution context
     * @param schemata the schemata
     * @param hints the hints, or null if there are no hints
     * @param problems the problems container, or null if a new problems container should be created
     * @param variables the mapping of variables and values, or null if there are no such variables
     * @throws IllegalArgumentException if the context or schmata are null
     */
    public QueryContext( ExecutionContext context,
                         Schemata schemata,
                         PlanHints hints,
                         Problems problems,
                         Map<String, Object> variables ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(schemata, "schemata");
        this.context = context;
        this.hints = hints != null ? hints : new PlanHints();
        this.schemata = schemata;
        this.problems = problems != null ? problems : new SimpleProblems();
        this.variables = variables != null ? Collections.<String, Object>unmodifiableMap(new HashMap<String, Object>(variables)) : Collections.<String, Object>emptyMap();
        assert this.context != null;
        assert this.hints != null;
        assert this.schemata != null;
        assert this.problems != null;
        assert this.variables != null;
    }

    /**
     * Create a new context for query execution.
     * 
     * @param context the execution context
     * @param schemata the schemata
     * @param hints the hints, or null if there are no hints
     * @param problems the problems container, or null if a new problems container should be created
     * @throws IllegalArgumentException if the context or schmata are null
     */
    public QueryContext( ExecutionContext context,
                         Schemata schemata,
                         PlanHints hints,
                         Problems problems ) {
        this(context, schemata, hints, problems, null);
    }

    /**
     * Create a new context for query execution.
     * 
     * @param context the execution context
     * @param schemata the schemata
     * @param hints the hints, or null if there are no hints
     * @throws IllegalArgumentException if the context or schmata are null
     */
    public QueryContext( ExecutionContext context,
                         Schemata schemata,
                         PlanHints hints ) {
        this(context, schemata, hints, null, null);
    }

    /**
     * Create a new context for query execution.
     * 
     * @param context the execution context
     * @param schemata the schemata
     * @throws IllegalArgumentException if the context or schmata are null
     */
    public QueryContext( ExecutionContext context,
                         Schemata schemata ) {
        this(context, schemata, null, null, null);
    }

    /**
     * Get the execution context available to this query context.
     * 
     * @return the execution context; never null
     */
    public final ExecutionContext getExecutionContext() {
        return context;
    }

    /**
     * Get the plan hints.
     * 
     * @return the plan hints; never null
     */
    public final PlanHints getHints() {
        return hints;
    }

    /**
     * Get the problem container used by this query context. Any problems that have been encountered will be accumlated in this
     * container.
     * 
     * @return the problem container; never null
     */
    public final Problems getProblems() {
        return problems;
    }

    /**
     * Get the definition of the tables available within this query context.
     * 
     * @return the schemata; never null
     */
    public Schemata getSchemata() {
        return schemata;
    }

    /**
     * Get the variables that are to be substituted into the {@link BindVariableName} used in the query.
     * 
     * @return immutable map of variable values keyed by their name; never null but possibly empty
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied execution context.
     * 
     * @param context the execution context that should be used in the new query context
     * @return the new context; never null
     * @throws IllegalArgumentException if the execution context reference is null
     */
    public QueryContext with( ExecutionContext context ) {
        CheckArg.isNotNull(context, "context");
        return new QueryContext(context, schemata, hints, problems, variables);
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied schemata.
     * 
     * @param schemata the schemata that should be used in the new context
     * @return the new context; never null
     * @throws IllegalArgumentException if the schemata reference is null
     */
    public QueryContext with( Schemata schemata ) {
        CheckArg.isNotNull(schemata, "schemata");
        return new QueryContext(context, schemata, hints, problems, variables);
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied hints.
     * 
     * @param hints the hints that should be used in the new context
     * @return the new context; never null
     * @throws IllegalArgumentException if the hints reference is null
     */
    public QueryContext with( PlanHints hints ) {
        CheckArg.isNotNull(hints, "hints");
        return new QueryContext(context, schemata, hints, problems, variables);
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied problem container.
     * 
     * @param problems the problems that should be used in the new context; may be null if a new problem container should be used
     * @return the new context; never null
     */
    public QueryContext with( Problems problems ) {
        return new QueryContext(context, schemata, hints, problems, variables);
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied variables.
     * 
     * @param variables the variables that should be used in the new context; may be null if there are no such variables
     * @return the new context; never null
     */
    public QueryContext with( Map<String, Object> variables ) {
        return new QueryContext(context, schemata, hints, problems, variables);
    }

}
