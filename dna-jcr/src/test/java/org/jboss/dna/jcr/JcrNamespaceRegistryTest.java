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

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.NamespaceException;
import javax.jcr.UnsupportedRepositoryOperationException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jverhaeg
 */
public class JcrNamespaceRegistryTest {

    private JcrNamespaceRegistry registry;

    @Before
    public void before() {
        registry = new JcrNamespaceRegistry();
    }

    @Test
    public void shouldProvidePrefixes() {
        String[] prefixes = registry.getPrefixes();
        assertThat(prefixes, notNullValue());
        assertThat(prefixes.length, is(6));
        assertThat(prefixes, hasItemInArray(""));
        assertThat(prefixes, hasItemInArray("dna"));
        assertThat(prefixes, hasItemInArray("jcr"));
        assertThat(prefixes, hasItemInArray("mix"));
        assertThat(prefixes, hasItemInArray("nt"));
        assertThat(prefixes, hasItemInArray("xml"));
    }

    @Test
    public void shouldProvideUris() {
        String[] uris = registry.getURIs();
        assertThat(uris, notNullValue());
        assertThat(uris.length, is(6));
        assertThat(uris, hasItemInArray(""));
        assertThat(uris, hasItemInArray("http://www.jboss.org/dna/1.0"));
        assertThat(uris, hasItemInArray("http://www.jcp.org/jcr/1.0"));
        assertThat(uris, hasItemInArray("http://www.jcp.org/jcr/mix/1.0"));
        assertThat(uris, hasItemInArray("http://www.jcp.org/jcr/nt/1.0"));
        assertThat(uris, hasItemInArray("http://www.w3.org/XML/1998/namespace"));
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowRegisterNamespace() throws Exception {
        registry.registerNamespace(null, null);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowUnregisterNamespace() throws Exception {
        registry.unregisterNamespace(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullPrefix() throws Exception {
        registry.getURI(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullUri() throws Exception {
        registry.getPrefix(null);
    }

    @Test
    public void shouldProvideUriForRegisteredPrefix() throws Exception {
        assertThat(registry.getPrefix(""), is(""));
        assertThat(registry.getPrefix("http://www.jboss.org/dna/1.0"), is("dna"));
        assertThat(registry.getPrefix("http://www.jcp.org/jcr/1.0"), is("jcr"));
        assertThat(registry.getPrefix("http://www.jcp.org/jcr/mix/1.0"), is("mix"));
        assertThat(registry.getPrefix("http://www.jcp.org/jcr/nt/1.0"), is("nt"));
        assertThat(registry.getPrefix("http://www.w3.org/XML/1998/namespace"), is("xml"));
    }

    @Test
    public void shouldProvidePrefixForRegisteredUri() throws Exception {
        assertThat(registry.getURI(""), is(""));
        assertThat(registry.getURI("dna"), is("http://www.jboss.org/dna/1.0"));
        assertThat(registry.getURI("jcr"), is("http://www.jcp.org/jcr/1.0"));
        assertThat(registry.getURI("mix"), is("http://www.jcp.org/jcr/mix/1.0"));
        assertThat(registry.getURI("nt"), is("http://www.jcp.org/jcr/nt/1.0"));
        assertThat(registry.getURI("xml"), is("http://www.w3.org/XML/1998/namespace"));
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteredPrefix() throws Exception {
        registry.getURI("bogus");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteredUri() throws Exception {
        registry.getPrefix("bogus");
    }
}
