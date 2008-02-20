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
package org.jboss.dna.maven;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class MavenIdTest {

    private String validGroupId;
    private String validArtifactId;
    private String validClassifier;
    private String validVersion;
    private String validArtifactIdToString;
    private String validArtifactIdWithNullClassifierToString;
    private MavenId validId;
    private MavenId validIdWithNullClassifier;

    @Before
    public void beforeEach() throws Exception {
        this.validGroupId = "org.jboss.dna";
        this.validArtifactId = "jboss-dna-core";
        this.validClassifier = "jdk1.4";
        this.validVersion = "1.0";
        this.validId = new MavenId(this.validGroupId, this.validArtifactId, this.validVersion, this.validClassifier);
        this.validIdWithNullClassifier = new MavenId(this.validGroupId, this.validArtifactId, this.validVersion, null);
        this.validArtifactIdToString = this.validGroupId + ":" + this.validArtifactId + ":" + this.validVersion + ":" + this.validClassifier;
        this.validArtifactIdWithNullClassifierToString = this.validGroupId + ":" + this.validArtifactId + ":" + this.validVersion + ":";
    }

    @Test
    public void shouldParseValidVersionStringIntoComponents() {
        assertArrayEquals(new Object[] {1, 2}, MavenId.getVersionComponents("1.2"));
        assertArrayEquals(new Object[] {1, 2, 3, 4, 5, 6, 7, 8}, MavenId.getVersionComponents("1.2.3.4.5.6.7.8"));
        assertArrayEquals(new Object[] {1, 0, "SNAPSHOT"}, MavenId.getVersionComponents("1.0-SNAPSHOT"));
        assertArrayEquals(new Object[] {1, 0, "SNAPSHOT"}, MavenId.getVersionComponents("1.0,SNAPSHOT"));
        assertArrayEquals(new Object[] {1, 0, "SNAPSHOT"}, MavenId.getVersionComponents("1.0/SNAPSHOT"));
        assertArrayEquals(new Object[] {1, 0, "SNAPSHOT"}, MavenId.getVersionComponents("1.0-SNAPSHOT"));
    }

    @Test
    public void shouldParseEmptyOrNullVersionStringIntoEmptyComponents() {
        assertArrayEquals(new Object[] {}, MavenId.getVersionComponents(null));
        assertArrayEquals(new Object[] {}, MavenId.getVersionComponents(""));
        assertArrayEquals(new Object[] {}, MavenId.getVersionComponents("   "));
    }

    @Test
    public void shouldParseVersionStringWithEmbeddedWhitespaceIntoEmptyComponents() {
        assertArrayEquals(new Object[] {1, 2, "SNAPSHOT"}, MavenId.getVersionComponents("  1.2-SNAPSHOT  "));
        assertArrayEquals(new Object[] {1, 2, "SNAPSHOT"}, MavenId.getVersionComponents("  1  . 2  -  SNAPSHOT  "));
        assertArrayEquals(new Object[] {1, 2, "SNAP SHOT"}, MavenId.getVersionComponents("  1  . 2  -  SNAP SHOT  "));
    }

    @Test
    public void shouldCreateInstanceWithValidArguments() {
        new MavenId(this.validGroupId, this.validArtifactId, this.validVersion);
        new MavenId(this.validGroupId, this.validArtifactId, this.validVersion, this.validClassifier);
    }

    @Test
    public void shouldCreateInstanceWithNullOrEmptyVersion() {
        assertThat(new MavenId(this.validGroupId, this.validArtifactId, null, this.validClassifier), is(notNullValue()));
        assertThat(new MavenId(this.validGroupId, this.validArtifactId, "", this.validClassifier), is(notNullValue()));
        assertThat(new MavenId(this.validGroupId, this.validArtifactId, "   ", this.validClassifier), is(notNullValue()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateInstanceWithNullGroupId() {
        new MavenId(null, this.validArtifactId, this.validVersion, this.validClassifier);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateInstanceWithNullArtifactId() {
        new MavenId(this.validGroupId, null, this.validVersion, this.validClassifier);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateInstanceWithEmptyGroupId() {
        new MavenId("  ", this.validArtifactId, this.validVersion, this.validClassifier);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateInstanceWithEmptyArtifactId() {
        new MavenId(this.validGroupId, "  ", this.validVersion, this.validClassifier);
    }

    @Test
    public void shouldCompareToSelfAsSame() {
        assertThat(this.validId.compareTo(this.validId), is(0));
        assertThat(this.validIdWithNullClassifier.compareTo(this.validIdWithNullClassifier), is(0));
    }

    @Test
    public void shouldEqualSelf() {
        assertThat(this.validId.equals(this.validId), is(true));
        assertThat(this.validIdWithNullClassifier.equals(this.validIdWithNullClassifier), is(true));
    }

    @Test
    public void shouldHaveToStringThatIsCombinationOfAllMembers() {
        assertThat(this.validId.toString(), is(this.validArtifactIdToString));
        assertThat(this.validIdWithNullClassifier.toString(), is(this.validArtifactIdWithNullClassifierToString));
    }

    @Test
    public void shouldHaveRepeatableHashCode() {
        int hc = this.validId.hashCode();
        for (int i = 0; i != 10; ++i) {
            assertThat(this.validId.hashCode(), is(hc));
        }
    }

    @Test
    public void shouldAlwaysEqualAnyVersion() {
        MavenId that = this.validId;
        MavenId any = new MavenId(that.getGroupId(), that.getArtifactId(), null, that.getClassifier());
        assertThat(any.equals(that), is(true));
    }

    @Test
    public void shouldParseNullOrEmptyStringOfCommanSeparatedCoordinates() {
        checkParsing("org.jboss.dna:dna-maven", new MavenId("org.jboss.dna", "dna-maven"));
        checkParsing("org.jboss.dna:dna-maven:", new MavenId("org.jboss.dna", "dna-maven"));
        checkParsing("org.jboss.dna:dna-maven::jdk1.4", new MavenId("org.jboss.dna", "dna-maven", null, "jdk1.4"));
        checkParsing("org.jboss.dna:dna-maven:1.0:jdk1.4", new MavenId("org.jboss.dna", "dna-maven", "1.0", "jdk1.4"));
        checkParsing("org.jboss.dna:dna-maven:1.0:jdk1.4,net.jcip:jcip-annotations:1.0", new MavenId("org.jboss.dna", "dna-maven", "1.0", "jdk1.4"), new MavenId("net.jcip", "jcip-annotations", "1.0"));
        checkParsing("org.jboss.dna:dna-maven:1.0:jdk1.4,,net.jcip:jcip-annotations:1.0,", new MavenId("org.jboss.dna", "dna-maven", "1.0", "jdk1.4"), new MavenId("net.jcip", "jcip-annotations",
                                                                                                                                                                   "1.0"));
        checkParsing(",,org.jboss.dna:dna-maven:1.0:jdk1.4,, net.jcip: jcip-annotations:1.0 ,,", new MavenId("org.jboss.dna", "dna-maven", "1.0", "jdk1.4"), new MavenId("net.jcip",
                                                                                                                                                                         "jcip-annotations", "1.0"));
    }

    public void checkParsing( String commaSeparatedCoordinates, MavenId... mavenIds ) {
        MavenId[] results = MavenId.parse(commaSeparatedCoordinates);
        assertThat(results, is(notNullValue()));
        assertThat(results.length, is(mavenIds.length));
        assertThat(results, is(mavenIds));
    }
}
