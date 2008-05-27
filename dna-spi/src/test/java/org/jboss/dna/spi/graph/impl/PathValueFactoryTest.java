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
import static org.jboss.dna.spi.graph.impl.IsPathContaining.hasSegments;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.ValueFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class PathValueFactoryTest {

    public static final TextEncoder NO_OP_ENCODER = Path.NO_OP_ENCODER;

    private BasicNamespaceRegistry registry;
    private ValueFactory<String> stringValueFactory;
    private NameValueFactory nameFactory;
    private PathValueFactory factory;
    private TextEncoder encoder;
    private Path path;
    private Path path2;

    @Before
    public void beforeEach() throws Exception {
        this.registry = new BasicNamespaceRegistry();
        this.registry.register("dna", "http://www.jboss.org/dna/namespace");
        this.encoder = Path.DEFAULT_ENCODER;
        this.stringValueFactory = new StringValueFactory(encoder);
        this.nameFactory = new NameValueFactory(registry, encoder, stringValueFactory);
        this.factory = new PathValueFactory(encoder, stringValueFactory, nameFactory);
    }

    protected List<Path.Segment> getSegments( String... segments ) {
        List<Path.Segment> result = new ArrayList<Path.Segment>();
        for (String segmentStr : segments) {
            Name name = nameFactory.create(segmentStr);
            BasicPathSegment segment = new BasicPathSegment(name);
            result.add(segment);
        }
        return result;
    }

    @Test
    public void shouldCreateFromStringWithAbsolutePathAndNoParentOrSelfReferences() {
        path = factory.create("/a/b/c/d/dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", "d", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithRelativePathAndNoParentOrSelfReferences() {
        path = factory.create("a/b/c/d/dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", "d", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithAbsolutePathAndParentReference() {
        path = factory.create("/a/b/c/../dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", "..", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithRelativePathAndParentReference() {
        path = factory.create("a/b/c/../dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", "..", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithAbsolutePathAndSelfReference() {
        path = factory.create("/a/b/c/./dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", ".", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithRelativePathAndSelfReference() {
        path = factory.create("a/b/c/./dna:e/dna:f");
        assertThat(path, hasSegments(factory, "a", "b", "c", ".", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateFromStringWithRelativePathBeginningWithSelfReference() {
        path = factory.create("./a/b/c/./dna:e/dna:f");
        assertThat(path, hasSegments(factory, ".", "a", "b", "c", ".", "dna:e", "dna:f"));
    }

    @Test
    public void shouldCreateEquivalentPathsWhetherOrNotThereIsATrailingDelimiter() {
        path = factory.create("/a/b/c/d/dna:e/dna:f");
        path2 = factory.create("/a/b/c/d/dna:e/dna:f/");
        assertThat(path.equals(path2), is(true));
        assertThat(path.compareTo(path2), is(0));
    }

}
