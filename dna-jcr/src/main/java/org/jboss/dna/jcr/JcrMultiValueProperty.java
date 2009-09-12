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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Property;

/**
 * A {@link javax.jcr.Property JCR Property} implementation that has multiple values.
 * 
 * @see JcrSingleValueProperty
 */
@NotThreadSafe
final class JcrMultiValueProperty extends AbstractJcrProperty {

    static final JcrValue[] EMPTY_VALUES = new JcrValue[] {};

    JcrMultiValueProperty( SessionCache cache,
                           AbstractJcrNode node,
                           Name name ) {
        super(cache, node, name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.AbstractJcrProperty#isMultiple()
     */
    @Override
    boolean isMultiple() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getBoolean()
     */
    public boolean getBoolean() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getDate()
     */
    public Calendar getDate() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getDouble()
     */
    public double getDouble() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getNode()
     */
    public Node getNode() throws ValueFormatException, RepositoryException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getLength()
     */
    public long getLength() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getLengths()
     */
    public long[] getLengths() throws RepositoryException {
        Property dnaProperty = propertyInfo().getProperty();
        long[] lengths = new long[dnaProperty.size()];
        Iterator<?> iter = dnaProperty.iterator();
        for (int ndx = 0; iter.hasNext(); ndx++) {
            lengths[ndx] = createValue(iter.next()).getLength();
        }
        return lengths;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getLong()
     */
    public long getLong() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getStream()
     */
    public InputStream getStream() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getString()
     */
    public String getString() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Per the JCR specification, these values need to be created each time this method is called, since the Value cannot be used
     * after {@link Value#getStream()} is called/processed. The spec says that the client simply needs to obtain a new Value (or
     * {@link #getValues()} for {@link JcrMultiValueProperty multi-valued properites}).
     * </p>
     * 
     * @see javax.jcr.Property#getValues()
     */
    public Value[] getValues() throws RepositoryException {
        Property dnaProperty = propertyInfo().getProperty();
        Value[] values = new JcrValue[dnaProperty.size()];
        Iterator<?> iter = dnaProperty.iterator();
        for (int ndx = 0; iter.hasNext(); ndx++) {
            values[ndx] = createValue(iter.next());
        }
        return values;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(javax.jcr.Value[])
     */
    public final void setValue( Value[] values )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {

        if (values == null) {
            this.remove();
            return;
        }

        for (int i = 0; i < values.length; i++) {
            // Force a conversion as per SetValueValueFormatExceptionTest in JR TCK
            if (values[i] != null) ((JcrValue)values[i]).asType(this.getType());
        }

        editor().setProperty(name(), values, PropertyType.UNDEFINED);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.lang.String[])
     */
    public final void setValue( String[] values )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (values == null) {
            this.remove();
            return;
        }

        Value[] jcrValues = null;
        if (values.length != 0) {
            int numValues = values.length;
            List<Value> valuesList = new ArrayList<Value>(numValues);
            jcrValues = new JcrValue[numValues];
            for (int i = 0; i != numValues; ++i) {
                String value = values[i];
                if (value == null) continue; // skip null values
                valuesList.add(createValue(values[i], PropertyType.STRING).asType(this.getType()));
            }
            if (valuesList.isEmpty()) {
                jcrValues = EMPTY_VALUES;
            } else {
                jcrValues = valuesList.toArray(new Value[valuesList.size()]);
            }
        } else {
            jcrValues = EMPTY_VALUES;
        }

        editor().setProperty(name(), jcrValues, this.getType());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getValue()
     */
    public Value getValue() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#setValue(javax.jcr.Value)
     */
    public final void setValue( Value value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#setValue(java.lang.String)
     */
    public final void setValue( String value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#setValue(java.io.InputStream)
     */
    public final void setValue( InputStream value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#setValue(long)
     */
    public final void setValue( long value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#setValue(double)
     */
    public final void setValue( double value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#setValue(java.util.Calendar)
     */
    public final void setValue( Calendar value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#setValue(boolean)
     */
    public final void setValue( boolean value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#setValue(javax.jcr.Node)
     */
    public final void setValue( Node value ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForMultiValuedProperty.text());
    }

}
