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
package org.jboss.dna.spi.graph.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Reference;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class BooleanValueFactoryTest {

    private BooleanValueFactory factory;
    private StringValueFactory stringFactory;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        stringFactory = new StringValueFactory(Path.URL_DECODER, Path.DEFAULT_ENCODER);
        factory = new BooleanValueFactory(Path.URL_DECODER, stringFactory);
    }

    @Test
    public void shouldCreateBooleanFromBoolean() {
        assertThat(factory.create(true), is(true));
        assertThat(factory.create(false), is(false));
    }

    @Test
    public void shouldCreateBooleanFromTrueAndFalseStringRegardlessOfCase() {
        assertThat(factory.create("true"), is(true));
        assertThat(factory.create("false"), is(false));
        assertThat(factory.create("TRUE"), is(true));
        assertThat(factory.create("FALSE"), is(false));
    }

    @Test
    public void shouldCreateBooleanFromTrueAndFalseStringRegardlessOfLeadingAndTrailingWhitespace() {
        assertThat(factory.create("  true  "), is(true));
        assertThat(factory.create("  false  "), is(false));
        assertThat(factory.create("  TRUE  "), is(true));
        assertThat(factory.create("  FALSE  "), is(false));
    }

    @Test
    public void shouldCreateFalseFromStringContainingOneOrZero() {
        assertThat(factory.create("1"), is(false));
        assertThat(factory.create("0"), is(false));
        assertThat(factory.create("  0  "), is(false));
        assertThat(factory.create("  1  "), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromIntegerValue() {
        factory.create(1);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromLongValue() {
        factory.create(1l);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromFloatValue() {
        factory.create(1.0f);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromDoubleValue() {
        factory.create(1.0d);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromBigDecimal() {
        factory.create(1.0d);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromDate() {
        factory.create(new Date());
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromCalendar() {
        factory.create(Calendar.getInstance());
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromName() {
        factory.create(mock(Name.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromPath() {
        factory.create(mock(Path.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromReference() {
        factory.create(mock(Reference.class));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotCreateBooleanFromUri() throws Exception {
        factory.create(new URI("http://www.jboss.org"));
    }

    @Test
    public void shouldCreateBooleanFromByteArrayContainingUtf8EncodingOfTrueOrFalseStringRegardlessOfCase() throws Exception {
        assertThat(factory.create("true".getBytes("UTF-8")), is(true));
        assertThat(factory.create("false".getBytes("UTF-8")), is(false));
        assertThat(factory.create("TRUE".getBytes("UTF-8")), is(true));
        assertThat(factory.create("FALSE".getBytes("UTF-8")), is(false));
        assertThat(factory.create("something else".getBytes("UTF-8")), is(false));
    }

    @Test
    public void shouldCreateBooleanFromInputStreamContainingUtf8EncodingOfTrueOrFalseStringRegardlessOfCase() throws Exception {
        assertThat(factory.create(new ByteArrayInputStream("true".getBytes("UTF-8"))), is(true));
        assertThat(factory.create(new ByteArrayInputStream("false".getBytes("UTF-8"))), is(false));
        assertThat(factory.create(new ByteArrayInputStream("TRUE".getBytes("UTF-8"))), is(true));
        assertThat(factory.create(new ByteArrayInputStream("FALSE".getBytes("UTF-8"))), is(false));
        assertThat(factory.create(new ByteArrayInputStream("something else".getBytes("UTF-8"))), is(false));
    }

    @Test
    public void shouldCreateBooleanFromReaderContainingTrueOrFalseStringRegardlessOfCase() throws Exception {
        assertThat(factory.create(new StringReader("true")), is(true));
        assertThat(factory.create(new StringReader("false")), is(false));
        assertThat(factory.create(new StringReader("TRUE")), is(true));
        assertThat(factory.create(new StringReader("FALSE")), is(false));
        assertThat(factory.create(new StringReader("something else")), is(false));
    }
}
