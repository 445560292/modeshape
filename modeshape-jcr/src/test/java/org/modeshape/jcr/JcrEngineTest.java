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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.net.URL;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import org.junit.After;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * 
 */
public class JcrEngineTest {

    protected static URL resourceUrl( String name ) {
        return JcrQueryManagerTest.class.getClassLoader().getResource(name);
    }

    private JcrConfiguration configuration;
    private JcrEngine engine;
    private JcrRepository repository;
    private Session session;

    @After
    public void afterEach() {
        configuration = null;

        try {
            if (session != null) {
                session.logout();
            }
        } finally {
            session = null;
            try {
                if (engine != null) {
                    engine.shutdown();
                }
            } finally {
                engine = null;
            }
        }
    }

    @Test
    public void shouldCreateRepositoryEngineFromConfigurationFileWithDefaultNamespace() throws Exception {
        configuration = new JcrConfiguration().loadFrom("src/test/resources/config/configRepositoryWithDefaultNamespace.xml");
        engine = configuration.build();
        engine.start();
        repository = engine.getRepository("mode:Car Repository");
        session = repository.login();
    }

    @Test
    public void shouldCreateRepositoryConfiguredWithOneCompactNodeTypeDefinitionFile() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        engine.start();
        repository = engine.getRepository("cars");
        session = repository.login();

        assertNodeType("car:Car", false, false, true, false, null, 0, 11, "nt:unstructured", "mix:created");
    }

    @Test( expected = JcrConfigurationException.class )
    public void shouldFailToStartIfAnyRepositorySourceDoesNotDefineClass() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
        // .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content");
        configuration.repository("cars")
                     .setSource("car-source-non-existant")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        engine.start();
    }

    @Test( expected = JcrConfigurationException.class )
    public void shouldFailToStartIfAnyRepositoryReferencesNonExistantSource() throws Exception {
        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(InMemoryRepositorySource.class)
                     .setDescription("The automobile content");
        configuration.repository("cars")
                     .setSource("car-source-non-existant")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        engine = configuration.build();
        engine.start();
    }

    // @Test
    // public void shouldCreateRepositoryConfiguredWithOneXmlNodeTypeDefinitionFiles() throws Exception {
    // configuration = new JcrConfiguration();
    // configuration.repositorySource("car-source")
    // .usingClass(InMemoryRepositorySource.class)
    // .setDescription("The automobile content");
    // configuration.repository("cars")
    // .setSource("car-source")
    // .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
    // .addNodeTypes(resourceUrl("xmlNodeTypeRegistration/owfe_nodetypes.xml"))
    // .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
    // engine = configuration.build();
    // engine.start();
    //
    // repository = engine.getRepository("cars");
    // session = repository.login();
    //
    // assertNodeType("mgnl:workItem", false, false, true, true, null, 1, 1, "nt:hierarchyNode");
    // }

    // @Test
    // public void shouldCreateRepositoryConfiguredWithMultipleNodeTypeDefinitionFiles() throws Exception {
    // configuration = new JcrConfiguration();
    // configuration.repositorySource("car-source")
    // .usingClass(InMemoryRepositorySource.class)
    // .setDescription("The automobile content");
    // configuration.repository("cars")
    // .setSource("car-source")
    // .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
    // .addNodeTypes(resourceUrl("cars.cnd"))
    // .addNodeTypes(resourceUrl("xmlNodeTypeRegistration/owfe_nodetypes.xml"))
    // .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
    // engine = configuration.build();
    // engine.start();
    //
    // repository = engine.getRepository("cars");
    // session = repository.login();
    //
    // assertNodeType("car:Car", false, false, true, false, null, 0, 11, "nt:unstructured", "mix:created");
    // assertNodeType("mgnl:workItem", false, false, true, true, null, 1, 1, "nt:hierarchyNode");
    // }

    protected void assertNodeType( String name,
                                   boolean isAbstract,
                                   boolean isMixin,
                                   boolean isQueryable,
                                   boolean hasOrderableChildNodes,
                                   String primaryItemName,
                                   int numberOfDeclaredChildNodeDefinitions,
                                   int numberOfDeclaredPropertyDefinitions,
                                   String... supertypes ) throws Exception {
        NodeType nodeType = session.getWorkspace().getNodeTypeManager().getNodeType(name);
        assertThat(nodeType, is(notNullValue()));
        assertThat(nodeType.isAbstract(), is(isAbstract));
        assertThat(nodeType.isMixin(), is(isMixin));
        assertThat(nodeType.isQueryable(), is(isQueryable));
        assertThat(nodeType.hasOrderableChildNodes(), is(hasOrderableChildNodes));
        assertThat(nodeType.getPrimaryItemName(), is(primaryItemName));
        assertThat(nodeType.getDeclaredSupertypes().length, is(supertypes.length));
        for (int i = 0; i != supertypes.length; ++i) {
            assertThat(nodeType.getDeclaredSupertypes()[i].getName(), is(supertypes[i]));
        }
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(numberOfDeclaredChildNodeDefinitions));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(numberOfDeclaredPropertyDefinitions));

    }

}
