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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.jcr.query.qom.Constraint;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.spi.index.IndexConstraints;

/**
 * An index for enumerated values. This index only supports string-based values, since all enumerated values are discrete.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
final class LocalEnumeratedIndex implements LocalIndex<String> {

    private static final Logger LOGGER = Logger.getLogger(LocalEnumeratedIndex.class);

    static LocalEnumeratedIndex create( String name,
                                        String workspaceName,
                                        DB db,
                                        Converter<String> converter,
                                        BTreeKeySerializer<String> valueSerializer,
                                        Set<String> enumeratedValues ) {
        return new LocalEnumeratedIndex(name, workspaceName, db, converter, valueSerializer, enumeratedValues);
    }

    static LocalEnumeratedIndex create( String name,
                                        String workspaceName,
                                        DB db,
                                        Converter<String> converter,
                                        BTreeKeySerializer<String> valueSerializer ) {
        return new LocalEnumeratedIndex(name, workspaceName, db, converter, valueSerializer, null);
    }

    private final String name;
    private final String workspace;

    protected final ConcurrentNavigableMap<String, Set<String>> nodeKeySetsByValue;
    private final Converter<String> converter;
    private final DB db;
    private final Set<String> possibleValues;
    private final String workspaceName;
    private final boolean isNew;
    private final IndexUpdater indexUpdater;

    LocalEnumeratedIndex( String name,
                          String workspaceName,
                          DB db,
                          Converter<String> converter,
                          BTreeKeySerializer<String> valueSerializer,
                          Set<String> possibleValues ) {
        this.name = name;
        this.workspace = workspaceName;
        this.converter = converter;
        this.db = db;
        this.indexUpdater = new IndexUpdater(db);
        this.workspaceName = workspaceName;
        this.possibleValues = possibleValues != null ? new HashSet<String>(possibleValues) : new HashSet<String>();
        this.nodeKeySetsByValue = new ConcurrentSkipListMap<>();
        // Read all of the existing collections ...
        boolean foundContent = false;
        for (String collectionName : db.getAll().keySet()) {
            String prefix = this.name + "/enumerated/";
            if (collectionName.startsWith(prefix)) {
                foundContent = true;
                if (collectionName.length() > prefix.length()) {
                    String valueString = collectionName.substring(prefix.length());
                    Set<String> keysForValue = createOrGetKeySet(valueString);
                    nodeKeySetsByValue.put(valueString, keysForValue);
                }
            }
        }
        // Add any that were not found in the DB ...
        for (String possibleValue : this.possibleValues) {
            if (!nodeKeySetsByValue.containsKey(possibleValue)) {
                Set<String> keysForValue = createOrGetKeySet(possibleValue);
                nodeKeySetsByValue.put(possibleValue, keysForValue);
            }
        }
        this.isNew = !foundContent;
    }

    private Set<String> createOrGetKeySet( String value ) {
        String collectionName = collectionName(value);
        // Try to create the set ...
        Set<String> keySet = null;
        if (db.exists(collectionName)) {
            LOGGER.debug("Reopening enum storage '{0}' for '{1}' index in workspace '{2}'", collectionName, name, workspaceName);
            keySet = db.getHashSet(collectionName);
        } else {
            LOGGER.debug("Creating enum storage '{0}' for '{1}' index in workspace '{2}'", collectionName, name, workspaceName);
            keySet = db.createHashSet(collectionName).make();
        }
        Set<String> previous = nodeKeySetsByValue.putIfAbsent(value, keySet);
        if (previous != null) keySet = previous;
        return keySet;
    }

    private String collectionName( String value ) {
        return name + "/enumerated/" + value;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getWorkspaceName() {
        return workspace;
    }
    
    @Override
    public boolean isNew() {
        return isNew;
    }

    protected final Converter<String> converter() {
        return converter;
    }

    @Override
    public long estimateTotalCount() {
        long count = 0L;
        for (Map.Entry<String, Set<String>> entry : nodeKeySetsByValue.entrySet()) {
            count += entry.getValue().size();
        }
        return count;
    }

    @Override
    public Results filter( IndexConstraints filter ) {
        // Find all sets that match the name pattern ...
        return Operations.createEnumeratedFilter(nodeKeySetsByValue, converter, filter.getConstraints(), filter.getVariables())
                         .getResults();
    }

    @Override
    public long estimateCardinality( Constraint constraint,
                                     Map<String, Object> variables ) {
        return Operations.createEnumeratedFilter(nodeKeySetsByValue, converter, Collections.singleton(constraint), variables)
                         .estimateCount();
    }

    @Override
    public void add( String nodeKey,
                     String value ) {
        // Find the set ...
        Set<String> keySet = nodeKeySetsByValue.get(value);
        if (keySet == null) {
            keySet = createOrGetKeySet(value);
        }
        keySet.add(nodeKey);
    }

    @Override
    public void remove( String nodeKey ) {
        for (Set<String> nodeKeySet : nodeKeySetsByValue.values()) {
            nodeKeySet.remove(nodeKey);
        }
    }

    @Override
    public void remove( String nodeKey,
                        String value ) {
        Set<String> nodeKeySet = nodeKeySetsByValue.get(value);
        if (nodeKeySet != null) {
            nodeKeySet.remove(nodeKey);
        }
    }

    @Override
    public synchronized void removeAll() {
        for (Map.Entry<String, Set<String>> entry : nodeKeySetsByValue.entrySet()) {
            entry.getValue().clear();
            String collectionName = collectionName(entry.getKey());
            if (db.exists(collectionName)) {
                db.delete(collectionName);
            }
        }
        nodeKeySetsByValue.clear();
    }

    @Override
    public void commit() {
        indexUpdater.commit();
    }

    @Override
    public synchronized void shutdown( boolean destroyed ) {
        if (destroyed) {
            // Remove the database since the index was destroyed ...
            for (String value : nodeKeySetsByValue.keySet()) {
                String collectionName = collectionName(value);
                if (db.exists(collectionName)) {
                    db.delete(collectionName);
                }
            }
            nodeKeySetsByValue.clear();
        }
    }

}
