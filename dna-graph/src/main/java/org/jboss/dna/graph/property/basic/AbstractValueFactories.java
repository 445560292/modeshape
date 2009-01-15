/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.property.basic;

import java.util.Iterator;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;

/**
 * Abstract implementation of {@link ValueFactories} that implements all the methods other than the <code>get*Factory()</code>
 * methods. Subclasses can simply implement these methods and inherit the {@link #iterator()}, {@link #getValueFactory(Object)}
 * and {@link #getValueFactory(PropertyType)} method implementations.
 * 
 * @author Randall Hauch
 */
public abstract class AbstractValueFactories implements ValueFactories {

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * <p>
     * This implementation always iterates over the instances return by the <code>get*Factory()</code> methods.
     * </p>
     */
    public Iterator<ValueFactory<?>> iterator() {
        return new ValueFactoryIterator();
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<?> getValueFactory( PropertyType type ) {
        CheckArg.isNotNull(type, "type");
        switch (type) {
            case BINARY:
                return getBinaryFactory();
            case BOOLEAN:
                return getBooleanFactory();
            case DATE:
                return getDateFactory();
            case DECIMAL:
                return getDecimalFactory();
            case DOUBLE:
                return getDoubleFactory();
            case LONG:
                return getLongFactory();
            case NAME:
                return getNameFactory();
            case PATH:
                return getPathFactory();
            case REFERENCE:
                return getReferenceFactory();
            case STRING:
                return getStringFactory();
            case URI:
                return getUriFactory();
            case UUID:
                return getUuidFactory();
            case OBJECT:
                return getObjectFactory();
        }
        return getObjectFactory();
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory<?> getValueFactory( Object prototype ) {
        CheckArg.isNotNull(prototype, "prototype");
        PropertyType inferredType = PropertyType.discoverType(prototype);
        assert inferredType != null;
        return getValueFactory(inferredType);
    }

    protected class ValueFactoryIterator implements Iterator<ValueFactory<?>> {
        private final Iterator<PropertyType> propertyTypeIter = PropertyType.iterator();

        protected ValueFactoryIterator() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return propertyTypeIter.hasNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        public ValueFactory<?> next() {
            PropertyType nextType = propertyTypeIter.next();
            return getValueFactory(nextType);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
