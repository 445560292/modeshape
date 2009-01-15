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
package org.jboss.dna.common.collection;

import java.util.LinkedList;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;

/**
 * A list of problems for some execution context. The problems will be {@link #iterator() returned} in the order in which they
 * were encountered (although this cannot be guaranteed in contexts involving multiple threads or processes).
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@NotThreadSafe
public class SimpleProblems extends AbstractProblems {

    private List<Problem> problems;

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#addProblem(Problem)
     */
    @Override
    protected void addProblem( Problem problem ) {
        if (problem == null) return;
        if (problems == null) problems = new LinkedList<Problem>();
        problems.add(problem);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.AbstractProblems#getProblems()
     */
    @Override
    protected List<Problem> getProblems() {
        return this.problems != null ? problems : EMPTY_PROBLEMS;
    }
}
