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
package org.jboss.dna.jcr;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.text.Inflector;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JaasSecurityContext;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.request.InvalidWorkspaceException;

/**
 * Creates JCR {@link Session sessions} to an underlying repository (which may be a federated repository).
 * <p>
 * This JCR repository must be configured with the ability to connect to a repository via a supplied
 * {@link RepositoryConnectionFactory repository connection factory} and repository source name. An {@link ExecutionContext
 * execution context} must also be supplied to enable working with the underlying DNA graph implementation to which this JCR
 * implementation delegates.
 * </p>
 * <p>
 * If {@link Credentials credentials} are used to login, implementations <em>must</em> also implement one of the following
 * methods:
 * 
 * <pre>
 * public {@link AccessControlContext} getAccessControlContext();
 * public {@link LoginContext} getLoginContext();
 * </pre>
 * 
 * Note, {@link Session#getAttributeNames() attributes} on credentials are not supported. JCR {@link SimpleCredentials} are also
 * not supported.
 * </p>
 * 
 * @author John Verhaeg
 * @author Randall Hauch
 */
@ThreadSafe
public class JcrRepository implements Repository {

    /**
     * The available options for the {@code JcrRepository}.
     */
    public enum Option {

        /**
         * Flag that defines whether or not the node types should be exposed as content under the "{@code
         * /jcr:system/jcr:nodeTypes}" node. Value is either "<code>true</code>" or "<code>false</code>" (default).
         * 
         * @see DefaultOption#PROJECT_NODE_TYPES
         */
        PROJECT_NODE_TYPES,
        /**
         * The {@link Configuration#getAppConfigurationEntry(String) JAAS application configuration name} that specifies which
         * login modules should be used to validate credentials.
         */
        JAAS_LOGIN_CONFIG_NAME;

        /**
         * Determine the option given the option name. This does more than {@link Option#valueOf(String)}, since this method first
         * tries to match the supplied string to the option's {@link Option#name() name}, then the uppercase version of the
         * supplied string to the option's name, and finally if the supplied string is a camel-case version of the name (e.g.,
         * "projectNodeTypes").
         * 
         * @param option the string version of the option's name
         * @return the matching Option instance, or null if an option could not be matched using the supplied value
         */
        public static Option findOption( String option ) {
            if (option == null) return null;
            try {
                return Option.valueOf(option);
            } catch (IllegalArgumentException e) {
                // Try an uppercased version ...
                try {
                    return Option.valueOf(option.toUpperCase());
                } catch (IllegalArgumentException e2) {
                    // Try a camel-case version ...
                    String underscored = Inflector.getInstance().underscore(option, '_');
                    if (underscored == null) {
                        throw e2;
                    }
                    return Option.valueOf(underscored.toUpperCase());
                }
            }
        }
    }

    /**
     * The default values for each of the {@link Option}.
     */
    public static class DefaultOption {
        /**
         * The default value for the {@link Option#PROJECT_NODE_TYPES} option is {@value} .
         */
        public static final String PROJECT_NODE_TYPES = Boolean.FALSE.toString();

        /**
         * The default value for the {@link Option#JAAS_LOGIN_CONFIG_NAME} option is {@value} .
         */
        public static final String JAAS_LOGIN_CONFIG_NAME = "dna-jcr";
    }

    /**
     * The static unmodifiable map of default options, which are initialized in the static initializer.
     */
    protected static final Map<Option, String> DEFAULT_OPTIONS;

    static {
        // Initialize the unmodifiable map of default options ...
        EnumMap<Option, String> defaults = new EnumMap<Option, String>(Option.class);
        defaults.put(Option.PROJECT_NODE_TYPES, DefaultOption.PROJECT_NODE_TYPES);
        defaults.put(Option.JAAS_LOGIN_CONFIG_NAME, DefaultOption.JAAS_LOGIN_CONFIG_NAME);
        DEFAULT_OPTIONS = Collections.<Option, String>unmodifiableMap(defaults);
    }

    private final String sourceName;
    private final Map<String, String> descriptors;
    private final ExecutionContext executionContext;
    private final RepositoryConnectionFactory connectionFactory;
    private final RepositoryNodeTypeManager repositoryTypeManager;
    private final Map<Option, String> options;

    /**
     * Creates a JCR repository that uses the supplied {@link RepositoryConnectionFactory repository connection factory} to
     * establish {@link Session sessions} to the underlying repository source upon {@link #login() login}.
     * 
     * @param executionContext An execution context.
     * @param connectionFactory A repository connection factory.
     * @param repositorySourceName the name of the repository source (in the connection factory) that should be used
     * @throws IllegalArgumentException If <code>executionContextFactory</code> or <code>connectionFactory</code> is
     *         <code>null</code>.
     */
    public JcrRepository( ExecutionContext executionContext,
                          RepositoryConnectionFactory connectionFactory,
                          String repositorySourceName ) {
        this(executionContext, connectionFactory, repositorySourceName, null, null);
    }

    /**
     * Creates a JCR repository that uses the supplied {@link RepositoryConnectionFactory repository connection factory} to
     * establish {@link Session sessions} to the underlying repository source upon {@link #login() login}.
     * 
     * @param executionContext the execution context in which this repository is to operate
     * @param connectionFactory the factory for repository connections
     * @param repositorySourceName the name of the repository source (in the connection factory) that should be used
     * @param descriptors the {@link #getDescriptorKeys() descriptors} for this repository; may be <code>null</code>.
     * @param options the optional {@link Option settings} for this repository; may be null
     * @throws IllegalArgumentException If <code>executionContextFactory</code> or <code>connectionFactory</code> is
     *         <code>null</code>.
     */
    public JcrRepository( ExecutionContext executionContext,
                          RepositoryConnectionFactory connectionFactory,
                          String repositorySourceName,
                          Map<String, String> descriptors,
                          Map<Option, String> options ) {
        CheckArg.isNotNull(executionContext, "executionContext");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isNotNull(repositorySourceName, "repositorySourceName");
        this.executionContext = executionContext;
        this.connectionFactory = connectionFactory;
        this.sourceName = repositorySourceName;
        Map<String, String> modifiableDescriptors;
        if (descriptors == null) {
            modifiableDescriptors = new HashMap<String, String>();
        } else {
            modifiableDescriptors = new HashMap<String, String>(descriptors);
        }
        // Initialize required JCR descriptors.
        modifiableDescriptors.put(Repository.LEVEL_1_SUPPORTED, "true");
        modifiableDescriptors.put(Repository.LEVEL_2_SUPPORTED, "true");
        modifiableDescriptors.put(Repository.OPTION_LOCKING_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.OPTION_OBSERVATION_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.OPTION_QUERY_SQL_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.OPTION_VERSIONING_SUPPORTED, "false");
        modifiableDescriptors.put(Repository.QUERY_XPATH_DOC_ORDER, "true");
        modifiableDescriptors.put(Repository.QUERY_XPATH_POS_INDEX, "true");
        // Vendor-specific descriptors (REP_XXX) will only be initialized if not already present, allowing for customer branding.
        if (!modifiableDescriptors.containsKey(Repository.REP_NAME_DESC)) {
            modifiableDescriptors.put(Repository.REP_NAME_DESC, JcrI18n.REP_NAME_DESC.text());
        }
        if (!modifiableDescriptors.containsKey(Repository.REP_VENDOR_DESC)) {
            modifiableDescriptors.put(Repository.REP_VENDOR_DESC, JcrI18n.REP_VENDOR_DESC.text());
        }
        if (!modifiableDescriptors.containsKey(Repository.REP_VENDOR_URL_DESC)) {
            modifiableDescriptors.put(Repository.REP_VENDOR_URL_DESC, "http://www.jboss.org/dna");
        }
        if (!modifiableDescriptors.containsKey(Repository.REP_VERSION_DESC)) {
            modifiableDescriptors.put(Repository.REP_VERSION_DESC, "0.4");
        }
        modifiableDescriptors.put(Repository.SPEC_NAME_DESC, JcrI18n.SPEC_NAME_DESC.text());
        modifiableDescriptors.put(Repository.SPEC_VERSION_DESC, "1.0");
        this.descriptors = Collections.unmodifiableMap(modifiableDescriptors);

        this.repositoryTypeManager = new RepositoryNodeTypeManager(this.executionContext);

        try {
            this.repositoryTypeManager.registerNodeTypes(new CndNodeTypeSource(new String[] {
                "/org/jboss/dna/jcr/jsr_170_builtins.cnd", "/org/jboss/dna/jcr/dna_builtins.cnd"}));
        } catch (RepositoryException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }

        if (options == null) {
            this.options = DEFAULT_OPTIONS;
        } else {
            // Initialize with defaults, then add supplied options ...
            EnumMap<Option, String> localOptions = new EnumMap<Option, String>(DEFAULT_OPTIONS);
            localOptions.putAll(options);
            this.options = Collections.unmodifiableMap(localOptions);
        }
    }

    /**
     * Returns the repository-level node type manager
     * 
     * @return the repository-level node type manager
     */
    RepositoryNodeTypeManager getRepositoryTypeManager() {
        return repositoryTypeManager;
    }

    /**
     * Get the options as configured for this repository.
     * 
     * @return the unmodifiable options; never null
     */
    public Map<Option, String> getOptions() {
        return options;
    }

    /**
     * Get the name of the repository source that this repository is using.
     * 
     * @return the name of the RepositorySource
     * @see #getConnectionFactory()
     */
    String getRepositorySourceName() {
        return sourceName;
    }

    /**
     * Get the connection factory that this repository is using.
     * 
     * @return the connection factory; never null
     */
    RepositoryConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>key</code> is <code>null</code>.
     * @see javax.jcr.Repository#getDescriptor(java.lang.String)
     */
    public String getDescriptor( String key ) {
        CheckArg.isNotEmpty(key, "key");
        return descriptors.get(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#getDescriptorKeys()
     */
    public String[] getDescriptorKeys() {
        return descriptors.keySet().toArray(new String[descriptors.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#login()
     */
    public synchronized Session login() throws RepositoryException {
        return login(null, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#login(javax.jcr.Credentials)
     */
    public synchronized Session login( Credentials credentials ) throws RepositoryException {
        return login(credentials, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Repository#login(java.lang.String)
     */
    public synchronized Session login( String workspaceName ) throws RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>credentials</code> is not <code>null</code> but:
     *         <ul>
     *         <li>provides neither a <code>getLoginContext()</code> nor a <code>getAccessControlContext()</code> method and is
     *         not an instance of {@code SimpleCredentials}.</li>
     *         <li>provides a <code>getLoginContext()</code> method that doesn't return a {@link LoginContext}.
     *         <li>provides a <code>getLoginContext()</code> method that returns a <code>null</code> {@link LoginContext}.
     *         <li>does not provide a <code>getLoginContext()</code> method, but provides a <code>getAccessControlContext()</code>
     *         method that doesn't return an {@link AccessControlContext}.
     *         <li>does not provide a <code>getLoginContext()</code> method, but provides a <code>getAccessControlContext()</code>
     *         method that returns a <code>null</code> {@link AccessControlContext}.
     *         </ul>
     * @see javax.jcr.Repository#login(javax.jcr.Credentials, java.lang.String)
     */
    public synchronized Session login( Credentials credentials,
                                       String workspaceName ) throws RepositoryException {
        // Ensure credentials are either null or provide a JAAS method
        Map<String, Object> sessionAttributes = new HashMap<String, Object>();
        ExecutionContext execContext = null;
        if (credentials == null) {
            try {
                Subject subject = Subject.getSubject(AccessController.getContext());
                if (subject == null) {
                    throw new javax.jcr.LoginException(JcrI18n.mustBeInPrivilegedAction.text());
                }
                execContext = executionContext.with(new JaasSecurityContext(subject));
            } catch (LoginException le) {
                // This really can't happen if you're creating the JAAS security context with an existing subject
                throw new IllegalStateException(le);
            }
        } else {
            try {
                if (credentials instanceof SimpleCredentials) {
                    SimpleCredentials simple = (SimpleCredentials)credentials;
                    execContext = executionContext.with(new JaasSecurityContext(options.get(Option.JAAS_LOGIN_CONFIG_NAME),
                                                                                simple.getUserID(), simple.getPassword()));
                    for (String attributeName : simple.getAttributeNames()) {
                        Object attributeValue = simple.getAttribute(attributeName);
                        sessionAttributes.put(attributeName, attributeValue);
                    }

                } else if (credentials instanceof SecurityContextCredentials) {
                    execContext = executionContext.with(((SecurityContextCredentials)credentials).getSecurityContext());
                } else {
                    // Check if credentials provide a login context
                    try {
                        Method method = credentials.getClass().getMethod("getLoginContext");
                        if (method.getReturnType() != LoginContext.class) {
                            throw new IllegalArgumentException(
                                                               JcrI18n.credentialsMustReturnLoginContext.text(credentials.getClass()));
                        }
                        LoginContext loginContext = (LoginContext)method.invoke(credentials);
                        if (loginContext == null) {
                            throw new IllegalArgumentException(
                                                               JcrI18n.credentialsMustReturnLoginContext.text(credentials.getClass()));
                        }
                        execContext = executionContext.with(new JaasSecurityContext(loginContext));
                    } catch (NoSuchMethodException error) {
                        throw new IllegalArgumentException(JcrI18n.credentialsMustProvideJaasMethod.text(credentials.getClass()),
                                                           error);
                    }
                }
            } catch (RuntimeException error) {
                throw error;
            } catch (Exception error) {
                throw new javax.jcr.LoginException(error);
            }
        }

        // Ensure valid workspace name
        Graph graph = Graph.create(sourceName, connectionFactory, executionContext);
        if (workspaceName == null) {
            try {
                // Get the correct workspace name given the desired workspace name (which may be null) ...
                workspaceName = graph.getCurrentWorkspace().getName();
            } catch (RepositorySourceException e) {
                throw new RepositoryException(JcrI18n.errorObtainingDefaultWorkspaceName.text(sourceName, e.getMessage()), e);
            }
        } else {
            try {
                // Verify that the workspace exists (or can be created) ...
                Set<String> workspaces = graph.getWorkspaces();
                if (!workspaces.contains(workspaceName)) {
                    // Per JCR 1.0 6.1.1, if the workspaceName is not recognized, a NoSuchWorkspaceException is thrown
                    throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(sourceName, workspaceName));
                }
                
                graph.useWorkspace(workspaceName);
            } catch (InvalidWorkspaceException e) {
                throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(sourceName, workspaceName), e);
            } catch (RepositorySourceException e) {
                String msg = JcrI18n.errorVerifyingWorkspaceName.text(sourceName, workspaceName, e.getMessage());
                throw new NoSuchWorkspaceException(msg, e);
            }
        }

        // Create the workspace, which will create its own session ...
        sessionAttributes = Collections.unmodifiableMap(sessionAttributes);
        JcrWorkspace workspace = new JcrWorkspace(this, workspaceName, execContext, sessionAttributes);
        
        JcrSession session = (JcrSession) workspace.getSession();
        
        // Need to make sure that the user has access to this session
        try {
            session.checkPermission(workspaceName, null, JcrSession.JCR_READ_PERMISSION);
        }
        catch (AccessControlException ace) {
            throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(sourceName, workspaceName));
        }
        return session;
    }

    /**
     * Returns the name of this repository
     * 
     * @return the name of this repository
     * @see #sourceName
     */
    String getName() {
        return this.sourceName;
    }
}
