/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.UUID;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFactories;

/**
 * ModeShape implementation of a {@link Property JCR Property} with a single value.
 * 
 * @see JcrMultiValueProperty
 */
@NotThreadSafe
final class JcrSingleValueProperty extends AbstractJcrProperty {

    JcrSingleValueProperty( SessionCache cache,
                            AbstractJcrNode node,
                            Name name ) {
        super(cache, node, name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrProperty#isMultiple()
     */
    @Override
    public boolean isMultiple() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getBoolean()
     */
    public boolean getBoolean() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getBooleanFactory().create(property().getFirstValue());
        } catch (org.modeshape.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDate()
     */
    public Calendar getDate() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getDateFactory().create(property().getFirstValue()).toCalendar();
        } catch (org.modeshape.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getDouble()
     */
    public double getDouble() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getDoubleFactory().create(property().getFirstValue());
        } catch (org.modeshape.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getLength()
     */
    public long getLength() throws RepositoryException {
        checkSession();
        return createValue(property().getFirstValue()).getLength();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getLengths()
     */
    public long[] getLengths() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForSingleValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getLong()
     */
    public long getLong() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getLongFactory().create(property().getFirstValue());
        } catch (org.modeshape.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getNode()
     */
    public final Node getNode() throws RepositoryException {
        checkSession();
        try {
            ValueFactories factories = context().getValueFactories();
            Reference dnaReference = factories.getReferenceFactory().create(property().getFirstValue());
            UUID uuid = factories.getUuidFactory().create(dnaReference);
            return cache.findJcrNode(Location.create(uuid));
        } catch (org.modeshape.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getStream()
     */
    public InputStream getStream() throws RepositoryException {
        checkSession();
        try {
            Binary binary = context().getValueFactories().getBinaryFactory().create(property().getFirstValue());
            return new SelfClosingInputStream(binary);
        } catch (org.modeshape.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getString()
     */
    public String getString() throws RepositoryException {
        checkSession();
        try {
            return context().getValueFactories().getStringFactory().create(property().getFirstValue());
        } catch (org.modeshape.graph.property.ValueFormatException e) {
            throw new ValueFormatException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#getValue()
     */
    public Value getValue() throws RepositoryException {
        checkSession();
        return createValue(property().getFirstValue());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(javax.jcr.Value)
     */
    public void setValue( Value value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        checkForLock();
        JcrValue jcrValue = null;

        if (value instanceof JcrValue) {
            jcrValue = (JcrValue)value;

            // Force a conversion as per SetValueValueFormatExceptionTest in JR TCK
            jcrValue.asType(this.getType());

            editor().setProperty(name(), jcrValue);
            return;
        }
        if (value == null) {
            // Then we're to delete the property ...
            editor().removeProperty(name());
            return;
        }

        // We have to convert from one Value implementation to ours ...
        switch (value.getType()) {
            case PropertyType.STRING:
                setValue(value.getString());
                break;
            case PropertyType.BINARY:
                setValue(value.getStream());
                break;
            case PropertyType.BOOLEAN:
                setValue(value.getBoolean());
                break;
            case PropertyType.DATE:
                setValue(value.getDate());
                break;
            case PropertyType.DOUBLE:
                setValue(value.getDouble());
                break;
            case PropertyType.LONG:
                setValue(value.getLong());
                break;
            case PropertyType.NAME:
                setValue(value.getString());
                break;
            case PropertyType.PATH:
                setValue(value.getString());
                break;
            case PropertyType.REFERENCE:
                setValue(value.getString());
                break;
            default:
                throw new RepositoryException(JcrI18n.invalidPropertyType.text(value.getType()));
        }
    }

    protected void setValue( JcrValue jcrValue )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        assert jcrValue != null;

        checkSession();
        checkForLock();

        editor().setProperty(name(), jcrValue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.lang.String)
     */
    public void setValue( String value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        setValue(createValue(value, PropertyType.STRING).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.io.InputStream)
     */
    public void setValue( InputStream value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        setValue(createValue(context().getValueFactories().getBinaryFactory().create(value), PropertyType.BINARY).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(long)
     */
    public void setValue( long value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(createValue(new Long(value), PropertyType.LONG).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(double)
     */
    public void setValue( double value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(createValue(new Double(value), PropertyType.DOUBLE).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.util.Calendar)
     */
    public void setValue( Calendar value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }
        setValue(createValue(context().getValueFactories().getDateFactory().create(value), PropertyType.DATE).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(boolean)
     */
    public void setValue( boolean value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(createValue(new Boolean(value), PropertyType.BOOLEAN).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(javax.jcr.Node)
     */
    public void setValue( Node value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            this.remove();
            return;
        }

        if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(this.context().getNamespaceRegistry()))) {
            throw new ValueFormatException(JcrI18n.nodeNotReferenceable.text());
        }

        String uuid = value.getUUID();
        setValue(createValue(uuid, PropertyType.REFERENCE).asType(this.getType()));
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ValueFormatException always
     * @see javax.jcr.Property#getValues()
     */
    public Value[] getValues() throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForSingleValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(javax.jcr.Value[])
     */
    public void setValue( Value[] values ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForSingleValuedProperty.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Property#setValue(java.lang.String[])
     */
    public void setValue( String[] values ) throws ValueFormatException {
        throw new ValueFormatException(JcrI18n.invalidMethodForSingleValuedProperty.text());
    }

    @Override
    public javax.jcr.Binary getBinary() throws ValueFormatException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Property getProperty() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setValue( BigDecimal value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setValue( javax.jcr.Binary value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // TODO Auto-generated method stub

    }

}
