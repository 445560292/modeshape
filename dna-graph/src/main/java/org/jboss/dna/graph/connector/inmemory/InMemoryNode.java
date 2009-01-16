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
package org.jboss.dna.graph.connector.inmemory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class InMemoryNode {

    private final UUID uuid;
    private InMemoryNode parent;
    private Path.Segment name;
    private final Map<Name, Property> properties = new HashMap<Name, Property>();
    private final List<InMemoryNode> children = new LinkedList<InMemoryNode>();

    public InMemoryNode( UUID uuid ) {
        assert uuid != null;
        this.uuid = uuid;
    }

    /**
     * @return uuid
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @return name
     */
    public Path.Segment getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    protected void setName( Path.Segment name ) {
        this.name = name;
    }

    /**
     * @return parent
     */
    public InMemoryNode getParent() {
        return parent;
    }

    /**
     * @param parent Sets parent to the specified value.
     */
    protected void setParent( InMemoryNode parent ) {
        this.parent = parent;
    }

    /**
     * @return children
     */
    protected List<InMemoryNode> getChildren() {
        return children;
    }

    /**
     * @return properties
     */
    protected Map<Name, Property> getProperties() {
        return properties;
    }

    public InMemoryNode setProperty( Property property ) {
        if (property != null) {
            this.properties.put(property.getName(), property);
        }
        return this;
    }

    public InMemoryNode setProperty( ExecutionContext context,
                             String name,
                             Object... values ) {
        PropertyFactory propertyFactory = context.getPropertyFactory();
        Name propertyName = context.getValueFactories().getNameFactory().create(name);
        return setProperty(propertyFactory.create(propertyName, values));
    }

    public Property getProperty( ExecutionContext context,
                                 String name ) {
        Name propertyName = context.getValueFactories().getNameFactory().create(name);
        return getProperty(propertyName);
    }

    public Property getProperty( Name name ) {
        return this.properties.get(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof InMemoryNode) {
            InMemoryNode that = (InMemoryNode)obj;
            if (!this.getUuid().equals(that.getUuid())) return false;
            return true;
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
        StringBuilder sb = new StringBuilder();
        if (this.name == null) {
            sb.append("");
        } else {
            sb.append(this.name);
        }
        sb.append(" (").append(uuid).append(")");
        return sb.toString();
    }
}
