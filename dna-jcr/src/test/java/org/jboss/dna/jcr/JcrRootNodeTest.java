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
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.nodetype.NodeDefinition;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrRootNodeTest {

    private JcrRootNode root;
    @Mock
    private JcrSession session;
    @Mock
    private NodeDefinition nodeDefinition;
    private Map<Name, Property> properties;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        properties = new HashMap<Name, Property>();
        ExecutionContext context = new ExecutionContext();
        UUID rootUuid = UUID.randomUUID();
        Path rootPath = context.getValueFactories().getPathFactory().createRootPath();
        Location rootLocation = Location.create(rootPath, rootUuid);
        root = new JcrRootNode(session, rootLocation, nodeDefinition);
        root.setProperties(properties);
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowAncestorDepthGreaterThanNodeDepth() throws Exception {
        root.getAncestor(1);
    }

    @Test
    public void shouldHaveZeroDepth() throws Exception {
        assertThat(root.getDepth(), is(0));
    }

    @Test
    public void shouldIndicateIndexIsOne() throws Exception {
        assertThat(root.getIndex(), is(1));
    }

    @Test
    public void shouldHaveEmptyName() throws Exception {
        String name = root.getName();
        assertThat(name, notNullValue());
        assertThat(name.length(), is(0));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldHaveNoParent() throws Exception {
        root.getParent();
    }

    @Test
    public void shouldProvidePath() throws Exception {
        assertThat(root.getPath(), is("/"));
    }
}
