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
package org.jboss.dna.graph.properties.basic;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.graph.properties.Name;

/**
 * An immutable version of a property that has 2 or more values. This is done for efficiency of the in-memory representation,
 * since many properties will have just a single value, while others will have multiple values.
 * 
 * @author Randall Hauch
 */
@Immutable
public class BasicMultiValueProperty extends BasicProperty {

    private final List<Object> values;

    /**
     * Create a property with 2 or more values. Note that the supplied list may be modifiable, as this object does not expose any
     * means for modifying the contents.
     * 
     * @param name the property name
     * @param values the property values
     * @throws IllegalArgumentException if the values is null or does not have at least 2 values
     */
    public BasicMultiValueProperty( Name name,
                                    List<Object> values ) {
        super(name);
        ArgCheck.isNotNull(values, "values");
        ArgCheck.hasSizeOfAtLeast(values, 2, "values");
        this.values = values;
    }

    /**
     * Create a property with 2 or more values.
     * 
     * @param name the property name
     * @param values the property values
     * @throws IllegalArgumentException if the values is null or does not have at least 2 values
     */
    public BasicMultiValueProperty( Name name,
                                    Object... values ) {
        super(name);
        ArgCheck.isNotNull(values, "values");
        ArgCheck.hasSizeOfAtLeast(values, 2, "values");
        this.values = Arrays.asList(values);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSingle() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return values.size();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Object> iterator() {
        return new ReadOnlyIterator(values.iterator());
    }

    protected class ReadOnlyIterator implements Iterator<Object> {

        private final Iterator<Object> values;

        protected ReadOnlyIterator( Iterator<Object> values ) {
            assert values != null;
            this.values = values;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return values.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        public Object next() {
            return values.next();
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
