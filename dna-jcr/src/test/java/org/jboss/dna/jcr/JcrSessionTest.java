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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.io.InputStream;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.Calendar;
import java.util.Collections;
import java.util.UUID;
import javax.jcr.Item;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.ExecutionContextFactory;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.SimpleRepository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;

/**
 * @author jverhaeg
 */
public class JcrSessionTest {

    static final String WORKSPACE_NAME = JcrI18n.defaultWorkspaceName.text();

    static ExecutionContext executionContext;
    static SimpleRepository simpleRepository;
    static RepositoryConnectionFactory connectionFactory;
    static RepositoryConnection connection;
    static Repository repository;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ExecutionContextFactory factory = TestUtil.getExecutionContextFactory();
        executionContext = factory.create();
        simpleRepository = SimpleRepository.get(WORKSPACE_NAME);
        simpleRepository.setProperty(executionContext, "/a/b", "booleanProperty", true);
        simpleRepository.setProperty(executionContext, "/a/b/c", "stringProperty", "value");
        connectionFactory = TestUtil.createJackRabbitConnectionFactory(simpleRepository, executionContext);
        repository = new JcrRepository(factory, connectionFactory);
    }

    @AfterClass
    public static void afterClass() {
        SimpleRepository.shutdownAll();
    }

    private Session session;
    private ReferenceMap<UUID, Node> nodesByUuid;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        nodesByUuid = new ReferenceMap<UUID, Node>(ReferenceType.STRONG, ReferenceType.SOFT);
        session = repository.login();
    }

    @After
    public void after() throws Exception {
        if (session.isLive()) {
            session.logout();
        }
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoRepository() throws Exception {
        Graph graph = Graph.create(WORKSPACE_NAME, connectionFactory, executionContext);
        new JcrSession(null, WORKSPACE_NAME, graph, nodesByUuid);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoWorkspaceName() throws Exception {
        Graph graph = Graph.create(WORKSPACE_NAME, connectionFactory, executionContext);
        new JcrSession(repository, null, graph, nodesByUuid);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoGraph() throws Exception {
        new JcrSession(repository, WORKSPACE_NAME, null, nodesByUuid);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoUuid2NodeMap() throws Exception {
        Graph graph = Graph.create(WORKSPACE_NAME, connectionFactory, executionContext);
        new JcrSession(repository, WORKSPACE_NAME, graph, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowAddLockToken() throws Exception {
        session.addLockToken(null);
    }

    @Test
    public void shouldAllowCheckReadPermission() throws Exception {
        session.checkPermission("/", "read");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCheckPermissionWithNoPath() throws Exception {
        session.checkPermission(null, "read");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCheckPermissionWithEmptyPath() throws Exception {
        session.checkPermission("", "read");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCheckPermissionWithNoActions() throws Exception {
        session.checkPermission("/", null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCheckPermissionWithEmptyActions() throws Exception {
        session.checkPermission("/", "");
    }

    @Test( expected = AccessControlException.class )
    public void shouldNotAllowCheckNonReadPermission() throws Exception {
        session.checkPermission("/", "any");
    }

    @Test
    public void shouldProvideNoAttributes() throws Exception {
        assertThat(session.getAttribute(null), nullValue());
    }

    @Test
    public void shouldProvideEmptyAttributeNames() throws Exception {
        String[] names = session.getAttributeNames();
        assertThat(names, notNullValue());
        assertThat(names.length, is(0));
    }

    @Test
    public void shouldProvideAccessToRepository() throws Exception {
        assertThat(session.getRepository(), is(repository));
    }

    @Test
    public void shouldProvideAccessToWorkspace() throws Exception {
        assertThat(session.getWorkspace(), notNullValue());
    }

    @Test
    public void shouldIndicateLiveBeforeLogout() throws Exception {
        assertThat(session.isLive(), is(true));
    }

    @Test
    public void shouldAllowLogout() throws Exception {
        session.logout();
    }

    @Test
    public void shouldIndicateNotLiveAfterLogout() throws Exception {
        session.logout();
        assertThat(session.isLive(), is(false));
    }

    @Test
    public void shouldProvideUserId() throws Exception {
        assertThat(session.getUserID(), nullValue());
        Principal principal = Mockito.mock(Principal.class);
        stub(principal.getName()).toReturn("name");
        Subject subject = new Subject(false, Collections.singleton(principal), Collections.EMPTY_SET, Collections.EMPTY_SET);
        ExecutionContext executionContext = Mockito.mock(ExecutionContext.class);
        stub(executionContext.getSubject()).toReturn(subject);
        stub(executionContext.getLoginContext()).toReturn(Mockito.mock(LoginContext.class));
        Graph graph = Mockito.mock(Graph.class);
        stub(graph.getContext()).toReturn(executionContext);
        Session session = new JcrSession(repository, WORKSPACE_NAME, graph, nodesByUuid);
        try {
            assertThat(session.getUserID(), is("name"));
        } finally {
            session.logout();
        }
    }

    @Test
    public void shouldProvideRootNode() throws Exception {
        ReferenceMap<UUID, Node> nodesByUuid = new ReferenceMap<UUID, Node>(ReferenceType.STRONG, ReferenceType.SOFT);
        Graph graph = Graph.create(WORKSPACE_NAME, connectionFactory, executionContext);
        Session session = new JcrSession(repository, WORKSPACE_NAME, graph, nodesByUuid);
        assertThat(nodesByUuid.isEmpty(), is(true));
        Node root = session.getRootNode();
        assertThat(root, notNullValue());
        UUID uuid = ((JcrRootNode)root).getInternalUuid();
        assertThat(uuid, notNullValue());
        assertThat(nodesByUuid.get(uuid), is(root));
    }

    @Test
    public void shouldProvideItemsByPath() throws Exception {
        Item item = session.getItem("/a");
        assertThat(item, instanceOf(Node.class));
        item = session.getItem("/a/b");
        assertThat(item, instanceOf(Node.class));
        item = session.getItem("/a/b/booleanProperty");
        assertThat(item, instanceOf(Property.class));
    }

    @Test
    public void shouldProvideValueFactory() throws Exception {
        ValueFactory factory = session.getValueFactory();
        assertThat(factory, notNullValue());
        assertThat(factory.createValue(false), notNullValue());
        assertThat(factory.createValue(Calendar.getInstance()), notNullValue());
        assertThat(factory.createValue(0.0), notNullValue());
        assertThat(factory.createValue(Mockito.mock(InputStream.class)), notNullValue());
        assertThat(factory.createValue(0L), notNullValue());
        Node node = Mockito.mock(Node.class);
        stub(node.getUUID()).toReturn(UUID.randomUUID().toString());
        assertThat(factory.createValue(node), notNullValue());
        assertThat(factory.createValue(""), notNullValue());
        assertThat(factory.createValue("", PropertyType.BINARY), notNullValue());
    }

    @Test
    public void shouldNotHavePendingChanges() throws Exception {
        assertThat(session.hasPendingChanges(), is(false));
    }

    @Test
    public void shouldAllowImpersonation() throws Exception {
        assertThat(session.impersonate(null), notNullValue());
    }

    @Test
    public void shouldProvideItemExists() throws Exception {
        assertThat(session.itemExists("/a/b"), is(true));
        assertThat(session.itemExists("/a/c"), is(false));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowItemExistsWithNoPath() throws Exception {
        session.itemExists(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowItemExistsWithEmptyPath() throws Exception {
        session.itemExists("");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNoNamespaceUri() throws Exception {
        session.getNamespacePrefix(null);
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotProvidePrefixForUnknownUri() throws Exception {
        session.getNamespacePrefix("bogus");
    }

    @Test
    public void shouldProvideNamespacePrefix() throws Exception {
        assertThat(session.getNamespacePrefix("http://www.jboss.org/dna/1.0"), is("dna"));
        assertThat(session.getNamespacePrefix("http://www.jcp.org/jcr/1.0"), is("jcr"));
        assertThat(session.getNamespacePrefix("http://www.jcp.org/jcr/mix/1.0"), is("mix"));
        assertThat(session.getNamespacePrefix("http://www.jcp.org/jcr/nt/1.0"), is("nt"));
        assertThat(session.getNamespacePrefix("http://www.jcp.org/jcr/sv/1.0"), is("sv"));
        // assertThat(session.getNamespacePrefix("http://www.w3.org/XML/1998/namespace"), is("xml"));
    }

    @Test
    public void shouldProvideNamespacePrefixes() throws Exception {
        String[] prefixes = session.getNamespacePrefixes();
        assertThat(prefixes, notNullValue());
        assertThat(prefixes.length, is(not(0)));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNoNamespacePrefix() throws Exception {
        session.getNamespaceURI(null);
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotProvideUriForUnknownPrefix() throws Exception {
        session.getNamespaceURI("bogus");
    }

    @Test
    public void shouldProvideNamespaceUri() throws Exception {
        assertThat(session.getNamespaceURI("dna"), is("http://www.jboss.org/dna/1.0"));
        assertThat(session.getNamespaceURI("jcr"), is("http://www.jcp.org/jcr/1.0"));
        assertThat(session.getNamespaceURI("mix"), is("http://www.jcp.org/jcr/mix/1.0"));
        assertThat(session.getNamespaceURI("nt"), is("http://www.jcp.org/jcr/nt/1.0"));
        assertThat(session.getNamespaceURI("sv"), is("http://www.jcp.org/jcr/sv/1.0"));
        // assertThat(session.getNamespaceURI("xml"), is("http://www.w3.org/XML/1998/namespace"));
    }
}
