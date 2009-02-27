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
package org.jboss.dna.graph.property.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ValueFactory;

/**
 * A {@link NamespaceRegistry} implementation that stores the namespaces in a Graph as individual nodes for each namespace, under
 * a parent supplied by the constructor.
 * 
 * @See {@link ThreadSafeNamespaceRegistry}
 */
@NotThreadSafe
public class GraphNamespaceRegistry implements NamespaceRegistry {

    public static final Name DEFAULT_URI_PROPERTY_NAME = DnaLexicon.NAMESPACE_URI;
    public static final String GENERATED_PREFIX = "ns";

    private SimpleNamespaceRegistry cache;
    private final Graph store;
    private final Path parentOfNamespaceNodes;
    private final Name uriPropertyName;
    private final List<Property> namespaceProperties;

    public GraphNamespaceRegistry( Graph store,
                                   Path parentOfNamespaceNodes,
                                   Name uriPropertyName,
                                   Property... additionalProperties ) {
        this.cache = new SimpleNamespaceRegistry();
        this.store = store;
        this.parentOfNamespaceNodes = parentOfNamespaceNodes;
        this.uriPropertyName = uriPropertyName != null ? uriPropertyName : DEFAULT_URI_PROPERTY_NAME;
        List<Property> properties = Collections.emptyList();
        if (additionalProperties != null && additionalProperties.length != 0) {
            properties = new ArrayList<Property>(additionalProperties.length);
            Set<Name> propertyNames = new HashSet<Name>();
            for (Property property : additionalProperties) {
                if (!propertyNames.contains(property.getName())) properties.add(property);
            }
        }
        this.namespaceProperties = Collections.unmodifiableList(properties);
        initializeCacheFromStore(cache);

        // Load in the namespaces from the execution context used by the store ...
        for (Namespace namespace : store.getContext().getNamespaceRegistry().getNamespaces()) {
            register(namespace.getPrefix(), namespace.getNamespaceUri());
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceForPrefix( String prefix ) {
        CheckArg.isNotNull(prefix, "prefix");
        // Try the cache first ...
        String uri = cache.getNamespaceForPrefix(prefix);
        if (uri == null) {
            // See if the store has it ...
            uri = readUriFor(prefix);
            if (uri != null) {
                // update the cache ...
                cache.register(prefix, uri);
            }
        }
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        // Try the cache first ...
        String prefix = cache.getPrefixForNamespaceUri(namespaceUri, false);
        if (prefix == null && generateIfMissing) {
            prefix = readPrefixFor(namespaceUri, generateIfMissing);
            if (prefix != null) {
                cache.register(prefix, namespaceUri);
            }
        }
        return prefix;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        if (cache.isRegisteredNamespaceUri(namespaceUri)) return true;
        // Otherwise it was not found in the cache, so check the store ...
        String prefix = readPrefixFor(namespaceUri, false);
        if (prefix != null) {
            cache.register(prefix, namespaceUri);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultNamespaceUri() {
        return this.getNamespaceForPrefix("");
    }

    /**
     * {@inheritDoc}
     */
    public String register( String prefix,
                            String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        namespaceUri = namespaceUri.trim();
        // Register it in the cache first ...
        String previousCachedUriForPrefix = this.cache.register(prefix, namespaceUri);
        // And register it in the source ...
        String previousPersistentUriForPrefix = doRegister(prefix, namespaceUri);
        return previousCachedUriForPrefix != null ? previousPersistentUriForPrefix : previousPersistentUriForPrefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#unregister(java.lang.String)
     */
    public boolean unregister( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        namespaceUri = namespaceUri.trim();
        // Remove it from the cache ...
        boolean found = this.cache.unregister(namespaceUri);
        // Then from the source ...
        return doUnregister(namespaceUri) || found;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getRegisteredNamespaceUris() {
        // Just return what's in the cache ...
        return cache.getRegisteredNamespaceUris();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#getNamespaces()
     */
    public Set<Namespace> getNamespaces() {
        // Just return what's in the cache ...
        return cache.getNamespaces();
    }

    public void refresh() {
        SimpleNamespaceRegistry newCache = new SimpleNamespaceRegistry();
        initializeCacheFromStore(newCache);
        this.cache = newCache;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        List<Namespace> namespaces = new ArrayList<Namespace>(getNamespaces());
        Collections.sort(namespaces);
        return namespaces.toString();
    }

    protected void initializeCacheFromStore( NamespaceRegistry cache ) {
        // Read the store ...
        try {
            Subgraph nsGraph = store.getSubgraphOfDepth(2).at(parentOfNamespaceNodes);
            ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
            for (Location nsLocation : nsGraph.getRoot().getChildren()) {
                Node ns = nsGraph.getNode(nsLocation);
                // This node is a namespace ...
                String uri = stringFactory.create(ns.getProperty(uriPropertyName).getFirstValue());
                if (uri != null) {
                    String prefix = getPrefixFor(nsLocation.getPath());
                    cache.register(prefix, uri);
                }
            }
        } catch (PathNotFoundException e) {
            // Nothing to read
        }
    }

    protected String readUriFor( String prefix ) {
        // Read the store ...
        try {
            PathFactory pathFactory = store.getContext().getValueFactories().getPathFactory();
            Path pathToNamespaceNode = pathFactory.create(parentOfNamespaceNodes, prefix);
            Property uri = store.getProperty(uriPropertyName).on(pathToNamespaceNode);
            // Get the URI property value ...
            ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
            return stringFactory.create(uri.getFirstValue());
        } catch (PathNotFoundException e) {
            // Nothing to read
            return null;
        }
    }

    protected String getPrefixFor( Path path ) {
        Path.Segment lastSegment = path.getLastSegment();
        String localName = lastSegment.getName().getLocalName();
        int index = lastSegment.getIndex();
        if (index == 1) {
            if (GENERATED_PREFIX.equals(localName)) return localName + "00" + index;
            return localName;
        }
        if (index < 10) {
            return localName + "00" + index;
        }
        if (index < 100) {
            return localName + "0" + index;
        }
        return localName + index;
    }

    protected String readPrefixFor( String namespaceUri,
                                    boolean generateIfMissing ) {
        // Read the store ...
        try {
            Subgraph nsGraph = store.getSubgraphOfDepth(2).at(parentOfNamespaceNodes);
            ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
            for (Location nsLocation : nsGraph.getRoot().getChildren()) {
                Node ns = nsGraph.getNode(nsLocation);
                String prefix = getPrefixFor(nsLocation.getPath());
                String uri = stringFactory.create(ns.getProperty(uriPropertyName).getFirstValue());
                if (prefix != null && uri != null) {
                    if (uri.equals(namespaceUri)) return prefix;
                }
            }
            if (generateIfMissing) {
                // Generated prefixes are simply "ns" followed by the SNS index ...
                PathFactory pathFactory = store.getContext().getValueFactories().getPathFactory();
                Path pathToNamespaceNode = pathFactory.create(parentOfNamespaceNodes, GENERATED_PREFIX);
                Location actualLocation = store.createAt(pathToNamespaceNode)
                                               .with(namespaceProperties)
                                               .and(uriPropertyName, namespaceUri)
                                               .getLocation();

                return getPrefixFor(actualLocation.getPath());
            }

        } catch (PathNotFoundException e) {
            // Nothing to read
        }
        return null;
    }

    protected String doRegister( String prefix,
                                 String uri ) {
        assert prefix != null;
        assert uri != null;
        prefix = prefix.trim();
        uri = uri.trim();

        // Read the store ...
        String previousUri = null;
        ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
        PathFactory pathFactory = store.getContext().getValueFactories().getPathFactory();
        Path pathToNamespaceNode = pathFactory.create(parentOfNamespaceNodes, prefix);
        try {
            Subgraph nsGraph = store.getSubgraphOfDepth(2).at(parentOfNamespaceNodes);
            // Iterate over the existing mappings, looking for one that uses the URI ...
            Location nsNodeWithPrefix = null;
            boolean updateNode = true;
            Set<Location> locationsToRemove = new HashSet<Location>();
            for (Location nsLocation : nsGraph.getRoot().getChildren()) {
                Node ns = nsGraph.getNode(nsLocation);
                String actualPrefix = getPrefixFor(nsLocation.getPath());
                String actualUri = stringFactory.create(ns.getProperty(uriPropertyName).getFirstValue());
                if (actualPrefix != null && actualUri != null) {
                    if (actualPrefix.equals(prefix)) {
                        nsNodeWithPrefix = nsLocation;
                        if (actualUri.equals(uri)) {
                            updateNode = false;
                            break;
                        }
                        previousUri = actualUri;
                    }
                    if (actualUri.equals(uri)) {
                        locationsToRemove.add(ns.getLocation());
                    }
                }
            }
            Graph.Batch batch = store.batch();
            // Remove any other nodes that have the same URI ...
            for (Location namespaceToRemove : locationsToRemove) {
                batch.delete(namespaceToRemove).and();
            }
            // Now update/create the namespace mapping ...
            if (nsNodeWithPrefix == null) {
                // We didn't find an existing node, so we have to create it ...
                batch.create(pathToNamespaceNode).with(namespaceProperties).and(uriPropertyName, uri).and();
            } else {
                if (updateNode) {
                    // There was already an existing node, so update it ...
                    batch.set(uriPropertyName).to(uri).on(pathToNamespaceNode).and();
                }
            }
            // Execute all these changes ...
            batch.execute();
        } catch (PathNotFoundException e) {
            // Nothing stored yet ...
            store.createAt(pathToNamespaceNode).with(namespaceProperties).and(uriPropertyName, uri).getLocation();
        }
        return previousUri;
    }

    protected boolean doUnregister( String uri ) {
        // Read the store ...
        ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
        boolean result = false;
        try {
            Subgraph nsGraph = store.getSubgraphOfDepth(2).at(parentOfNamespaceNodes);
            // Iterate over the existing mappings, looking for one that uses the prefix and uri ...
            Set<Location> locationsToRemove = new HashSet<Location>();
            for (Location nsLocation : nsGraph.getRoot().getChildren()) {
                Node ns = nsGraph.getNode(nsLocation);
                String actualUri = stringFactory.create(ns.getProperty(uriPropertyName).getFirstValue());
                if (actualUri.equals(uri)) {
                    locationsToRemove.add(ns.getLocation());
                    result = true;
                }
            }
            // Remove any other nodes that have the same URI ...
            Graph.Batch batch = store.batch();
            for (Location namespaceToRemove : locationsToRemove) {
                batch.delete(namespaceToRemove).and();
            }
            // Execute all these changes ...
            batch.execute();
        } catch (PathNotFoundException e) {
            // Nothing stored yet, so do nothing ...
        }
        return result;
    }

}
