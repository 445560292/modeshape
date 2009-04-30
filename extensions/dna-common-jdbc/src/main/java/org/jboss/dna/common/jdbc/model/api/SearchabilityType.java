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
package org.jboss.dna.common.jdbc.model.api;

import java.sql.DatabaseMetaData;

/**
 * Provides RDBMS supported search ability as enumeration set.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public enum SearchabilityType {

    NOT_SUPPORTED(DatabaseMetaData.typePredNone), // Indicates that <code>WHERE</code> search clauses are not supported for this
                                                  // type.
    WHERE_LIKE(DatabaseMetaData.typePredChar), // Indicates that the only <code>WHERE</code> search clause that can be based on
                                               // this type is <code>WHERE . . . LIKE</code>.
    BASIC(DatabaseMetaData.typePredBasic), // Indicates that one can base all <code>WHERE</code> search clauses except <code>WHERE
                                           // . . . LIKE</code> on this data type.
    SEARCHABLE(DatabaseMetaData.typeSearchable); // Indicates that all <code>WHERE</code> search clauses can be based on this
                                                 // type.

    private final int searchability;

    SearchabilityType( int searchability ) {
        this.searchability = searchability;
    }

    public int getSearchability() {
        return searchability;
    }

    public String getName() {
        return name();
    }
}
