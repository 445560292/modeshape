/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.jcr.spi.index.provider;

import java.util.Set;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.JcrNtLexicon;
import org.modeshape.jcr.cache.change.ChangeSetAdapter.NodeTypePredicate;
import org.modeshape.jcr.value.Name;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
@ThreadSafe
public abstract class NodeTypeMatcher implements NodeTypePredicate {

    public static NodeTypeMatcher create( Set<Name> nodeTypeNames ) {
        NodeTypePredicate delegate = null;
        if (nodeTypeNames == null || nodeTypeNames.isEmpty()) {
            delegate = MatchNonePredicate.INSTANCE;
        } else if (nodeTypeNames.contains(JcrNtLexicon.BASE)) {
            delegate = NtBaseMatchPredicate.INSTANCE;
        } else {
            delegate = new NodeTypeSetMatcher(nodeTypeNames);
        }
        return new MutableNodeTypeMatcher(delegate);
    }

    public abstract void use( NodeTypePredicate other );

    private static final class MutableNodeTypeMatcher extends NodeTypeMatcher {

        private volatile NodeTypePredicate delegate;

        protected MutableNodeTypeMatcher( NodeTypePredicate delegate ) {
            this.delegate = delegate;
        }

        @Override
        public boolean matchesType( Name primaryType,
                                    Set<Name> mixinTypes ) {
            return delegate.matchesType(primaryType, mixinTypes);
        }

        @Override
        public void use( NodeTypePredicate other ) {
            if (other instanceof MatchNonePredicate) {
                delegate = MatchNonePredicate.INSTANCE;
            } else if (other instanceof NtBaseMatchPredicate) {
                delegate = NtBaseMatchPredicate.INSTANCE;
            }
            assert other instanceof MutableNodeTypeMatcher;
            delegate = ((MutableNodeTypeMatcher)other).delegate;
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    private static final class NodeTypeSetMatcher implements NodeTypePredicate {
        private final Set<Name> allNodeTypes;

        protected NodeTypeSetMatcher( final Set<Name> allNodeTypes ) {
            this.allNodeTypes = allNodeTypes;
        }

        @Override
        public boolean matchesType( Name primaryType,
                                    Set<Name> mixinTypes ) {
            if (allNodeTypes.contains(primaryType)) return true;
            if (mixinTypes != null) {
                for (Name mixinType : mixinTypes) {
                    if (allNodeTypes.contains(mixinType)) return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "<Match=" + allNodeTypes + ">";
        }
    }

    private static final class NtBaseMatchPredicate implements NodeTypePredicate {
        protected static final NtBaseMatchPredicate INSTANCE = new NtBaseMatchPredicate();

        @Override
        public boolean matchesType( Name primaryType,
                                    Set<Name> mixinTypes ) {
            return true;
        }

        @Override
        public String toString() {
            return "<MatchAll>";
        }
    }

    private static final class MatchNonePredicate implements NodeTypePredicate {
        protected static final MatchNonePredicate INSTANCE = new MatchNonePredicate();

        @Override
        public boolean matchesType( Name primaryType,
                                    Set<Name> mixinTypes ) {
            return false;
        }

        @Override
        public String toString() {
            return "<MatchNone>";
        }
    }
}
