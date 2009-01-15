/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.federation.merge.strategy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.merge.FederatedNode;
import org.jboss.dna.connector.federation.merge.MergePlan;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class OneContributionMergeStrategyTest {

    private OneContributionMergeStrategy strategy;
    private List<Contribution> contributions;
    private ExecutionContext context;
    private FederatedNode node;
    private Map<Name, Property> properties;
    private List<Location> children;
    private Path parentPath;
    @Mock
    private Contribution contribution;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        strategy = new OneContributionMergeStrategy();
        contributions = new LinkedList<Contribution>();
        contributions.add(contribution);
        context = new ExecutionContext();
        context.getNamespaceRegistry().register("dna", "http://www.jboss.org/dna/something");
        context.getNamespaceRegistry().register("jcr", "http://www.jcr.org");
        parentPath = context.getValueFactories().getPathFactory().create("/a/b/c");
        node = new FederatedNode(new Location(parentPath), UUID.randomUUID());
        stub(contribution.getSourceName()).toReturn("source name");
        children = new LinkedList<Location>();
        for (int i = 0; i != 10; ++i) {
            Path childPath = context.getValueFactories().getPathFactory().create(parentPath, "a" + i);
            children.add(new Location(childPath));
        }
        properties = new HashMap<Name, Property>();
        for (int i = 0; i != 10; ++i) {
            Name propertyName = context.getValueFactories().getNameFactory().create("property" + i);
            properties.put(propertyName, context.getPropertyFactory().create(propertyName, "value"));
        }
    }

    @Test
    public void shouldMergeTheChildrenFromTheFirstContribution() {
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        strategy.merge(node, contributions, context);
        assertThat(node.getChildren(), is(children));
    }

    @Test
    public void shouldMergeThePropertiesFromTheFirstContribution() {
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        strategy.merge(node, contributions, context);
        properties.put(DnaLexicon.UUID, node.getPropertiesByName().get(DnaLexicon.UUID));
        assertThat(node.getPropertiesByName(), is(properties));
    }

    @Test
    public void shouldCreateMergePlanInTheFederatedNode() {
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        strategy.merge(node, contributions, context);
        MergePlan mergePlan = node.getMergePlan();
        assertThat(mergePlan.getContributionFrom(contribution.getSourceName()), is(sameInstance(contribution)));
        assertThat(mergePlan.getContributionCount(), is(1));
    }

    @Test
    public void shouldSetTheUuidOnTheNodeIfThereIsASingleValuedPropertyNamedUuidWithValueThatConvertsToUuidInstance() {
        // Test the "dna:uuid" property ...
        Name uuidName = context.getValueFactories().getNameFactory().create("dna:uuid");
        UUID uuid = UUID.randomUUID();
        Property uuidProperty = context.getPropertyFactory().create(uuidName, uuid);
        properties.put(uuidProperty.getName(), uuidProperty);
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        assertThat(node.getUuid(), is(not(uuid)));
        strategy.merge(node, contributions, context);
        assertThat(node.getUuid(), is(uuid));
        properties.remove(uuidProperty.getName());

        // Test the "jcr:uuid" property ...
        uuidName = context.getValueFactories().getNameFactory().create("jcr:uuid");
        uuid = UUID.randomUUID();
        uuidProperty = context.getPropertyFactory().create(uuidName, uuid);
        properties.put(uuidProperty.getName(), uuidProperty);
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        assertThat(node.getUuid(), is(not(uuid)));
        strategy.merge(node, contributions, context);
        assertThat(node.getUuid(), is(uuid));
        properties.remove(uuidProperty.getName());

        // Test the "uuid" property ...
        uuidName = context.getValueFactories().getNameFactory().create("uuid");
        uuid = UUID.randomUUID();
        uuidProperty = context.getPropertyFactory().create(uuidName, uuid);
        properties.put(uuidProperty.getName(), uuidProperty);
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        assertThat(node.getUuid(), is(not(uuid)));
        strategy.merge(node, contributions, context);
        assertThat(node.getUuid(), is(uuid));
        properties.remove(uuidProperty.getName());

        // Test the "uuid" property whose value is a String ...
        uuidName = context.getValueFactories().getNameFactory().create("uuid");
        uuid = UUID.randomUUID();
        uuidProperty = context.getPropertyFactory().create(uuidName, uuid.toString());
        properties.put(uuidProperty.getName(), uuidProperty);
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        assertThat(node.getUuid(), is(not(uuid)));
        strategy.merge(node, contributions, context);
        assertThat(node.getUuid(), is(uuid));
        properties.remove(uuidProperty.getName());
    }

    @Test
    public void shouldNotSetTheUuidOnTheNodeIfThereIsNoPropertyNamedUuid() {
        // Test the "dna:uuid" property ...
        Name uuidName = context.getValueFactories().getNameFactory().create("dna:uuid");
        UUID uuid = UUID.randomUUID();
        Property uuidProperty = context.getPropertyFactory().create(uuidName, uuid);
        properties.put(uuidProperty.getName(), uuidProperty);
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        assertThat(node.getUuid(), is(not(uuid)));
        strategy.merge(node, contributions, context);
        assertThat(node.getUuid(), is(uuid));
        properties.remove(uuidProperty.getName());
    }

    @Test
    public void shouldNotSetTheUuidOnTheNodeIfThereIsAMultiValuedPropertyNamedUuid() {
        final UUID originalUuid = node.getUuid();
        // Test the "dna:uuid" property ...
        Name uuidName = context.getValueFactories().getNameFactory().create("dna:uuid");
        Property uuidProperty = context.getPropertyFactory().create(uuidName,
                                                                    UUID.randomUUID(),
                                                                    UUID.randomUUID(),
                                                                    UUID.randomUUID());
        properties.put(uuidProperty.getName(), uuidProperty);
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        strategy.merge(node, contributions, context);
        assertThat(node.getUuid(), is(originalUuid));
    }

    @Test
    public void shouldNotSetTheUuidOnTheNodeIfThereIsASingleValuedPropertyNamedUuidWithValueThatDoesNotConvertToUuidInstance() {
        final UUID originalUuid = node.getUuid();
        // Test the "dna:uuid" property ...
        Name uuidName = context.getValueFactories().getNameFactory().create("dna:uuid");
        Property uuidProperty = context.getPropertyFactory().create(uuidName, 3.33d);
        properties.put(uuidProperty.getName(), uuidProperty);
        stub(contribution.getChildren()).toReturn(children.iterator());
        stub(contribution.getProperties()).toReturn(properties.values().iterator());
        strategy.merge(node, contributions, context);
        assertThat(node.getUuid(), is(originalUuid));
    }

}
