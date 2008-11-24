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
package org.jboss.dna.connector.store.jpa.models.basic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Table;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.connector.store.jpa.models.common.NamespaceEntity;

/**
 * An entity representing the parent-child relationship between two nodes. In addition to the references to the parent and child
 * nodes, this entity also maintains the indexInParent of the indexInParent within the parent node's list of all children, the
 * child's name ( {@link #getChildName() local part} and {@link #getChildNamespace() namespace}), and the same-name-sibiling
 * indexInParent (if there is one).
 * 
 * @author Randall Hauch
 */
@Entity
@javax.persistence.Table( name = "DNA_BASIC_CHILDREN" )
@Table( appliesTo = "DNA_BASIC_CHILDREN", indexes = @Index( name = "CHILDINDEX_INX", columnNames = {"PARENT_UUID", "CHILD_INDEX"} ) )
@NamedQueries( {
    @NamedQuery( name = "ChildEntity.findByPathSegment", query = "select child from ChildEntity as child where child.id.parentUuidString = :parentUuid AND child.childNamespace.id = :ns AND child.childName = :childName AND child.sameNameSiblingIndex = :sns" ),
    @NamedQuery( name = "ChildEntity.findAllUnderParent", query = "select child from ChildEntity as child where child.id.parentUuidString = :parentUuid" ),
    @NamedQuery( name = "ChildEntity.findByChildUuid", query = "select child from ChildEntity as child where child.id.childUuidString = :childUuidString" ),
    @NamedQuery( name = "ChildEntity.findMaximumSnsIndex", query = "select max(child.sameNameSiblingIndex) from ChildEntity as child where child.id.parentUuidString = :parentUuid AND child.childNamespace.id = :ns AND child.childName = :childName" ),
    @NamedQuery( name = "ChildEntity.findMaximumChildIndex", query = "select max(child.indexInParent) from ChildEntity as child where child.id.parentUuidString = :parentUuid" )} )
public class ChildEntity {

    @Id
    private ChildId id;

    @Column( name = "CHILD_INDEX", nullable = false, unique = false )
    private Integer indexInParent;

    @ManyToOne
    @JoinColumn( name = "CHILD_NAME_NS_ID", nullable = false )
    private NamespaceEntity childNamespace;

    @Column( name = "CHILD_NAME_LOCAL", nullable = true, unique = false, length = 512 )
    private String childName;

    @Column( name = "SNS_INDEX", nullable = true, unique = false )
    private Integer sameNameSiblingIndex;

    public ChildEntity() {
    }

    public ChildEntity( ChildId id,
                        int indexInParent,
                        NamespaceEntity ns,
                        String name ) {
        this.id = id;
        this.indexInParent = indexInParent;
        this.childNamespace = ns;
        this.childName = name;
    }

    public ChildEntity( ChildId id,
                        int indexInParent,
                        NamespaceEntity ns,
                        String name,
                        int sameNameSiblingIndex ) {
        this.id = id;
        this.indexInParent = indexInParent;
        this.childNamespace = ns;
        this.childName = name;
        this.sameNameSiblingIndex = sameNameSiblingIndex;
    }

    /**
     * @return parent
     */
    public ChildId getId() {
        return id;
    }

    /**
     * @param childId Sets parent to the specified value.
     */
    public void setId( ChildId childId ) {
        this.id = childId;
    }

    /**
     * @return indexInParent
     */
    public Integer getIndexInParent() {
        return indexInParent;
    }

    /**
     * @param index Sets indexInParent to the specified value.
     */
    public void setIndexInParent( Integer index ) {
        this.indexInParent = index;
    }

    /**
     * @return childName
     */
    public String getChildName() {
        return childName;
    }

    /**
     * @param childName Sets childName to the specified value.
     */
    public void setChildName( String childName ) {
        this.childName = childName;
    }

    /**
     * @return childNamespace
     */
    public NamespaceEntity getChildNamespace() {
        return childNamespace;
    }

    /**
     * @param childNamespace Sets childNamespace to the specified value.
     */
    public void setChildNamespace( NamespaceEntity childNamespace ) {
        this.childNamespace = childNamespace;
    }

    /**
     * @return sameNameSiblingIndex
     */
    public Integer getSameNameSiblingIndex() {
        return sameNameSiblingIndex;
    }

    /**
     * @param sameNameSiblingIndex Sets sameNameSiblingIndex to the specified value.
     */
    public void setSameNameSiblingIndex( Integer sameNameSiblingIndex ) {
        this.sameNameSiblingIndex = sameNameSiblingIndex;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(id);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ChildEntity) {
            ChildEntity that = (ChildEntity)obj;
            if (this.id == null) {
                if (that.id != null) return false;
            } else {
                if (!this.id.equals(that.id)) return false;
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
        StringBuilder sb = new StringBuilder();
        if (childNamespace != null) {
            sb.append('{').append(childNamespace).append("}:");
        }
        sb.append(childName);
        if (sameNameSiblingIndex != null && sameNameSiblingIndex.intValue() > 1) {
            sb.append('[').append(sameNameSiblingIndex).append(']');
        }
        if (id != null) {
            sb.append(" (id=").append(id.getChildUuidString()).append(")");
            String parentId = id.getParentUuidString();
            if (parentId != null) {
                sb.append(" is child of ").append(parentId);
            } else {
                sb.append(" is root");
            }
        }
        return sb.toString();
    }

}
