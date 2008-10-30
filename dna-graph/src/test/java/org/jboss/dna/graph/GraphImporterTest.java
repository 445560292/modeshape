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
package org.jboss.dna.graph;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.transaction.xa.XAResource;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositoryConnectionFactory;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.connectors.RepositorySourceListener;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.requests.CompositeRequest;
import org.jboss.dna.graph.requests.CreateNodeRequest;
import org.jboss.dna.graph.requests.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class GraphImporterTest {

    private Graph graph;
    private GraphImporter importer;
    private String sourceName;
    private ExecutionContext context;
    private URI xmlContent;
    private MockRepositoryConnection connection;
    private Request lastExecutedRequest;
    private Path destinationPath;
    @Mock
    private RepositoryConnectionFactory sources;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        xmlContent = new File("src/test/resources/repositoryImporterTestData1.xml").toURI();
        context = new BasicExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        context.getNamespaceRegistry().register("jcr", "http://www.jcp.org/jcr/1.0");
        context.getNamespaceRegistry().register("nt", "http://www.jcp.org/jcr/nt/1.0");
        sourceName = "sourceA";
        destinationPath = context.getValueFactories().getPathFactory().create("/a/b");
        graph = Graph.create(sourceName, sources, context);
        importer = new GraphImporter(graph);
        connection = new MockRepositoryConnection();
        stub(sources.createConnection(sourceName)).toReturn(connection);
    }

    @Test
    public void shouldImportXmlContentAndGenerateTheCorrectCommands() throws Exception {
        System.out.println(xmlContent);
        Graph.Batch batch = importer.importXml(xmlContent, new Location(destinationPath));
        batch.execute();
        // 'lastExecutedCommand'
        assertThat(lastExecutedRequest, is(instanceOf(CompositeRequest.class)));
        Iterator<Request> iter = ((CompositeRequest)lastExecutedRequest).iterator();
        // assertCreateNode(iter, "/a/b/", "jcr:primaryType={http://www.jboss.org/dna/xml/1.0}document");
        assertCreateNode(iter, "/a/b/dna:system[1]", "jcr:primaryType={http://www.jcp.org/jcr/nt/1.0}unstructured");
        assertCreateNode(iter, "/a/b/dna:system[1]/dna:sources[1]", "jcr:primaryType={http://www.jcp.org/jcr/nt/1.0}unstructured");
        assertCreateNode(iter,
                         "/a/b/dna:system[1]/dna:sources[1]/sourceA[1]",
                         "repositoryName=repositoryA",
                         "retryLimit=3",
                         "jcr:primaryType={http://www.jboss.org/dna}xyz",
                         "dna:classname=org.jboss.dna.connector.inmemory.InMemoryRepositorySource");
        assertCreateNode(iter,
                         "/a/b/dna:system[1]/dna:sources[1]/sourceB[1]",
                         "repositoryName=repositoryB",
                         "jcr:primaryType={http://www.jcp.org/jcr/nt/1.0}unstructured",
                         "dna:classname=org.jboss.dna.connector.inmemory.InMemoryRepositorySource");
        assertThat(iter.hasNext(), is(false));
    }

    public void assertCreateNode( Iterator<Request> iterator,
                                  String path,
                                  String... properties ) {
        Request nextCommand = iterator.next();
        assertThat(nextCommand, is(instanceOf(CreateNodeRequest.class)));
        CreateNodeRequest createNode = (CreateNodeRequest)nextCommand;
        Path expectedPath = context.getValueFactories().getPathFactory().create(path);
        assertThat(createNode.at().getPath(), is(expectedPath));
        Map<Name, Property> propertiesByName = new HashMap<Name, Property>();
        for (Property prop : createNode.properties()) {
            propertiesByName.put(prop.getName(), prop);
        }
        for (String propertyStr : properties) {
            if (propertyStr == "any properties") {
                propertiesByName.clear();
                break;
            }
            Matcher matcher = Pattern.compile("([^=]+)=(.*)").matcher(propertyStr);
            if (!matcher.matches()) continue;
            System.out.println("Property: " + propertyStr + " ==> " + matcher);
            Name propertyName = context.getValueFactories().getNameFactory().create(matcher.group(1));
            System.out.println("Property name: " + matcher.group(1));
            String value = matcher.group(2); // doesn't handle multiple values!!
            if (value.trim().length() == 0) value = null;
            Property actual = propertiesByName.remove(propertyName);
            Property expectedProperty = context.getPropertyFactory().create(propertyName, value);
            assertThat("missing property " + propertyName, actual, is(expectedProperty));
        }
        if (!propertiesByName.isEmpty()) {
            System.out.println("Properties for " + path + "\n" + StringUtil.readableString(propertiesByName));
        }
        assertThat(propertiesByName.isEmpty(), is(true));
    }

    protected class MockRepositoryConnection implements RepositoryConnection {
        public void close() {
        }

        @SuppressWarnings( "synthetic-access" )
        public void execute( ExecutionContext context,
                             Request request ) throws RepositorySourceException {
            lastExecutedRequest = request;
        }

        public CachePolicy getDefaultCachePolicy() {
            return null;
        }

        @SuppressWarnings( "synthetic-access" )
        public String getSourceName() {
            return sourceName;
        }

        public XAResource getXAResource() {
            return null;
        }

        public boolean ping( long time,
                             TimeUnit unit ) {
            return true;
        }

        public void setListener( RepositorySourceListener listener ) {
        }
    }

}
