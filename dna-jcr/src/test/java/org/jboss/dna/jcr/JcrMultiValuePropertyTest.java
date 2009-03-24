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
import static org.mockito.Mockito.stub;
import java.util.UUID;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.jcr.cache.PropertyInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrMultiValuePropertyTest {

    private PropertyId propertyId;
    private JcrMultiValueProperty prop;
    private ExecutionContext executionContext;
    private org.jboss.dna.graph.property.Property dnaProperty;
    @Mock
    private SessionCache cache;
    @Mock
    private JcrSession session;
    @Mock
    private PropertyInfo propertyInfo;
    @Mock
    private JcrPropertyDefinition definition;
    @Mock
    private JcrNodeTypeManager nodeTypes;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        executionContext = new ExecutionContext();
        stub(cache.session()).toReturn(session);
        stub(cache.context()).toReturn(executionContext);
        stub(session.nodeTypeManager()).toReturn(nodeTypes);

        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, true);
        stub(definition.getRequiredType()).toReturn(PropertyType.BOOLEAN);
        stub(definition.isMultiple()).toReturn(true);
        PropertyDefinitionId definitionId = new PropertyDefinitionId(name("nodeTypeName"), name("propDefnName"),
                                                                     PropertyType.BOOLEAN, true);
        stub(nodeTypes.getPropertyDefinition(definitionId)).toReturn(definition);

        UUID uuid = UUID.randomUUID();
        propertyId = new PropertyId(uuid, JcrLexicon.MIMETYPE);
        prop = new JcrMultiValueProperty(cache, propertyId);

        stub(cache.findPropertyInfo(propertyId)).toReturn(propertyInfo);
        stub(propertyInfo.getDefinitionId()).toReturn(definitionId);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.BOOLEAN);
        stub(propertyInfo.isMultiValued()).toReturn(true);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
    }

    protected Name name( String name ) {
        return executionContext.getValueFactories().getNameFactory().create(name);
    }

    @Test
    public void shouldProvideAppropriateType() throws Exception {
        assertThat(prop.getType(), is(definition.getRequiredType()));
    }

    @Test
    public void shouldProvidePropertyDefinition() throws Exception {
        assertThat(prop.getDefinition(), is((PropertyDefinition)definition));
    }

    @Test
    public void shouldIndicateHasMultipleValues() throws Exception {
        PropertyDefinition def = prop.getDefinition();
        assertThat(def, notNullValue());
        assertThat(def.isMultiple(), is(true));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideBooleanForMultiValuedProperty() throws Exception {
        prop.getBoolean();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDateForMultiValuedProperty() throws Exception {
        prop.getDate();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDoubleForMultiValuedProperty() throws Exception {
        prop.getDouble();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLongForMultiValuedProperty() throws Exception {
        prop.getLong();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideStreamForMultiValuedProperty() throws Exception {
        prop.getStream();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideStringForMultiValuedProperty() throws Exception {
        prop.getString();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideValue() throws Exception {
        prop.getValue();
    }

    @Test
    public void shouldProvideValues() throws Exception {
        Value[] vals = prop.getValues();
        assertThat(vals, notNullValue());
        assertThat(vals.length, is(1));
        assertThat(vals[0].getBoolean(), is(true));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLength() throws Exception {
        prop.getLength();
    }

    @Test
    public void shouldProvideLengths() throws Exception {
        long[] lengths = prop.getLengths();
        assertThat(lengths, notNullValue());
        assertThat(lengths.length, is(1));
        assertThat(lengths[0], is(4L));

        Object value = "value";
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, value);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(definition.getRequiredType()).toReturn(PropertyType.STRING);
        stub(definition.isMultiple()).toReturn(true);
        prop = new JcrMultiValueProperty(cache, propertyId);
        lengths = prop.getLengths();
        assertThat(lengths, notNullValue());
        assertThat(lengths.length, is(1));
        assertThat(lengths[0], is(5L));

        value = new Object();
        long expectedLength = executionContext.getValueFactories().getBinaryFactory().create(value).getSize();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, value);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(definition.getRequiredType()).toReturn(PropertyType.STRING);
        stub(definition.isMultiple()).toReturn(true);
        prop = new JcrMultiValueProperty(cache, propertyId);
        lengths = prop.getLengths();
        assertThat(lengths, notNullValue());
        assertThat(lengths.length, is(1));
        assertThat(lengths[0], is(expectedLength));

        String[] values = new String[] {"value1", "value2", "value 3 is longer"};
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, (Object[])values);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(definition.getRequiredType()).toReturn(PropertyType.STRING);
        stub(definition.isMultiple()).toReturn(true);
        prop = new JcrMultiValueProperty(cache, propertyId);
        lengths = prop.getLengths();
        assertThat(lengths, notNullValue());
        assertThat(lengths.length, is(values.length));
        assertThat(lengths[0], is((long)values[0].length()));
        assertThat(lengths[1], is((long)values[1].length()));
        assertThat(lengths[2], is((long)values[2].length()));
    }
}
