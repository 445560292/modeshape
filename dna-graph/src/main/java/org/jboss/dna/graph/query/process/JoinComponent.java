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
import java.util.Comparator;
import java.util.List;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueComparators;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.ChildNodeJoinCondition;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.DescendantNodeJoinCondition;
import org.jboss.dna.graph.query.model.EquiJoinCondition;
import org.jboss.dna.graph.query.model.JoinCondition;
import org.jboss.dna.graph.query.model.JoinType;
import org.jboss.dna.graph.query.model.SameNodeJoinCondition;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.validate.Schemata;

/**
 * 
 */
public abstract class JoinComponent extends ProcessingComponent {

    protected static final Comparator<Location> LOCATION_COMPARATOR = Location.comparator();

    private final ProcessingComponent left;
    private final ProcessingComponent right;
    private final JoinCondition condition;
    private final JoinType joinType;

    protected JoinComponent( QueryContext context,
                             ProcessingComponent left,
                             ProcessingComponent right,
                             JoinCondition condition,
                             JoinType joinType ) {
        super(context, computeJoinedColumns(left.getColumns(), right.getColumns()));
        this.left = left;
        this.right = right;
        this.joinType = joinType;
        this.condition = condition;
        assert this.left != null;
        assert this.right != null;
        assert this.joinType != null;
    }

    /**
     * Get the type of join this processor represents.
     * 
     * @return the join type; never null
     */
    public final JoinType getJoinType() {
        return joinType;
    }

    /**
     * Get the join condition.
     * 
     * @return the join condition; never null
     */
    public final JoinCondition getJoinCondition() {
        return condition;
    }

    /**
     * Get the processing component that serves as the left side of the join.
     * 
     * @return the left-side processing component; never null
     */
    protected final ProcessingComponent left() {
        return left;
    }

    /**
     * Get the processing component that serves as the right side of the join.
     * 
     * @return the right-side processing component; never null
     */
    protected final ProcessingComponent right() {
        return right;
    }

    /**
     * Get the columns definition for the results from the left side of the join.
     * 
     * @return the left-side columns that feed this join; never null
     */
    protected final Columns leftColunns() {
        return left.getColumns();
    }

    /**
     * Get the columns definition for the results from the right side of the join.
     * 
     * @return the right-side columns that feed this join; never null
     */
    protected final Columns rightColumns() {
        return right.getColumns();
    }

    protected static Columns computeJoinedColumns( Columns leftColumns,
                                                   Columns rightColumns ) {
        List<Column> columns = new ArrayList<Column>(leftColumns.getColumnCount() + rightColumns.getColumnCount());
        columns.addAll(leftColumns.getColumns());
        columns.addAll(rightColumns.getColumns());
        boolean includeFullTextScores = leftColumns.hasFullTextSearchScores() || rightColumns.hasFullTextSearchScores();
        return new QueryResultColumns(columns, includeFullTextScores);
    }

    protected static TupleMerger createMerger( Columns joinColumns,
                                               Columns leftColumns,
                                               Columns rightColumns ) {
        final int joinTupleSize = joinColumns.getTupleSize();
        final int joinColumnCount = joinColumns.getColumnCount();
        final int joinLocationCount = joinColumns.getLocationCount();
        final int leftColumnCount = leftColumns.getColumnCount();
        final int leftLocationCount = leftColumns.getLocationCount();
        final int leftTupleSize = leftColumns.getTupleSize();
        final int rightColumnCount = rightColumns.getColumnCount();
        final int rightLocationCount = rightColumns.getLocationCount();
        final int rightTupleSize = rightColumns.getTupleSize();
        final int startLeftLocations = joinColumnCount;
        final int startRightLocations = startLeftLocations + leftLocationCount;

        // The left and right selectors should NOT overlap ...
        assert joinLocationCount == leftLocationCount + rightLocationCount;

        // Create different implementations depending upon the options, since this save us from having to make
        // these decisions while doing the merges...
        if (joinColumns.hasFullTextSearchScores()) {
            final int leftScoreCount = leftTupleSize - leftColumnCount - leftLocationCount;
            final int rightScoreCount = rightTupleSize - rightColumnCount - rightLocationCount;
            final int startLeftScores = startRightLocations + rightLocationCount;
            final int startRightScores = startLeftScores + leftScoreCount;
            final boolean leftScores = leftScoreCount > 0;
            final boolean rightScores = rightScoreCount > 0;

            return new TupleMerger() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.query.process.JoinComponent.TupleMerger#merge(java.lang.Object[],
                 *      java.lang.Object[])
                 */
                public Object[] merge( Object[] leftTuple,
                                       Object[] rightTuple ) {
                    Object[] result = new Object[joinTupleSize]; // initialized to null
                    // If the tuple arrays are null, then we don't need to copy because the arrays are
                    // initialized to null values.
                    if (leftTuple != null) {
                        // Copy the left tuple values ...
                        System.arraycopy(result, 0, leftTuple, 0, leftColumnCount);
                        System.arraycopy(result, startLeftLocations, leftTuple, leftColumnCount, leftLocationCount);
                        if (leftScores) {
                            System.arraycopy(result, startLeftScores, leftTuple, leftLocationCount, leftScoreCount);
                        }
                    }
                    if (rightTuple != null) {
                        // Copy the right tuple values ...
                        System.arraycopy(result, leftColumnCount, rightTuple, 0, rightColumnCount);
                        System.arraycopy(result, startRightLocations, rightTuple, rightColumnCount, rightLocationCount);
                        if (rightScores) {
                            System.arraycopy(result, startRightScores, rightTuple, rightLocationCount, rightScoreCount);
                        }
                    }
                    return result;
                }
            };
        }
        // There are no full-text search scores ...
        return new TupleMerger() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.query.process.JoinComponent.TupleMerger#merge(java.lang.Object[], java.lang.Object[])
             */
            public Object[] merge( Object[] leftTuple,
                                   Object[] rightTuple ) {
                Object[] result = new Object[joinTupleSize]; // initialized to null
                // If the tuple arrays are null, then we don't need to copy because the arrays are
                // initialized to null values.
                if (leftTuple != null) {
                    // Copy the left tuple values ...
                    System.arraycopy(result, 0, leftTuple, 0, leftColumnCount);
                    System.arraycopy(result, startLeftLocations, leftTuple, leftColumnCount, leftLocationCount);
                }
                if (rightTuple != null) {
                    // Copy the right tuple values ...
                    System.arraycopy(result, leftColumnCount, rightTuple, 0, rightColumnCount);
                    System.arraycopy(result, startRightLocations, rightTuple, rightColumnCount, rightLocationCount);
                }
                return result;
            }
        };
    }

    protected static interface TupleMerger {
        Object[] merge( Object[] leftTuple,
                        Object[] rightTuple );
    }

    /**
     * Interface defining the value of a tuple that is used in the join condition.
     */
    protected static interface ValueSelector {
        /**
         * Obtain the value that is to be used in the join condition.
         * 
         * @param tuple the tuple
         * @return the value that should be used
         */
        Object evaluate( Object[] tuple );
    }

    /**
     * Create a {@link ValueSelector} that obtains the value required to use the supplied join condition.
     * 
     * @param source the source component; may not be null
     * @param condition the join condition; may not be null
     * @return the value selector; never null
     */
    protected static ValueSelector valueSelectorFor( ProcessingComponent source,
                                                     JoinCondition condition ) {
        if (condition instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition joinCondition = (ChildNodeJoinCondition)condition;
            String childSelectorName = joinCondition.getChildSelectorName().getName();
            if (source.getColumns().hasSelector(childSelectorName)) {
                return selectPath(source, childSelectorName);
            }
            String parentSelectorName = joinCondition.getParentSelectorName().getName();
            return selectPath(source, parentSelectorName);
        } else if (condition instanceof SameNodeJoinCondition) {
            SameNodeJoinCondition joinCondition = (SameNodeJoinCondition)condition;
            String selector1Name = joinCondition.getSelector1Name().getName();
            if (source.getColumns().hasSelector(selector1Name)) {
                return selectPath(source, selector1Name);
            }
            String selector2Name = joinCondition.getSelector2Name().getName();
            return selectPath(source, selector2Name);
        } else if (condition instanceof DescendantNodeJoinCondition) {
            DescendantNodeJoinCondition joinCondition = (DescendantNodeJoinCondition)condition;
            String ancestorSelectorName = joinCondition.getAncestorSelectorName().getName();
            if (source.getColumns().hasSelector(ancestorSelectorName)) {
                return selectPath(source, ancestorSelectorName);
            }
            String descendantSelectorName = joinCondition.getDescendantSelectorName().getName();
            return selectPath(source, descendantSelectorName);
        } else if (condition instanceof EquiJoinCondition) {
            EquiJoinCondition joinCondition = (EquiJoinCondition)condition;
            SelectorName selector1Name = joinCondition.getSelector1Name();
            Name propName1 = joinCondition.getProperty1Name();
            if (source.getColumns().hasSelector(selector1Name.getName())) {
                return selectValue(source, selector1Name, propName1);
            }
            SelectorName selector2Name = joinCondition.getSelector2Name();
            Name propName2 = joinCondition.getProperty2Name();
            return selectValue(source, selector2Name, propName2);
        }
        throw new IllegalArgumentException();
    }

    private static ValueSelector selectPath( ProcessingComponent component,
                                             String selectorName ) {
        final int index = component.getColumns().getLocationIndex(selectorName);
        return new ValueSelector() {
            public Object evaluate( Object[] tuple ) {
                Location location = (Location)tuple[index];
                return location != null ? location.getPath() : null;
            }
        };
    }

    private static ValueSelector selectValue( ProcessingComponent component,
                                              SelectorName selectorName,
                                              Name propertyName ) {
        final int index = component.getColumns().getColumnIndexForProperty(selectorName.getName(), propertyName);
        return new ValueSelector() {
            public Object evaluate( Object[] tuple ) {
                return tuple[index];
            }
        };
    }

    /**
     * Interface defining the value of a tuple that is used in the join condition.
     */
    protected static interface Joinable {
        /**
         * Obtain the value that is to be used in the join condition.
         * 
         * @param leftValue the value from the left tuple; never null
         * @param rightValue the value from the right tuple; never null
         * @return true if the tuples are to be joined
         */
        boolean evaluate( Object leftValue,
                          Object rightValue );
    }

    /**
     * Create a {@link ValueSelector} that obtains the value required to use the supplied join condition.
     * 
     * @param left the left source component; may not be null
     * @param right the left source component; may not be null
     * @param condition the join condition; may not be null
     * @return the value selector; never null
     */
    protected static Joinable joinableFor( ProcessingComponent left,
                                           ProcessingComponent right,
                                           JoinCondition condition ) {
        if (condition instanceof SameNodeJoinCondition) {
            return new Joinable() {
                public boolean evaluate( Object locationA,
                                         Object locationB ) {
                    Location location1 = (Location)locationA;
                    Location location2 = (Location)locationB;
                    return location1.isSame(location2);
                }
            };
        } else if (condition instanceof EquiJoinCondition) {
            return new Joinable() {
                public boolean evaluate( Object leftValue,
                                         Object rightValue ) {
                    return leftValue.equals(rightValue);
                }
            };
        } else if (condition instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition joinCondition = (ChildNodeJoinCondition)condition;
            String childSelectorName = joinCondition.getChildSelectorName().getName();
            if (left.getColumns().hasSelector(childSelectorName)) {
                // The child is on the left ...
                return new Joinable() {
                    public boolean evaluate( Object childLocation,
                                             Object parentLocation ) {
                        Path childPath = ((Location)childLocation).getPath();
                        Path parentPath = ((Location)parentLocation).getPath();
                        return childPath.getParent().isSameAs(parentPath);
                    }
                };
            }
            // The child is on the right ...
            return new Joinable() {
                public boolean evaluate( Object parentLocation,
                                         Object childLocation ) {
                    Path childPath = ((Location)childLocation).getPath();
                    Path parentPath = ((Location)parentLocation).getPath();
                    return childPath.getParent().isSameAs(parentPath);
                }
            };
        } else if (condition instanceof DescendantNodeJoinCondition) {
            DescendantNodeJoinCondition joinCondition = (DescendantNodeJoinCondition)condition;
            String ancestorSelectorName = joinCondition.getAncestorSelectorName().getName();
            if (left.getColumns().hasSelector(ancestorSelectorName)) {
                // The ancestor is on the left ...
                return new Joinable() {
                    public boolean evaluate( Object ancestorLocation,
                                             Object descendantLocation ) {
                        Path ancestorPath = ((Location)ancestorLocation).getPath();
                        Path descendantPath = ((Location)descendantLocation).getPath();
                        return ancestorPath.isAncestorOf(descendantPath);
                    }
                };
            }
            // The ancestor is on the right ...
            return new Joinable() {
                public boolean evaluate( Object descendantLocation,
                                         Object ancestorLocation ) {
                    Path ancestorPath = ((Location)ancestorLocation).getPath();
                    Path descendantPath = ((Location)descendantLocation).getPath();
                    return ancestorPath.isAncestorOf(descendantPath);
                }
            };
        }
        throw new IllegalArgumentException();
    }

    /**
     * Create a {@link Comparable} that can be used to compare the values required to evaluate the supplied join condition.
     * 
     * @param context the context in which this query is being evaluated; may not be null
     * @param left the left source component; may not be null
     * @param right the left source component; may not be null
     * @param condition the join condition; may not be null
     * @return the comparator; never null
     */
    @SuppressWarnings( "unchecked" )
    protected static Comparator<Object> comparatorFor( QueryContext context,
                                                       ProcessingComponent left,
                                                       ProcessingComponent right,
                                                       JoinCondition condition ) {
        final Comparator<Path> pathComparator = ValueComparators.PATH_COMPARATOR;
        if (condition instanceof SameNodeJoinCondition) {
            return new Comparator<Object>() {
                public int compare( Object location1,
                                    Object location2 ) {
                    Path path1 = ((Location)location1).getPath();
                    Path path2 = ((Location)location2).getPath();
                    return pathComparator.compare(path1, path2);
                }
            };
        }
        if (condition instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition joinCondition = (ChildNodeJoinCondition)condition;
            String childSelectorName = joinCondition.getChildSelectorName().getName();
            if (left.getColumns().hasSelector(childSelectorName)) {
                // The child is on the left ...
                return new Comparator<Object>() {
                    public int compare( Object childLocation,
                                        Object parentLocation ) {
                        Path childPath = ((Location)childLocation).getPath();
                        Path parentPath = ((Location)parentLocation).getPath();
                        if (childPath.isRoot()) return parentPath.isRoot() ? 0 : -1;
                        Path parentOfChild = childPath.getParent();
                        return pathComparator.compare(parentPath, parentOfChild);
                    }
                };
            }
            // The child is on the right ...
            return new Comparator<Object>() {
                public int compare( Object parentLocation,
                                    Object childLocation ) {
                    Path childPath = ((Location)childLocation).getPath();
                    Path parentPath = ((Location)parentLocation).getPath();
                    if (childPath.isRoot()) return parentPath.isRoot() ? 0 : -1;
                    Path parentOfChild = childPath.getParent();
                    return pathComparator.compare(parentPath, parentOfChild);
                }
            };
        }
        if (condition instanceof EquiJoinCondition) {
            EquiJoinCondition joinCondition = (EquiJoinCondition)condition;
            SelectorName leftSelectorName = joinCondition.getSelector1Name();
            SelectorName rightSelectorName = joinCondition.getSelector2Name();
            Name leftPropertyName = joinCondition.getProperty1Name();
            Name rightPropertyName = joinCondition.getProperty2Name();

            ValueFactory<String> stringFactory = context.getExecutionContext().getValueFactories().getStringFactory();
            Schemata schemata = context.getSchemata();
            Schemata.Table leftTable = schemata.getTable(leftSelectorName);
            Schemata.Column leftColumn = leftTable.getColumn(stringFactory.create(leftPropertyName));
            PropertyType leftType = leftColumn.getPropertyType();

            Schemata.Table rightTable = schemata.getTable(rightSelectorName);
            Schemata.Column rightColumn = rightTable.getColumn(stringFactory.create(rightPropertyName));
            PropertyType rightType = rightColumn.getPropertyType();

            return leftType == rightType ? (Comparator<Object>)leftType.getComparator() : ValueComparators.OBJECT_COMPARATOR;
        }
        throw new IllegalArgumentException();
    }
}
