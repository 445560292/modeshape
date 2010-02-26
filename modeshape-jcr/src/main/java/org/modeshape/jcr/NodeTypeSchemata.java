/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.NamespaceRegistry.Namespace;
import org.modeshape.graph.property.basic.LocalNamespaceRegistry;
import org.modeshape.graph.query.model.AllNodes;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.validate.ImmutableSchemata;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.search.lucene.IndexRules;
import org.modeshape.search.lucene.LuceneSearchEngine;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A {@link Schemata} implementation that is constructed from the {@link NodeType}s and {@link PropertyDefinition}s contained
 * within a {@link RepositoryNodeTypeManager}. The resulting {@link Schemata.Table}s will never change, so the
 * {@link RepositoryNodeTypeManager} must replace it's cached instance whenever the node types change.
 */
@Immutable
class NodeTypeSchemata implements Schemata {

    private final Schemata schemata;
    private final Map<Integer, String> types;
    private final Map<String, String> prefixesByUris = new HashMap<String, String>();
    private final boolean includeColumnsForInheritedProperties;
    private final Iterable<JcrPropertyDefinition> propertyDefinitions;
    private final Map<Name, JcrNodeType> nodeTypesByName;
    private final Multimap<JcrNodeType, JcrNodeType> subtypesByName = LinkedHashMultimap.create();
    private final IndexRules indexRules;

    NodeTypeSchemata( ExecutionContext context,
                      Map<Name, JcrNodeType> nodeTypes,
                      Iterable<JcrPropertyDefinition> propertyDefinitions,
                      boolean includeColumnsForInheritedProperties ) {
        this.includeColumnsForInheritedProperties = includeColumnsForInheritedProperties;
        this.propertyDefinitions = propertyDefinitions;
        this.nodeTypesByName = nodeTypes;

        // Register all the namespace prefixes by URIs ...
        for (Namespace namespace : context.getNamespaceRegistry().getNamespaces()) {
            this.prefixesByUris.put(namespace.getNamespaceUri(), namespace.getPrefix());
        }

        // Identify the subtypes for each node type, and do this before we build any views ...
        for (JcrNodeType nodeType : nodeTypesByName.values()) {
            // For each of the supertypes ...
            for (JcrNodeType supertype : nodeType.getTypeAndSupertypes()) {
                subtypesByName.put(supertype, nodeType);
            }
        }

        // Build the schemata for the current node types ...
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();
        ImmutableSchemata.Builder builder = ImmutableSchemata.createBuilder(typeSystem);

        // Build the fast-search for type names based upon PropertyType values ...
        types = new HashMap<Integer, String>();
        for (String typeName : typeSystem.getTypeNames()) {
            org.modeshape.graph.property.PropertyType dnaType = org.modeshape.graph.property.PropertyType.valueOf(typeName);
            int jcrType = PropertyTypeUtil.jcrPropertyTypeFor(dnaType);
            types.put(jcrType, typeName);
        }

        // Create the "ALLNODES" table, which will contain all possible properties ...
        IndexRules.Builder indexRulesBuilder = IndexRules.createBuilder(LuceneSearchEngine.DEFAULT_RULES);
        indexRulesBuilder.defaultTo(Field.Store.YES, Field.Index.ANALYZED, true);
        addAllNodesTable(builder, indexRulesBuilder, context);

        // Define a view for each node type ...
        for (JcrNodeType nodeType : nodeTypesByName.values()) {
            addView(builder, context, nodeType);
        }

        schemata = builder.build();
        indexRules = indexRulesBuilder.build();
    }

    /**
     * Get the index rules ...
     * 
     * @return indexRules
     */
    public IndexRules getIndexRules() {
        return indexRules;
    }

    protected JcrNodeType getNodeType( Name nodeTypeName ) {
        return nodeTypesByName.get(nodeTypeName);
    }

    protected final void addAllNodesTable( ImmutableSchemata.Builder builder,
                                           IndexRules.Builder indexRuleBuilder,
                                           ExecutionContext context ) {
        NamespaceRegistry registry = context.getNamespaceRegistry();
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();

        String tableName = AllNodes.ALL_NODES_NAME.getName();
        boolean first = true;
        Map<String, String> typesForNames = new HashMap<String, String>();
        Set<String> fullTextSearchableNames = new HashSet<String>();
        for (JcrPropertyDefinition defn : propertyDefinitions) {
            if (defn.isResidual()) continue;
            if (defn.isPrivate()) continue;
            // if (defn.isMultiple()) continue;
            Name name = defn.getInternalName();

            String columnName = name.getString(registry);
            if (first) {
                builder.addTable(tableName, columnName);
                first = false;
            }
            boolean canBeReference = false;
            switch (defn.getRequiredType()) {
                case PropertyType.REFERENCE:
                case PropertyType.UNDEFINED:
                    canBeReference = true;
            }
            String type = typeSystem.getDefaultType();
            if (defn.getRequiredType() != PropertyType.UNDEFINED) {
                type = types.get(defn.getRequiredType());
            }
            assert type != null;
            String previousType = typesForNames.put(columnName, type);
            if (previousType != null && !previousType.equals(type)) {
                // There are two property definitions with the same name but different types, so we need to find a common type ...
                type = typeSystem.getCompatibleType(previousType, type);
            }
            boolean fullTextSearchable = fullTextSearchableNames.contains(columnName) || defn.isFullTextSearchable();
            if (fullTextSearchable) fullTextSearchableNames.add(columnName);
            // Add (or overwrite) the column ...
            builder.addColumn(tableName, columnName, type, fullTextSearchable);

            // And build an indexing rule for this type ...
            if (indexRuleBuilder != null) addIndexRule(indexRuleBuilder, defn, type, typeSystem, canBeReference);
        }
    }

    /**
     * Add an index rule for the given property definition and the type in the {@link TypeSystem}.
     * 
     * @param builder the index rule builder; never null
     * @param defn the property definition; never null
     * @param type the TypeSystem type, which may be a more general type than dictated by the definition, since multiple
     *        definitions with the same name require the index rule to use the common base type; never null
     * @param canBeReference true if the property described the rule can hold reference values, or false otherwise
     * @param typeSystem the type system; never null
     */
    protected final void addIndexRule( IndexRules.Builder builder,
                                       JcrPropertyDefinition defn,
                                       String type,
                                       TypeSystem typeSystem,
                                       boolean canBeReference ) {
        Store store = Store.YES;
        Index index = defn.isFullTextSearchable() ? Index.ANALYZED : Index.NO;
        if (typeSystem.getStringFactory().getTypeName().equals(type)) {
            builder.stringField(defn.getInternalName(), store, index, canBeReference);
        } else if (typeSystem.getDateTimeFactory().getTypeName().equals(type)) {
            Long minimum = typeSystem.getLongFactory().create(defn.getMinimumValue());
            Long maximum = typeSystem.getLongFactory().create(defn.getMaximumValue());
            builder.dateField(defn.getInternalName(), store, index, minimum, maximum);
        } else if (typeSystem.getLongFactory().getTypeName().equals(type)) {
            Long minimum = typeSystem.getLongFactory().create(defn.getMinimumValue());
            Long maximum = typeSystem.getLongFactory().create(defn.getMaximumValue());
            builder.longField(defn.getInternalName(), store, index, minimum, maximum);
        } else if (typeSystem.getDoubleFactory().getTypeName().equals(type)) {
            Double minimum = typeSystem.getDoubleFactory().create(defn.getMinimumValue());
            Double maximum = typeSystem.getDoubleFactory().create(defn.getMaximumValue());
            builder.doubleField(defn.getInternalName(), store, index, minimum, maximum);
        } else if (typeSystem.getBooleanFactory().getTypeName().equals(type)) {
            builder.booleanField(defn.getInternalName(), store, index);
        } else if (typeSystem.getBinaryFactory().getTypeName().equals(type)) {
            store = Store.NO;
            builder.binaryField(defn.getInternalName(), store, index);
        } else if (typeSystem.getReferenceFactory().getTypeName().equals(type)) {
            store = Store.NO;
            builder.referenceField(defn.getInternalName(), store, index);
        } else if (typeSystem.getPathFactory().getTypeName().equals(type)) {
            store = Store.NO;
            builder.weakReferenceField(defn.getInternalName(), store, index);
        } else {
            // Everything else gets stored as a string ...
            builder.stringField(defn.getInternalName(), store, index, canBeReference);
        }

    }

    protected final void addView( ImmutableSchemata.Builder builder,
                                  ExecutionContext context,
                                  JcrNodeType nodeType ) {
        NamespaceRegistry registry = context.getNamespaceRegistry();

        String tableName = nodeType.getName();
        JcrPropertyDefinition[] defns = null;
        if (includeColumnsForInheritedProperties) {
            defns = nodeType.getPropertyDefinitions();
        } else {
            defns = nodeType.getDeclaredPropertyDefinitions();
        }
        if (defns.length == 0) {
            // There are no properties, so there's no reason to have the view ...
            return;
        }
        // Create the SQL statement ...
        StringBuilder viewDefinition = new StringBuilder("SELECT ");
        boolean first = true;
        for (JcrPropertyDefinition defn : defns) {
            if (defn.isResidual()) continue;
            if (defn.isMultiple()) continue;
            if (defn.isPrivate()) continue;
            Name name = defn.getInternalName();

            String columnName = name.getString(registry);
            if (first) first = false;
            else viewDefinition.append(',');
            viewDefinition.append('[').append(columnName).append(']');
        }
        if (first) {
            // All the properties were skipped ...
            return;
        }
        viewDefinition.append(" FROM ").append(AllNodes.ALL_NODES_NAME);

        // The 'nt:base' node type will have every single object in it, so we don't need to add the type criteria ...
        if (!JcrNtLexicon.BASE.equals(nodeType.getInternalName())) {
            // The node type is not 'nt:base', which
            viewDefinition.append(" WHERE ");

            Collection<JcrNodeType> typeAndSubtypes = subtypesByName.get(nodeType);
            if (nodeType.isMixin()) {
                // Build the list of mixin types ...
                StringBuilder mixinTypes = null;
                int count = 0;
                for (JcrNodeType thisOrSupertype : typeAndSubtypes) {
                    if (!thisOrSupertype.isMixin()) continue;
                    if (mixinTypes == null) {
                        mixinTypes = new StringBuilder();
                    } else {
                        mixinTypes.append(',');
                    }
                    assert prefixesByUris.containsKey(thisOrSupertype.getInternalName().getNamespaceUri());
                    String name = thisOrSupertype.getInternalName().getString(registry);
                    mixinTypes.append('[').append(name).append(']');
                    ++count;
                }
                assert mixinTypes != null; // should at least include itself
                assert count > 0;
                viewDefinition.append('[').append(JcrLexicon.MIXIN_TYPES.getString(registry)).append(']');
                if (count == 1) {
                    viewDefinition.append('=').append(mixinTypes);
                } else {
                    viewDefinition.append(" IN (").append(mixinTypes).append(')');
                }
            } else {
                // Build the list of node type names ...
                StringBuilder primaryTypes = null;
                int count = 0;
                for (JcrNodeType thisOrSupertype : typeAndSubtypes) {
                    if (thisOrSupertype.isMixin()) continue;
                    if (primaryTypes == null) {
                        primaryTypes = new StringBuilder();
                    } else {
                        primaryTypes.append(',');
                    }
                    assert prefixesByUris.containsKey(thisOrSupertype.getInternalName().getNamespaceUri());
                    String name = thisOrSupertype.getInternalName().getString(registry);
                    primaryTypes.append('[').append(name).append(']');
                    ++count;
                }
                assert primaryTypes != null; // should at least include itself
                assert count > 0;
                viewDefinition.append('[').append(JcrLexicon.PRIMARY_TYPE.getString(registry)).append(']');
                if (count == 1) {
                    viewDefinition.append('=').append(primaryTypes);
                } else {
                    viewDefinition.append(" IN (").append(primaryTypes).append(')');
                }
            }
        }

        // Define the view ...
        builder.addView(tableName, viewDefinition.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.validate.Schemata#getTable(org.modeshape.graph.query.model.SelectorName)
     */
    public Table getTable( SelectorName name ) {
        return schemata.getTable(name);
    }

    /**
     * Get a schemata instance that works with the suppplied session and that uses the session-specific namespace mappings. Note
     * that the resulting instance does not change as the session's namespace mappings are changed, so when that happens the
     * JcrSession must call this method again to obtain a new schemata.
     * 
     * @param session the session; may not be null
     * @return the schemata that can be used for the session; never null
     */
    public Schemata getSchemataForSession( JcrSession session ) {
        assert session != null;
        // If the session does not override any namespace mappings used in this schemata ...
        if (!overridesNamespaceMappings(session)) {
            // Then we can just use this schemata instance ...
            return this;
        }

        // Otherwise, the session has some custom namespace mappings, so we need to return a session-specific instance...
        return new SessionSchemata(session);
    }

    /**
     * Determine if the session overrides any namespace mappings used by this schemata.
     * 
     * @param session the session; may not be null
     * @return true if the session overrides one or more namespace mappings used in this schemata, or false otherwise
     */
    private boolean overridesNamespaceMappings( JcrSession session ) {
        NamespaceRegistry registry = session.getExecutionContext().getNamespaceRegistry();
        if (registry instanceof LocalNamespaceRegistry) {
            Set<Namespace> localNamespaces = ((LocalNamespaceRegistry)registry).getLocalNamespaces();
            if (localNamespaces.isEmpty()) {
                // There are no local mappings ...
                return false;
            }
            for (Namespace namespace : localNamespaces) {
                if (prefixesByUris.containsKey(namespace.getNamespaceUri())) return true;
            }
            // None of the local namespace mappings overrode any namespaces used by this schemata ...
            return false;
        }
        // We can't find the local mappings, so brute-force it ...
        for (Namespace namespace : registry.getNamespaces()) {
            String expectedPrefix = prefixesByUris.get(namespace.getNamespaceUri());
            if (expectedPrefix == null) {
                // This namespace is not used by this schemata ...
                continue;
            }
            if (!namespace.getPrefix().equals(expectedPrefix)) return true;
        }
        return false;
    }

    /**
     * Implementation class that builds the tables lazily.
     */
    @NotThreadSafe
    protected class SessionSchemata implements Schemata {
        private final JcrSession session;
        private final ExecutionContext context;
        private final ImmutableSchemata.Builder builder;
        private final NameFactory nameFactory;
        private Schemata schemata;

        protected SessionSchemata( JcrSession session ) {
            this.session = session;
            this.context = this.session.getExecutionContext();
            this.nameFactory = context.getValueFactories().getNameFactory();
            this.builder = ImmutableSchemata.createBuilder(context.getValueFactories().getTypeSystem());
            // Add the "AllNodes" table ...
            addAllNodesTable(builder, null, context);
            this.schemata = builder.build();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.validate.Schemata#getTable(org.modeshape.graph.query.model.SelectorName)
         */
        public Table getTable( SelectorName name ) {
            Table table = schemata.getTable(name);
            if (table == null) {
                // Try getting it ...
                Name nodeTypeName = nameFactory.create(name.getName());
                JcrNodeType nodeType = getNodeType(nodeTypeName);
                if (nodeType == null) return null;
                addView(builder, context, nodeType);
                schemata = builder.build();
            }
            return schemata.getTable(name);
        }
    }

}
