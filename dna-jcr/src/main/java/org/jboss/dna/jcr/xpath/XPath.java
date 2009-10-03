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
package org.jboss.dna.jcr.xpath;

import java.util.List;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.ObjectUtil;
import org.jboss.dna.graph.query.model.Operator;

/**
 * 
 */
public class XPath {

    public static enum NodeComparisonOperator {
        IS,
        PRECEDES,
        FOLLOWS;
    }

    public static interface Component {
    }

    public static abstract class UnaryComponent implements Component {
        protected final Component wrapped;

        public UnaryComponent( Component wrapped ) {
            this.wrapped = wrapped;
        }
    }

    public static class Negation extends UnaryComponent {
        public Negation( Component wrapped ) {
            super(wrapped);
        }

        public Component getNegated() {
            return wrapped;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "-" + wrapped;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Negation) {
                Negation that = (Negation)obj;
                return this.wrapped.equals(that.wrapped);
            }
            return false;
        }
    }

    public static abstract class BinaryComponent implements Component {
        private final Component left;
        private final Component right;

        public BinaryComponent( Component left,
                                Component right ) {
            this.left = left;
            this.right = right;
        }

        /**
         * @return left
         */
        public Component getLeft() {
            return left;
        }

        /**
         * @return right
         */
        public Component getRight() {
            return right;
        }
    }

    public static class Comparison extends BinaryComponent {
        private final Operator operator;

        public Comparison( Component left,
                           Operator operator,
                           Component right ) {
            super(left, right);
            this.operator = operator;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " " + operator + " " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Comparison) {
                Comparison that = (Comparison)obj;
                if (this.operator != that.operator) return false;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class NodeComparison extends BinaryComponent {
        private final NodeComparisonOperator operator;

        public NodeComparison( Component left,
                               NodeComparisonOperator operator,
                               Component right ) {
            super(left, right);
            this.operator = operator;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " " + operator + " " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof NodeComparison) {
                NodeComparison that = (NodeComparison)obj;
                if (this.operator != that.operator) return false;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Add extends BinaryComponent {
        public Add( Component left,
                    Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " + " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Add) {
                Add that = (Add)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Subtract extends BinaryComponent {
        public Subtract( Component left,
                         Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " - " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Subtract) {
                Subtract that = (Subtract)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class And extends BinaryComponent {
        public And( Component left,
                    Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " and " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof And) {
                And that = (And)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Union extends BinaryComponent {
        public Union( Component left,
                      Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " union " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Union) {
                Union that = (Union)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class Or extends BinaryComponent {
        public Or( Component left,
                   Component right ) {
            super(left, right);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getLeft() + " or " + getRight();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Or) {
                Or that = (Or)obj;
                return this.getLeft().equals(that.getLeft()) && this.getRight().equals(that.getRight());
            }
            return false;
        }
    }

    public static class ContextItem implements Component {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof ContextItem;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return ".";
        }
    }

    public static class Literal implements Component {
        private final String value;

        public Literal( String value ) {
            this.value = value;
        }

        /**
         * @return value
         */
        public String getValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Literal) {
                return this.value.equals(((Literal)obj).value);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return value;
        }
    }

    public static class FunctionCall implements Component {
        private final NameTest name;
        private final List<Component> arguments;

        public FunctionCall( NameTest name,
                             List<Component> arguments ) {
            assert name != null;
            assert arguments != null;
            this.name = name;
            this.arguments = arguments;
        }

        /**
         * @return name
         */
        public NameTest getName() {
            return name;
        }

        /**
         * @return arguments
         */
        public List<Component> getParameters() {
            return arguments;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof FunctionCall) {
                FunctionCall that = (FunctionCall)obj;
                return this.name.equals(that.name) && this.arguments.equals(that.arguments);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return name + "(" + asString(arguments, ",") + ")";
        }
    }

    protected static String asString( Iterable<?> components,
                                      String delimiter ) {
        StringBuilder sb = new StringBuilder();
        for (Object component : components) {
            if (sb.length() != 0) sb.append(delimiter);
            sb.append(component);
        }
        return sb.toString();
    }

    public static class PathExpression implements Component {
        private final List<StepExpression> steps;
        private final boolean relative;

        public PathExpression( boolean relative,
                               List<StepExpression> steps ) {
            this.steps = steps;
            this.relative = relative;
        }

        /**
         * @return relative
         */
        public boolean isRelative() {
            return relative;
        }

        /**
         * @return steps
         */
        public List<StepExpression> getSteps() {
            return steps;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof PathExpression) {
                PathExpression that = (PathExpression)obj;
                return this.relative == that.relative && this.steps.equals(that.steps);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return (relative ? "" : "/") + asString(steps, "/");
        }
    }

    public static interface StepExpression extends Component {
    }

    public static class FilterStep implements StepExpression {
        private final Component primaryExpression;
        private final List<Component> predicates;

        public FilterStep( Component primaryExpression,
                           List<Component> predicates ) {
            assert primaryExpression != null;
            assert predicates != null;
            this.primaryExpression = primaryExpression;
            this.predicates = predicates;
        }

        /**
         * @return nodeTest
         */
        public Component getPrimaryExpression() {
            return primaryExpression;
        }

        /**
         * @return predicates
         */
        public List<Component> getPredicates() {
            return predicates;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof FilterStep) {
                FilterStep that = (FilterStep)obj;
                return this.primaryExpression.equals(that.primaryExpression) && this.predicates.equals(that.predicates);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return primaryExpression + (predicates.isEmpty() ? "" : predicates.toString());
        }
    }

    public static class DescendantOrSelf implements StepExpression {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof DescendantOrSelf;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "descendant-or-self::node()";
        }
    }

    public static class AxisStep implements StepExpression {
        private final NodeTest nodeTest;
        private final List<Component> predicates;

        public AxisStep( NodeTest nodeTest,
                         List<Component> predicates ) {
            assert nodeTest != null;
            assert predicates != null;
            this.nodeTest = nodeTest;
            this.predicates = predicates;
        }

        /**
         * @return nodeTest
         */
        public NodeTest getNodeTest() {
            return nodeTest;
        }

        /**
         * @return predicates
         */
        public List<Component> getPredicates() {
            return predicates;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof AxisStep) {
                AxisStep that = (AxisStep)obj;
                return this.nodeTest.equals(that.nodeTest) && this.predicates.equals(that.predicates);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return nodeTest + (predicates.isEmpty() ? "" : predicates.toString());
        }
    }

    public static class ParenthesizedExpression implements Component {
        private final Component wrapped;

        public ParenthesizedExpression() {
            this.wrapped = null;
        }

        public ParenthesizedExpression( Component wrapped ) {
            this.wrapped = wrapped; // may be null
        }

        /**
         * @return wrapped
         */
        public Component getWrapped() {
            return wrapped;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ParenthesizedExpression) {
                ParenthesizedExpression that = (ParenthesizedExpression)obj;
                return this.wrapped.equals(that.wrapped);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "(" + wrapped + ")";
        }
    }

    public static interface NodeTest extends Component {
    }

    public static interface KindTest extends NodeTest {
    }

    public static class NameTest implements NodeTest {
        private final String prefixTest;
        private final String localTest;

        public NameTest( String prefixTest,
                         String localTest ) {
            this.prefixTest = prefixTest;
            this.localTest = localTest;
        }

        /**
         * @return prefixTest
         */
        public String getPrefixTest() {
            return prefixTest;
        }

        /**
         * @return localTest
         */
        public String getLocalTest() {
            return localTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof NameTest) {
                NameTest that = (NameTest)obj;
                return ObjectUtil.isEqualWithNulls(this.prefixTest, that.prefixTest)
                       && ObjectUtil.isEqualWithNulls(this.localTest, that.localTest);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            String local = localTest != null ? localTest : "*";
            return prefixTest == null ? local : (prefixTest + ":" + local);
        }
    }

    public static class AttributeNameTest implements NodeTest {
        private final NodeTest nodeTest;

        public AttributeNameTest( NodeTest nodeTest ) {
            this.nodeTest = nodeTest;
        }

        /**
         * @return nodeTest
         */
        public NodeTest getNodeTest() {
            return nodeTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see XPath.NameTest#toString()
         */
        @Override
        public String toString() {
            return "@" + nodeTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof AttributeNameTest) {
                AttributeNameTest that = (AttributeNameTest)obj;
                return this.nodeTest.equals(that.nodeTest);
            }
            return false;
        }

    }

    public static class AnyKindTest implements KindTest {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof AnyKindTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "node()";
        }
    }

    public static class TextTest implements KindTest {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof TextTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "text()";
        }
    }

    public static class CommentTest implements KindTest {
        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            return obj == this || obj instanceof CommentTest;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "comment()";
        }
    }

    public static class ProcessingInstructionTest implements KindTest {
        private final String nameOrStringLiteral;

        public ProcessingInstructionTest( String nameOrStringLiteral ) {
            this.nameOrStringLiteral = nameOrStringLiteral;
        }

        /**
         * @return nameOrStringLiteral
         */
        public String getNameOrStringLiteral() {
            return nameOrStringLiteral;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ProcessingInstructionTest) {
                ProcessingInstructionTest that = (ProcessingInstructionTest)obj;
                return this.nameOrStringLiteral.equals(that.nameOrStringLiteral);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "processing-instruction(" + nameOrStringLiteral + ")";
        }
    }

    public static class DocumentTest implements KindTest {
        private KindTest elementOrSchemaElementTest;

        public DocumentTest( ElementTest elementTest ) {
            CheckArg.isNotNull(elementTest, "elementTest");
            this.elementOrSchemaElementTest = elementTest;
        }

        public DocumentTest( SchemaElementTest schemaElementTest ) {
            CheckArg.isNotNull(schemaElementTest, "schemaElementTest");
            this.elementOrSchemaElementTest = schemaElementTest;
        }

        /**
         * @return elementOrSchemaElementTest
         */
        public ElementTest getElementTest() {
            return elementOrSchemaElementTest instanceof ElementTest ? (ElementTest)elementOrSchemaElementTest : null;
        }

        /**
         * @return elementOrSchemaElementTest
         */
        public SchemaElementTest getSchemaElementTest() {
            return elementOrSchemaElementTest instanceof SchemaElementTest ? (SchemaElementTest)elementOrSchemaElementTest : null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof DocumentTest) {
                DocumentTest that = (DocumentTest)obj;
                return this.elementOrSchemaElementTest.equals(that.elementOrSchemaElementTest);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "document-node(" + elementOrSchemaElementTest + ")";
        }
    }

    public static class AttributeTest implements KindTest {
        private final NameTest attributeNameOrWildcard;
        private final NameTest typeName;

        public AttributeTest( NameTest attributeNameOrWildcard,
                              NameTest typeName ) {
            this.attributeNameOrWildcard = attributeNameOrWildcard;
            this.typeName = typeName;
        }

        /**
         *@return the attribute name, which may be a wilcard
         */
        public NameTest getAttributeName() {
            return attributeNameOrWildcard;
        }

        /**
         * @return typeName
         */
        public NameTest getTypeName() {
            return typeName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof AttributeTest) {
                AttributeTest that = (AttributeTest)obj;
                return ObjectUtil.isEqualWithNulls(this.typeName, that.typeName)
                       && ObjectUtil.isEqualWithNulls(this.attributeNameOrWildcard, that.attributeNameOrWildcard);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "attribute(" + attributeNameOrWildcard + (typeName != null ? "," + typeName : "") + ")";
        }
    }

    public static class ElementTest implements KindTest {
        private final NameTest elementNameOrWildcard;
        private final NameTest typeName;

        public ElementTest( NameTest elementNameOrWildcard,
                            NameTest typeName ) {
            this.elementNameOrWildcard = elementNameOrWildcard;
            this.typeName = typeName;
        }

        /**
         *@return the element name, which may be a wilcard
         */
        public NameTest getElementName() {
            return elementNameOrWildcard;
        }

        /**
         * @return typeName
         */
        public NameTest getTypeName() {
            return typeName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ElementTest) {
                ElementTest that = (ElementTest)obj;
                return ObjectUtil.isEqualWithNulls(this.typeName, that.typeName)
                       && ObjectUtil.isEqualWithNulls(this.elementNameOrWildcard, that.elementNameOrWildcard);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "element(" + elementNameOrWildcard + (typeName != null ? "," + typeName : "") + ")";
        }
    }

    public static class SchemaElementTest implements KindTest {
        private final NameTest elementDeclarationName;

        public SchemaElementTest( NameTest elementDeclarationName ) {
            this.elementDeclarationName = elementDeclarationName;
        }

        /**
         *@return the element declaration name, which will be a qualified name
         */
        public NameTest getElementDeclarationName() {
            return elementDeclarationName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SchemaElementTest) {
                SchemaElementTest that = (SchemaElementTest)obj;
                return this.elementDeclarationName.equals(that.elementDeclarationName);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "schema-element(" + elementDeclarationName + ")";
        }
    }

    public static class SchemaAttributeTest implements KindTest {
        private final NameTest attributeDeclarationName;

        public SchemaAttributeTest( NameTest attributeDeclarationName ) {
            this.attributeDeclarationName = attributeDeclarationName;
        }

        /**
         *@return the attribute declaration name, which will be a qualified name
         */
        public NameTest getAttributeDeclarationName() {
            return attributeDeclarationName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SchemaAttributeTest) {
                SchemaAttributeTest that = (SchemaAttributeTest)obj;
                return this.attributeDeclarationName.equals(that.attributeDeclarationName);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "schema-attribute(" + attributeDeclarationName + ")";
        }
    }
}
