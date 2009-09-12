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

import java.util.Collections;
import java.util.Map;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;

/**
 * Instruction to update the properties on the node at the specified location.
 * <p>
 * This request is capable of specifying that certain properties are to have new values and that other properties are to be
 * removed. The request has a single map of properties keyed by their name. If a property is to be set with new values, the map
 * will contain an entry with the property keyed by its name. However, if a property is to be removed, the entry will contain the
 * property name for the key but will have a null entry value.
 * </p>
 * <p>
 * The use of the map also ensures that a single property appears only once in the request (it either has new values or it is to
 * be removed).
 * </p>
 * <p>
 * Note that the number of values in a property (e.g., {@link Property#size()}, {@link Property#isEmpty()},
 * {@link Property#isSingle()}, and {@link Property#isMultiple()}) has no influence on whether the property should be removed. It
 * is possible for a property to have no values.
 * </p>
 */
public class UpdatePropertiesRequest extends ChangeRequest {

    private static final long serialVersionUID = 1L;

    private final Location on;
    private final String workspaceName;
    private final Map<Name, Property> properties;
    private Location actualLocation;

    /**
     * Create a request to update the properties on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param properties the map of properties (keyed by their name), which is reused without copying
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to update
     */
    public UpdatePropertiesRequest( Location on,
                                    String workspaceName,
                                    Map<Name, Property> properties ) {
        CheckArg.isNotNull(on, "on");
        CheckArg.isNotEmpty(properties, "properties");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.on = on;
        this.properties = Collections.unmodifiableMap(properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Get the location defining the node that is to be updated.
     * 
     * @return the location of the node; never null
     */
    public Location on() {
        return on;
    }

    /**
     * Get the name of the workspace in which the node exists.
     * 
     * @return the name of the workspace; never null
     */
    public String inWorkspace() {
        return workspaceName;
    }

    /**
     * Get the map of properties for the node, keyed by property name. Any property to be removed will have a map entry with a
     * null value.
     * 
     * @return the properties being updated; never null and never empty
     */
    public Map<Name, Property> properties() {
        return properties;
    }

    /**
     * Sets the actual and complete location of the node being updated. This method must be called when processing the request,
     * and the actual location must have a {@link Location#getPath() path}.
     * 
     * @param actual the actual location of the node being updated, or null if the {@link #on() current location} should be used
     * @throws IllegalArgumentException if the actual location does represent the {@link Location#isSame(Location) same location}
     *         as the {@link #on() current location}, or if the actual location does not have a path.
     * @throws IllegalStateException if the request is frozen
     */
    public void setActualLocationOfNode( Location actual ) {
        checkNotFrozen();
        if (!on.isSame(actual)) { // not same if actual is null
            throw new IllegalArgumentException(GraphI18n.actualLocationIsNotSameAsInputLocation.text(actual, on));
        }
        assert actual != null;
        if (!actual.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualLocationMustHavePath.text(actual));
        }
        this.actualLocation = actual;
    }

    /**
     * Get the actual location of the node that was updated.
     * 
     * @return the actual location, or null if the actual location was not set
     */
    public Location getActualLocationOfNode() {
        return actualLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.ChangeRequest#changes(java.lang.String, org.jboss.dna.graph.property.Path)
     */
    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return this.workspaceName.equals(workspace) && on.hasPath() && on.getPath().isAtOrBelow(path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.actualLocation = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(on, workspaceName);
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
            UpdatePropertiesRequest that = (UpdatePropertiesRequest)obj;
            if (!this.on().equals(that.on())) return false;
            if (!this.properties().equals(that.properties())) return false;
            if (!this.inWorkspace().equals(that.inWorkspace())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.ChangeRequest#changedLocation()
     */
    @Override
    public Location changedLocation() {
        return on;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.ChangeRequest#changedWorkspace()
     */
    @Override
    public String changedWorkspace() {
        return workspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "update properties on " + on() + " in the \"" + workspaceName + "\" workspace to " + properties();
    }

}
