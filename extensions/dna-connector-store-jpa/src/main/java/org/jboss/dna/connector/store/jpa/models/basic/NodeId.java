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
package org.jboss.dna.connector.store.jpa.models.basic;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import net.jcip.annotations.Immutable;

/**
 * An identifier for a node, comprised of a single {@link UUID}, and {@link Embeddable embeddable} in a persistent entity. The
 * identifier takes the form of two <code>long</code> columns: one for the UUID's {@link UUID#getMostSignificantBits() most
 * significant bits} and one for its {@link UUID#getLeastSignificantBits() least significant bits}.
 * 
 * @author Randall Hauch
 */
@Embeddable
@Immutable
@org.hibernate.annotations.Immutable
public class NodeId implements Serializable {

    /**
     * Version {@value}
     */
    private static final long serialVersionUID = 1L;

    @Column( name = "UUID", nullable = true )
    private String uuidString;

    public NodeId() {
    }

    public NodeId( String uuidString ) {
        this.uuidString = uuidString;
    }

    /**
     * @return uuidString
     */
    public String getUuidString() {
        return uuidString;
    }

    /**
     * @param uuidString Sets uuidString to the specified value.
     */
    public void setUuidString( String uuidString ) {
        this.uuidString = uuidString;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return uuidString.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof NodeId) {
            NodeId that = (NodeId)obj;
            if (this.uuidString == null) {
                if (that.uuidString != null) return false;
            } else {
                if (!this.uuidString.equals(that.uuidString)) return false;
            }
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
        return uuidString;
    }
}
