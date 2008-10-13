/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.requests;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.properties.Name;

/**
 * Instruction to remove properties from the node at the specified location.
 * 
 * @author Randall Hauch
 */
public class RemovePropertiesRequest extends Request implements Iterable<Name> {

    private final Location from;
    private final Set<Name> propertyNames;

    /**
     * Create a request to remove the properties with the given names from the node at the supplied location.
     * 
     * @param from the location of the node to be read
     * @param propertyNames the names of the properties to be removed from the node
     * @throws IllegalArgumentException if the location is null or if there are no properties to remove
     */
    public RemovePropertiesRequest( Location from,
                                    Name... propertyNames ) {
        CheckArg.isNotNull(from, "from");
        CheckArg.isNotEmpty(propertyNames, "propertyNames");
        this.from = from;
        Set<Name> names = new HashSet<Name>();
        for (Name name : propertyNames) {
            if (name != null) names.add(name);
        }
        this.propertyNames = Collections.unmodifiableSet(names);
    }

    /**
     * Create a request to remove the properties with the given names from the node at the supplied location.
     * 
     * @param from the location of the node to be read
     * @param propertyNames the names of the properties to be removed from the node
     * @throws IllegalArgumentException if the location is null or if there are no properties to remove
     */
    public RemovePropertiesRequest( Location from,
                                    Iterable<Name> propertyNames ) {
        CheckArg.isNotNull(from, "from");
        this.from = from;
        Set<Name> names = new HashSet<Name>();
        for (Name name : propertyNames) {
            if (name != null) names.add(name);
        }
        this.propertyNames = Collections.unmodifiableSet(names);
        CheckArg.isNotEmpty(this.propertyNames, "propertyNames");
    }

    /**
     * Create a request to remove the properties with the given names from the node at the supplied location.
     * 
     * @param from the location of the node to be read
     * @param propertyNames the names of the properties to be removed from the node
     * @throws IllegalArgumentException if the location is null or if there are no properties to remove
     */
    public RemovePropertiesRequest( Location from,
                                    Iterator<Name> propertyNames ) {
        CheckArg.isNotNull(from, "from");
        this.from = from;
        Set<Name> names = new HashSet<Name>();
        while (propertyNames.hasNext()) {
            Name name = propertyNames.next();
            if (name != null) names.add(name);
        }
        this.propertyNames = Collections.unmodifiableSet(names);
        CheckArg.isNotEmpty(this.propertyNames, "propertyNames");
    }

    /**
     * Get the location defining the node from which the properties are to be removed.
     * 
     * @return the location of the node; never null
     */
    public Location from() {
        return from;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Name> iterator() {
        return this.propertyNames.iterator();
    }

    /**
     * Get the names of the properties that are to be removed from the node.
     * 
     * @return the collection of property names; never null and never empty
     */
    public Collection<Name> propertyNames() {
        return propertyNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            RemovePropertiesRequest that = (RemovePropertiesRequest)obj;
            if (!this.from().equals(that.from())) return false;
            if (!this.propertyNames().equals(that.propertyNames())) return false;
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
        return "remove from " + from() + " properties named " + StringUtil.readableString(propertyNames());
    }

}
