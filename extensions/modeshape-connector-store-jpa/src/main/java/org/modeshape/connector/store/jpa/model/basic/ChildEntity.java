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
package org.modeshape.connector.store.jpa.model.basic;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;
import org.hibernate.annotations.Index;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.HashCode;
import org.modeshape.connector.store.jpa.model.common.NamespaceEntity;

/**
 * An entity representing the parent-child relationship between two nodes. In addition to the references to the parent and child
 * nodes, this entity also maintains the indexInParent of the indexInParent within the parent node's list of all children, the
 * child's name ( {@link #getChildName() local part} and {@link #getChildNamespace() namespace}), and the same-name-sibling
 * indexInParent (if there is one).
 */
@Entity
@Table( name = "DNA_BASIC_CHILDREN" )
@org.hibernate.annotations.Table( appliesTo = "DNA_BASIC_CHILDREN", indexes = {
    @Index( name = "CHILDINDEX_INX", columnNames = {"WORKSPACE_ID", "PARENT_UUID", "CHILD_INDEX"} ),
    @Index( name = "CHILDNAME_INX", columnNames = {"WORKSPACE_ID", "PARENT_UUID", "CHILD_NAME_NS_ID", "CHILD_NAME_LOCAL",
        "SNS_INDEX"} )} )
@NamedQueries( {
    @NamedQuery( name = "ChildEntity.findByPathSegment", query = "select child from ChildEntity as child where child.id.workspaceId = :workspaceId and child.parentUuidString = :parentUuidString AND child.childNamespace.id = :ns AND child.childName = :childName AND child.sameNameSiblingIndex = :sns" ),
    @NamedQuery( name = "ChildEntity.findAllUnderParent", query = "select child from ChildEntity as child where child.id.workspaceId = :workspaceId and child.parentUuidString = :parentUuidString order by child.indexInParent" ),
    @NamedQuery( name = "ChildEntity.findRangeUnderParent", query = "select child from ChildEntity as child where child.id.workspaceId = :workspaceId and child.parentUuidString = :parentUuidString and child.indexInParent >= :firstIndex and child.indexInParent < :afterIndex order by child.indexInParent" ),
    @NamedQuery( name = "ChildEntity.findChildrenAfterIndexUnderParent", query = "select child from ChildEntity as child where child.id.workspaceId = :workspaceId and child.parentUuidString = :parentUuidString and child.indexInParent >= :afterIndex order by child.indexInParent" ),
    @NamedQuery( name = "ChildEntity.findByChildUuid", query = "select child from ChildEntity as child where child.id.workspaceId = :workspaceId and child.id.childUuidString = :childUuidString" ),
    @NamedQuery( name = "ChildEntity.findMaximumSnsIndex", query = "select max(child.sameNameSiblingIndex) from ChildEntity as child where child.id.workspaceId = :workspaceId and child.parentUuidString = :parentUuid AND child.childNamespace.id = :ns AND child.childName = :childName" ),
    @NamedQuery( name = "ChildEntity.findMaximumChildIndex", query = "select max(child.indexInParent) from ChildEntity as child where child.id.workspaceId = :workspaceId and child.parentUuidString = :parentUuid" ),
    @NamedQuery( name = "ChildEntity.findInWorkspace", query = "select child from ChildEntity as child where child.id.workspaceId = :workspaceId" )} )
public class ChildEntity {

    @Id
    private ChildId id;

    @Column( name = "PARENT_UUID", nullable = false, length = 36 )
    private String parentUuidString;

    /** The zero-based index */
    @Column( name = "CHILD_INDEX", nullable = false, unique = false )
    private int indexInParent;

    @ManyToOne
    @JoinColumn( name = "CHILD_NAME_NS_ID", nullable = false )
    private NamespaceEntity childNamespace;

    @Column( name = "CHILD_NAME_LOCAL", nullable = false, unique = false, length = 512 )
    private String childName;

    @Column( name = "SNS_INDEX", nullable = false, unique = false )
    private int sameNameSiblingIndex;

    /**
     * Tracks whether this node allows or disallows its children to have the same names (to be same-name-siblings). The model uses
     * this to know whether it can optimization the database operations when creating, inserting, or removing children.
     */
    @Column( name = "ALLOWS_SNS", nullable = false, unique = false )
    private boolean allowsSameNameChildren;

    public ChildEntity() {
    }

    public ChildEntity( ChildId id,
                        String parentUuidString,
                        int indexInParent,
                        NamespaceEntity ns,
                        String name ) {
        this.id = id;
        this.parentUuidString = parentUuidString;
        this.indexInParent = indexInParent;
        this.childNamespace = ns;
        this.childName = name;
        this.sameNameSiblingIndex = 1;
    }

    public ChildEntity( ChildId id,
                        String parentUuidString,
                        int indexInParent,
                        NamespaceEntity ns,
                        String name,
                        int sameNameSiblingIndex ) {
        this.id = id;
        this.parentUuidString = parentUuidString;
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
     * Returns the parent UUID string
     * 
     * @return the parent UUID string
     */
    public String getParentUuidString() {
        return parentUuidString;
    }

    /**
     * Sets the parent UUID string
     * 
     * @param parentUuidString the parent UUID string
     */
    public void setParentUuidString( String parentUuidString ) {
        this.parentUuidString = parentUuidString;
    }

    /**
     * Get the zero-based index of this child within the parent's list of children
     * 
     * @return the zero-based index of this child
     */
    public int getIndexInParent() {
        return indexInParent;
    }

    /**
     * @param index Sets indexInParent to the specified value.
     */
    public void setIndexInParent( int index ) {
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
    public int getSameNameSiblingIndex() {
        return sameNameSiblingIndex;
    }

    /**
     * @param sameNameSiblingIndex Sets sameNameSiblingIndex to the specified value.
     */
    public void setSameNameSiblingIndex( int sameNameSiblingIndex ) {
        this.sameNameSiblingIndex = sameNameSiblingIndex;
    }

    /**
     * @return allowsSameNameChildren
     */
    public boolean getAllowsSameNameChildren() {
        return allowsSameNameChildren;
    }

    /**
     * @param allowsSameNameChildren Sets allowsSameNameChildren to the specified value.
     */
    public void setAllowsSameNameChildren( boolean allowsSameNameChildren ) {
        this.allowsSameNameChildren = allowsSameNameChildren;
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
        if (sameNameSiblingIndex > 1) {
            sb.append('[').append(sameNameSiblingIndex).append(']');
        }
        if (id != null) {
            sb.append(" (id=").append(id.getChildUuidString()).append(")");
            String parentId = getParentUuidString();
            if (parentId != null) {
                sb.append(" is ");
                sb.append(Inflector.getInstance().ordinalize(indexInParent));
                sb.append(" child of ");
                sb.append(parentId);
                sb.append(" in workspace ");
                sb.append(id.getWorkspaceId());
            } else {
                sb.append(" is root in workspace ");
                sb.append(id.getWorkspaceId());
            }
        }
        return sb.toString();
    }

    @SuppressWarnings( "unchecked" )
    public static void adjustSnsIndexesAndIndexesAfterRemoving( EntityManager entities,
                                                                Long workspaceId,
                                                                String uuidParent,
                                                                String childName,
                                                                long childNamespaceIndex,
                                                                int childIndex ) {
        // Decrement the 'indexInParent' index values for all nodes above the previously removed sibling ...
        Query query = entities.createNamedQuery("ChildEntity.findChildrenAfterIndexUnderParent");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("parentUuidString", uuidParent);
        query.setParameter("afterIndex", childIndex);
        for (ChildEntity entity : (List<ChildEntity>)query.getResultList()) {
            // Decrement the index in parent ...
            entity.setIndexInParent(entity.getIndexInParent() - 1);
            if (entity.getChildName().equals(childName) && entity.getChildNamespace().getId() == childNamespaceIndex) {
                // The name matches, so decrement the SNS index ...
                entity.setSameNameSiblingIndex(entity.getSameNameSiblingIndex() - 1);
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    public static int adjustSnsIndexesAndIndexes( EntityManager entities,
                                                  Long workspaceId,
                                                  String uuidParent,
                                                  int afterIndex,
                                                  int untilIndex,
                                                  long childNamespaceIndex,
                                                  String childName,
                                                  int modifier ) {
        int snsCount = 0;

        // Decrement the 'indexInParent' index values for all nodes above the previously removed sibling ...
        Query query = entities.createNamedQuery("ChildEntity.findChildrenAfterIndexUnderParent");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("parentUuidString", uuidParent);
        query.setParameter("afterIndex", afterIndex);

        int index = afterIndex;
        for (ChildEntity entity : (List<ChildEntity>)query.getResultList()) {
            if (++index > untilIndex) {
                break;
            }

            // Decrement the index in parent ...
            entity.setIndexInParent(entity.getIndexInParent() + modifier);
            if (entity.getChildName().equals(childName) && entity.getChildNamespace().getId() == childNamespaceIndex) {
                // The name matches, so decrement the SNS index ...
                entity.setSameNameSiblingIndex(entity.getSameNameSiblingIndex() + modifier);
                snsCount++;
            }
        }

        return snsCount;
    }
}
