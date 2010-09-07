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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.Base64;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Content handler that provides SAX-based event handling that maps incoming documents to the repository based on the
 * functionality described in section 7.3 of the JCR 1.0.1 specification.
 * <p>
 * Each content handler is only intended to be used once and discarded. This class is <b>NOT</b> thread-safe.
 * </p>
 * 
 * @see JcrSession#getImportContentHandler(String, int)
 * @see JcrWorkspace#getImportContentHandler(String, int)
 */
@NotThreadSafe
class JcrContentHandler extends DefaultHandler {

    /**
     * Encoder to properly escape XML names.
     * 
     * @see XmlNameEncoder
     */
    protected static final TextDecoder SYSTEM_VIEW_NAME_DECODER = new XmlNameEncoder();

    protected static final TextDecoder DOCUMENT_VIEW_NAME_DECODER = new JcrDocumentViewExporter.JcrDocumentViewPropertyEncoder();

    private static final String ALT_XML_SCHEMA_NAMESPACE_PREFIX = "xsd";
    private final NameFactory nameFactory;
    private final PathFactory pathFactory;
    private final org.modeshape.graph.property.ValueFactory<String> stringFactory;
    private final NamespaceRegistry namespaces;
    private final ValueFactory jcrValueFactory;
    private final JcrNodeTypeManager nodeTypes;
    private final javax.jcr.NamespaceRegistry jcrNamespaceRegistry;
    private final SaveMode saveMode;
    protected final int uuidBehavior;

    protected final String primaryTypeName;
    protected final String mixinTypesName;
    protected final String uuidName;

    private AbstractJcrNode currentNode;
    private ContentHandler delegate;
    protected final List<AbstractJcrProperty> refPropsRequiringConstraintValidation = new LinkedList<AbstractJcrProperty>();

    private SessionCache cache;

    enum SaveMode {
        WORKSPACE,
        SESSION
    }

    JcrContentHandler( JcrSession session,
                       Path parentPath,
                       int uuidBehavior,
                       SaveMode saveMode ) throws PathNotFoundException, RepositoryException {
        assert session != null;
        assert parentPath != null;
        assert uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW
               || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING
               || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING
               || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;

        ExecutionContext context = session.getExecutionContext();
        this.namespaces = context.getNamespaceRegistry();
        this.nameFactory = context.getValueFactories().getNameFactory();
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.uuidBehavior = uuidBehavior;

        this.saveMode = saveMode;
        switch (this.saveMode) {
            case SESSION:
                cache = session.cache();
                break;
            case WORKSPACE:
                cache = new SessionCache(session);
                break;
        }
        assert cache != null;

        try {
            this.currentNode = cache.findJcrNode(null, parentPath);
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e.getLocalizedMessage(), e);
        }

        if (!currentNode.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(currentNode.getPath()));
        }

        this.jcrValueFactory = session.getValueFactory();
        this.nodeTypes = session.nodeTypeManager();
        this.jcrNamespaceRegistry = session.workspace().getNamespaceRegistry();

        this.primaryTypeName = JcrLexicon.PRIMARY_TYPE.getString(this.namespaces);
        this.mixinTypesName = JcrLexicon.MIXIN_TYPES.getString(this.namespaces);
        this.uuidName = JcrLexicon.UUID.getString(this.namespaces);
    }

    protected final NamespaceRegistry namespaces() {
        return namespaces;
    }

    protected final JcrNodeTypeManager nodeTypes() {
        return nodeTypes;
    }

    protected final JcrNodeType nodeTypeFor( String name ) {
        return nodeTypes.getNodeType(nameFor(name));
    }

    protected final String stringFor( Object name ) {
        return stringFactory.create(name);
    }

    protected final Name nameFor( String name ) {
        return nameFactory.create(name);
    }

    protected final Path pathFor( Name... names ) {
        return pathFor(pathFactory.createRootPath(), names);
    }

    protected final Path pathFor( Path parentPath,
                                  Name... names ) {
        return pathFactory.create(parentPath, names);
    }

    protected final Value valueFor( String value,
                                    int type ) throws ValueFormatException {
        return jcrValueFactory.createValue(value, type);
    }

    protected final Value valueFor( InputStream stream ) throws RepositoryException {
        return jcrValueFactory.createValue(jcrValueFactory.createBinary(stream));
    }

    protected final SessionCache cache() {
        return cache;
    }

    protected void validateReferenceConstraints() throws SAXException {
        if (refPropsRequiringConstraintValidation.isEmpty()) return;
        try {
            for (AbstractJcrProperty refProp : refPropsRequiringConstraintValidation) {
                JcrPropertyDefinition defn = refProp.getDefinition();
                if (refProp.isMultiple()) {
                    for (Value value : refProp.getValues()) {
                        if (!defn.canCastToTypeAndSatisfyConstraints(value)) {
                            String name = stringFor(refProp.name());
                            throw new ConstraintViolationException(JcrI18n.constraintViolatedOnReference.text(name, defn));
                        }
                    }
                } else {
                    Value value = refProp.getValue();
                    if (!defn.canCastToTypeAndSatisfyConstraints(value)) {
                        String name = stringFor(refProp.name());
                        throw new ConstraintViolationException(JcrI18n.constraintViolatedOnReference.text(name, defn));
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new EnclosingSAXException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters( char[] ch,
                            int start,
                            int length ) throws SAXException {
        assert this.delegate != null;
        delegate.characters(ch, start, length);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
        validateReferenceConstraints();
        if (saveMode == SaveMode.WORKSPACE) {
            try {
                cache.save();
            } catch (RepositoryException e) {
                throw new EnclosingSAXException(e);
            }
        }
        super.endDocument();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement( String uri,
                            String localName,
                            String name ) throws SAXException {
        assert this.delegate != null;
        delegate.endElement(uri, localName, name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement( String uri,
                              String localName,
                              String name,
                              Attributes atts ) throws SAXException {
        checkDelegate(uri);
        assert this.delegate != null;

        delegate.startElement(uri, localName, name, atts);
    }

    private void checkDelegate( String namespaceUri ) {
        if (delegate != null) return;

        if (JcrSvLexicon.Namespace.URI.equals(namespaceUri)) {
            this.delegate = new SystemViewContentHandler(this.currentNode);
        } else {
            this.delegate = new DocumentViewContentHandler(this.currentNode);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    @Override
    public void startPrefixMapping( String prefix,
                                    String uri ) throws SAXException {
        try {
            if (ALT_XML_SCHEMA_NAMESPACE_PREFIX.equals(prefix) && uri.equals(JcrNamespaceRegistry.XML_SCHEMA_NAMESPACE_URI)) {
                prefix = JcrNamespaceRegistry.XML_SCHEMA_NAMESPACE_PREFIX;
            }

            // Read from the workspace's ModeShape registry, as its semantics are more friendly
            String existingUri = namespaces.getNamespaceForPrefix(prefix);

            if (existingUri != null) {
                if (existingUri.equals(uri)) {
                    // prefix/uri mapping is already in registry
                    return;
                }
                throw new RepositoryException("Prefix " + prefix + " is already permanently mapped");
            }
            // Register through the JCR workspace to ensure consistency
            this.jcrNamespaceRegistry.registerNamespace(prefix, uri);
        } catch (RepositoryException re) {
            throw new EnclosingSAXException(re);
        }
    }

    class EnclosingSAXException extends SAXException {

        /**
         */
        private static final long serialVersionUID = -1044992767566435542L;

        /**
         * @param e
         */
        EnclosingSAXException( Exception e ) {
            super(e);

        }

    }

    // ----------------------------------------------------------------------------------------------------------------
    // NodeHandler framework ...
    // ----------------------------------------------------------------------------------------------------------------

    @SuppressWarnings( "unused" )
    protected abstract class NodeHandler {
        public void finish() throws SAXException {
        }

        public AbstractJcrNode node() throws SAXException {
            return null;
        }

        public NodeHandler parentHandler() {
            return null;
        }

        public void addPropertyValue( Name name,
                                      String value,
                                      int propertyType,
                                      TextDecoder decoder ) throws EnclosingSAXException {
        }

        protected String name() {
            try {
                Path path = node().path();
                return path.isRoot() ? "" : stringFor(path.getLastSegment());
            } catch (Exception e) {
                throw new SystemFailureException(e);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            NodeHandler parent = parentHandler();
            if (parent != null) {
                return parent.toString() + "/" + name();
            }
            try {
                return node().getPath();
            } catch (Throwable e) {
                try {
                    return node().toString();
                } catch (SAXException e2) {
                    throw new SystemFailureException(e2);
                }
            }
        }
    }

    /**
     * The set of properties that should be skipped on import. Currently, this list includes all properties of "mix:lockable",
     * since upon import no node should be locked.
     */
    protected static final Set<Name> PROPERTIES_TO_SKIP_ON_IMPORT = Collections.unmodifiableSet(JcrLexicon.LOCK_IS_DEEP,
                                                                                                JcrLexicon.LOCK_OWNER);

    // JcrLexicon.VERSION_HISTORY,
    // JcrLexicon.PREDECESSORS,
    // JcrLexicon.MERGE_FAILED,
    // JcrLexicon.BASE_VERSION,
    // JcrLexicon.IS_CHECKED_OUT,
    // );

    protected class BasicNodeHandler extends NodeHandler {
        private final Map<Name, List<Value>> properties;
        private final Name nodeName;
        private NodeHandler parentHandler;
        private AbstractJcrNode node;
        private final int uuidBehavior;

        protected BasicNodeHandler( Name name,
                                    NodeHandler parentHandler,
                                    int uuidBehavior ) {
            this.nodeName = name;
            this.parentHandler = parentHandler;
            this.properties = new HashMap<Name, List<Value>>();
            this.uuidBehavior = uuidBehavior;
        }

        @Override
        public void finish() throws SAXException {
            node();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.JcrContentHandler.NodeHandler#name()
         */
        @Override
        protected String name() {
            return stringFor(nodeName);
        }

        @Override
        public AbstractJcrNode node() throws SAXException {
            if (node == null) create();
            assert node != null;
            return node;
        }

        @Override
        public NodeHandler parentHandler() {
            return parentHandler;
        }

        protected boolean shouldNotImportProperty( Name propertyName ) {
            return false;
            // return PROPERTIES_TO_SKIP_ON_IMPORT.contains(propertyName);
        }

        @Override
        public void addPropertyValue( Name name,
                                      String value,
                                      int propertyType,
                                      TextDecoder decoder ) throws EnclosingSAXException {
            try {
                if (node != null) {
                    if (JcrLexicon.PRIMARY_TYPE.equals(name)) return;
                    if (JcrLexicon.MIXIN_TYPES.equals(name)) return;
                    if (JcrLexicon.UUID.equals(name)) return;
                    if (shouldNotImportProperty(name)) return; // ignore some properties

                    // The node was already created, so set the property using the editor ...
                    node.editor().setProperty(name, (JcrValue)valueFor(value, propertyType));
                } else {
                    // The node hasn't been created yet, so just enqueue the property value into the map ...
                    List<Value> values = properties.get(name);
                    if (values == null) {
                        values = new ArrayList<Value>();
                        properties.put(name, values);
                    }
                    if (propertyType == PropertyType.BINARY) {
                        ByteArrayInputStream is = new ByteArrayInputStream(Base64.decode(value, Base64.URL_SAFE));
                        values.add(valueFor(is));
                    } else {
                        if (decoder != null) value = decoder.decode(value);
                        if (value != null && propertyType == PropertyType.STRING) {
                            // Strings and binaries can be empty -- other data types cannot
                            values.add(valueFor(value, propertyType));
                        } else if (value != null && value.length() > 0) {
                            values.add(valueFor(value, propertyType));
                        }
                    }
                }
            } catch (IOException ioe) {
                throw new EnclosingSAXException(ioe);
            } catch (RepositoryException re) {
                throw new EnclosingSAXException(re);
            }
        }

        protected void create() throws SAXException {
            try {
                AbstractJcrNode parent = parentHandler.node();
                assert parent != null;

                // Figure out the UUID for the node ...
                UUID uuid = null;
                List<Value> rawUuid = properties.get(JcrLexicon.UUID);
                if (rawUuid != null) {
                    assert rawUuid.size() == 1;
                    uuid = UUID.fromString(rawUuid.get(0).getString());

                    try {
                        // Deal with any existing node ...
                        AbstractJcrNode existingNodeWithUuid = cache().findJcrNode(Location.create(uuid));
                        switch (uuidBehavior) {
                            case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING:
                                parent = existingNodeWithUuid.getParent();
                                existingNodeWithUuid.remove();
                                break;
                            case ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW:
                                uuid = UUID.randomUUID();
                                break;
                            case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING:
                                if (existingNodeWithUuid.path().isAtOrAbove(parent.path())) {
                                    throw new ConstraintViolationException(
                                                                           JcrI18n.cannotRemoveParentNodeOfTarget.text(existingNodeWithUuid.getPath(),
                                                                                                                       uuid,
                                                                                                                       parent.getPath()));
                                }
                                existingNodeWithUuid.remove();
                                break;
                            case ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW:
                                throw new ItemExistsException(
                                                              JcrI18n.itemAlreadyExistsWithUuid.text(uuid,
                                                                                                     cache().session()
                                                                                                            .workspace()
                                                                                                            .getName(),
                                                                                                     existingNodeWithUuid.getPath()));
                        }
                    } catch (ItemNotFoundException e) {
                        // there wasn't an existing item, so just continue
                    }

                }

                // See if the node was already autocreated by the parent
                AbstractJcrNode existingNode = parent.getNode(nodeName);
                boolean nodeAlreadyExists = existingNode != null && existingNode.getDefinition().isAutoCreated();

                // Create the new node ...
                AbstractJcrNode child;
                if (!nodeAlreadyExists) {
                    List<Value> primaryTypeValueList = properties.get(JcrLexicon.PRIMARY_TYPE);
                    String typeName = primaryTypeValueList != null ? primaryTypeValueList.get(0).getString() : null;
                    Name primaryTypeName = nameFor(typeName);
                    if (JcrNtLexicon.SHARE.equals(primaryTypeName) && uuid != null) {
                        // Per Section 14.7 and 14.8 of the JCR 2.0 specification, shared nodes are imported in a special way ...
                        child = parent.editor().createChild(nodeName, UUID.randomUUID(), ModeShapeLexicon.SHARE);
                        SessionCache.NodeEditor newNodeEditor = child.editor();
                        JcrValue uuidValue = (JcrValue)valueFor(uuid.toString(), PropertyType.STRING);
                        newNodeEditor.setProperty(ModeShapeLexicon.SHARED_UUID, uuidValue, false, true);
                        node = child;
                        return;
                    }
                    // Otherwise, it's just a regular node...
                    child = parent.editor().createChild(nodeName, uuid, primaryTypeName);
                } else {
                    child = existingNode;
                }

                // Set the properties on the new node ...
                SessionCache.NodeEditor newNodeEditor = child.editor();

                // Set the mixin types first (before we set any properties that may require the mixins to be present) ...
                List<Value> mixinTypeValueList = properties.get(JcrLexicon.MIXIN_TYPES);
                if (mixinTypeValueList != null) {
                    for (Value value : mixinTypeValueList) {
                        JcrNodeType mixinType = nodeTypeFor(value.getString());
                        newNodeEditor.addMixin(mixinType);
                    }
                }

                boolean skipProtected = false;
                for (Map.Entry<Name, List<Value>> entry : properties.entrySet()) {
                    Name propertyName = entry.getKey();

                    // These are all handled earlier ...
                    if (JcrLexicon.PRIMARY_TYPE.equals(propertyName)) {
                        continue;
                    }
                    if (JcrLexicon.MIXIN_TYPES.equals(propertyName)) {
                        continue;
                    }
                    if (JcrLexicon.UUID.equals(propertyName)) {
                        continue;
                    }

                    // Should we ignore this property?
                    if (shouldNotImportProperty(propertyName)) {
                        continue;
                    }

                    List<Value> values = entry.getValue();

                    if (values.size() == 1) {
                        AbstractJcrProperty prop = newNodeEditor.setProperty(propertyName,
                                                                             (JcrValue)values.get(0),
                                                                             skipProtected,
                                                                             false);
                        if (prop.getType() == PropertyType.REFERENCE && prop.getDefinition().getValueConstraints().length != 0) {
                            // This reference needs to be validated after all nodes have been imported ...
                            refPropsRequiringConstraintValidation.add(prop);
                        }
                    } else {
                        newNodeEditor.setProperty(propertyName,
                                                  values.toArray(new Value[values.size()]),
                                                  PropertyType.UNDEFINED,
                                                  skipProtected);
                    }
                }

                node = child;
            } catch (RepositoryException re) {
                throw new EnclosingSAXException(re);
            }
        }
    }

    protected class ExistingNodeHandler extends NodeHandler {
        private final AbstractJcrNode node;
        private final NodeHandler parentHandler;

        protected ExistingNodeHandler( AbstractJcrNode node,
                                       NodeHandler parentHandler ) {
            this.node = node;
            this.parentHandler = parentHandler;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.JcrContentHandler.NodeHandler#node()
         */
        @Override
        public AbstractJcrNode node() {
            return node;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.JcrContentHandler.NodeHandler#parentHandler()
         */
        @Override
        public NodeHandler parentHandler() {
            return parentHandler;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.JcrContentHandler.NodeHandler#addPropertyValue(Name, String, int, TextDecoder)
         */
        @Override
        public void addPropertyValue( Name propertyName,
                                      String value,
                                      int propertyType,
                                      TextDecoder decoder ) {
            throw new UnsupportedOperationException();
        }
    }

    protected class JcrRootHandler extends ExistingNodeHandler {
        protected JcrRootHandler( AbstractJcrNode root ) {
            super(root, null);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.JcrContentHandler.NodeHandler#addPropertyValue(Name, String, int, TextDecoder)
         */
        @Override
        public void addPropertyValue( Name propertyName,
                                      String value,
                                      int propertyType,
                                      TextDecoder decoder ) {
            // do nothing ...
        }
    }

    protected class IgnoreBranchHandler extends NodeHandler {
        private NodeHandler parentHandler;

        protected IgnoreBranchHandler( NodeHandler parentHandler ) {
            this.parentHandler = parentHandler;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.JcrContentHandler.NodeHandler#parentHandler()
         */
        @Override
        public NodeHandler parentHandler() {
            return parentHandler;
        }
    }

    protected class JcrSystemHandler extends IgnoreBranchHandler {

        protected JcrSystemHandler( NodeHandler parentHandler ) {
            super(parentHandler);
        }
    }

    protected interface NodeHandlerFactory {
        NodeHandler createFor( Name nodeName,
                               NodeHandler parentHandler,
                               int uuidBehavior ) throws SAXException;
    }

    protected class StandardNodeHandlerFactory implements NodeHandlerFactory {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.JcrContentHandler.NodeHandlerFactory#createFor(org.modeshape.graph.property.Name,
         *      org.modeshape.jcr.JcrContentHandler.NodeHandler,int)
         */
        @Override
        public NodeHandler createFor( Name name,
                                      NodeHandler parentHandler,
                                      int uuidBehavior ) throws SAXException {
            if (parentHandler instanceof IgnoreBranchHandler) {
                return new IgnoreBranchHandler(parentHandler);
            }
            if (JcrLexicon.ROOT.equals(name)) {
                try {
                    JcrRootNode rootNode = cache().findJcrRootNode();
                    return new JcrRootHandler(rootNode);
                } catch (RepositoryException re) {
                    throw new EnclosingSAXException(re);
                }
            }
            if (JcrLexicon.SYSTEM.equals(name)) {
                // Always do this, regardless of where the "jcr:system" branch is located ...
                return new JcrSystemHandler(parentHandler);
            }
            return new BasicNodeHandler(name, parentHandler, uuidBehavior);
        }
    }

    private class SystemViewContentHandler extends DefaultHandler {
        private final String svNameName;
        private final String svTypeName;
        private NodeHandler current;
        private final NodeHandlerFactory nodeHandlerFactory;
        private String currentPropertyName;
        private int currentPropertyType;
        private StringBuilder currentPropertyValue;

        SystemViewContentHandler( AbstractJcrNode parent ) {
            super();
            this.svNameName = JcrSvLexicon.NAME.getString(namespaces());
            this.svTypeName = JcrSvLexicon.TYPE.getString(namespaces());
            this.current = new ExistingNodeHandler(parent, null);
            this.nodeHandlerFactory = new StandardNodeHandlerFactory();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
         *      org.xml.sax.Attributes)
         */
        @Override
        public void startElement( String uri,
                                  String localName,
                                  String name,
                                  Attributes atts ) throws SAXException {
            // Always create a new string buffer for the content value, because we're starting a new element ...
            currentPropertyValue = new StringBuilder();

            if ("node".equals(localName)) {
                // Finish the parent handler ...
                current.finish();
                // Create a new handler for this element ...
                String nodeName = atts.getValue(SYSTEM_VIEW_NAME_DECODER.decode(svNameName));
                current = nodeHandlerFactory.createFor(nameFor(nodeName), current, uuidBehavior);
            } else if ("property".equals(localName)) {
                currentPropertyName = atts.getValue(SYSTEM_VIEW_NAME_DECODER.decode(svNameName));
                currentPropertyType = PropertyType.valueFromName(atts.getValue(svTypeName));
            } else if (!"value".equals(localName)) {
                throw new IllegalStateException("Unexpected element '" + name + "' in system view");
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        @Override
        public void characters( char[] ch,
                                int start,
                                int length ) {
            currentPropertyValue.append(ch, start, length);
        }

        @Override
        public void endElement( String uri,
                                String localName,
                                String name ) throws SAXException {
            if ("node".equals(localName)) {
                current.finish(); // make sure the node is created
                current = current.parentHandler();
            } else if ("value".equals(localName)) {
                // Add the content for the current property ...
                String currentPropertyString = currentPropertyValue.toString();
                current.addPropertyValue(nameFor(currentPropertyName),
                                         currentPropertyString,
                                         currentPropertyType,
                                         SYSTEM_VIEW_NAME_DECODER);
            } else if ("property".equals(localName)) {
            } else {
                throw new IllegalStateException("Unexpected element '" + name + "' in system view");
            }
            currentPropertyValue = new StringBuilder();
        }
    }

    private class DocumentViewContentHandler extends DefaultHandler {
        private NodeHandler current;
        private final NodeHandlerFactory nodeHandlerFactory;

        /**
         * @param currentNode
         */
        DocumentViewContentHandler( AbstractJcrNode currentNode ) {
            super();
            this.current = new ExistingNodeHandler(currentNode, null);
            this.nodeHandlerFactory = new StandardNodeHandlerFactory();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
         *      org.xml.sax.Attributes)
         */
        @Override
        public void startElement( String uri,
                                  String localName,
                                  String name,
                                  Attributes atts ) throws SAXException {
            // Create the new handler for the new node ...
            String nodeName = DOCUMENT_VIEW_NAME_DECODER.decode(name);
            current = nodeHandlerFactory.createFor(nameFor(nodeName), current, uuidBehavior);

            // Add the properties ...
            for (int i = 0; i < atts.getLength(); i++) {
                String value = atts.getValue(i);
                String propertyName = DOCUMENT_VIEW_NAME_DECODER.decode(atts.getQName(i));
                current.addPropertyValue(nameFor(propertyName), value, PropertyType.STRING, null);// don't decode the value
            }

            // Now create the node ...
            current.finish();
        }

        @Override
        public void endElement( String uri,
                                String localName,
                                String name ) throws SAXException {
            current.finish();
            current = current.parentHandler();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        @Override
        public void characters( char[] ch,
                                int start,
                                int length ) throws SAXException {
            String value = new String(ch, start, length);
            // Create a 'jcr:xmltext' child node with a single 'jcr:xmlcharacters' property ...
            current = nodeHandlerFactory.createFor(JcrLexicon.XMLTEXT, current, uuidBehavior);
            current.addPropertyValue(JcrLexicon.PRIMARY_TYPE,
                                     stringFor(JcrNtLexicon.UNSTRUCTURED),
                                     PropertyType.NAME,
                                     DOCUMENT_VIEW_NAME_DECODER);
            current.addPropertyValue(JcrLexicon.XMLCHARACTERS, value, PropertyType.STRING, null);// don't decode value
            current.finish();
            // Pop the stack ...
            current = current.parentHandler();
        }
    }
}
