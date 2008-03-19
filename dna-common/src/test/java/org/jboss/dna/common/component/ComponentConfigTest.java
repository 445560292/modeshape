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

package org.jboss.dna.common.component;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ComponentConfigTest {

    private ComponentConfig configA;
    private ComponentConfig configB;
    private ComponentConfig configA2;
    private String validName;
    private String validDescription;
    private String validClassname;
    private String[] validClasspath;

    @Before
    public void beforeEach() throws Exception {
        this.validName = "valid configuration name";
        this.validDescription = "a Component";
        this.validClassname = "org.jboss.dna.acme.GenericComponent";
        this.validClasspath = new String[] {"com.acme:configA:1.0,com.acme:configB:1.0"};
        this.configA = new ComponentConfig("configA", validDescription, "org.jboss.dna.acme.GenericComponent", validClasspath);
        this.configB = new ComponentConfig("configB", validDescription, "org.jboss.dna.acme.GenericComponentB", validClasspath);
        this.configA2 = new ComponentConfig("conFigA", validDescription, "org.jboss.dna.acme.GenericComponent", validClasspath);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullNameInConstructor() {
        new ComponentConfig(null, validDescription, validClassname, validClasspath);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyNameInConstructor() {
        new ComponentConfig("", validDescription, validClassname, validClasspath);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankNameInConstructor() {
        new ComponentConfig("   \t", validDescription, validClassname, validClasspath);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullClassNameInConstructor() {
        new ComponentConfig(validName, validDescription, null, validClasspath);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyClassNameInConstructor() {
        new ComponentConfig(validName, validDescription, "", validClasspath);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowBlankClassNameInConstructor() {
        new ComponentConfig(validName, validDescription, "   \t", validClasspath);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowInvalidClassNameInConstructor() {
        new ComponentConfig(validName, validDescription, "12.this is not a valid classname", validClasspath);
    }

    @Test
    public void shouldConsiderSameIfNamesAreEqualIgnoringCase() {
        assertThat(configA.equals(configA2), is(true));
    }

    @Test
    public void shouldConsiderNotSameIfNamesAreNotEqualIgnoringCase() {
        assertThat(configA.equals(configB), is(false));
    }

    @Test
    public void shouldSetClasspathWithValidMavenIds() {
        assertThat(configA.getComponentClasspath().size(), is(validClasspath.length));
        assertThat(configA.getComponentClasspathArray(), is(validClasspath));
    }

    @Test
    public void shouldGetNonNullComponentClasspathWhenEmpty() {
        configA = new ComponentConfig("configA", validDescription, validClassname, (String[])null);
        assertThat(configA.getComponentClasspath().size(), is(0));
        assertThat(configA.getComponentClasspathArray().length, is(0));
    }

}
