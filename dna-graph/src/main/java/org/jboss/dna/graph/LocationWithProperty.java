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
package org.jboss.dna.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.BasicSingleValueProperty;

/**
 * General purpose location type that supports a path and a property. This class should never be directly instantiated by users of
 * the DNA framework. Instead, use @{link Location#create} to create the correct location.
 * 
 * @see Location
 */
@Immutable
final class LocationWithProperty extends LocationWithProperties {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new location with a given path and identification property.
     * 
     * @param idProperty the identification property
     */
    LocationWithProperty( Property idProperty ) {
        super(Collections.singletonList(idProperty));
        assert idProperty != null;
        assert !idProperty.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#getIdProperty(Name)
     */
    @Override
    public final Property getIdProperty( Name name ) {
        CheckArg.isNotNull(name, "name");
        Property property = getIdProperties().get(0); // this is fast
        return property.getName().equals(name) ? property : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#getUuid()
     */
    @Override
    public UUID getUuid() {
        Property property = getIdProperties().get(0); // this is fast
        if (DnaLexicon.UUID.equals(property.getName())) {
            Object value = property.getFirstValue();
            if (value instanceof UUID) return (UUID)value;
            if (value instanceof String) return UUID.fromString((String)value);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Property)
     */
    @Override
    public Location with( Property newIdProperty ) {
        if (newIdProperty == null || newIdProperty.isEmpty()) return this;
        Property idProperty = getIdProperties().get(0); // fast
        if (newIdProperty.getName().equals(idProperty.getName())) {
            return Location.create(newIdProperty);
        }
        List<Property> newIdProperties = new ArrayList<Property>(getIdProperties().size() + 1);
        newIdProperties.add(newIdProperty);
        newIdProperties.addAll(getIdProperties());
        return Location.create(newIdProperties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(Path)
     */
    @Override
    public Location with( Path newPath ) {
        if (newPath == null) return this;
        Property idProperty = getIdProperties().get(0); // fast
        return Location.create(newPath, idProperty);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Location#with(UUID)
     */
    @Override
    public Location with( UUID uuid ) {
        if (uuid == null) return this;
        Property idProperty = getIdProperties().get(0); // fast
        if (DnaLexicon.UUID.equals(idProperty.getName())) {
            return Location.create(uuid);
        }
        List<Property> newIdProperties = new ArrayList<Property>(getIdProperties().size() + 1);
        newIdProperties.add(new BasicSingleValueProperty(DnaLexicon.UUID, uuid));
        newIdProperties.addAll(getIdProperties());
        return Location.create(newIdProperties);
    }
}
