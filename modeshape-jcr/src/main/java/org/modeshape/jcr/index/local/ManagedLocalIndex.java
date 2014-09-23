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

package org.modeshape.jcr.index.local;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jcr.query.qom.Constraint;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.provider.IndexChangeAdapter;
import org.modeshape.jcr.spi.index.provider.ManagedIndex;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class ManagedLocalIndex implements ManagedIndex {

    private final LocalIndex<?> index;
    private final IndexChangeAdapter adapter;
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    ManagedLocalIndex( LocalIndex<?> index,
                       IndexChangeAdapter adapter ) {
        assert adapter != null;
        assert index != null;
        this.index = index;
        this.adapter = adapter;
    }

    public boolean isNew() {
        return index.isNew();
    }

    @Override
    public long estimateTotalCount() {
        return index.estimateTotalCount();
    }

    @Override
    public long estimateCardinality( Constraint constraint,
                                     Map<String, Object> variables ) {
        return index.estimateCardinality(constraint, variables);
    }

    @Override
    public Results filter( IndexConstraints constraints ) {
        return index.filter(constraints);
    }

    @Override
    public IndexChangeAdapter getIndexChangeAdapter() {
        return adapter;
    }

    @Override
    public void enable( boolean enable ) {
        this.enabled.set(enable);
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }

    @Override
    public void shutdown( boolean destroyed ) {
        index.shutdown(destroyed);
    }

    @Override
    public void removeAll() {
        index.removeAll();
    }

}
