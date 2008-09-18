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
package org.jboss.dna.graph.commands;

import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathNotFoundException;

/**
 * Aspect interface for any repository command that acts upon a specific path. This aspect adds a method that can be used by the
 * recipient to obtain the path that the command applies to.
 * 
 * @author Randall Hauch
 */
public interface ActsOnPath {

    /**
     * Get the path to which this command applies. If the path does not exist, an {@link PathNotFoundException} exception should
     * be recorded as an {@link GraphCommand#setError(Throwable) error}.
     * 
     * @return the path; never null
     */
    Path getPath();
}
