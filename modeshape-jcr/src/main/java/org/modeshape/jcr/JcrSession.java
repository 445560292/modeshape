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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Credentials;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.VersionException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.SecurityContext;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.query.QueryBuilder;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.session.GraphSession;
import org.modeshape.jcr.JcrContentHandler.EnclosingSAXException;
import org.modeshape.jcr.JcrContentHandler.SaveMode;
import org.modeshape.jcr.JcrNamespaceRegistry.Behavior;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;
import org.modeshape.jcr.WorkspaceLockManager.ModeShapeLock;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * The ModeShape implementation of a {@link Session JCR Session}.
 */
@NotThreadSafe
class JcrSession implements Session {

    private static final String[] NO_ATTRIBUTES_NAMES = new String[] {};

    /**
     * The repository that created this session.
     */
    private final JcrRepository repository;

    /**
     * The workspace that corresponds to this session.
     */
    private final JcrWorkspace workspace;

    /**
     * A JCR namespace registry that is specific to this session, with any locally-defined namespaces defined in this session.
     * This is backed by the workspace's namespace registry.
     */
    private final JcrNamespaceRegistry sessionRegistry;

    /**
     * The execution context for this session, which uses the {@link #sessionRegistry session's namespace registry}
     */
    protected final ExecutionContext executionContext;

    /**
     * The session-specific attributes that came from the {@link SimpleCredentials}' {@link SimpleCredentials#getAttributeNames()}
     */
    private final Map<String, Object> sessionAttributes;

    /**
     * The graph representing this session, which uses the {@link #graph session's graph}.
     */
    private final Graph graph;

    private final SessionCache cache;

    private final Set<String> lockTokens;

    /**
     * A cached instance of the root path.
     */
    private final Path rootPath;

    private boolean isLive;

    private final boolean performReferentialIntegrityChecks;
    /**
     * The locations of the nodes that were (transiently) removed in this session and not yet saved.
     */
    private Set<Location> removedNodes = null;
    /**
     * The UUIDs of the mix:referenceable nodes that were (transiently) removed in this session and not yet saved.
     */
    private Set<String> removedReferenceableNodeUuids = null;

    JcrSession( JcrRepository repository,
                JcrWorkspace workspace,
                ExecutionContext sessionContext,
                NamespaceRegistry globalNamespaceRegistry,
                Map<String, Object> sessionAttributes ) {
        assert repository != null;
        assert workspace != null;
        assert sessionAttributes != null;
        assert sessionContext != null;
        this.repository = repository;
        this.sessionAttributes = sessionAttributes;
        this.workspace = workspace;

        // Create an execution context for this session, which should use the local namespace registry ...
        this.executionContext = sessionContext;
        NamespaceRegistry local = sessionContext.getNamespaceRegistry();
        this.sessionRegistry = new JcrNamespaceRegistry(Behavior.JSR170_SESSION, local, globalNamespaceRegistry, this);
        this.rootPath = this.executionContext.getValueFactories().getPathFactory().createRootPath();

        // Set up the graph to use for this session (which uses the session's namespace registry and context) ...
        this.graph = workspace.graph();

        this.cache = new SessionCache(this);
        this.isLive = true;
        this.lockTokens = new HashSet<String>();

        this.performReferentialIntegrityChecks = Boolean.valueOf(repository.getOptions()
                                                                           .get(Option.PERFORM_REFERENTIAL_INTEGRITY_CHECKS))
                                                        .booleanValue();

        assert this.sessionAttributes != null;
        assert this.workspace != null;
        assert this.repository != null;
        assert this.executionContext != null;
        assert this.sessionRegistry != null;
        assert this.graph != null;
        assert this.executionContext.getSecurityContext() != null;
    }

    // Added to facilitate mock testing of items without necessarily requiring an entire repository structure to be built
    final SessionCache cache() {
        return this.cache;
    }

    ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    String sessionId() {
        return this.executionContext.getId();
    }

    JcrNodeTypeManager nodeTypeManager() {
        return this.workspace.nodeTypeManager();
    }

    NamespaceRegistry namespaces() {
        return this.executionContext.getNamespaceRegistry();
    }

    void signalNamespaceChanges( boolean global ) {
        nodeTypeManager().signalNamespaceChanges();
        if (global) repository.getRepositoryTypeManager().signalNamespaceChanges();
    }

    JcrWorkspace workspace() {
        return this.workspace;
    }

    JcrRepository repository() {
        return this.repository;
    }

    final Collection<String> lockTokens() {
        return lockTokens;
    }

    Graph.Batch createBatch() {
        return graph.batch();
    }

    Graph graph() {
        return graph;
    }

    String sourceName() {
        return this.repository.getRepositorySourceName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getWorkspace()
     */
    public Workspace getWorkspace() {
        return this.workspace;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getRepository()
     */
    public Repository getRepository() {
        return this.repository;
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>null</code>
     * @see javax.jcr.Session#getAttribute(java.lang.String)
     */
    public Object getAttribute( String name ) {
        return sessionAttributes.get(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @return An empty array
     * @see javax.jcr.Session#getAttributeNames()
     */
    public String[] getAttributeNames() {
        Set<String> names = sessionAttributes.keySet();
        if (names.isEmpty()) return NO_ATTRIBUTES_NAMES;
        return names.toArray(new String[names.size()]);
    }

    /**
     * @return a copy of the session attributes for this session
     */
    Map<String, Object> sessionAttributes() {
        return new HashMap<String, Object>(sessionAttributes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespacePrefix(java.lang.String)
     */
    public String getNamespacePrefix( String uri ) throws RepositoryException {
        return sessionRegistry.getPrefix(uri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespacePrefixes()
     */
    public String[] getNamespacePrefixes() {
        return sessionRegistry.getPrefixes();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespaceURI(java.lang.String)
     */
    public String getNamespaceURI( String prefix ) throws RepositoryException {
        return sessionRegistry.getURI(prefix);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#setNamespacePrefix(java.lang.String, java.lang.String)
     */
    public void setNamespacePrefix( String newPrefix,
                                    String existingUri ) throws NamespaceException, RepositoryException {
        sessionRegistry.registerNamespace(newPrefix, existingUri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#addLockToken(java.lang.String)
     */
    public void addLockToken( String lt ) throws LockException {
        CheckArg.isNotNull(lt, "lock token");

        // Trivial case of giving a token back to ourself
        if (lockTokens.contains(lt)) {
            return;
        }

        if (workspace().lockManager().isHeldBySession(this, lt)) {
            throw new LockException(JcrI18n.lockTokenAlreadyHeld.text(lt));
        }

        workspace().lockManager().setHeldBySession(this, lt, true);
        lockTokens.add(lt);
    }

    /**
     * Returns whether the authenticated user has the given role.
     * 
     * @param roleName the name of the role to check
     * @param workspaceName the workspace under which the user must have the role. This may be different from the current
     *        workspace.
     * @return true if the user has the role and is logged in; false otherwise
     */
    final boolean hasRole( String roleName,
                           String workspaceName ) {
        SecurityContext context = getExecutionContext().getSecurityContext();

        return context.hasRole(roleName) || context.hasRole(roleName + "." + this.repository.getRepositorySourceName())
               || context.hasRole(roleName + "." + this.repository.getRepositorySourceName() + "." + workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if either <code>path</code> or <code>actions</code> is empty or <code>null</code>.
     * @see javax.jcr.Session#checkPermission(java.lang.String, java.lang.String)
     */
    public void checkPermission( String path,
                                 String actions ) {
        CheckArg.isNotEmpty(path, "path");

        this.checkPermission(executionContext.getValueFactories().getPathFactory().create(path), actions);
    }

    /**
     * Throws an {@link AccessControlException} if the current user does not have permission for all of the named actions in the
     * current workspace, otherwise returns silently.
     * <p>
     * The {@code path} parameter is included for future use and is currently ignored
     * </p>
     * 
     * @param path the path on which the actions are occurring
     * @param actions a comma-delimited list of actions to check
     */
    void checkPermission( Path path,
                          String actions ) {
        checkPermission(this.workspace().getName(), path, actions);
    }

    /**
     * Throws an {@link AccessControlException} if the current user does not have permission for all of the named actions in the
     * named workspace, otherwise returns silently.
     * <p>
     * The {@code path} parameter is included for future use and is currently ignored
     * </p>
     * 
     * @param workspaceName the name of the workspace in which the path exists
     * @param path the path on which the actions are occurring
     * @param actions a comma-delimited list of actions to check
     */
    void checkPermission( String workspaceName,
                          Path path,
                          String actions ) {

        CheckArg.isNotEmpty(actions, "actions");

        boolean hasPermission = true;
        for (String action : actions.split(",")) {
            if (ModeShapePermissions.READ.equals(action)) {
                hasPermission &= hasRole(ModeShapeRoles.READONLY, workspaceName)
                                 || hasRole(ModeShapeRoles.READWRITE, workspaceName)
                                 || hasRole(ModeShapeRoles.ADMIN, workspaceName);
            } else if (ModeShapePermissions.REGISTER_NAMESPACE.equals(action)
                       || ModeShapePermissions.REGISTER_TYPE.equals(action) || ModeShapePermissions.UNLOCK_ANY.equals(action)) {
                hasPermission &= hasRole(ModeShapeRoles.ADMIN, workspaceName);
            } else {
                hasPermission &= hasRole(ModeShapeRoles.ADMIN, workspaceName) || hasRole(ModeShapeRoles.READWRITE, workspaceName);
            }
        }

        if (hasPermission) return;

        String pathAsString = path != null ? path.getString(this.namespaces()) : "<unknown>";
        throw new AccessControlException(JcrI18n.permissionDenied.text(pathAsString, actions));

    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportDocumentView(java.lang.String, org.xml.sax.ContentHandler, boolean, boolean)
     */
    public void exportDocumentView( String absPath,
                                    ContentHandler contentHandler,
                                    boolean skipBinary,
                                    boolean noRecurse ) throws RepositoryException, SAXException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(contentHandler, "contentHandler");

        Path exportRootPath = executionContext.getValueFactories().getPathFactory().create(absPath);
        Node exportRootNode = getNode(exportRootPath);

        AbstractJcrExporter exporter = new JcrDocumentViewExporter(this);

        exporter.exportView(exportRootNode, contentHandler, skipBinary, noRecurse);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportDocumentView(java.lang.String, java.io.OutputStream, boolean, boolean)
     */
    public void exportDocumentView( String absPath,
                                    OutputStream out,
                                    boolean skipBinary,
                                    boolean noRecurse ) throws RepositoryException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(out, "out");

        Path exportRootPath = executionContext.getValueFactories().getPathFactory().create(absPath);
        Node exportRootNode = getNode(exportRootPath);

        AbstractJcrExporter exporter = new JcrDocumentViewExporter(this);

        exporter.exportView(exportRootNode, out, skipBinary, noRecurse);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportSystemView(java.lang.String, org.xml.sax.ContentHandler, boolean, boolean)
     */
    public void exportSystemView( String absPath,
                                  ContentHandler contentHandler,
                                  boolean skipBinary,
                                  boolean noRecurse ) throws RepositoryException, SAXException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(contentHandler, "contentHandler");

        Path exportRootPath = executionContext.getValueFactories().getPathFactory().create(absPath);
        Node exportRootNode = getNode(exportRootPath);

        AbstractJcrExporter exporter = new JcrSystemViewExporter(this);

        exporter.exportView(exportRootNode, contentHandler, skipBinary, noRecurse);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportSystemView(java.lang.String, java.io.OutputStream, boolean, boolean)
     */
    public void exportSystemView( String absPath,
                                  OutputStream out,
                                  boolean skipBinary,
                                  boolean noRecurse ) throws RepositoryException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(out, "out");

        Path exportRootPath = executionContext.getValueFactories().getPathFactory().create(absPath);
        Node exportRootNode = getNode(exportRootPath);

        AbstractJcrExporter exporter = new JcrSystemViewExporter(this);

        exporter.exportView(exportRootNode, out, skipBinary, noRecurse);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getImportContentHandler(java.lang.String, int)
     */
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior ) throws PathNotFoundException, RepositoryException {
        Path parentPath = this.executionContext.getValueFactories().getPathFactory().create(parentAbsPath);

        return new JcrContentHandler(this, parentPath, uuidBehavior, SaveMode.SESSION);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Session#getItem(java.lang.String)
     */
    public Item getItem( String absolutePath ) throws RepositoryException {
        CheckArg.isNotEmpty(absolutePath, "absolutePath");
        // Return root node if path is "/"
        Path path = executionContext.getValueFactories().getPathFactory().create(absolutePath);
        if (path.isRoot()) {
            return getRootNode();
        }
        // Since we don't know whether path refers to a node or a property, look to see if we can tell it's a node ...
        if (path.getLastSegment().hasIndex()) {
            return getNode(path);
        }
        // We can't tell from the name, so ask for an item ...
        try {
            return cache.findJcrItem(null, rootPath, path.relativeTo(rootPath));
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getLockTokens()
     */
    public String[] getLockTokens() {
        return lockTokens.toArray(new String[lockTokens.size()]);
    }

    /**
     * Find or create a JCR Node for the given path. This method works for the root node, too.
     * 
     * @param path the path; may not be null
     * @return the JCR node instance for the given path; never null
     * @throws PathNotFoundException if the path could not be found
     * @throws RepositoryException if there is a problem
     */
    AbstractJcrNode getNode( Path path ) throws RepositoryException, PathNotFoundException {
        if (path.isRoot()) return cache.findJcrRootNode();
        try {
            return cache.findJcrNode(null, path);
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNodeByUUID(java.lang.String)
     */
    public AbstractJcrNode getNodeByUUID( String uuid ) throws ItemNotFoundException, RepositoryException {
        AbstractJcrNode node = cache.findJcrNode(Location.create(UUID.fromString(uuid)));

        return node;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getRootNode()
     */
    public Node getRootNode() throws RepositoryException {
        return cache.findJcrRootNode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getUserID()
     * @see SecurityContext#getUserName()
     */
    public String getUserID() {
        return executionContext.getSecurityContext().getUserName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getValueFactory()
     */
    public ValueFactory getValueFactory() {
        final ValueFactories valueFactories = executionContext.getValueFactories();
        final SessionCache sessionCache = this.cache;

        return new ValueFactory() {

            public Value createValue( String value,
                                      int propertyType ) throws ValueFormatException {
                return new JcrValue(valueFactories, sessionCache, propertyType, convertValueToType(value, propertyType));
            }

            public Value createValue( Node value ) throws RepositoryException {
                if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(JcrSession.this.namespaces()))) {
                    throw new RepositoryException(JcrI18n.nodeNotReferenceable.text());
                }
                String uuid = valueFactories.getStringFactory().create(value.getUUID());
                return new JcrValue(valueFactories, sessionCache, PropertyType.REFERENCE, uuid);
            }

            public Value createValue( InputStream value ) {
                Binary binary = valueFactories.getBinaryFactory().create(value);
                return new JcrValue(valueFactories, sessionCache, PropertyType.BINARY, binary);
            }

            public Value createValue( Calendar value ) {
                DateTime dateTime = valueFactories.getDateFactory().create(value);
                return new JcrValue(valueFactories, sessionCache, PropertyType.DATE, dateTime);
            }

            public Value createValue( boolean value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.BOOLEAN, value);
            }

            public Value createValue( double value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.DOUBLE, value);
            }

            public Value createValue( long value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.LONG, value);
            }

            public Value createValue( String value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.STRING, value);
            }

            Object convertValueToType( Object value,
                                       int toType ) throws ValueFormatException {
                switch (toType) {
                    case PropertyType.BOOLEAN:
                        try {
                            return valueFactories.getBooleanFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.DATE:
                        try {
                            return valueFactories.getDateFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.NAME:
                        try {
                            return valueFactories.getNameFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.PATH:
                        try {
                            return valueFactories.getPathFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.REFERENCE:
                        try {
                            return valueFactories.getReferenceFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.DOUBLE:
                        try {
                            return valueFactories.getDoubleFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.LONG:
                        try {
                            return valueFactories.getLongFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                        // Anything can be converted to these types
                    case PropertyType.BINARY:
                        try {
                            return valueFactories.getBinaryFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.STRING:
                        try {
                            return valueFactories.getStringFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.UNDEFINED:
                        return value;

                    default:
                        assert false : "Unexpected JCR property type " + toType;
                        // This should still throw an exception even if assertions are turned off
                        throw new IllegalStateException("Invalid property type " + toType);
                }
            }

        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#hasPendingChanges()
     */
    public boolean hasPendingChanges() {
        return cache.hasPendingChanges();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    public Session impersonate( Credentials credentials ) throws RepositoryException {
        return repository.login(credentials, this.workspace.getName());
    }

    /**
     * Returns a new {@link JcrSession session} that uses the same security information to create a session that points to the
     * named workspace.
     * 
     * @param workspaceName the name of the workspace to connect to
     * @return a new session that uses the named workspace
     * @throws RepositoryException if an error occurs creating the session
     */
    JcrSession with( String workspaceName ) throws RepositoryException {
        return repository.sessionForContext(executionContext, workspaceName, sessionAttributes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#importXML(java.lang.String, java.io.InputStream, int)
     */
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior ) throws IOException, InvalidSerializedDataException, RepositoryException {

        try {
            XMLReader parser = XMLReaderFactory.createXMLReader();

            parser.setContentHandler(getImportContentHandler(parentAbsPath, uuidBehavior));
            parser.parse(new InputSource(in));
        } catch (EnclosingSAXException ese) {
            Exception cause = ese.getException();
            if (cause instanceof ItemExistsException) {
                throw (ItemExistsException)cause;
            } else if (cause instanceof ConstraintViolationException) {
                throw (ConstraintViolationException)cause;
            } else if (cause instanceof VersionException) {
                throw (VersionException)cause;
            }
            throw new RepositoryException(cause);
        } catch (SAXParseException se) {
            throw new InvalidSerializedDataException(se);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#isLive()
     */
    public boolean isLive() {
        return isLive;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Session#itemExists(java.lang.String)
     */
    public boolean itemExists( String absolutePath ) throws RepositoryException {
        try {
            return (getItem(absolutePath) != null);
        } catch (PathNotFoundException error) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#logout()
     */
    public void logout() {
        if (!isLive()) {
            return;
        }

        isLive = false;
        this.workspace().observationManager().removeAllEventListeners();
        this.workspace().lockManager().cleanLocks(this);
        this.repository.sessionLoggedOut(this);
        this.executionContext.getSecurityContext().logout();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#move(java.lang.String, java.lang.String)
     */
    public void move( String srcAbsPath,
                      String destAbsPath ) throws ItemExistsException, RepositoryException {
        CheckArg.isNotNull(srcAbsPath, "srcAbsPath");
        CheckArg.isNotNull(destAbsPath, "destAbsPath");

        PathFactory pathFactory = executionContext.getValueFactories().getPathFactory();
        Path destPath = pathFactory.create(destAbsPath);

        Path.Segment newNodeName = destPath.getSegment(destPath.size() - 1);
        // Doing a literal test here because the path factory will canonicalize "/node[1]" to "/node"
        if (destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        AbstractJcrNode sourceNode = getNode(pathFactory.create(srcAbsPath));
        AbstractJcrNode newParentNode = getNode(destPath.getParent());

        if (sourceNode.isLocked()) {
            javax.jcr.lock.Lock sourceLock = sourceNode.getLock();
            if (sourceLock != null && sourceLock.getLockToken() == null) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(srcAbsPath));
            }
        }

        if (newParentNode.isLocked()) {
            javax.jcr.lock.Lock newParentLock = newParentNode.getLock();
            if (newParentLock != null && newParentLock.getLockToken() == null) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(destAbsPath));
            }
        }

        if (!sourceNode.getParent().isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(sourceNode.getPath()));
        }

        if (!newParentNode.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(newParentNode.getPath()));
        }

        newParentNode.editor().moveToBeChild(sourceNode, newNodeName.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#refresh(boolean)
     */
    public void refresh( boolean keepChanges ) {
        this.cache.refresh(keepChanges);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#removeLockToken(java.lang.String)
     */
    public void removeLockToken( String lt ) {
        CheckArg.isNotNull(lt, "lock token");
        // A LockException is thrown if the lock associated with the specified lock token is session-scoped.
        /*
         * The JCR API library that we're using diverges from the spec in that it doesn't declare
         * this method to throw a LockException.  We'll throw a runtime exception for now.
         */

        ModeShapeLock lock = workspace().lockManager().lockFor(lt);
        if (lock == null) {
            // The lock is no longer valid
            lockTokens.remove(lt);
            return;
        }

        if (lock.isSessionScoped()) {
            throw new IllegalStateException(JcrI18n.cannotRemoveLockToken.text(lt));
        }

        workspace().lockManager().setHeldBySession(this, lt, false);
        lockTokens.remove(lt);
    }

    void recordRemoval( Location location ) throws RepositoryException {
        if (!performReferentialIntegrityChecks) {
            return;
        }
        if (removedNodes == null) {
            removedNodes = new HashSet<Location>();
            removedReferenceableNodeUuids = new HashSet<String>();
        }

        // Find the UUIDs of all of the mix:referenceable nodes that are below this node being removed ...
        Path path = location.getPath();
        org.modeshape.graph.property.ValueFactory<String> stringFactory = executionContext.getValueFactories().getStringFactory();
        String pathStr = stringFactory.create(path);
        int sns = path.getLastSegment().getIndex();
        if (sns == Path.DEFAULT_INDEX) pathStr = pathStr + "[1]";

        TypeSystem typeSystem = executionContext.getValueFactories().getTypeSystem();
        QueryBuilder builder = new QueryBuilder(typeSystem);
        QueryCommand query = builder.select("jcr:uuid")
                                    .from("mix:referenceable AS referenceable")
                                    .where()
                                    .path("referenceable")
                                    .isLike(pathStr + "%")
                                    .end()
                                    .query();
        JcrQueryManager queryManager = workspace().queryManager();
        Query jcrQuery = queryManager.createQuery(query);
        QueryResult result = jcrQuery.execute();
        RowIterator rows = result.getRows();
        while (rows.hasNext()) {
            Row row = rows.nextRow();
            String uuid = row.getValue("jcr:uuid").getString();
            if (uuid != null) removedReferenceableNodeUuids.add(uuid);
        }

        // Now record that this location is being removed ...
        Set<Location> extras = null;
        for (Location alreadyDeleted : removedNodes) {
            Path alreadyDeletedPath = alreadyDeleted.getPath();
            if (alreadyDeletedPath.isAtOrAbove(path)) {
                // Already covered by the alreadyDeleted location ...
                return;
            }
            if (alreadyDeletedPath.isDecendantOf(path)) {
                // The path being deleted is above the path that was already deleted, so remove the already-deleted one ...
                if (extras == null) {
                    extras = new HashSet<Location>();
                }
                extras.add(alreadyDeleted);
            }
        }
        // Not covered by any already-deleted location, so add it ...
        removedNodes.add(location);
        if (extras != null) {
            // Remove the nodes that will be covered by the node being deleted now ...
            removedNodes.removeAll(extras);
        }
    }

    boolean wasRemovedInSession( Location location ) {
        if (removedNodes == null) return false;
        if (removedNodes.contains(location)) return true;
        Path path = location.getPath();
        for (Location removed : removedNodes) {
            if (removed.getPath().isAtOrAbove(path)) return true;
        }
        return false;
    }

    boolean wasRemovedInSession( UUID uuid ) {
        if (removedReferenceableNodeUuids == null) return false;
        return removedReferenceableNodeUuids.contains(uuid);

    }

    /**
     * Determine whether there is at least one other node outside this branch that has a reference to nodes within the branch
     * rooted by this node.
     * 
     * @param subgraphRoot the root of the subgraph under which the references should be checked, or null if the root node should
     *        be used (meaning all references in the workspace should be checked)
     * @throws ReferentialIntegrityException if the changes would leave referential integrity problems
     * @throws RepositoryException if an error occurs while obtaining the information
     */
    void checkReferentialIntegrityOfChanges( AbstractJcrNode subgraphRoot )
        throws ReferentialIntegrityException, RepositoryException {
        if (removedNodes == null) return;
        if (removedReferenceableNodeUuids.isEmpty()) return;

        if (removedNodes.size() == 1 && removedNodes.iterator().next().getPath().isRoot()) {
            // The root node is being removed, so there will be no referencing nodes remaining ...
            return;
        }

        String subgraphPath = null;
        if (subgraphRoot != null) {
            subgraphPath = subgraphRoot.getPath();
            if (subgraphRoot.getIndex() == Path.DEFAULT_INDEX) subgraphPath = subgraphPath + "[1]";
        }

        // Build one (or several) queries to find the first reference to any 'mix:referenceable' nodes
        // that have been (transiently) removed from the session ...
        int maxBatchSize = 100;
        List<Object> someUuidsInBranch = new ArrayList<Object>(maxBatchSize);
        Iterator<String> uuidIter = removedReferenceableNodeUuids.iterator();
        while (uuidIter.hasNext()) {
            // Accumulate the next 100 UUIDs of referenceable nodes inside this branch ...
            while (uuidIter.hasNext() && someUuidsInBranch.size() <= maxBatchSize) {
                String uuid = uuidIter.next();
                someUuidsInBranch.add(uuid);
            }
            assert !someUuidsInBranch.isEmpty();
            // Now issue a query to see if any nodes outside this branch references these referenceable nodes ...
            TypeSystem typeSystem = executionContext.getValueFactories().getTypeSystem();
            QueryBuilder builder = new QueryBuilder(typeSystem);
            QueryCommand query = null;
            if (subgraphPath != null) {
                query = builder.select("jcr:primaryType")
                               .fromAllNodesAs("allNodes")
                               .where()
                               .referenceValue("allNodes")
                               .isIn(someUuidsInBranch)
                               .and()
                               .path("allNodes")
                               .isLike(subgraphPath + "%")
                               .end()
                               .query();
            } else {
                query = builder.select("jcr:primaryType")
                               .fromAllNodesAs("allNodes")
                               .where()
                               .referenceValue("allNodes")
                               .isIn(someUuidsInBranch)
                               .end()
                               .query();
            }
            Query jcrQuery = workspace().queryManager().createQuery(query);
            // The nodes that have been (transiently) deleted will not appear in these results ...
            QueryResult result = jcrQuery.execute();
            NodeIterator referencingNodes = result.getNodes();
            while (referencingNodes.hasNext()) {
                // There is at least one reference to nodes in this branch, so we can stop here ...
                throw new ReferentialIntegrityException();
            }
            someUuidsInBranch.clear();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#save()
     */
    public void save() throws RepositoryException {
        checkReferentialIntegrityOfChanges(null);
        removedNodes = null;
        cache.save();
    }

    /**
     * Crawl and index the content in this workspace.
     * 
     * @throws IllegalArgumentException if the workspace is null
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public void reindexContent() {
        repository().queryManager().reindexContent(workspace());
    }

    /**
     * Crawl and index the content starting at the supplied path in this workspace, to the designated depth.
     * 
     * @param path the path of the content to be indexed
     * @param depth the depth of the content to be indexed
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public void reindexContent( String path,
                                int depth ) {
        repository().queryManager().reindexContent(workspace(), path, depth);
    }

    /**
     * Get a snapshot of the current session state. This snapshot is immutable and will not reflect any future state changes in
     * the session.
     * 
     * @return the snapshot; never null
     */
    public Snapshot getSnapshot() {
        return new Snapshot(cache.graphSession().getRoot().getSnapshot(false));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getSnapshot().toString();
    }

    @Immutable
    public class Snapshot {
        private final GraphSession.StructureSnapshot<JcrPropertyPayload> rootSnapshot;

        protected Snapshot( GraphSession.StructureSnapshot<JcrPropertyPayload> snapshot ) {
            this.rootSnapshot = snapshot;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return rootSnapshot.toString();
        }
    }
}
