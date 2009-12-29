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
package org.jboss.dna.connector.filesystem;

import org.jboss.dna.graph.connector.path.PathRepositoryTransaction;

/**
 * A transaction returned by the {@link FileSystemRepository#startTransaction(boolean)}.
 */
public class FileSystemTransaction implements PathRepositoryTransaction {

    /**
     * Commit any changes that have been made to the repository. This method may throw runtime exceptions if there are failures
     * committing the changes, but the transaction is still expected to be closed.
     * 
     * @see #rollback()
     * @see FileSystemRepository#startTransaction(boolean)
     */
    public void commit() {
    }

    /**
     * Rollback any changes that have been made to this repository. This method may throw runtime exceptions if there are failures
     * rolling back the changes, but the transaction is still expected to be closed.
     * 
     * @see #commit()
     * @see FileSystemRepository#startTransaction(boolean)
     */
    public void rollback() {
    }

}
