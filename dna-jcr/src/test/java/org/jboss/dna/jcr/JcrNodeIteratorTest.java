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
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path.Segment;
import org.jboss.dna.jcr.AbstractJcrNodeTest.MockAbstractJcrNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrNodeIteratorTest {

    private AbstractJcrNode node;
    @Mock
    private JcrSession session;
    private List<Segment> children;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        NamespaceRegistry registry = Mockito.mock(NamespaceRegistry.class);
        ExecutionContext context = Mockito.mock(ExecutionContext.class);
        stub(context.getNamespaceRegistry()).toReturn(registry);
        stub(session.getExecutionContext()).toReturn(context);
        children = new ArrayList<Segment>();
        node = new MockAbstractJcrNode(session, "node", null);
    }

    @Test
    public void shouldProvideNodeIterator() throws Exception {
        Node child1 = AbstractJcrNodeTest.createChild(session, "child1", 1, children, node);
        Node child2_1 = AbstractJcrNodeTest.createChild(session, "child2", 1, children, node);
        Node child2_2 = AbstractJcrNodeTest.createChild(session, "child2", 2, children, node);
        AbstractJcrNodeTest.createChild(session, "child3", 1, children, node);
        AbstractJcrNodeTest.createChild(session, "child4", 1, children, node);
        Node child5 = AbstractJcrNodeTest.createChild(session, "child5", 1, children, node);
        node.setChildren(children);
        NodeIterator iter = node.getNodes();
        assertThat(iter, notNullValue());
        assertThat(iter.getSize(), is(-1L));
        assertThat(iter.getPosition(), is(0L));
        assertThat(iter.hasNext(), is(true));
        assertThat((Node)iter.next(), is(child1));
        assertThat(iter.getPosition(), is(1L));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.nextNode(), is(child2_1));
        assertThat(iter.getPosition(), is(2L));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.nextNode(), is(child2_2));
        assertThat(iter.getPosition(), is(3L));
        assertThat(iter.hasNext(), is(true));
        iter.skip(2);
        assertThat(iter.getPosition(), is(5L));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.nextNode(), is(child5));
        assertThat(iter.getPosition(), is(6L));
        assertThat(iter.hasNext(), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowNodeIteratorRemove() throws Exception {
        node.getNodes().remove();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNodeIteratorNegativeSkip() throws Exception {
        node.getNodes().skip(-1);
    }
}
