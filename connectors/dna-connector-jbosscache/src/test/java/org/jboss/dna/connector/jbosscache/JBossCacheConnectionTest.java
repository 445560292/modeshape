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
package org.jboss.dna.connector.jbosscache;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.dna.spi.DnaLexicon;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PathNotFoundException;
import org.jboss.dna.spi.graph.connection.BasicExecutionContext;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class JBossCacheConnectionTest {

    private JBossCacheConnection connection;
    private CacheFactory<Name, Object> cacheFactory;
    private Cache<Name, Object> cache;
    private ExecutionContext context;
    private PathFactory pathFactory;
    @Mock
    private JBossCacheSource source;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new BasicExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.NAMESPACE_PREFIX, DnaLexicon.NAMESPACE_URI);
        pathFactory = context.getValueFactories().getPathFactory();
        cacheFactory = new DefaultCacheFactory<Name, Object>();
        cache = cacheFactory.createCache();
        connection = new JBossCacheConnection(source, cache);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToInstantiateIfCacheReferenceIsNull() {
        cache = null;
        connection = new JBossCacheConnection(source, cache);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToInstantiateIfSourceReferenceIsNull() {
        source = null;
        connection = new JBossCacheConnection(source, cache);
    }

    @Test
    public void shouldInstantiateWithValidSourceAndCacheReferences() {
        assertThat(connection, is(notNullValue()));
    }

    @Test
    public void shouldDelegateToTheSourceForTheConnectionsSourceName() {
        stub(source.getName()).toReturn("the source name");
        assertThat(connection.getSourceName(), is("the source name"));
        verify(source).getName();
    }

    @Test
    public void shouldDelegateToTheSourceForTheConnectionsDefaultCachePolicy() {
        CachePolicy policy = mock(CachePolicy.class);
        stub(source.getDefaultCachePolicy()).toReturn(policy);
        assertThat(connection.getDefaultCachePolicy(), is(sameInstance(policy)));
        verify(source).getDefaultCachePolicy();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldGetTheRootFromTheCacheWhenPinged() {
        cache = mock(Cache.class);
        connection = new JBossCacheConnection(source, cache);
        stub(cache.getRoot()).toReturn(null);
        assertThat(connection.ping(1, TimeUnit.SECONDS), is(true));
        verify(cache).getRoot();
    }

    @Test
    public void shouldHaveNoOpListenerWhenCreated() {
        assertThat(connection.getListener(), is(sameInstance(JBossCacheConnection.NO_OP_LISTENER)));
    }

    @Test
    public void shouldUseNoOpListenerWhenSettingListenerToNull() {
        connection.setListener(null);
        assertThat(connection.getListener(), is(sameInstance(JBossCacheConnection.NO_OP_LISTENER)));
    }

    @Test
    public void shouldSetListenerToNonNullValue() {
        RepositorySourceListener listener = mock(RepositorySourceListener.class);
        connection.setListener(listener);
        assertThat(connection.getListener(), is(sameInstance(listener)));
        connection.setListener(null);
        assertThat(connection.getListener(), is(sameInstance(JBossCacheConnection.NO_OP_LISTENER)));
    }

    @Test
    public void shouldGetUuidPropertyNameFromSouceAndShouldNotChangeDuringLifetimeOfConnection() {
        stub(source.getUuidPropertyName()).toReturn(DnaLexicon.PropertyNames.UUID);
        Name name = connection.getUuidPropertyName(context);
        verify(source).getUuidPropertyName();
        assertThat(name.getLocalName(), is("uuid"));
        assertThat(name.getNamespaceUri(), is(DnaLexicon.NAMESPACE_URI));
        stub(source.getUuidPropertyName()).toReturn("something else");
        for (int i = 0; i != 10; ++i) {
            Name name2 = connection.getUuidPropertyName(context);
            assertThat(name2, is(sameInstance(name)));
        }
        verifyNoMoreInteractions(source);
    }

    @Test
    public void shouldGenerateUuid() {
        for (int i = 0; i != 100; ++i) {
            assertThat(connection.generateUuid(), is(notNullValue()));
        }
    }

    @Test
    public void shouldCreateFullyQualifiedNodeOfPathSegmentsFromPath() {
        Path path = pathFactory.create("/a/b/c/d");
        Fqn<Path.Segment> fqn = connection.getFullyQualifiedName(path);
        assertThat(fqn.size(), is(4));
        assertThat(fqn.isRoot(), is(false));
        for (int i = 0; i != path.size(); ++i) {
            assertThat(fqn.get(i), is(path.getSegment(i)));
        }
    }

    @Test
    public void shouldCreateFullyQualifiedNodeOfPathSegmentsFromRootPath() {
        Path path = pathFactory.createRootPath();
        Fqn<Path.Segment> fqn = connection.getFullyQualifiedName(path);
        assertThat(fqn.size(), is(0));
        assertThat(fqn.isRoot(), is(true));
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToCreateFullyQualifiedNodeFromNullPath() {
        connection.getFullyQualifiedName((Path)null);
    }

    @Test
    public void shouldCreateFullyQualifiedNodeFromPathSegment() {
        Path.Segment segment = pathFactory.createSegment("a");
        Fqn<Path.Segment> fqn = connection.getFullyQualifiedName(segment);
        assertThat(fqn.size(), is(1));
        assertThat(fqn.isRoot(), is(false));
        assertThat(fqn.get(0), is(segment));
    }

    @Test( expected = AssertionError.class )
    public void shouldFailToCreateFullyQualifiedNodeFromNullPathSegment() {
        connection.getFullyQualifiedName((Path.Segment)null);
    }

    @Test
    public void shouldCreatePathFromFullyQualifiedNode() {
        Path path = pathFactory.create("/a/b/c/d");
        Fqn<Path.Segment> fqn = connection.getFullyQualifiedName(path);
        assertThat(connection.getPath(pathFactory, fqn), is(path));
    }

    @Test
    public void shouldCreateRootPathFromRootFullyQualifiedNode() {
        Path path = pathFactory.createRootPath();
        Fqn<Path.Segment> fqn = connection.getFullyQualifiedName(path);
        assertThat(connection.getPath(pathFactory, fqn), is(path));
    }

    @Test
    public void shouldGetNodeIfItExistsInCache() {
        // Set up the cache with data ...
        Name uuidProperty = connection.getUuidPropertyName(context);
        Path[] paths = {pathFactory.create("/a"), pathFactory.create("/a/b"), pathFactory.create("/a/b/c")};
        Path nonExistantPath = pathFactory.create("/a/d");
        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        cache.put(Fqn.fromList(paths[0].getSegmentsList()), uuidProperty, uuids[0]);
        cache.put(Fqn.fromList(paths[1].getSegmentsList()), uuidProperty, uuids[1]);
        cache.put(Fqn.fromList(paths[2].getSegmentsList()), uuidProperty, uuids[2]);
        Node<Name, Object> nodeA = cache.getNode(Fqn.fromList(paths[0].getSegmentsList()));
        Node<Name, Object> nodeB = cache.getNode(Fqn.fromList(paths[1].getSegmentsList()));
        Node<Name, Object> nodeC = cache.getNode(Fqn.fromList(paths[2].getSegmentsList()));
        Node<Name, Object> nodeD = cache.getNode(Fqn.fromList(nonExistantPath.getSegmentsList()));
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeB, is(notNullValue()));
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeD, is(nullValue()));
        // Test the getNode(...) method for existing nodes ...
        assertThat(connection.getNode(context, paths[0]), is(sameInstance(nodeA)));
        assertThat(connection.getNode(context, paths[1]), is(sameInstance(nodeB)));
        assertThat(connection.getNode(context, paths[2]), is(sameInstance(nodeC)));
    }

    @Test
    public void shouldThrowExceptionWithLowestExistingNodeFromGetNodeIfTheNodeDoesNotExist() {
        // Set up the cache with data ...
        Name uuidProperty = connection.getUuidPropertyName(context);
        Path[] paths = {pathFactory.create("/a"), pathFactory.create("/a/b"), pathFactory.create("/a/b/c")};
        Path nonExistantPath = pathFactory.create("/a/d");
        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        cache.put(Fqn.fromList(paths[0].getSegmentsList()), uuidProperty, uuids[0]);
        cache.put(Fqn.fromList(paths[1].getSegmentsList()), uuidProperty, uuids[1]);
        cache.put(Fqn.fromList(paths[2].getSegmentsList()), uuidProperty, uuids[2]);
        Node<Name, Object> nodeA = cache.getNode(Fqn.fromList(paths[0].getSegmentsList()));
        Node<Name, Object> nodeB = cache.getNode(Fqn.fromList(paths[1].getSegmentsList()));
        Node<Name, Object> nodeC = cache.getNode(Fqn.fromList(paths[2].getSegmentsList()));
        Node<Name, Object> nodeD = cache.getNode(Fqn.fromList(nonExistantPath.getSegmentsList()));
        assertThat(nodeA, is(notNullValue()));
        assertThat(nodeB, is(notNullValue()));
        assertThat(nodeC, is(notNullValue()));
        assertThat(nodeD, is(nullValue()));
        try {
            connection.getNode(context, nonExistantPath);
            fail();
        } catch (PathNotFoundException e) {
            assertThat(e.getLowestAncestorThatDoesExist(), is(paths[0]));
        }
    }

}
