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
package org.jboss.dna.graph.properties;

import org.jboss.dna.graph.Location;

/**
 * @author Randall Hauch
 */
public class PathNotFoundException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = -3703984046286975978L;

    private final Location location;
    private final Path lowestAncestorThatDoesExist;

    /**
     * @param location the location of the node that does not exist
     * @param lowestAncestorThatDoesExist the path of the lowest (closest) ancestor that does exist
     */
    public PathNotFoundException( Location location,
                                  Path lowestAncestorThatDoesExist ) {
        this.location = location;
        this.lowestAncestorThatDoesExist = lowestAncestorThatDoesExist;
    }

    /**
     * @param location the location of the node that does not exist
     * @param lowestAncestorThatDoesExist the path of the lowest (closest) ancestor that does exist
     * @param message
     */
    public PathNotFoundException( Location location,
                                  Path lowestAncestorThatDoesExist,
                                  String message ) {
        super(message);
        this.location = location;
        this.lowestAncestorThatDoesExist = lowestAncestorThatDoesExist;
    }

    /**
     * @param location the location of the node that does not exist
     * @param lowestAncestorThatDoesExist the path of the lowest (closest) ancestor that does exist
     * @param cause
     */
    public PathNotFoundException( Location location,
                                  Path lowestAncestorThatDoesExist,
                                  Throwable cause ) {
        super(cause);
        this.location = location;
        this.lowestAncestorThatDoesExist = lowestAncestorThatDoesExist;
    }

    /**
     * @param location the location of the node that does not exist
     * @param lowestAncestorThatDoesExist the path of the lowest (closest) ancestor that does exist
     * @param message
     * @param cause
     */
    public PathNotFoundException( Location location,
                                  Path lowestAncestorThatDoesExist,
                                  String message,
                                  Throwable cause ) {
        super(message, cause);
        this.location = location;
        this.lowestAncestorThatDoesExist = lowestAncestorThatDoesExist;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Get the path that was not found
     * 
     * @return the path that was not found
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Get the lowest (closest) existing {@link Path#getParent() ancestor} of the {@link #getLocation() non-existant location}.
     * 
     * @return the lowest ancestor that does exist
     */
    public Path getLowestAncestorThatDoesExist() {
        return lowestAncestorThatDoesExist;
    }
}
