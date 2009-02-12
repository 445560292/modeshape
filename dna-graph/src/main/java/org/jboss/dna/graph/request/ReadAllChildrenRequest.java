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
package org.jboss.dna.graph.request;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;

/**
 * Instruction to read all of the children of a node at a specific location.
 * 
 * @author Randall Hauch
 */
public class ReadAllChildrenRequest extends CacheableRequest implements Iterable<Location> {

    private static final long serialVersionUID = 1L;

    private final Location of;
    private final String workspaceName;
    private final List<Location> children = new LinkedList<Location>();
    private Location actualOf;

    /**
     * Create a request to read the children of a node at the supplied location in the designated workspace.
     * 
     * @param of the location of the node whose children are to be read
     * @param workspaceName the name of the workspace
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public ReadAllChildrenRequest( Location of,
                                   String workspaceName ) {
        CheckArg.isNotNull(of, "of");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.of = of;
        this.workspaceName = workspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Get the location defining the node whose children are to be read.
     * 
     * @return the location of the parent node; never null
     */
    public Location of() {
        return of;
    }

    /**
     * Get the name of the workspace in which the parent and children exist.
     * 
     * @return the name of the workspace; never null
     */
    public String inWorkspace() {
        return workspaceName;
    }

    /**
     * Get the children that were read from the {@link RepositoryConnection} after the request was processed. Each child is
     * represented by a location.
     * 
     * @return the children that were read; never null
     */
    public List<Location> getChildren() {
        return children;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Location> iterator() {
        return children.iterator();
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification properties. The children
     * should be added in order.
     * 
     * @param child the location of the child that was read
     * @throws IllegalArgumentException if the location is null
     * @see #addChild(Path, Property)
     * @see #addChild(Path, Property, Property...)
     */
    public void addChild( Location child ) {
        CheckArg.isNotNull(child, "child");
        this.children.add(child);
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification properties. The children
     * should be added in order.
     * 
     * @param pathToChild the path of the child that was just read
     * @param firstIdProperty the first identification property of the child that was just read
     * @param remainingIdProperties the remaining identification properties of the child that was just read
     * @throws IllegalArgumentException if the path or identification properties are null
     * @see #addChild(Location)
     * @see #addChild(Path, Property)
     */
    public void addChild( Path pathToChild,
                          Property firstIdProperty,
                          Property... remainingIdProperties ) {
        Location child = new Location(pathToChild, firstIdProperty, remainingIdProperties);
        this.children.add(child);
    }

    /**
     * Add to the list of children that has been read the child with the given path and identification property. The children
     * should be added in order.
     * 
     * @param pathToChild the path of the child that was just read
     * @param idProperty the identification property of the child that was just read
     * @throws IllegalArgumentException if the path or identification properties are null
     * @see #addChild(Location)
     * @see #addChild(Path, Property, Property...)
     */
    public void addChild( Path pathToChild,
                          Property idProperty ) {
        Location child = new Location(pathToChild, idProperty);
        this.children.add(child);
    }

    /**
     * Sets the actual and complete location of the node whose children have been read. This method must be called when processing
     * the request, and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actualLocation the actual location of the node being read, or null if the {@link #of() current location} should be
     *        used
     * @throws IllegalArgumentException if the actual location does not represent the {@link Location#isSame(Location) same
     *         location} as the {@link #of() current location}; if the actual location does not have a path; or if the actual
     *         workspace name is null
     */
    public void setActualLocationOfNode( Location actualLocation ) {
        if (!this.of.isSame(actualLocation)) { // not same if actualLocation is null
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotSameAsInputLocation.text(actualLocation, of));
        }
        assert actualLocation != null;
        if (!actualLocation.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualLocationMustHavePath.text(actualLocation));
        }
        this.actualOf = actualLocation;
    }

    /**
     * Get the actual location of the node whose children were read.
     * 
     * @return the actual location, or null if the actual location was not set
     */
    public Location getActualLocationOfNode() {
        return actualOf;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            ReadAllChildrenRequest that = (ReadAllChildrenRequest)obj;
            if (!this.of().equals(that.of())) return false;
            if (!this.inWorkspace().equals(that.inWorkspace())) return false;
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
        String workspaceName = this.workspaceName != null ? "\"" + this.workspaceName + "\"" : "default";
        return "read children of " + of() + " in the \"" + workspaceName + "\" workspace";
    }
}
