/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.federation;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.connector.federation.executor.FederatingCommandExecutor;
import org.jboss.dna.connector.federation.executor.SingleProjectionCommandExecutor;
import org.jboss.dna.spi.ExecutionContextFactory;
import org.jboss.dna.spi.cache.BasicCachePolicy;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.InvalidPathException;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.executor.CommandExecutor;
import org.jboss.dna.spi.graph.commands.executor.LoggingCommandExecutor;
import org.jboss.dna.spi.graph.commands.executor.NoOpCommandExecutor;
import org.jboss.dna.spi.graph.commands.impl.BasicCompositeCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetChildrenCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetNodeCommand;
import org.jboss.dna.spi.graph.connection.AbstractRepositorySource;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionFactories;
import org.jboss.dna.spi.graph.connection.RepositorySource;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class FederatedRepositorySource extends AbstractRepositorySource {

    /**
     */
    private static final long serialVersionUID = 7587346948013486977L;

    public static final String[] DEFAULT_CONFIGURATION_SOURCE_PROJECTION_RULES = {"/dna:system => /"};

    protected static final String REPOSITORY_NAME = "repositoryName";
    protected static final String SOURCE_NAME = "sourceName";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String CONFIGURATION_SOURCE_NAME = "configurationSourceName";
    protected static final String CONFIGURATION_SOURCE_PROJECTION_RULES = "configurationSourceProjectionRules";
    protected static final String CONNECTION_FACTORIES_JNDI_NAME = "connectionFactoriesJndiName";
    protected static final String EXECUTION_CONTEXT_FACTORY_JNDI_NAME = "executionContextFacotryJndiName";
    protected static final String REPOSITORY_JNDI_NAME = "repositoryJndiName";
    protected static final String SECURITY_DOMAIN = "securityDomain";
    protected static final String RETRY_LIMIT = "retryLimit";

    protected static final String PROJECTION_RULES_CONFIG_PROPERTY_NAME = "dna:projectionRules";
    protected static final String CACHE_POLICY_TIME_TO_EXPIRE_CONFIG_PROPERTY_NAME = "dna:timeToExpire";
    protected static final String CACHE_POLICY_TIME_TO_CACHE_CONFIG_PROPERTY_NAME = "dna:timeToCache";

    private String repositoryName;
    private String sourceName;
    private String username;
    private String password;
    private String configurationSourceName;
    private String[] configurationSourceProjectionRules = DEFAULT_CONFIGURATION_SOURCE_PROJECTION_RULES;
    private String connectionFactoriesJndiName;
    private String executionContextFactoryJndiName;
    private String securityDomain;
    private String repositoryJndiName;
    private transient FederatedRepository repository;
    private transient Context jndiContext;

    /**
     * Create a new instance of the source, which must still be properly initialized with a {@link #setRepositoryName(String)
     * repository name}.
     */
    public FederatedRepositorySource() {
        super();
    }

    /**
     * Create a new instance of the source with the required repository name and federation service.
     * 
     * @param repositoryName the repository name
     * @throws IllegalArgumentException if the federation service is null or the repository name is null or blank
     */
    public FederatedRepositorySource( String repositoryName ) {
        super();
        ArgCheck.isNotNull(repositoryName, "repositoryName");
        this.repositoryName = repositoryName;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getName() {
        return sourceName;
    }

    /**
     * Set the name of this source.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @param sourceName the name of this repository source
     * @see #setConfigurationSourceName(String)
     * @see #setConnectionFactoriesJndiName(String)
     * @see #setConfigurationSourceProjectionRules(String[])
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setExecutionContextFactoryJndiName(String)
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setName(String)
     */
    public synchronized void setName( String sourceName ) {
        if (this.sourceName == sourceName || this.sourceName != null && this.sourceName.equals(sourceName)) return; // unchanged
        this.sourceName = sourceName;
        changeRepositoryConfig();
    }

    /**
     * Get the name in JNDI of a {@link FederatedRepository} instance that should be used. If this is set (and an instance can be
     * found at that location), few of the remaining properties on this instance may not be used (basically just
     * {@link #getUsername() username}, {@link #getPassword() password}, and {@link #getName() source name}).
     * <p>
     * This is an optional property.
     * </p>
     * 
     * @return the location in JNDI of the {@link FederatedRepository} that should be used by this source, or null if the
     *         {@link FederatedRepository} instance will be created from the properties of this instance
     * @see #setRepositoryJndiName(String)
     */
    public String getRepositoryJndiName() {
        return repositoryJndiName;
    }

    /**
     * Set the name in JNDI of a {@link FederatedRepository} instance that should be used. If this is set (and an instance can be
     * found at that location), few of the remaining properties on this instance may not be used (basically just
     * {@link #getUsername() username}, {@link #getPassword() password}, and {@link #getName() source name}).
     * <p>
     * This is an optional property.
     * </p>
     * 
     * @param jndiName the JNDI name where the {@link FederatedRepository} instance can be found, or null if the instance is not
     *        to be found in JNDI but one should be instantiated from this instance's properties
     * @see #getRepositoryJndiName()
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setName(String)
     */
    public void setRepositoryJndiName( String jndiName ) {
        if (this.repositoryJndiName == jndiName || this.repositoryJndiName != null && this.repositoryJndiName.equals(jndiName)) return; // unchanged
        this.repositoryJndiName = jndiName;
        changeRepositoryConfig();
    }

    /**
     * Get the name in JNDI of a {@link RepositorySource} instance that should be used by the {@link FederatedRepository federated
     * repository} as the configuration repository.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @return the JNDI name of the {@link RepositorySource} instance that should be used for the configuration, or null if the
     *         federated repository instance is to be found in JNDI
     * @see #setConfigurationSourceName(String)
     */
    public String getConfigurationSourceName() {
        return configurationSourceName;
    }

    /**
     * Get the name of a {@link RepositorySource} instance that should be used by the {@link FederatedRepository federated
     * repository} as the configuration repository. The instance will be retrieved from the {@link RepositoryConnectionFactories}
     * instance {@link #getConnectionFactoriesJndiName() found in JDNI}.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @param sourceName the name of the {@link RepositorySource} instance that should be used for the configuration, or null if
     *        the federated repository instance is to be found in JNDI
     * @see #getConfigurationSourceName()
     * @see #setConnectionFactoriesJndiName(String)
     * @see #setConfigurationSourceProjectionRules(String[])
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setExecutionContextFactoryJndiName(String)
     * @see #setName(String)
     */
    public void setConfigurationSourceName( String sourceName ) {
        if (this.configurationSourceName == sourceName || this.configurationSourceName != null
            && this.configurationSourceName.equals(sourceName)) return; // unchanged
        this.configurationSourceName = sourceName;
        changeRepositoryConfig();
    }

    /**
     * Get the projection rule definitions used for the {@link #getConfigurationSourceName() configuration source}. The
     * {@link #DEFAULT_CONFIGURATION_SOURCE_PROJECTION_RULES default projection rules} map the root of the configuration source
     * into the <code>/dna:system</code> branch of the repository.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @return the string array of projection rules, or null if the projection rules haven't yet been set or if the federated
     *         repository instance is to be found in JNDI
     * @see #setConfigurationSourceProjectionRules(String[])
     */
    public String[] getConfigurationSourceProjectionRules() {
        return configurationSourceProjectionRules;
    }

    /**
     * Get the projection rule definitions used for the {@link #getConfigurationSourceName() configuration source}. The
     * {@link #DEFAULT_CONFIGURATION_SOURCE_PROJECTION_RULES default projection rules} map the root of the configuration source
     * into the <code>/dna:system</code> branch of the repository.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @param projectionRules the string array of projection rules, or null if the projection rules haven't yet been set or if the
     *        federated repository instance is to be found in JNDI
     * @see #setConfigurationSourceProjectionRules(String[])
     * @see #setConnectionFactoriesJndiName(String)
     * @see #setConfigurationSourceName(String)
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setExecutionContextFactoryJndiName(String)
     * @see #setName(String)
     */
    public void setConfigurationSourceProjectionRules( String[] projectionRules ) {
        if (projectionRules != null) {
            List<String> rules = new LinkedList<String>();
            for (String rule : projectionRules) {
                if (rule != null && rule.trim().length() != 0) rules.add(rule);
            }
            projectionRules = rules.toArray(new String[rules.size()]);
        }
        this.configurationSourceProjectionRules = projectionRules != null ? projectionRules : DEFAULT_CONFIGURATION_SOURCE_PROJECTION_RULES;
    }

    /**
     * Get the name in JNDI of a {@link ExecutionContextFactory} instance that should be used to obtain the
     * {@link ExecutionEnvironment execution context} used by the {@link FederatedRepository federated repository}.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @return the JNDI name of the {@link ExecutionContextFactory} instance that should be used, or null if the federated
     *         repository instance is to be found in JNDI
     * @see #setExecutionContextFactoryJndiName(String)
     */
    public String getExecutionContextFactoryJndiName() {
        return executionContextFactoryJndiName;
    }

    /**
     * Set the name in JNDI of a {@link ExecutionContextFactory} instance that should be used to obtain the
     * {@link ExecutionEnvironment execution context} used by the {@link FederatedRepository federated repository}.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @param jndiName the JNDI name where the {@link ExecutionContextFactory} instance can be found, or null if the federated
     *        repository instance is to be found in JNDI
     * @see #getExecutionContextFactoryJndiName()
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationSourceProjectionRules(String[])
     * @see #setConnectionFactoriesJndiName(String)
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setName(String)
     */
    public synchronized void setExecutionContextFactoryJndiName( String jndiName ) {
        if (this.repositoryJndiName == jndiName || this.repositoryJndiName != null && this.repositoryJndiName.equals(jndiName)) return;
        this.executionContextFactoryJndiName = jndiName; // unchanged
        changeRepositoryConfig();
    }

    /**
     * Get the name in JNDI where the {@link RepositoryConnectionFactories} instance that can be used by the
     * {@link FederatedRepository federated repository} can find any {@link RepositorySource} sources it needs, including those
     * used for {@link Projection sources} and that used for it's {@link #getConfigurationSourceName() configuration}.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @return the JNDI name where the {@link RepositoryConnectionFactories} instance can be found, or null if the federated
     *         repository instance is to be found in JNDI
     * @see #setConnectionFactoriesJndiName(String)
     */
    public String getConnectionFactoriesJndiName() {
        return connectionFactoriesJndiName;
    }

    /**
     * Set the name in JNDI where the {@link RepositoryConnectionFactories} instance that can be used by the
     * {@link FederatedRepository federated repository} can find any {@link RepositorySource} sources it needs, including those
     * used for {@link Projection sources} and that used for it's {@link #getConfigurationSourceName() configuration}.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @param jndiName the JNDI name where the {@link RepositoryConnectionFactories} instance can be found, or null if the
     *        federated repository instance is to be found in JNDI
     * @see #getConnectionFactoriesJndiName()
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationSourceProjectionRules(String[])
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setExecutionContextFactoryJndiName(String)
     * @see #setName(String)
     */
    public synchronized void setConnectionFactoriesJndiName( String jndiName ) {
        if (this.connectionFactoriesJndiName == jndiName || this.connectionFactoriesJndiName != null
            && this.connectionFactoriesJndiName.equals(jndiName)) return; // unchanged
        this.connectionFactoriesJndiName = jndiName;
        changeRepositoryConfig();
    }

    /**
     * Get the name of the security domain that should be used by JAAS to identify the application or security context. This
     * should correspond to the JAAS login configuration located within the JAAS login configuration file.
     * 
     * @return securityDomain
     */
    public String getSecurityDomain() {
        return securityDomain;
    }

    /**
     * Set the name of the security domain that should be used by JAAS to identify the application or security context. This
     * should correspond to the JAAS login configuration located within the JAAS login configuration file.
     * 
     * @param securityDomain Sets securityDomain to the specified value.
     */
    public void setSecurityDomain( String securityDomain ) {
        if (this.securityDomain != null && this.securityDomain.equals(securityDomain)) return; // unchanged
        this.securityDomain = securityDomain;
        changeRepositoryConfig();
    }

    /**
     * Get the name of the federated repository.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @return the name of the repository
     * @see #setRepositoryName(String)
     */
    public synchronized String getRepositoryName() {
        return this.repositoryName;
    }

    /**
     * Get the name of the federated repository.
     * <p>
     * This is a required property (unless the {@link #getRepositoryJndiName() federated repository is to be found in JDNI}).
     * </p>
     * 
     * @param repositoryName the new name of the repository
     * @throws IllegalArgumentException if the repository name is null, empty or blank
     * @see #getRepositoryName()
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationSourceProjectionRules(String[])
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setConnectionFactoriesJndiName(String)
     * @see #setExecutionContextFactoryJndiName(String)
     * @see #setName(String)
     */
    public synchronized void setRepositoryName( String repositoryName ) {
        ArgCheck.isNotEmpty(repositoryName, "repositoryName");
        if (this.repositoryName != null && this.repositoryName.equals(repositoryName)) return; // unchanged
        this.repositoryName = repositoryName;
        changeRepositoryConfig();
    }

    /**
     * Get the username that should be used when authenticating and {@link #getConnection() creating connections}.
     * <p>
     * This is an optional property, required only when authentication is to be used.
     * </p>
     * 
     * @return the username, or null if no username has been set or are not to be used
     * @see #setUsername(String)
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Set the username that should be used when authenticating and {@link #getConnection() creating connections}.
     * <p>
     * This is an optional property, required only when authentication is to be used.
     * </p>
     * 
     * @param username the username, or null if no username has been set or are not to be used
     * @see #getUsername()
     * @see #setPassword(String)
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationSourceProjectionRules(String[])
     * @see #setPassword(String)
     * @see #setRepositoryName(String)
     * @see #setConnectionFactoriesJndiName(String)
     * @see #setExecutionContextFactoryJndiName(String)
     * @see #setName(String)
     */
    public void setUsername( String username ) {
        if (this.username != null && this.username.equals(username)) return; // unchanged
        this.username = username;
        changeRepositoryConfig();
    }

    /**
     * Get the password that should be used when authenticating and {@link #getConnection() creating connections}.
     * <p>
     * This is an optional property, required only when authentication is to be used.
     * </p>
     * 
     * @return the password, or null if no password have been set or are not to be used
     * @see #setPassword(String)
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Get the password that should be used when authenticating and {@link #getConnection() creating connections}.
     * <p>
     * This is an optional property, required only when authentication is to be used.
     * </p>
     * 
     * @param password the password, or null if no password have been set or are not to be used
     * @see #getPassword()
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationSourceProjectionRules(String[])
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setConnectionFactoriesJndiName(String)
     * @see #setExecutionContextFactoryJndiName(String)
     * @see #setName(String)
     */
    public void setPassword( String password ) {
        if (this.password != null && this.password.equals(password)) return; // unchanged
        this.password = password;
        changeRepositoryConfig();
    }

    /**
     * This method is called to signal that some aspect of the configuration has changed. If a {@link #getRepository() repository}
     * instance has been created, it's configuration is
     * {@link #getRepositoryConfiguration(ExecutionEnvironment, RepositoryConnectionFactories) rebuilt} and updated. Nothing is
     * done, however, if there is currently no {@link #getRepository() repository}.
     */
    protected synchronized void changeRepositoryConfig() {
        if (this.repository != null) {
            // Find in JNDI the repository connection factories and the environment ...
            ExecutionEnvironment env = getExecutionEnvironment();
            RepositoryConnectionFactories factories = getRepositoryConnectionFactories();
            // Compute a new repository config and set it on the repository ...
            FederatedRepositoryConfig newConfig = getRepositoryConfiguration(env, factories);
            this.repository.setConfiguration(newConfig);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.AbstractRepositorySource#createConnection()
     */
    @Override
    protected synchronized RepositoryConnection createConnection() throws RepositorySourceException {
        if (getName() == null) {
            throw new RepositorySourceException(FederationI18n.propertyIsRequired.text("name"));
        }
        if (getExecutionContextFactoryJndiName() == null) {
            throw new RepositorySourceException(FederationI18n.propertyIsRequired.text("execution context factory JNDI name"));
        }
        if (getSecurityDomain() == null) {
            throw new RepositorySourceException(FederationI18n.propertyIsRequired.text("security domain"));
        }
        if (getConnectionFactoriesJndiName() == null) {
            throw new RepositorySourceException(FederationI18n.propertyIsRequired.text("connection factories JNDI name"));
        }
        // Find the repository ...
        FederatedRepository repository = getRepository();
        // Authenticate the user ...
        String username = this.username;
        Object credentials = this.password;
        RepositoryConnection connection = repository.createConnection(this, username, credentials);
        if (connection == null) {
            I18n msg = FederationI18n.unableToAuthenticateConnectionToFederatedRepository;
            throw new RepositorySourceException(msg.text(this.repositoryName, username));
        }
        // Return the new connection ...
        return connection;
    }

    /**
     * Get the {@link FederatedRepository} instance that this source is using. This method uses the following logic:
     * <ol>
     * <li>If a {@link FederatedRepository} already was obtained from a prior call, the same instance is returned.</li>
     * <li>A {@link FederatedRepository} is created using a {@link FederatedRepositoryConfig} is created from this instance's
     * properties and {@link ExecutionEnvironment} and {@link RepositoryConnectionFactories} instances obtained from JNDI.</li>
     * <li></li>
     * <li></li>
     * </ol>
     * 
     * @return the federated repository instance
     * @throws RepositorySourceException
     */
    protected synchronized FederatedRepository getRepository() throws RepositorySourceException {
        if (repository == null) {
            String jndiName = this.getRepositoryJndiName();
            Context context = getContext();
            if (jndiName != null && jndiName.trim().length() != 0) {
                // Look for an existing repository in JNDI ...
                try {
                    if (context == null) context = new InitialContext();
                    repository = (FederatedRepository)context.lookup(jndiName);
                } catch (Throwable err) {
                    I18n msg = FederationI18n.unableToFindFederatedRepositoryInJndi;
                    throw new RepositorySourceException(msg.text(this.sourceName, jndiName), err);
                }
            }

            if (repository == null) {
                // Find in JNDI the repository connection factories and the environment ...
                ExecutionEnvironment env = getExecutionEnvironment();
                RepositoryConnectionFactories factories = getRepositoryConnectionFactories();
                // And create the configuration and the repository ...
                FederatedRepositoryConfig config = getRepositoryConfiguration(env, factories);
                repository = new FederatedRepository(env, factories, config);
            }
        }
        return repository;
    }

    protected ExecutionEnvironment getExecutionEnvironment() {
        ExecutionContextFactory factory = null;
        Context context = getContext();
        String jndiName = getExecutionContextFactoryJndiName();
        if (jndiName != null && jndiName.trim().length() != 0) {
            try {
                if (context == null) context = new InitialContext();
                factory = (ExecutionContextFactory)context.lookup(jndiName);
            } catch (Throwable err) {
                I18n msg = FederationI18n.unableToFindExecutionContextFactoryInJndi;
                throw new RepositorySourceException(msg.text(this.sourceName, jndiName), err);
            }
        }
        if (factory == null) {
            I18n msg = FederationI18n.unableToFindExecutionContextFactoryInJndi;
            throw new RepositorySourceException(msg.text(this.sourceName, jndiName));
        }
        String securityDomain = getSecurityDomain();
        CallbackHandler handler = createCallbackHandler();
        try {
            return factory.create(securityDomain, handler);
        } catch (LoginException e) {
            I18n msg = FederationI18n.unableToCreateExecutionContext;
            throw new RepositorySourceException(msg.text(this.sourceName, jndiName, securityDomain), e);
        }
    }

    protected RepositoryConnectionFactories getRepositoryConnectionFactories() {
        RepositoryConnectionFactories factories = null;
        Context context = getContext();
        String jndiName = getConnectionFactoriesJndiName();
        if (jndiName != null && jndiName.trim().length() != 0) {
            try {
                if (context == null) context = new InitialContext();
                factories = (RepositoryConnectionFactories)context.lookup(jndiName);
            } catch (Throwable err) {
                I18n msg = FederationI18n.unableToFindRepositoryConnectionFactoriesInJndi;
                throw new RepositorySourceException(msg.text(this.sourceName, jndiName), err);
            }
        }
        if (factories == null) {
            I18n msg = FederationI18n.noRepositoryConnectionFactories;
            throw new RepositorySourceException(msg.text(this.repositoryName));
        }
        return factories;
    }

    protected CallbackHandler createCallbackHandler() {
        return new CallbackHandler() {
            public void handle( Callback[] callbacks ) {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        NameCallback nameCallback = (NameCallback)callback;
                        nameCallback.setName(FederatedRepositorySource.this.getUsername());
                    }
                    if (callback instanceof PasswordCallback) {
                        PasswordCallback passwordCallback = (PasswordCallback)callback;
                        passwordCallback.setPassword(FederatedRepositorySource.this.getPassword().toCharArray());
                    }
                }
            }
        };
    }

    protected Context getContext() {
        return this.jndiContext;
    }

    protected synchronized void setContext( Context context ) {
        this.jndiContext = context;
    }

    /**
     * Create a {@link FederatedRepositoryConfig} instance from the current properties of this instance. This method does
     * <i>not</i> modify the state of this instance.
     * 
     * @param env the execution environment that should be used to read the configuration; may not be null
     * @param factories the factories from which can be obtained the RepositoryConnectionFactory instances for each name source;
     *        may not be null
     * @return a configuration reflecting the current state of this instance
     */
    protected synchronized FederatedRepositoryConfig getRepositoryConfiguration( ExecutionEnvironment env,
                                                                                 RepositoryConnectionFactories factories ) {
        Problems problems = new SimpleProblems();
        ValueFactories valueFactories = env.getValueFactories();
        PathFactory pathFactory = valueFactories.getPathFactory();
        NameFactory nameFactory = valueFactories.getNameFactory();
        ValueFactory<Long> longFactory = valueFactories.getLongFactory();

        // Create the configuration projection ...
        ProjectionParser projectionParser = ProjectionParser.getInstance();
        Projection.Rule[] rules = projectionParser.rulesFromStrings(env, this.getConfigurationSourceProjectionRules());
        Projection configurationProjection = new Projection(this.getConfigurationSourceName(), rules);

        // Create a federating command executor to execute the commands and merge the results into a single set of
        // commands.
        final String configurationSourceName = configurationProjection.getSourceName();
        List<Projection> projections = Collections.singletonList(configurationProjection);
        CommandExecutor executor = null;
        if (configurationProjection.getRules().size() == 1) {
            // There is just a single projection for the configuration repository, so just use an executor that
            // translates the paths using the projection
            executor = new SingleProjectionCommandExecutor(env, configurationSourceName, configurationProjection, factories);
        } else if (configurationProjection.getRules().size() == 0) {
            // There is no projection for the configuration repository, so just use a no-op executor
            executor = new NoOpCommandExecutor(env, configurationSourceName);
        } else {
            // The configuration repository has more than one projection, so we need to merge the results
            executor = new FederatingCommandExecutor(env, configurationSourceName, null, projections, factories);
        }
        // Wrap the executor with a logging executor ...
        executor = new LoggingCommandExecutor(executor, Logger.getLogger(getClass()), Logger.Level.INFO);

        // The configuration projection (via "executor") will convert this path into a path that exists in the configuration
        // repository
        Path configNode = pathFactory.create("/dna:system/dna:federation");

        try {
            // Get the repository node ...
            BasicGetNodeCommand getRepository = new BasicGetNodeCommand(configNode);
            executor.execute(getRepository);
            if (getRepository.hasError()) {
                throw new FederationException(FederationI18n.federatedRepositoryCannotBeFound.text(repositoryName));
            }

            // Add a command to get the projection defining the cache ...
            Path pathToCacheRegion = pathFactory.create(configNode, nameFactory.create("dna:cache"));
            BasicGetNodeCommand getCacheRegion = new BasicGetNodeCommand(pathToCacheRegion);
            executor.execute(getCacheRegion);
            Projection cacheProjection = createProjection(env,
                                                          projectionParser,
                                                          getCacheRegion.getPath(),
                                                          getCacheRegion.getProperties(),
                                                          problems);

            if (getCacheRegion.hasError()) {
                I18n msg = FederationI18n.requiredNodeDoesNotExistRelativeToNode;
                throw new FederationException(msg.text("dna:cache", configNode));
            }

            // Get the source projections for the repository ...
            Path projectionsNode = pathFactory.create(configNode, nameFactory.create("dna:projections"));
            BasicGetChildrenCommand getProjections = new BasicGetChildrenCommand(projectionsNode);

            executor.execute(getProjections);
            if (getProjections.hasError()) {
                I18n msg = FederationI18n.requiredNodeDoesNotExistRelativeToNode;
                throw new FederationException(msg.text("dna:projections", configNode));
            }

            // Build the commands to get each of the projections (children of the "dna:projections" node) ...
            List<Projection> sourceProjections = new LinkedList<Projection>();
            if (getProjections.hasNoError() && !getProjections.getChildren().isEmpty()) {
                BasicCompositeCommand commands = new BasicCompositeCommand();
                for (Path.Segment child : getProjections.getChildren()) {
                    final Path pathToSource = pathFactory.create(projectionsNode, child);
                    commands.add(new BasicGetNodeCommand(pathToSource));
                }
                // Now execute these commands ...
                executor.execute(commands);

                // Iterate over each region node obtained ...
                for (GraphCommand command : commands) {
                    BasicGetNodeCommand getProjectionCommand = (BasicGetNodeCommand)command;
                    if (getProjectionCommand.hasNoError()) {
                        Projection projection = createProjection(env,
                                                                 projectionParser,
                                                                 getProjectionCommand.getPath(),
                                                                 getProjectionCommand.getProperties(),
                                                                 problems);
                        if (projection != null) sourceProjections.add(projection);
                    }
                }
            }

            // Look for the default cache policy ...
            BasicCachePolicy cachePolicy = new BasicCachePolicy();
            Property timeToExpireProperty = getRepository.getProperties().get(nameFactory.create(CACHE_POLICY_TIME_TO_EXPIRE_CONFIG_PROPERTY_NAME));
            Property timeToCacheProperty = getRepository.getProperties().get(nameFactory.create(CACHE_POLICY_TIME_TO_CACHE_CONFIG_PROPERTY_NAME));
            if (timeToCacheProperty != null && !timeToCacheProperty.isEmpty()) {
                cachePolicy.setTimeToCache(longFactory.create(timeToCacheProperty.getValues().next()));
            }
            if (timeToExpireProperty != null && !timeToExpireProperty.isEmpty()) {
                cachePolicy.setTimeToExpire(longFactory.create(timeToExpireProperty.getValues().next()));
            }
            CachePolicy defaultCachePolicy = cachePolicy.isEmpty() ? null : cachePolicy.getUnmodifiable();
            return new FederatedRepositoryConfig(repositoryName, cacheProjection, sourceProjections, defaultCachePolicy);
        } catch (InvalidPathException err) {
            I18n msg = FederationI18n.federatedRepositoryCannotBeFound;
            throw new FederationException(msg.text(repositoryName));
        } catch (InterruptedException err) {
            I18n msg = FederationI18n.interruptedWhileUsingFederationConfigurationRepository;
            throw new FederationException(msg.text(repositoryName));
        }

    }

    /**
     * Instantiate the {@link Projection} described by the supplied properties.
     * 
     * @param env the execution environment that should be used to read the configuration; may not be null
     * @param projectionParser the projection rule parser that should be used; may not be null
     * @param path the path to the node where these properties were found; never null
     * @param properties the properties; never null
     * @param problems the problems container in which any problems should be reported; never null
     * @return the region instance, or null if it could not be created
     */
    protected Projection createProjection( ExecutionEnvironment env,
                                           ProjectionParser projectionParser,
                                           Path path,
                                           Map<Name, Property> properties,
                                           Problems problems ) {
        ValueFactories valueFactories = env.getValueFactories();
        NameFactory nameFactory = valueFactories.getNameFactory();
        ValueFactory<String> stringFactory = valueFactories.getStringFactory();

        String sourceName = path.getLastSegment().getName().getLocalName();

        // Get the rules ...
        Projection.Rule[] projectionRules = null;
        Property projectionRulesProperty = properties.get(nameFactory.create(PROJECTION_RULES_CONFIG_PROPERTY_NAME));
        if (projectionRulesProperty != null && !projectionRulesProperty.isEmpty()) {
            String[] projectionRuleStrs = stringFactory.create(projectionRulesProperty.getValuesAsArray());
            if (projectionRuleStrs != null && projectionRuleStrs.length != 0) {
                projectionRules = projectionParser.rulesFromStrings(env, projectionRuleStrs);
            }
        }
        if (problems.hasErrors()) return null;

        Projection region = new Projection(sourceName, projectionRules);
        return region;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = NamingContextObjectFactory.class.getName();
        Reference ref = new Reference(className, factoryClassName, null);

        if (getRepositoryName() != null) {
            ref.add(new StringRefAddr(REPOSITORY_NAME, getRepositoryName()));
        }
        if (getName() != null) {
            ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        }
        if (getUsername() != null) {
            ref.add(new StringRefAddr(USERNAME, getUsername()));
        }
        if (getPassword() != null) {
            ref.add(new StringRefAddr(PASSWORD, getPassword()));
        }
        if (getConfigurationSourceName() != null) {
            ref.add(new StringRefAddr(CONFIGURATION_SOURCE_NAME, getConfigurationSourceName()));
        }
        if (getConfigurationSourceProjectionRules() != null) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String rule : getConfigurationSourceProjectionRules()) {
                if (!first) {
                    sb.append("\n");
                    first = false;
                }
                sb.append(rule);
            }
            ref.add(new StringRefAddr(CONFIGURATION_SOURCE_PROJECTION_RULES, sb.toString()));
        }
        if (getConnectionFactoriesJndiName() != null) {
            ref.add(new StringRefAddr(CONNECTION_FACTORIES_JNDI_NAME, getConnectionFactoriesJndiName()));
        }
        if (getExecutionContextFactoryJndiName() != null) {
            ref.add(new StringRefAddr(EXECUTION_CONTEXT_FACTORY_JNDI_NAME, getExecutionContextFactoryJndiName()));
        }
        if (getSecurityDomain() != null) {
            ref.add(new StringRefAddr(SECURITY_DOMAIN, getSecurityDomain()));
        }
        if (getRepositoryJndiName() != null) {
            ref.add(new StringRefAddr(REPOSITORY_JNDI_NAME, getRepositoryJndiName()));
        }
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        return ref;
    }

    public static class NamingContextObjectFactory implements ObjectFactory {

        public NamingContextObjectFactory() {
        }

        /**
         * {@inheritDoc}
         */
        public Object getObjectInstance( Object obj,
                                         javax.naming.Name name,
                                         Context nameCtx,
                                         Hashtable<?, ?> environment ) throws Exception {
            if (obj instanceof Reference) {
                Map<String, String> values = new HashMap<String, String>();
                Reference ref = (Reference)obj;
                Enumeration<?> en = ref.getAll();
                while (en.hasMoreElements()) {
                    RefAddr subref = (RefAddr)en.nextElement();
                    if (subref instanceof StringRefAddr) {
                        String key = subref.getType();
                        Object value = subref.getContent();
                        if (value != null) values.put(key, value.toString());
                    }
                }
                String repositoryName = values.get(FederatedRepositorySource.REPOSITORY_NAME);
                String sourceName = values.get(FederatedRepositorySource.SOURCE_NAME);
                String username = values.get(FederatedRepositorySource.USERNAME);
                String password = values.get(FederatedRepositorySource.PASSWORD);
                String configurationSourceName = values.get(FederatedRepositorySource.CONFIGURATION_SOURCE_NAME);
                String projectionRules = values.get(FederatedRepositorySource.CONFIGURATION_SOURCE_PROJECTION_RULES);
                String connectionFactoriesJndiName = values.get(FederatedRepositorySource.CONNECTION_FACTORIES_JNDI_NAME);
                String environmentJndiName = values.get(FederatedRepositorySource.EXECUTION_CONTEXT_FACTORY_JNDI_NAME);
                String repositoryJndiName = values.get(FederatedRepositorySource.REPOSITORY_JNDI_NAME);
                String securityDomain = values.get(FederatedRepositorySource.SECURITY_DOMAIN);
                String retryLimit = values.get(FederatedRepositorySource.RETRY_LIMIT);

                // Create the source instance ...
                FederatedRepositorySource source = new FederatedRepositorySource();
                if (repositoryName != null) source.setRepositoryName(repositoryName);
                if (sourceName != null) source.setName(sourceName);
                if (username != null) source.setUsername(username);
                if (password != null) source.setPassword(password);
                if (configurationSourceName != null) source.setConfigurationSourceName(configurationSourceName);
                if (projectionRules != null) {
                    List<String> rules = StringUtil.splitLines(projectionRules);
                    source.setConfigurationSourceProjectionRules(rules.toArray(new String[rules.size()]));
                }
                if (connectionFactoriesJndiName != null) source.setConnectionFactoriesJndiName(connectionFactoriesJndiName);
                if (environmentJndiName != null) source.setExecutionContextFactoryJndiName(environmentJndiName);
                if (repositoryJndiName != null) source.setRepositoryJndiName(repositoryJndiName);
                if (securityDomain != null) source.setSecurityDomain(securityDomain);
                if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
                return source;
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return repositoryName.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof FederatedRepositorySource) {
            FederatedRepositorySource that = (FederatedRepositorySource)obj;
            // The repository name, source name, and federation service must all match
            if (!this.getRepositoryName().equals(that.getRepositoryName())) return false;
            if (this.getName() == null) {
                if (that.getName() != null) return false;
            } else {
                if (!this.getName().equals(that.getName())) return false;
            }
            return true;
        }
        return false;
    }

}
