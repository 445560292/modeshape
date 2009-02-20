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

import java.io.File;
import java.net.URI;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Collections;
import java.util.Properties;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.jackrabbit.test.JCRTestSuite;
import org.apache.jackrabbit.test.RepositoryStub;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.GraphImporter;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;

/**
 * Test suite to wrap Apache Jackrabbit JCR technology compatibility kit (TCK) tests.
 */
public class JackrabbitJcrTckTest {

    /**
     * Wrapper so that the Jackrabbit TCK test suite gets picked up by the DNA Maven test target.
     * 
     * @return a new instance of {@link JCRTestSuite}.
     */
    public static Test suite() {
        // Uncomment this to execute all tests
        // return new JCRTestSuite();

        // Uncomment this to execute level one tests only
        return new JcrLevelOneTestSuite();
    }

    /** Test suite that includes all of the Jackrabbit TCK API tests <b>except</b> the level two tests. */
    private static class JcrLevelOneTestSuite extends TestSuite {
        public JcrLevelOneTestSuite() {
            super("JCR 1.0 Level 1 API tests");

            // We currently don't pass the tests in those suites that are commented out
            // See https://jira.jboss.org/jira/browse/DNA-285

            addTestSuite(org.apache.jackrabbit.test.api.RootNodeTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.NodeReadMethodsTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.PropertyTypeTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.NodeDiscoveringNodeTypesTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.BinaryPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.BooleanPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.DatePropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.DoublePropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.LongPropertyTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.NamePropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.PathPropertyTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.ReferencePropertyTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.StringPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.UndefinedPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.NamespaceRegistryReadMethodsTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.NamespaceRemappingTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.NodeIteratorTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.PropertyReadMethodsTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.RepositoryDescriptorTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.SessionReadMethodsTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.WorkspaceReadMethodsTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.ReferenceableRootNodesTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.ExportSysViewTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.ExportDocViewTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.RepositoryLoginTest.class);

            // These might not all be level one tests
            // addTestSuite(org.apache.jackrabbit.test.api.query.XPathPosIndexTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.XPathDocOrderTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.XPathOrderByTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.XPathJcrPathTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.DerefQueryLevel1Test.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.GetLanguageTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.GetPersistentQueryPathLevel1Test.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.GetStatementTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.GetSupportedQueryLanguagesTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.GetPropertyNamesTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.PredicatesTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.SimpleSelectionTest.class);

            // The tests in this suite are level one
            // addTest(org.apache.jackrabbit.test.api.nodetype.TestAll.suite());
        }

    }

    /**
     * Concrete implementation of {@link RepositoryStub} based on DNA-specific configuration.
     */
    public static class InMemoryRepositoryStub extends RepositoryStub {
        private Repository repository;
        protected RepositoryConnection connection;
        protected AccessControlContext accessControlContext = AccessController.getContext();

        private Credentials credentials = new Credentials() {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public AccessControlContext getAccessControlContext() {
                return accessControlContext;
            }
        };

        protected ExecutionContext executionContext = new ExecutionContext() {

            @Override
            public ExecutionContext create( AccessControlContext accessControlContext ) {
                return executionContext;
            }
        };

        protected RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) {
                return connection;
            }
        };

        public InMemoryRepositoryStub( Properties env ) {
            super(env);

            // Create the in-memory (DNA) repository
            InMemoryRepositorySource source = new InMemoryRepositorySource();

            // Various calls will fail if you do not set a non-null name for the source
            source.setName("TestRepositorySource");

            // Wrap a connection to the in-memory (DNA) repository in a (JCR) repository
            connection = source.getConnection();
            repository = new JcrRepository(Collections.<String, String>emptyMap(), executionContext.create(accessControlContext),
                                           connectionFactory);

            // Set up some sample nodes in the graph to match the expected test configuration
            try {

                // TODO: Should there be an easier way to define these since they will be needed for all JCR repositories?
                executionContext.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
                executionContext.getNamespaceRegistry().register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
                executionContext.getNamespaceRegistry().register(JcrNtLexicon.Namespace.PREFIX, JcrNtLexicon.Namespace.URI);
                executionContext.getNamespaceRegistry().register("sv", "http://www.jcp.org/jcr/sv/1.0");

                Path destinationPath = executionContext.getValueFactories().getPathFactory().create("/");
                Graph graph = Graph.create(source.getName(), connectionFactory, executionContext);
                GraphImporter importer = new GraphImporter(graph);

                URI xmlContent = new File("src/test/resources/repositoryJackrabbitTck.xml").toURI();
                Graph.Batch batch = importer.importXml(xmlContent, Location.create(destinationPath));
                batch.execute();

            } catch (Exception ex) {
                // The TCK tries to quash this exception. Print it out to be more obvious.
                ex.printStackTrace();
                throw new IllegalStateException("Repository initialization failed.", ex);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.jackrabbit.test.RepositoryStub#getSuperuserCredentials()
         */
        @Override
        public Credentials getSuperuserCredentials() {
            // TODO: Why must we override this method? The default TCK implementation just returns a particular instance of
            // SimpleCredentials.
            return credentials;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.jackrabbit.test.RepositoryStub#getReadOnlyCredentials()
         */
        @Override
        public Credentials getReadOnlyCredentials() {
            // TODO: Why must we override this method? The default TCK implementation just returns a particular instance of
            // SimpleCredentials.
            return credentials;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.jackrabbit.test.RepositoryStub#getRepository()
         */
        @Override
        public Repository getRepository() {
            return repository;
        }

    }

}
