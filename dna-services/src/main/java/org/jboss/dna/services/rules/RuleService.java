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
package org.jboss.dna.services.rules;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.rules.ConfigurationException;
import javax.rules.RuleRuntime;
import javax.rules.RuleServiceProvider;
import javax.rules.RuleServiceProviderManager;
import javax.rules.RuleSession;
import javax.rules.StatelessRuleSession;
import javax.rules.admin.LocalRuleExecutionSetProvider;
import javax.rules.admin.RuleAdministrator;
import javax.rules.admin.RuleExecutionSet;
import javax.rules.admin.RuleExecutionSetCreateException;
import javax.rules.admin.RuleExecutionSetDeregistrationException;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.services.AbstractServiceAdministrator;
import org.jboss.dna.services.AdministeredService;
import org.jboss.dna.services.ServiceAdministrator;

/**
 * A rule service that is capable of executing rule sets using one or more JSR-94 rule engines. Sets of rules are
 * {@link #addRuleSet(RuleSet) added}, {@link #updateRuleSet(RuleSet) updated}, and {@link #removeRuleSet(RuleSet) removed}
 * (usually by some other component), and then these named rule sets can be {@link #executeRules(String, Map, Object...) run} with
 * inputs and facts to obtain output.
 * <p>
 * This service is thread safe. While multiple rule sets can be safely {@link #executeRules(String, Map, Object...) executed} at
 * the same time, all executions will be properly synchronized with methods to {@link #addRuleSet(RuleSet) add},
 * {@link #updateRuleSet(RuleSet) update}, and {@link #removeRuleSet(RuleSet) remove} rule sets.
 * </p>
 * @author Randall Hauch
 */
@ThreadSafe
public class RuleService implements AdministeredService {

    protected static final ClassLoaderFactory DEFAULT_CLASSLOADER_FACTORY = new StandardClassLoaderFactory(RuleService.class.getClassLoader());

    /**
     * The administrative component for this service.
     * @author Randall Hauch
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(State.PAUSED);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String serviceName() {
            return "RuleService";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
            // Remove all rule sets ...
            removeAllRuleSets();
        }

    }

    private Logger logger;
    private ClassLoaderFactory classLoaderFactory = DEFAULT_CLASSLOADER_FACTORY;
    private final Administrator administrator = new Administrator();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    @GuardedBy( "lock" )
    private final Map<String, RuleSet> ruleSets = new HashMap<String, RuleSet>();

    /**
     * Create a new rule service, configured with no rule sets. Upon construction, the system is
     * {@link ServiceAdministrator#isPaused() paused} and must be configured and then {@link ServiceAdministrator#start() started}.
     */
    public RuleService() {
        this.logger = Logger.getLogger(this.getClass());
    }

    /**
     * Return the administrative component for this service.
     * @return the administrative component; never null
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * Get the class loader factory that should be used to load sequencers. By default, this service uses a factory that will
     * return either the {@link Thread#getContextClassLoader() current thread's context class loader} (if not null) or the class
     * loader that loaded this class.
     * @return the class loader factory; never null
     * @see #setClassLoaderFactory(ClassLoaderFactory)
     */
    public ClassLoaderFactory getClassLoaderFactory() {
        return this.classLoaderFactory;
    }

    /**
     * Set the Maven Repository that should be used to load the sequencer classes. By default, this service uses a class loader
     * factory that will return either the {@link Thread#getContextClassLoader() current thread's context class loader} (if not
     * null) or the class loader that loaded this class.
     * @param classLoaderFactory the class loader factory reference, or null if the default class loader factory should be used.
     * @see #getClassLoaderFactory()
     */
    public void setClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
        this.classLoaderFactory = classLoaderFactory != null ? classLoaderFactory : DEFAULT_CLASSLOADER_FACTORY;
    }

    /**
     * Obtain the rule sets that are currently available in this service.
     * @return an unmodifiable copy of the rule sets; never null, but possibly empty ...
     */
    public Collection<RuleSet> getRuleSets() {
        List<RuleSet> results = new ArrayList<RuleSet>();
        try {
            this.lock.readLock().lock();
            // Make a copy of the rule sets ...
            if (ruleSets.size() != 0) results.addAll(this.ruleSets.values());
        } finally {
            this.lock.readLock().unlock();
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Add the configuration for a sequencer, or update any existing one that represents the
     * {@link RuleSet#equals(Object) same rule set}
     * @param ruleSet the new rule set
     * @return true if the rule set was added, or false if the rule set was not added (because it wasn't necessary)
     * @throws IllegalArgumentException if <code>ruleSet</code> is null
     * @throws InvalidRuleSetException if the supplied rule set is invalid, incomplete, incorrectly defined, or uses a JSR-94
     * service provider that cannot be found
     * @see #updateRuleSet(RuleSet)
     * @see #removeRuleSet(RuleSet)
     */
    public boolean addRuleSet( RuleSet ruleSet ) {
        if (ruleSet == null) throw new IllegalArgumentException("The rule set reference may not be null");
        final String providerUri = ruleSet.getProviderUri();
        final String ruleSetName = ruleSet.getName();
        final String rules = ruleSet.getRules();
        final Map<?, ?> properties = ruleSet.getExecutionSetProperties();
        final Reader ruleReader = new StringReader(rules);
        boolean updatedRuleSets = false;
        try {
            this.lock.writeLock().lock();

            // Make sure the rule service provider is available ...
            RuleServiceProvider ruleServiceProvider = findRuleServiceProvider(ruleSet);
            assert ruleServiceProvider != null;

            // Now register a new execution set ...
            RuleAdministrator ruleAdmin = ruleServiceProvider.getRuleAdministrator();
            if (ruleAdmin == null) {
                String msg = "Unable to obtain the rule administrator for JSR-94 service provider {1} ({2}) while adding/updating rule set {3}";
                msg = StringUtil.createString(msg, providerUri, ruleSet.getComponentClassname(), ruleSetName);
                throw new InvalidRuleSetException(msg);
            }

            // Is there is an existing rule set and, if so, whether it has changed ...
            RuleSet existing = this.ruleSets.get(ruleSetName);

            // Create the rule execution set (do this before deregistering, in case there is a problem)...
            LocalRuleExecutionSetProvider ruleExecutionSetProvider = ruleAdmin.getLocalRuleExecutionSetProvider(null);
            RuleExecutionSet executionSet = ruleExecutionSetProvider.createRuleExecutionSet(ruleReader, properties);

            // We should add the execiting rule set if there wasn't one or if the rule set has changed ...
            boolean shouldAdd = existing == null || ruleSet.hasChanged(existing);
            if (existing != null && shouldAdd) {
                // There is an existing execution set and it needs to be updated, so deregister it ...
                ruleServiceProvider = deregister(ruleSet);
            }
            if (shouldAdd) {
                boolean rollback = false;
                try {
                    // Now register the new execution set and update the rule set managed by this service ...
                    ruleAdmin.registerRuleExecutionSet(ruleSetName, executionSet, null);
                    this.ruleSets.remove(ruleSet.getName());
                    this.ruleSets.put(ruleSet.getName(), ruleSet);
                    updatedRuleSets = true;
                } catch (Throwable t) {
                    rollback = true;
                    String msg = "Error adding/updating rule set '{1}'";
                    msg = StringUtil.createString(msg, ruleSet.getName());
                    throw new InvalidRuleSetException(msg, t);
                } finally {
                    if (rollback) {
                        try {
                            // There was a problem, so re-register the original existing rule set ...
                            if (existing != null) {
                                final String oldRules = existing.getRules();
                                final Map<?, ?> oldProperties = existing.getExecutionSetProperties();
                                final Reader oldRuleReader = new StringReader(oldRules);
                                ruleServiceProvider = findRuleServiceProvider(existing);
                                assert ruleServiceProvider != null;
                                executionSet = ruleExecutionSetProvider.createRuleExecutionSet(oldRuleReader, oldProperties);
                                ruleAdmin.registerRuleExecutionSet(ruleSetName, executionSet, null);
                                this.ruleSets.remove(ruleSetName);
                                this.ruleSets.put(ruleSetName, existing);
                            }
                        } catch (Throwable rollbackError) {
                            // There was a problem rolling back to the existing rule set, and we're going to throw the
                            // exception associated with the updated/new rule set, so just log this problem
                            this.logger.error(rollbackError, "Error rolling back rule set {} after new rule set failed", ruleSetName);
                        }
                    }
                }
            }
        } catch (InvalidRuleSetException e) {
            throw e;
        } catch (ConfigurationException t) {
            String msg = "Error while trying to obtain the rule administrator for JSR-94 service provider {1} ({2}) while adding/updating rule set {3}";
            msg = StringUtil.createString(msg, providerUri, ruleSet.getComponentClassname(), ruleSetName);
            throw new InvalidRuleSetException(msg);
        } catch (RemoteException t) {
            String msg = "Error using rule administrator for JSR-94 service provider {1} ({2}) while adding/updating rule set {3}";
            msg = StringUtil.createString(msg, providerUri, ruleSet.getComponentClassname(), ruleSetName);
            throw new InvalidRuleSetException(msg);
        } catch (IOException t) {
            String msg = "Error reading the rules and properties while adding/updating rule set {1}";
            msg = StringUtil.createString(msg, ruleSetName);
            throw new InvalidRuleSetException(msg);
        } catch (RuleExecutionSetDeregistrationException t) {
            String msg = "Error deregistering the existing rule set {1} before updating it";
            msg = StringUtil.createString(msg, ruleSetName);
            throw new InvalidRuleSetException(msg);
        } catch (RuleExecutionSetCreateException t) {
            String msg = "Error (re)creating the rule set {1}";
            msg = StringUtil.createString(msg, ruleSetName);
            throw new InvalidRuleSetException(msg);
        } finally {
            this.lock.writeLock().unlock();
        }
        return updatedRuleSets;
    }

    /**
     * Update the configuration for a sequencer, or add it if there is no {@link RuleSet#equals(Object) matching configuration}.
     * @param ruleSet the rule set to be updated
     * @return true if the rule set was updated, or false if the rule set was not updated (because it wasn't necessary)
     * @throws InvalidRuleSetException if the supplied rule set is invalid, incomplete, incorrectly defined, or uses a JSR-94
     * service provider that cannot be found
     * @see #addRuleSet(RuleSet)
     * @see #removeRuleSet(RuleSet)
     */
    public boolean updateRuleSet( RuleSet ruleSet ) {
        return addRuleSet(ruleSet);
    }

    /**
     * Remove the configuration for a sequencer.
     * @param ruleSet the rule set to be removed
     * @return true if the rule set was removed, or if it was not an existing rule set
     * @throws IllegalArgumentException if <code>ruleSet</code> is null
     * @throws SystemFailureException if the rule set was found but there was a problem removing it
     * @see #addRuleSet(RuleSet)
     * @see #updateRuleSet(RuleSet)
     */
    public boolean removeRuleSet( RuleSet ruleSet ) {
        if (ruleSet == null) throw new IllegalArgumentException("The rule set reference may not be null");
        boolean found = false;
        try {
            this.lock.writeLock().lock();
            try {
                deregister(ruleSet);
            } finally {
                // No matter what, remove the rule set from this service ...
                found = this.ruleSets.remove(ruleSet.getName()) != null;
            }
        } catch (Throwable t) {
            String msg = "Error removing rule set '{1}'";
            msg = StringUtil.createString(msg, ruleSet.getName());
            throw new SystemFailureException(msg, t);
        } finally {
            this.lock.writeLock().unlock();
        }
        return found;
    }

    /**
     * Get the logger for this system
     * @return the logger
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Set the logger for this system.
     * @param logger the logger, or null if the standard logging should be used
     */
    public void setLogger( Logger logger ) {
        this.logger = logger != null ? logger : Logger.getLogger(this.getClass());
    }

    /**
     * Execute the set of rules defined by the supplied rule set name. This method is safe to be concurrently called by multiple
     * threads, and is properly synchronized with the methods to {@link #addRuleSet(RuleSet) add},
     * {@link #updateRuleSet(RuleSet) update}, and {@link #removeRuleSet(RuleSet) remove} rule sets.
     * @param ruleSetName the {@link RuleSet#getName() name} of the {@link RuleSet} that should be used
     * @param globals the global variables
     * @param facts the facts
     * @return the results of executing the rule set
     * @throws IllegalArgumentException if the rule set name is null, empty or blank, or if there is no rule set with the given
     * name
     * @throws SystemFailureException if there is no JSR-94 rule service provider with the
     * {@link RuleSet#getProviderUri() RuleSet's provider URI}.
     */
    public List<?> executeRules( String ruleSetName, Map<String, Object> globals, Object... facts ) {
        ArgCheck.isNotEmpty(ruleSetName, "rule set name");
        List<?> result = null;
        List<?> factList = Arrays.asList(facts);
        try {
            this.lock.readLock().lock();

            // Find the rule set ...
            RuleSet ruleSet = this.ruleSets.get(ruleSetName);
            if (ruleSet == null) {
                throw new IllegalArgumentException("Unable to find rule set with name \"" + ruleSetName + "\"");
            }

            // Look up the provider ...
            RuleServiceProvider ruleServiceProvider = findRuleServiceProvider(ruleSet);
            assert ruleServiceProvider != null;

            // Create the rule session ...
            RuleRuntime ruleRuntime = ruleServiceProvider.getRuleRuntime();
            String executionSetName = ruleSet.getRuleSetUri();
            RuleSession session = ruleRuntime.createRuleSession(executionSetName, globals, RuleRuntime.STATELESS_SESSION_TYPE);
            try {
                StatelessRuleSession statelessSession = (StatelessRuleSession)session;
                result = statelessSession.executeRules(factList);
            } finally {
                session.release();
            }
            if (this.logger.isTraceEnabled()) {
                String msg = "Executed rule set '{1}' with globals {2} and facts {3} resulting in {4}";
                msg = StringUtil.createString(msg, ruleSetName, StringUtil.readableString(globals), StringUtil.readableString(facts), StringUtil.readableString(result));
                this.logger.trace(msg);
            }
        } catch (Throwable t) {
            String msg = "Error executing rule set '{1}' and with globals {2} and facts {3}";
            msg = StringUtil.createString(msg, ruleSetName, StringUtil.readableString(globals), StringUtil.readableString(facts));
            throw new SystemFailureException(msg, t);
        } finally {
            this.lock.readLock().unlock();
        }
        return result;
    }

    protected void removeAllRuleSets() {
        try {
            lock.writeLock().lock();
            for (RuleSet ruleSet : ruleSets.values()) {
                try {
                    deregister(ruleSet);
                } catch (Throwable t) {
                    logger.error(t, "Error removing rule set '{}' upon rule service shutdown", ruleSet.getName());
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Finds the JSR-94 service provider instance and returns it. If it could not be found, this method attempts to load it.
     * @param ruleSet the rule set for which the service provider is to be found; may not be null
     * @return the rule service provider; never null
     * @throws ConfigurationException if there is a problem loading the service provider
     * @throws InvalidRuleSetException if the service provider could not be found
     */
    private RuleServiceProvider findRuleServiceProvider( RuleSet ruleSet ) throws ConfigurationException {
        assert ruleSet != null;
        String providerUri = ruleSet.getProviderUri();
        RuleServiceProvider ruleServiceProvider = null;
        try {
            // If the provider could not be found, then a ConfigurationException will be thrown ...
            ruleServiceProvider = RuleServiceProviderManager.getRuleServiceProvider(providerUri);
        } catch (ConfigurationException e) {
            try {
                // Use JSR-94 to load the RuleServiceProvider instance ...
                ClassLoader loader = this.classLoaderFactory.getClassLoader(ruleSet.getComponentClasspathArray());
                // Don't call ClassLoader.loadClass(String), as this doesn't initialize the class!!
                Class.forName(ruleSet.getComponentClassname(), true, loader);
                ruleServiceProvider = RuleServiceProviderManager.getRuleServiceProvider(providerUri);
                this.logger.debug("Loaded the rule service provider {} ({})", providerUri, ruleSet.getComponentClassname());
            } catch (ConfigurationException ce) {
                throw ce;
            } catch (Throwable t) {
                String msg = "Unable to find or load the JSR-94 service provider {1} ({2})";
                msg = StringUtil.createString(msg, providerUri, ruleSet.getComponentClassname());
                throw new InvalidRuleSetException(msg, t);
            }
        }
        if (ruleServiceProvider == null) {
            String msg = "Unable to find or load the JSR-94 service provider {1} ({2})";
            msg = StringUtil.createString(msg, providerUri, ruleSet.getComponentClassname());
            throw new InvalidRuleSetException(msg);
        }
        return ruleServiceProvider;
    }

    /**
     * Deregister the supplied rule set, if it could be found. This method does nothing if any of the service provider components
     * could not be found.
     * @param ruleSet the rule set to be deregistered; may not be null
     * @return the service provider reference, or null if the service provider could not be found ...
     * @throws ConfigurationException
     * @throws RuleExecutionSetDeregistrationException
     * @throws RemoteException
     */
    private RuleServiceProvider deregister( RuleSet ruleSet ) throws ConfigurationException, RuleExecutionSetDeregistrationException, RemoteException {
        assert ruleSet != null;
        // Look up the provider ...
        String providerUri = ruleSet.getProviderUri();
        assert providerUri != null;

        // Look for the provider ...
        RuleServiceProvider ruleServiceProvider = RuleServiceProviderManager.getRuleServiceProvider(providerUri);
        if (ruleServiceProvider != null) {
            // Deregister the rule set ...
            RuleAdministrator ruleAdmin = ruleServiceProvider.getRuleAdministrator();
            if (ruleAdmin != null) {
                ruleAdmin.deregisterRuleExecutionSet(ruleSet.getRuleSetUri(), null);
            }
        }
        return ruleServiceProvider;
    }

}
