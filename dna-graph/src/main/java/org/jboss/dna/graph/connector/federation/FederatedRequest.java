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
package org.jboss.dna.graph.connector.federation;

import java.util.concurrent.CountDownLatch;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.request.Request;

/**
 * A wrapper for a request submitted to the federated repository, and the corresponding source-specific {@link ProjectedRequest
 * projected requests}.
 */
@NotThreadSafe
class FederatedRequest {
    private final Request original;
    private CountDownLatch forkLatch;
    private int incompleteCount;
    private ProjectedRequest first;

    FederatedRequest( Request original ) {
        this.original = original;
    }

    public Request original() {
        return original;
    }

    public final FederatedRequest add( Request request,
                                       boolean isSameLocationAsOriginal,
                                       boolean isComplete,
                                       Projection projection,
                                       Projection secondProjection ) {
        if (!isComplete) ++incompleteCount;
        if (first == null) {
            if (isSameLocationAsOriginal) {
                first = new MirrorRequest(request, isComplete, projection, secondProjection);
            } else {
                first = new ProjectedRequest(request, isComplete, projection, secondProjection);
            }
        } else {
            first.addNext(request, isComplete, projection);
        }
        return this;
    }

    public final FederatedRequest add( Request request,
                                       boolean isSameLocationAsOriginal,
                                       boolean isComplete,
                                       Projection projection ) {
        return add(request, isSameLocationAsOriginal, isComplete, projection, null);
    }

    public void freeze() {
        if (incompleteCount > 0 && forkLatch == null) {
            forkLatch = new CountDownLatch(incompleteCount);
        }
    }

    public ProjectedRequest getFirstProjectedRequest() {
        return first;
    }

    public boolean hasIncompleteRequests() {
        return incompleteCount != 0;
    }

    public CountDownLatch getLatch() {
        return forkLatch;
    }

    public void await() throws InterruptedException {
        if (forkLatch != null) forkLatch.await();
    }

    class ProjectedRequest {
        private final Projection projection;
        private final Projection projection2;
        private final Request request;
        private final boolean isComplete;
        private ProjectedRequest next;

        protected ProjectedRequest( Request request,
                                    boolean isComplete,
                                    Projection projection,
                                    Projection secondProjection ) {
            this.projection = projection;
            this.request = request;
            this.isComplete = isComplete;
            this.projection2 = secondProjection;
        }

        public Projection getProjection() {
            return projection;
        }

        public Projection getSecondProjection() {
            return projection2;
        }

        public Request getRequest() {
            return request;
        }

        public boolean isComplete() {
            return isComplete;
        }

        public boolean isSameLocation() {
            return false;
        }

        public ProjectedRequest next() {
            return next;
        }

        public boolean hasNext() {
            return next != null;
        }

        protected final ProjectedRequest addNext( Request request,
                                                  boolean isComplete,
                                                  Projection projection,
                                                  Projection secondProjection ) {
            ProjectedRequest last = this;
            while (last.next != null) {
                last = last.next;
            }
            last.next = new ProjectedRequest(request, isComplete, projection, secondProjection);
            return last.next;
        }

        protected final ProjectedRequest addNext( Request request,
                                                  boolean isComplete,
                                                  Projection projection ) {
            return addNext(request, isComplete, projection, null);
        }
    }

    class MirrorRequest extends ProjectedRequest {
        protected MirrorRequest( Request request,
                                 boolean isComplete,
                                 Projection projection,
                                 Projection secondProjection ) {
            super(request, isComplete, projection, secondProjection);
        }

        @Override
        public boolean isSameLocation() {
            return true;
        }
    }

}
