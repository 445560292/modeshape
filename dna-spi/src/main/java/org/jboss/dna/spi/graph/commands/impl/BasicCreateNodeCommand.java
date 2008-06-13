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
package org.jboss.dna.spi.graph.commands.impl;

import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.CreateNodeCommand;
import org.jboss.dna.spi.graph.commands.NodeConflictBehavior;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicCreateNodeCommand extends BasicGraphCommand implements CreateNodeCommand {

    /**
     */
    private static final long serialVersionUID = -5452285887178397354L;
    private final Path path;
    private final List<Property> properties;
    private final NodeConflictBehavior conflictBehavior;

    /**
     * @param path the path to the node; may not be null
     * @param properties the properties of the node; may not be null
     * @param conflictBehavior the desired behavior when a node exists at the <code>path</code>; may not be null
     */
    public BasicCreateNodeCommand( Path path,
                                   List<Property> properties,
                                   NodeConflictBehavior conflictBehavior ) {
        super();
        assert path != null;
        assert properties != null;
        assert conflictBehavior != null;
        this.properties = properties;
        this.path = path;
        this.conflictBehavior = conflictBehavior;
    }

    /**
     * {@inheritDoc}
     */
    public Path getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<Property> getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     */
    public NodeConflictBehavior getConflictBehavior() {
        return this.conflictBehavior;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( CreateNodeCommand that ) {
        if (this == that) return 0;
        return this.path.compareTo(that.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof CreateNodeCommand) {
            CreateNodeCommand that = (CreateNodeCommand)obj;
            return this.path.equals(that.getPath());
        }
        return false;
    }
}
