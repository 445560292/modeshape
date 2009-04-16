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
package org.jboss.dna.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Name;

/**
 * Implementation of {@link JcrNodeTypeSource} that supports chaining of node type sources.
 */
abstract class AbstractJcrNodeTypeSource implements JcrNodeTypeSource {

    // Convenience constants to help improve readability
    protected static final Value[] NO_DEFAULT_VALUES = new Value[0];
    protected static final String[] NO_CONSTRAINTS = new String[0];
    protected static final List<JcrNodeType> NO_SUPERTYPES = Collections.<JcrNodeType>emptyList();
    protected static final List<JcrNodeDefinition> NO_CHILD_NODES = Collections.<JcrNodeDefinition>emptyList();
    protected static final List<JcrPropertyDefinition> NO_PROPERTIES = Collections.<JcrPropertyDefinition>emptyList();
    protected static final String[] ALL_PROPERTY_TYPES_WITH_UNDEFINED = new String[] {
        PropertyType.nameFromValue(PropertyType.BINARY), PropertyType.nameFromValue(PropertyType.BOOLEAN),
        PropertyType.nameFromValue(PropertyType.DATE), PropertyType.nameFromValue(PropertyType.DOUBLE),
        PropertyType.nameFromValue(PropertyType.LONG), PropertyType.nameFromValue(PropertyType.NAME),
        PropertyType.nameFromValue(PropertyType.PATH), PropertyType.nameFromValue(PropertyType.REFERENCE),
        PropertyType.nameFromValue(PropertyType.STRING), PropertyType.nameFromValue(PropertyType.UNDEFINED),};

    /** Indicates that the node type has no primary item name - added for readability */
    protected static final Name NO_PRIMARY_ITEM_NAME = null;
    /** Indicates that the definition should apply to all property definition or child node definitions - added for readability */
    protected static final Name ALL_NODES = null;

    // Indicates whether or not the node type is a mixin - added for readability
    protected static final boolean IS_A_MIXIN = true;
    protected static final boolean NOT_MIXIN = false;

    // Indicates whether or not the node type has orderable children - added for readability
    protected static final boolean ORDERABLE_CHILD_NODES = true;
    protected static final boolean UNORDERABLE_CHILD_NODES = false;

    /** Link to the repository node type manager to which the nodes defined in this source belong. */
    protected static final RepositoryNodeTypeManager NO_NODE_TYPE_MANAGER = null;

    /** The predecessor node type source. */
    private final JcrNodeTypeSource predecessor;

    AbstractJcrNodeTypeSource() {
        this(null);
    }

    AbstractJcrNodeTypeSource( JcrNodeTypeSource parent ) {
        this.predecessor = parent;
    }

    /**
     * Returns the list of node types declared in this type source.
     * 
     * @return the list of node types declared in this type source.
     * @see org.jboss.dna.jcr.JcrNodeTypeSource#getNodeTypes()
     */
    public abstract Collection<JcrNodeType> getDeclaredNodeTypes();

    /**
     * Returns the list of node types returned by this and any predecessor source.
     * 
     * @return the list of node types returned by this and any predecessor source.
     * @see org.jboss.dna.jcr.JcrNodeTypeSource#getNodeTypes()
     */
    public Collection<JcrNodeType> getNodeTypes() {
        if (predecessor == null) {
            return getDeclaredNodeTypes();
        }

        Collection<JcrNodeType> declaredTypes = getDeclaredNodeTypes();
        Collection<JcrNodeType> predecessorTypes = predecessor.getNodeTypes();

        List<JcrNodeType> primaries = new ArrayList<JcrNodeType>(declaredTypes.size() + predecessorTypes.size());
        primaries.addAll(predecessorTypes);
        primaries.addAll(declaredTypes);

        return primaries;
    }

    /**
     * Finds the type with the given name and returns its definition.
     * <p>
     * This implementation delegates to the <code>predecessor</code> (if it exists) if the type is not found within the declared
     * primary and mixin types.
     * </p>
     * 
     * @param typeName the name of the type to return
     * @return the type named <code>typeName</code> if it exists, otherwise <code>null</code>.
     * @throw IllegalArgumentException if <code>typeName</code> is <code>null</code>.
     * @see org.jboss.dna.jcr.JcrNodeTypeSource#findType(org.jboss.dna.graph.property.Name)
     */
    public JcrNodeType findType( Name typeName ) {
        CheckArg.isNotNull(typeName, "typeName");
        for (JcrNodeType type : getDeclaredNodeTypes()) {
            if (typeName.equals(type.getInternalName())) {
                return type;
            }
        }

        if (predecessor != null) {
            return predecessor.findType(typeName);
        }

        return null;
    }
}
