/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Path;

/**
 * A Request to make changes in a graph.
 */
public abstract class ChangeRequest extends Request {

    private static final long serialVersionUID = 1L;

    protected ChangeRequest() {
    }

    /**
     * Determine if this request changes the branch at the given path.
     * 
     * @param workspace the name of the workspace; may not be null
     * @param path the path; may not be null
     * @return true if this request changes a node under the given path
     */
    public abstract boolean changes( String workspace,
                                     Path path );

    /**
     * Get the location of the top-most node that is to be changed by this request.
     * 
     * @return the location changed by this request
     */
    public abstract Location changedLocation();

    /**
     * Get the name of the workspace that was changed by this request.
     * 
     * @return the name of the workspace changed by this request
     */
    public abstract String changedWorkspace();
}
