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
package org.jboss.dna.graph.query.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.QueryResults.Statistics;
import org.jboss.dna.graph.query.model.ChildNodeJoinCondition;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.EquiJoinCondition;
import org.jboss.dna.graph.query.model.JoinCondition;
import org.jboss.dna.graph.query.model.JoinType;
import org.jboss.dna.graph.query.model.Limit;
import org.jboss.dna.graph.query.model.Ordering;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.SameNodeJoinCondition;
import org.jboss.dna.graph.query.model.SetQuery.Operation;
import org.jboss.dna.graph.query.plan.JoinAlgorithm;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;
import org.jboss.dna.graph.query.process.SelectComponent.Analyzer;

/**
 * An abstract {@link Processor} implementation that builds a tree of {@link ProcessingComponent} objects to perform the different
 * parts of the query processing logic. Subclasses are required to only implement one method: the
 * {@link #createAccessComponent(QueryContext, PlanNode, Columns, Analyzer)} should create a ProcessorComponent object that will
 * perform the (low-level access) query described by the {@link PlanNode plan} given as a parameter.
 */
public abstract class QueryProcessor implements Processor {

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.process.Processor#execute(org.jboss.dna.graph.query.QueryContext,
     *      org.jboss.dna.graph.query.model.QueryCommand, org.jboss.dna.graph.query.QueryResults.Statistics,
     *      org.jboss.dna.graph.query.plan.PlanNode)
     */
    public QueryResults execute( QueryContext context,
                                 QueryCommand command,
                                 Statistics statistics,
                                 PlanNode plan ) {
        long nanos = System.nanoTime();
        Columns columns = null;
        List<Object[]> tuples = null;
        try {
            // Find the topmost PROJECT node and build the Columns ...
            PlanNode project = plan.findAtOrBelow(Type.PROJECT);
            assert project != null;
            List<Column> projectedColumns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
            assert projectedColumns != null;
            assert !projectedColumns.isEmpty();
            columns = new QueryResultColumns(projectedColumns, context.getHints().hasFullTextSearch);

            // Go through the plan and create the corresponding ProcessingComponents ...
            Analyzer analyzer = createAnalyzer(context);
            ProcessingComponent component = createComponent(context, plan, columns, analyzer);
            long nanos2 = System.nanoTime();
            statistics = statistics.withResultsFormulationTime(nanos2 - nanos);

            // Now execute the component ...
            nanos = nanos2;
            tuples = component.execute();

        } finally {
            statistics = statistics.withExecutionTime(System.nanoTime() - nanos);
        }
        if (tuples == null) tuples = Collections.emptyList();
        return new org.jboss.dna.graph.query.process.QueryResults(context, command, columns, statistics, tuples);
    }

    /**
     * Create an {@link Analyzer} implementation that should be used by the non-access {@link ProcessingComponent}s that evaluate
     * criteria. By default, this method returns null, which means that any criteria evaluation will likely be pushed down under
     * an {@link Type#ACCESS ACCESS} node (and thus handled by an
     * {@link #createAccessComponent(QueryContext, PlanNode, Columns, Analyzer) access component}.
     * <p>
     * However, for more simple access components that are not capable of handling joins and other non-trivial criteria, simply
     * return an Analyzer implementation that implements the methods using the source.
     * </p>
     * 
     * @param context the context in which query is being evaluated
     * @return the analyzer, or null if the ProcessingComponent objects that evaluate criteria should use a best-effort approach
     */
    protected Analyzer createAnalyzer( QueryContext context ) {
        return null;
    }

    /**
     * Create the {@link ProcessingComponent} that processes a single {@link Type#ACCESS} branch of a query plan.
     * 
     * @param context the context in which query is being evaluated; never null
     * @param accessNode the node in the query plan that represents the {@link Type#ACCESS} plan; never null
     * @param resultColumns the columns that are to be returned; never null
     * @param analyzer the criteria analyzer; never null
     * @return the processing component; may not be null
     */
    protected abstract ProcessingComponent createAccessComponent( QueryContext context,
                                                                  PlanNode accessNode,
                                                                  Columns resultColumns,
                                                                  Analyzer analyzer );

    /**
     * Method that is called to build up the {@link ProcessingComponent} objects that correspond to the optimized query plan. This
     * method is called by {@link #execute(QueryContext, QueryCommand, Statistics, PlanNode)} for each of the various
     * {@link PlanNode} objects in the optimized query plan, and the method is actually recursive (since the optimized query plan
     * forms a tree). However, whenever this call structure reaches the {@link Type#ACCESS ACCESS} nodes in the query plan (which
     * each represents a separate atomic low-level query to the underlying system), the
     * {@link #createAccessComponent(QueryContext, PlanNode, Columns, Analyzer)} method is called. Subclasses should create an
     * appropriate ProcessingComponent implementation that performs this atomic low-level query.
     * 
     * @param context the context in which query is being evaluated
     * @param node the plan node for which the ProcessingComponent is to be created
     * @param columns the definition of the result columns for this portion of the query
     * @param analyzer the analyzer (returned from {@link #createAnalyzer(QueryContext)}) that should be used on the components
     *        that evaluate criteria; may be null if a best-effort should be made for the evaluation
     * @return the processing component for this plan node; never null
     */
    protected ProcessingComponent createComponent( QueryContext context,
                                                   PlanNode node,
                                                   Columns columns,
                                                   Analyzer analyzer ) {
        ProcessingComponent component = null;
        switch (node.getType()) {
            case ACCESS:
                // Create the component to handle the ACCESS node ...
                assert node.getChildCount() == 1;
                component = createAccessComponent(context, node, columns, analyzer);
                // // Don't do anything special with an access node at the moment ...
                // component = createComponent(context, node.getFirstChild(), columns, analyzer);
                break;
            case DUP_REMOVE:
                // Create the component under the DUP_REMOVE ...
                assert node.getChildCount() == 1;
                ProcessingComponent distinctDelegate = createComponent(context, node.getFirstChild(), columns, analyzer);
                component = new DistinctComponent(distinctDelegate);
                break;
            case GROUP:
                throw new UnsupportedOperationException();
            case JOIN:
                // Create the components under the JOIN ...
                assert node.getChildCount() == 2;
                ProcessingComponent left = createComponent(context, node.getFirstChild(), columns, analyzer);
                ProcessingComponent right = createComponent(context, node.getLastChild(), columns, analyzer);
                // Create the join component ...
                JoinAlgorithm algorithm = node.getProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.class);
                JoinType joinType = node.getProperty(Property.JOIN_TYPE, JoinType.class);
                JoinCondition joinCondition = node.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
                switch (algorithm) {
                    case MERGE:
                        if (joinCondition instanceof SameNodeJoinCondition) {
                            SameNodeJoinCondition condition = (SameNodeJoinCondition)joinCondition;
                            component = new MergeJoinComponent(context, left, right, condition, joinType);
                        } else if (joinCondition instanceof ChildNodeJoinCondition) {
                            ChildNodeJoinCondition condition = (ChildNodeJoinCondition)joinCondition;
                            component = new MergeJoinComponent(context, left, right, condition, joinType);
                        } else if (joinCondition instanceof EquiJoinCondition) {
                            EquiJoinCondition condition = (EquiJoinCondition)joinCondition;
                            component = new MergeJoinComponent(context, left, right, condition, joinType);
                        } else {
                            assert false : "Unable to use merge algorithm with descendant node join conditions";
                            throw new UnsupportedOperationException();
                        }
                        break;
                    case NESTED_LOOP:
                        component = new NestedLoopJoinComponent(context, left, right, joinCondition, joinType);
                        break;
                }
                // For each Constraint object applied to the JOIN, simply create a SelectComponent on top ...
                List<Constraint> constraints = node.getPropertyAsList(Property.JOIN_CONSTRAINTS, Constraint.class);
                for (Constraint constraint : constraints) {
                    component = new SelectComponent(component, constraint, context.getVariables());
                }
                break;
            case LIMIT:
                // Create the component under the LIMIT ...
                assert node.getChildCount() == 1;
                ProcessingComponent limitDelegate = createComponent(context, node.getFirstChild(), columns, analyzer);
                // Then create the limit component ...
                Integer rowLimit = node.getProperty(Property.LIMIT_COUNT, Integer.class);
                Integer offset = node.getProperty(Property.LIMIT_OFFSET, Integer.class);
                Limit limit = Limit.NONE;
                if (rowLimit != null) limit = limit.withRowLimit(rowLimit.intValue());
                if (offset != null) limit = limit.withOffset(offset.intValue());
                component = new LimitComponent(limitDelegate, limit);
                break;
            case NULL:
                component = new NoResultsComponent(context, columns);
                break;
            case PROJECT:
                // Create the component under the PROJECT ...
                assert node.getChildCount() == 1;
                ProcessingComponent projectDelegate = createComponent(context, node.getFirstChild(), columns, analyzer);
                // Then create the project component ...
                List<Column> projectedColumns = node.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
                component = new ProjectComponent(projectDelegate, projectedColumns);
                break;
            case SELECT:
                // Create the component under the SELECT ...
                assert node.getChildCount() == 1;
                ProcessingComponent selectDelegate = createComponent(context, node.getFirstChild(), columns, analyzer);
                // Then create the select component ...
                Constraint constraint = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                component = new SelectComponent(selectDelegate, constraint, context.getVariables(), analyzer);
                break;
            case SET_OPERATION:
                // Create the components under the SET_OPERATION ...
                List<ProcessingComponent> setDelegates = new LinkedList<ProcessingComponent>();
                for (PlanNode child : node) {
                    setDelegates.add(createComponent(context, child, columns, analyzer));
                }
                // Then create the select component ...
                Operation operation = node.getProperty(Property.SET_OPERATION, Operation.class);
                boolean all = node.getProperty(Property.SET_USE_ALL, Boolean.class);
                boolean alreadySorted = false; // ????
                switch (operation) {
                    case EXCEPT:
                        component = new ExceptComponent(context, columns, setDelegates, alreadySorted, all);
                        break;
                    case INTERSECT:
                        component = new IntersectComponent(context, columns, setDelegates, alreadySorted, all);
                        break;
                    case UNION:
                        component = new UnionComponent(context, columns, setDelegates, alreadySorted, all);
                        break;
                }
                break;
            case SORT:
                // Create the component under the SORT ...
                assert node.getChildCount() == 1;
                ProcessingComponent sortDelegate = createComponent(context, node.getFirstChild(), columns, analyzer);
                // Then create the sort component ...
                List<Object> orderBys = node.getPropertyAsList(Property.SORT_ORDER_BY, Object.class);
                if (orderBys.isEmpty()) {
                    component = sortDelegate;
                } else {
                    if (orderBys.get(0) instanceof Ordering) {
                        List<Ordering> orderings = new ArrayList<Ordering>(orderBys.size());
                        for (Object orderBy : orderBys) {
                            orderings.add((Ordering)orderBy);
                        }
                        component = new SortValuesComponent(sortDelegate, orderings);
                    } else {
                        // Order by the location(s) because it's before a merge-join ...
                        component = new SortLocationsComponent(sortDelegate);
                    }
                }
                break;
            case SOURCE:
                assert false : "Source nodes should always be below ACCESS nodes by the time a plan is executed";
                throw new UnsupportedOperationException();
        }
        assert component != null;
        return component;
    }
}
