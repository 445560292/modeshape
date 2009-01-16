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
package org.jboss.dna.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.SimpleRepository;
import org.jboss.dna.graph.connector.SimpleRepositorySource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedRepositorySourceTest {

    private FederatedRepositorySource source;
    private String sourceName;
    private String repositoryName;
    private String username;
    private String credentials;
    private String configurationSourceName;
    private String securityDomain;
    private SimpleRepository configRepository;
    private SimpleRepositorySource configRepositorySource;
    private RepositoryConnection configRepositoryConnection;
    private ExecutionContext context;
    @Mock
    private RepositoryConnection connection;
    @Mock
    private RepositoryConnectionFactory connectionFactory;
    @Mock
    private ExecutionContext executionContextFactory;
    @Mock
    private RepositoryContext repositoryContext;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        configurationSourceName = "configuration source name";
        repositoryName = "Test Repository";
        securityDomain = "security domain";
        source = new FederatedRepositorySource(repositoryName);
        sourceName = "federated source";
        username = "valid username";
        credentials = "valid password";
        source.setName(sourceName);
        source.setUsername(username);
        source.setPassword(credentials);
        source.setConfigurationSourceName(configurationSourceName);
        source.setConfigurationSourcePath("/dna:repositories/Test Repository");
        source.setSecurityDomain(securityDomain);
        source.initialize(repositoryContext);
        configRepository = SimpleRepository.get("Configuration Repository");
        configRepository.setProperty(context, "/dna:repositories/Test Repository/dna:federation/", "dna:timeToExpire", "100000");
        configRepository.setProperty(context, "/dna:repositories/Test Repository/dna:federation/", "dna:timeToCache", "100000");
        configRepository.setProperty(context,
                                     "/dna:repositories/Test Repository/dna:federation/dna:cache/cache source",
                                     "dna:projectionRules",
                                     "/ => /");
        configRepository.setProperty(context,
                                     "/dna:repositories/Test Repository/dna:federation/dna:projections/source 1/",
                                     "dna:projectionRules",
                                     "/ => /s1");
        configRepository.setProperty(context,
                                     "/dna:repositories/Test Repository/dna:federation/dna:projections/source 2/",
                                     "dna:projectionRules",
                                     "/ => /s1");
        configRepositorySource = new SimpleRepositorySource();
        configRepositorySource.setRepositoryName(configRepository.getRepositoryName());
        configRepositorySource.setName(configurationSourceName);
        configRepositoryConnection = configRepositorySource.getConnection();
        stub(repositoryContext.getExecutionContext()).toReturn(executionContextFactory);
        stub(repositoryContext.getRepositoryConnectionFactory()).toReturn(connectionFactory);
        stub(connectionFactory.createConnection(configurationSourceName)).toReturn(configRepositoryConnection);
        stub(executionContextFactory.with(eq(securityDomain), anyCallbackHandler())).toReturn(context);
    }

    protected static CallbackHandler anyCallbackHandler() {
        return argThat(new ArgumentMatcher<CallbackHandler>() {
            @Override
            public boolean matches( Object callback ) {
                return callback != null;
            }
        });
    }

    @After
    public void afterEach() throws Exception {
        SimpleRepository.shutdownAll();
    }

    @Test
    public void shouldReturnNonNullCapabilities() {
        assertThat(source.getCapabilities(), is(notNullValue()));
    }

    @Test
    public void shouldSupportSameNameSiblings() {
        assertThat(source.getCapabilities().supportsSameNameSiblings(), is(true));
    }

    @Test
    public void shouldSupportUpdates() {
        assertThat(source.getCapabilities().supportsUpdates(), is(true));
    }

    @Test
    public void shouldCreateConnectionsByAuthenticateUsingFederationRepository() throws Exception {
        connection = source.getConnection();
        assertThat(connection, is(notNullValue()));
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldNotCreateConnectionWhenAuthenticationFails() throws Exception {
        // Stub the execution context factory to throw a LoginException to simulate failed authentication
        stub(executionContextFactory.with(eq(securityDomain), anyCallbackHandler())).toThrow(new LoginException());
        source.getConnection();
    }

    @Test( expected = NullPointerException.class )
    public void shouldPropogateAllExceptionsExceptLoginExceptionThrownFromExecutionContextFactory() throws Exception {
        // Stub the execution context factory to throw a LoginException to simulate failed authentication
        stub(executionContextFactory.with(eq(securityDomain), anyCallbackHandler())).toThrow(new NullPointerException());
        source.getConnection();
    }

    @Test
    public void shouldHaveNameSuppliedInConstructor() {
        source = new FederatedRepositorySource(repositoryName);
        assertThat(source.getRepositoryName(), is(repositoryName));
    }

    @Test
    public void shouldHaveNullSourceNameUponConstruction() {
        source = new FederatedRepositorySource(repositoryName);
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingName() {
        source.setName("Something");
        assertThat(source.getName(), is("Something"));
        source.setName("another name");
        assertThat(source.getName(), is("another name"));
    }

    @Test
    public void shouldAllowSettingNameToNull() {
        source.setName("some name");
        source.setName(null);
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingUsername() {
        source.setUsername("Something");
        assertThat(source.getUsername(), is("Something"));
        source.setUsername("another name");
        assertThat(source.getUsername(), is("another name"));
    }

    @Test
    public void shouldAllowSettingUsernameToNull() {
        source.setUsername("some name");
        source.setUsername(null);
        assertThat(source.getUsername(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingCredentials() {
        source.setPassword("Something");
        assertThat(source.getPassword(), is("Something"));
        source.setPassword("another name");
        assertThat(source.getPassword(), is("another name"));
    }

    @Test
    public void shouldAllowSettingCredentialsToNull() {
        source.setPassword("some name");
        source.setPassword(null);
        assertThat(source.getPassword(), is(nullValue()));
    }

    @Test
    public void shouldHaveDefaultRetryLimit() {
        assertThat(source.getRetryLimit(), is(FederatedRepositorySource.DEFAULT_RETRY_LIMIT));
    }

    @Test
    public void shouldSetRetryLimitToZeroWhenSetWithNonPositiveValue() {
        source.setRetryLimit(0);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-1);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-100);
        assertThat(source.getRetryLimit(), is(0));
    }

    @Test
    public void shouldAllowRetryLimitToBeSet() {
        for (int i = 0; i != 100; ++i) {
            source.setRetryLimit(i);
            assertThat(source.getRetryLimit(), is(i));
        }
    }

    @Test
    public void shouldCreateJndiReferenceAndRecreatedObjectFromReference() throws Exception {
        int retryLimit = 100;
        source.setPassword(credentials);
        source.setUsername(username);
        source.setRetryLimit(retryLimit);
        source.setName("Some source");
        source.setConfigurationSourceName("config source");
        source.setConfigurationSourcePath("/a/b/c");

        Reference ref = source.getReference();
        assertThat(ref.getClassName(), is(FederatedRepositorySource.class.getName()));
        assertThat(ref.getFactoryClassName(), is(FederatedRepositorySource.class.getName()));

        Map<String, Object> refAttributes = new HashMap<String, Object>();
        Enumeration<RefAddr> enumeration = ref.getAll();
        while (enumeration.hasMoreElements()) {
            RefAddr addr = enumeration.nextElement();
            refAttributes.put(addr.getType(), addr.getContent());
        }

        assertThat((String)refAttributes.remove(FederatedRepositorySource.USERNAME), is(username));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.PASSWORD), is(credentials));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.SOURCE_NAME), is(source.getName()));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.REPOSITORY_NAME), is(repositoryName));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.RETRY_LIMIT), is(Integer.toString(retryLimit)));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.CONFIGURATION_SOURCE_NAME),
                   is(source.getConfigurationSourceName()));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.CONFIGURATION_SOURCE_PATH), is("/a/b/c/"));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.SECURITY_DOMAIN), is(securityDomain));
        assertThat(refAttributes.isEmpty(), is(true));

        // Recreate the object, use a newly constructed source ...
        ObjectFactory factory = new FederatedRepositorySource();
        Name name = mock(Name.class);
        Context context = mock(Context.class);
        Hashtable<?, ?> env = new Hashtable<Object, Object>();
        FederatedRepositorySource recoveredSource = (FederatedRepositorySource)factory.getObjectInstance(ref, name, context, env);
        assertThat(recoveredSource, is(notNullValue()));

        assertThat(recoveredSource.getName(), is(source.getName()));
        assertThat(recoveredSource.getUsername(), is(source.getUsername()));
        assertThat(recoveredSource.getPassword(), is(source.getPassword()));
        assertThat(recoveredSource.getRepositoryName(), is(source.getRepositoryName()));
        assertThat(recoveredSource.getRetryLimit(), is(source.getRetryLimit()));
        assertThat(recoveredSource.getConfigurationSourceName(), is(source.getConfigurationSourceName()));
        assertThat(recoveredSource.getConfigurationSourcePath(), is(source.getConfigurationSourcePath()));
        assertThat(recoveredSource.getSecurityDomain(), is(source.getSecurityDomain()));

        assertThat(recoveredSource.equals(source), is(true));
        assertThat(source.equals(recoveredSource), is(true));
    }
}
