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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.basic.BasicName;
import org.jboss.dna.jcr.nodetype.NodeTypeTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * BDD test cases for property and child node definition inheritance. Could be part of RepositoryNodeTypeManagerTest, but split
 * off to isolate tests for this behavior vs. projection/inference and registration/unregistration behavior.
 */
public class ItemDefinitionTest {

    private static final Name NODE_TYPE_A = new BasicName(TestLexicon.Namespace.URI, "nodeA");
    private static final Name NODE_TYPE_B = new BasicName(TestLexicon.Namespace.URI, "nodeB");
    private static final Name NODE_TYPE_C = new BasicName(TestLexicon.Namespace.URI, "nodeC");

    private static final Name SINGLE_PROP1 = new BasicName(TestLexicon.Namespace.URI, "singleProp1");
    private static final Name SINGLE_PROP2 = new BasicName(TestLexicon.Namespace.URI, "singleProp2");

    private String workspaceName;
    protected ExecutionContext context;
    private InMemoryRepositorySource source;
    private JcrWorkspace workspace;
    private JcrSession session;
    private Graph graph;
    private RepositoryConnectionFactory connectionFactory;
    private RepositoryNodeTypeManager repoTypeManager;
    private Map<String, Object> sessionAttributes;
    private ValueFactory<Name> nameFactory;
    @Mock
    private JcrRepository repository;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        workspaceName = "workspace1";
        final String repositorySourceName = "repository";

        // Set up the source ...
        source = new InMemoryRepositorySource();
        source.setName(workspaceName);
        source.setDefaultWorkspaceName(workspaceName);

        // Set up the execution context ...
        context = new ExecutionContext();
        // Register the test namespace
        context.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        // Set up the initial content ...
        graph = Graph.create(source, context);

        // Make sure the path to the namespaces exists ...
        graph.create("/jcr:system").and().create("/jcr:system/dna:namespaces");

        // Stub out the connection factory ...
        connectionFactory = new RepositoryConnectionFactory() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
             */
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return repositorySourceName.equals(sourceName) ? source.getConnection() : null;
            }
        };

        // Stub out the repository, since we only need a few methods ...
        repoTypeManager = new RepositoryNodeTypeManager(context);
        try {
            this.repoTypeManager.registerNodeTypes(new CndNodeTypeSource(new String[] {"/org/jboss/dna/jcr/jsr_170_builtins.cnd",
                "/org/jboss/dna/jcr/dna_builtins.cnd"}));
            this.repoTypeManager.registerNodeTypes(new NodeTemplateNodeTypeSource(getTestTypes()));
        } catch (RepositoryException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }

        stub(repository.getRepositoryTypeManager()).toReturn(repoTypeManager);
        stub(repository.getRepositorySourceName()).toReturn(repositorySourceName);
        stub(repository.getConnectionFactory()).toReturn(connectionFactory);

        // Set up the session attributes ...
        sessionAttributes = new HashMap<String, Object>();

        // Now create the workspace ...
        workspace = new JcrWorkspace(repository, workspaceName, context, sessionAttributes);

        // Create the session and log in ...
        session = (JcrSession)workspace.getSession();

        nameFactory = session.getExecutionContext().getValueFactories().getNameFactory();
    }

    @After
    public void after() throws Exception {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    @Test
    public void shouldNotFindInvalidPropertyDefinition() throws Exception {
        // This property name is not defined for any of our test types
        Name badName = nameFactory.create("undefinedName");
        JcrPropertyDefinition propDef;

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_A, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_B, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C, Collections.<Name>emptyList(), badName, null, true, true);
        assertThat(propDef, is(nullValue()));
    }

    @Test
    public void shouldUseNearestPropertyDefinition() {
        // If a property is defined at multiple points in the type hierarchy, the property definition closest to the given type
        // should be used.

        JcrPropertyDefinition propDef;

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_A,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.STRING);

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_B,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.DOUBLE);

        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP1,
                                                         null,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.LONG);
    }

    @Test
    public void shouldFindBestMatchDefinition() {
        /*
         * In cases where there is more than one valid definition for the same property,
         * the best match should be returned.
         */
        Value doubleValue = session.getValueFactory().createValue(0.7);
        Value longValue = session.getValueFactory().createValue(10);
        Value stringValue = session.getValueFactory().createValue("Should not work");

        JcrPropertyDefinition propDef;

        // Should prefer the double definition from NODE_TYPE_C since the value is of type double
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP2,
                                                         doubleValue,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.DOUBLE);

        // Should prefer the long definition from NODE_TYPE_C since the value is of type long
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP2,
                                                         longValue,
                                                         true,
                                                         true);
        assertThat(propDef, is(notNullValue()));
        assertEquals(propDef.getRequiredType(), PropertyType.LONG);

        // Should not allow a string though, since the NODE_TYPE_C definition narrows the acceptable types to double and long
        propDef = repoTypeManager.findPropertyDefinition(NODE_TYPE_C,
                                                         Collections.<Name>emptyList(),
                                                         SINGLE_PROP2,
                                                         stringValue,
                                                         true,
                                                         true);
        assertThat(propDef, is(nullValue()));

    }

    /*
    * Build a hierarchy of node types with the following relationships:
    *  
    *   dnatest:nodeA extends nt:base
    *   dnatest:nodeB extends nt:base
    *   dnatest:nodeC extends dnatest:nodeB
    *   
    * And the following single-valued property definitions
    * 
    *   dnatest:nodeA defines properties:
    *      dnatest:singleProp1 of type STRING
    *   dnatest:nodeB defines properties:
    *      dnatest:singleProp1 of type DOUBLE
    *      dnatest:singleProp2 of type UNDEFINED
    *   dnatest:nodeC defines properties:
    *      dnatest:singleProp1 of type LONG
    *      dnatest:singleProp2 of type DOUBLE     
    *      dnatest:singleProp2 of type LONG (note the double-definition)
    */

    private List<NodeTypeTemplate> getTestTypes() {
        NodeTypeTemplate nodeA = new JcrNodeTypeTemplate(context);
        nodeA.setName("dnatest:nodeA");

        JcrPropertyDefinitionTemplate nodeASingleProp1 = new JcrPropertyDefinitionTemplate(context);
        nodeASingleProp1.setName("dnatest:singleProp1");
        nodeASingleProp1.setRequiredType(PropertyType.STRING);
        nodeA.getPropertyDefinitionTemplates().add(nodeASingleProp1);

        NodeTypeTemplate nodeB = new JcrNodeTypeTemplate(context);
        nodeB.setName("dnatest:nodeB");

        JcrPropertyDefinitionTemplate nodeBSingleProp1 = new JcrPropertyDefinitionTemplate(context);
        nodeBSingleProp1.setName("dnatest:singleProp1");
        nodeBSingleProp1.setRequiredType(PropertyType.DOUBLE);
        nodeB.getPropertyDefinitionTemplates().add(nodeBSingleProp1);

        JcrPropertyDefinitionTemplate nodeBSingleProp2 = new JcrPropertyDefinitionTemplate(context);
        nodeBSingleProp2.setName("dnatest:singleProp2");
        nodeBSingleProp2.setRequiredType(PropertyType.UNDEFINED);
        nodeB.getPropertyDefinitionTemplates().add(nodeBSingleProp2);

        NodeTypeTemplate nodeC = new JcrNodeTypeTemplate(context);
        nodeC.setName("dnatest:nodeC");
        nodeC.setDeclaredSupertypeNames(new String[] {"dnatest:nodeB"});

        JcrPropertyDefinitionTemplate nodeCSingleProp1 = new JcrPropertyDefinitionTemplate(context);
        nodeCSingleProp1.setName("dnatest:singleProp1");
        nodeCSingleProp1.setRequiredType(PropertyType.LONG);
        nodeC.getPropertyDefinitionTemplates().add(nodeCSingleProp1);

        JcrPropertyDefinitionTemplate nodeCSingleProp2Double = new JcrPropertyDefinitionTemplate(context);
        nodeCSingleProp2Double.setName("dnatest:singleProp2");
        nodeCSingleProp2Double.setRequiredType(PropertyType.DOUBLE);
        nodeC.getPropertyDefinitionTemplates().add(nodeCSingleProp2Double);

        JcrPropertyDefinitionTemplate nodeCSingleProp2Long = new JcrPropertyDefinitionTemplate(context);
        nodeCSingleProp2Long.setName("dnatest:singleProp2");
        nodeCSingleProp2Long.setRequiredType(PropertyType.LONG);
        nodeC.getPropertyDefinitionTemplates().add(nodeCSingleProp2Long);

        return Arrays.asList(new NodeTypeTemplate[] {nodeA, nodeB, nodeC});
    }
}
