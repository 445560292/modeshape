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
package org.jboss.dna.connector.store.jpa.util;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.ejb.Ejb3Configuration;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.connector.store.jpa.JpaSource;
import org.jboss.dna.connector.store.jpa.Model;

/**
 * An option for the store. This is typically used to save store-specific values.
 * <p>
 * This JPA entity is always added to the {@link Ejb3Configuration} in the {@link JpaSource#getConnection() JpaSource}, and
 * therefore should not be {@link Model#configure(Ejb3Configuration) added to the configuration} by a {@link Model}.
 * </p>
 * 
 * @author Randall Hauch
 */
@Entity( name = "DNA_OPTIONS" )
@NamedQueries( {@NamedQuery( name = "StoreOptionEntity.findAll", query = "SELECT option FROM DNA_OPTIONS AS option" )} )
public class StoreOptionEntity {

    @Id
    @Column( name = "NAME", nullable = false, unique = true, length = 512 )
    private String name;

    @Column( name = "VALUE", nullable = false, unique = false, length = 512 )
    private String value;

    /**
     * 
     */
    protected StoreOptionEntity() {
    }

    /**
     * @param name the name of the option; may not be null or empty
     * @param value the value of the option; may be null
     */
    public StoreOptionEntity( String name,
                              String value ) {
        CheckArg.isNotEmpty(name, "name");
        setName(name);
        setValue(value);
    }

    /**
     * @param name the name of the option; may not be null or empty
     */
    public StoreOptionEntity( String name ) {
        CheckArg.isNotEmpty(name, "name");
        setName(name);
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value Sets value to the specified value.
     */
    public void setValue( String value ) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof StoreOptionEntity) {
            StoreOptionEntity that = (StoreOptionEntity)obj;
            if (!this.getName().equals(that.getName())) return false;
            if (!this.getValue().equals(that.getValue())) return false;
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
        return "Option " + getName() + " = \"" + getValue() + "\"";
    }
}
