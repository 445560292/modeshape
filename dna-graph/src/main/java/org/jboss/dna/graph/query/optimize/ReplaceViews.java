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
package org.jboss.dna.graph.query.optimize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.plan.CanonicalPlanner;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.PlanUtil;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.query.validate.Schemata.Table;
import org.jboss.dna.graph.query.validate.Schemata.View;

/**
 * An {@link OptimizerRule optimizer rule} that replaces any SOURCE nodes that happen to be {@link View views}. This rewriting
 * changes all of the elements of the plan that reference the SOURCE and it's columns, including criteria, project nodes, etc.
 * <p>
 * For example, here is the portion of a plan that uses a single SOURCE that is defined to use a view.
 * 
 * <pre>
 *          ...
 *           |
 *        SOURCE1
 * </pre>
 * 
 * This same SOURCE node is then replaced with the view's definition:
 * 
 * <pre>
 *          ...
 *           |
 *        PROJECT      with the list of columns being SELECTed
 *           |
 *        SELECT1
 *           |         One or more SELECT plan nodes that each have
 *        SELECT2      a single non-join constraint that are then all AND-ed
 *           |         together
 *        SELECTn
 *           |
 *        SOURCE
 * </pre>
 * 
 * </p>
 */
@Immutable
public class ReplaceViews implements OptimizerRule {

    public static final ReplaceViews INSTANCE = new ReplaceViews();

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.optimize.OptimizerRule#execute(org.jboss.dna.graph.query.QueryContext,
     *      org.jboss.dna.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // Prepare a cache of the view plans ...
        Map<SelectorName, PlanNode> viewPlanCache = new HashMap<SelectorName, PlanNode>();
        CanonicalPlanner planner = new CanonicalPlanner();

        // Prepare the maps that will record the old-to-new mappings from the old view SOURCE nodes to the table SOURCEs ...

        // For each of the SOURCE nodes ...
        Schemata schemata = context.getSchemata();
        Set<PlanNode> processedSources = new HashSet<PlanNode>();
        boolean foundViews = false;
        do {
            foundViews = false;
            for (PlanNode sourceNode : plan.findAllAtOrBelow(Type.SOURCE)) {
                if (processedSources.contains(sourceNode)) continue;
                processedSources.add(sourceNode);

                // Resolve the node to find the definition in the schemata ...
                SelectorName tableName = sourceNode.getProperty(Property.SOURCE_NAME, SelectorName.class);
                Table table = schemata.getTable(tableName);
                if (table instanceof View) {
                    View view = (View)table;
                    PlanNode viewPlan = viewPlanCache.get(tableName);
                    if (viewPlan == null) {
                        viewPlan = planner.createPlan(context, view.getDefinition());
                        if (viewPlan != null) viewPlanCache.put(tableName, viewPlan);
                    }
                    if (viewPlan == null) continue; // there were likely errors when creating the plan
                    viewPlan = viewPlan.clone();

                    // Insert the view plan under the parent SOURCE node ...
                    sourceNode.addLastChild(viewPlan);

                    // Update the plan above this node to replace references to the view columns with references to the
                    // tables/views used by the view ...
                    PlanUtil.ColumnMapping viewMappings = PlanUtil.createMappingFor(view, viewPlan);
                    PlanUtil.replaceViewReferences(context, viewPlan, viewMappings);

                    if (viewPlan.is(Type.PROJECT)) {
                        // The PROJECT from the plan may actually not be needed if there is another PROJECT above it ...
                        PlanNode node = viewPlan.getParent();
                        while (node != null) {
                            if (node.isOneOf(Type.JOIN)) break;
                            if (node.is(Type.PROJECT) && viewPlan.getSelectors().containsAll(node.getSelectors())) {
                                viewPlan.extractFromParent();
                                break;
                            }
                            node = node.getParent();
                        }
                    }
                    foundViews = true;
                }
            }
        } while (foundViews);

        if (foundViews) {
            // We'll need to try to push up criteria from the join, but we only should do this after this rule
            // is completely done ...
            if (!(ruleStack.getFirst() instanceof RaiseSelectCriteria)) {
                ruleStack.addFirst(RaiseSelectCriteria.INSTANCE);
            }

            // We re-wrote at least one SOURCE, but the resulting plan tree for the view could actually reference
            // other views. Therefore, re-run this rule ...
            ruleStack.addFirst(this);
        }
        return plan;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
