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
package org.jboss.dna.graph.query.process;

import java.util.List;
import org.jboss.dna.graph.query.model.Limit;

/**
 */
public class LimitComponent extends DelegatingComponent {

    private final Limit limit;

    public LimitComponent( ProcessingComponent delegate,
                           Limit limit ) {
        super(delegate);
        this.limit = limit;
        assert this.limit != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.process.ProcessingComponent#execute()
     */
    @Override
    public List<Object[]> execute() {
        List<Object[]> tuples = delegate().execute();
        if (limit.isOffset()) {
            if (limit.getOffset() >= tuples.size()) {
                // There aren't enough results, so return an empty list ...
                return emptyTuples();
            }
            if (limit.isUnlimited()) {
                // An offset, but no row limit ...
                tuples = tuples.subList(limit.getOffset(), tuples.size());
            } else {
                // Both an offset AND a row limit (which may be more than the number of rows available)...
                int toIndex = Math.min(limit.getOffset() + limit.getRowLimit(), tuples.size());
                tuples = tuples.subList(limit.getOffset(), toIndex);
            }
        } else {
            // No offset, but perhaps there's a row limit ...
            if (!limit.isUnlimited()) {
                int toIndex = Math.min(limit.getRowLimit(), tuples.size());
                tuples = tuples.subList(0, toIndex);
            }
        }
        return tuples;
    }
}
