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
package org.jboss.dna.connector.federation.merge;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.connector.federation.contribution.Contribution;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class DoubleContributionMergePlan extends MergePlan {

    private static final long serialVersionUID = 1L;
    private final Contribution contribution1;
    private final Contribution contribution2;

    /**
     * @param contribution1 the first contribution for this merge plan
     * @param contribution2 the second contribution for this merge plan
     */
    public DoubleContributionMergePlan( Contribution contribution1,
                                        Contribution contribution2 ) {
        assert contribution1 != null;
        assert contribution2 != null;
        this.contribution1 = contribution1;
        this.contribution2 = contribution2;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#getContributionCount()
     */
    @Override
    public int getContributionCount() {
        return 2;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#getContributionFrom(java.lang.String)
     */
    @Override
    public Contribution getContributionFrom( String sourceName ) {
        if (contribution1.getSourceName().equals(sourceName)) return contribution1;
        if (contribution2.getSourceName().equals(sourceName)) return contribution2;
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Contribution> iterator() {
        return new Iterator<Contribution>() {
            private int next = 2;

            public boolean hasNext() {
                return next > 0;
            }

            @SuppressWarnings( "synthetic-access" )
            public Contribution next() {
                if (next == 2) {
                    next = 1;
                    return contribution2;
                }
                if (next == 1) {
                    next = 0;
                    return contribution1;
                }
                throw new NoSuchElementException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.federation.merge.MergePlan#isSource(java.lang.String)
     */
    @Override
    public boolean isSource( String sourceName ) {
        return contribution1.getSourceName().equals(sourceName) || contribution2.getSourceName().equals(sourceName);
    }

}
