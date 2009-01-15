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
package org.jboss.dna.graph.property.basic;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.IoException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;

/**
 * The standard {@link ValueFactory} for {@link PropertyType#BOOLEAN} values.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class BooleanValueFactory extends AbstractValueFactory<Boolean> {

    public BooleanValueFactory( TextDecoder decoder,
                                ValueFactory<String> stringValueFactory ) {
        super(PropertyType.BOOLEAN, decoder, stringValueFactory);
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( String value ) {
        if (value == null) return null;
        return Boolean.valueOf(value.trim());
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( String value,
                           TextDecoder decoder ) {
        // this probably doesn't really need to call the decoder, but by doing so then we don't care at all what the decoder does
        return create(getDecoder(decoder).decode(value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( int value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Integer.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( long value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Long.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( boolean value ) {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( float value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Float.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( double value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Double.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( BigDecimal value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                        BigDecimal.class.getSimpleName(),
                                                                        value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( Calendar value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Calendar.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( Date value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Date.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(org.jboss.dna.graph.property.DateTime)
     */
    public Boolean create( DateTime value ) throws ValueFormatException {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  DateTime.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( Name value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Name.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( Path value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  Path.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( Reference value ) {
        throw new ValueFormatException(value, getPropertyType(),
                                       GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                        Reference.class.getSimpleName(),
                                                                        value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( URI value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  URI.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(java.util.UUID)
     */
    public Boolean create( UUID value ) {
        throw new ValueFormatException(value, getPropertyType(), GraphI18n.unableToCreateValue.text(getPropertyType().getName(),
                                                                                                  UUID.class.getSimpleName(),
                                                                                                  value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( byte[] value ) {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.ValueFactory#create(org.jboss.dna.graph.property.Binary)
     */
    public Boolean create( Binary value ) throws ValueFormatException, IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(value));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( InputStream stream,
                           long approximateLength ) throws IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(stream, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    public Boolean create( Reader reader,
                           long approximateLength ) throws IoException {
        // First create a string and then create the boolean from the string value ...
        return create(getStringValueFactory().create(reader, approximateLength));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Boolean[] createEmptyArray( int length ) {
        return new Boolean[length];
    }

}
