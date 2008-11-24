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
package org.jboss.dna.graph.properties.basic;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;

/**
 * A basic implementation of {@link Name}.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class BasicName implements Name {

    /**
     */
    private static final long serialVersionUID = -1737537720336990144L;
    private final String namespaceUri;
    private final String localName;
    private final int hc;

    public BasicName( String namespaceUri,
                      String localName ) {
        CheckArg.isNotEmpty(localName, "localName");
        this.namespaceUri = namespaceUri != null ? namespaceUri.trim() : "";
        this.localName = localName != null ? localName.trim() : "";
        this.hc = HashCode.compute(this.namespaceUri, this.localName);
    }

    /**
     * {@inheritDoc}
     */
    public String getLocalName() {
        return this.localName;
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceUri() {
        return this.namespaceUri;
    }

    /**
     * {@inheritDoc}
     */
    public String getString() {
        return getString(Path.DEFAULT_ENCODER);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( TextEncoder encoder ) {
        if (this.getNamespaceUri().length() == 0) {
            if (this.getLocalName().equals(Path.SELF)) return Path.SELF;
            if (this.getLocalName().equals(Path.PARENT)) return Path.PARENT;
        }
        if (encoder == null) encoder = Path.DEFAULT_ENCODER;
        return "{" + encoder.encode(this.namespaceUri) + "}" + encoder.encode(this.localName);
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry ) {
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        String prefix = namespaceRegistry.getPrefixForNamespaceUri(this.namespaceUri, true);
        if (prefix != null && prefix.length() != 0) {
            return prefix + ":" + this.localName;
        }
        return this.localName;
    }

    /**
     * {@inheritDoc}
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder ) {
        // This is the most-often used method, so implement it directly
        CheckArg.isNotNull(namespaceRegistry, "namespaceRegistry");
        String prefix = namespaceRegistry.getPrefixForNamespaceUri(this.namespaceUri, true);
        if (prefix != null && prefix.length() != 0) {
            return encoder.encode(prefix) + ":" + encoder.encode(this.localName);
        }
        return this.localName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.properties.Name#getString(org.jboss.dna.graph.properties.NamespaceRegistry,
     *      org.jboss.dna.common.text.TextEncoder, org.jboss.dna.common.text.TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder ) {
        if (namespaceRegistry == null) {
            if (this.getNamespaceUri().length() == 0) {
                if (this.getLocalName().equals(Path.SELF)) return Path.SELF;
                if (this.getLocalName().equals(Path.PARENT)) return Path.PARENT;
            }
            if (encoder == null) encoder = Path.DEFAULT_ENCODER;
            if (delimiterEncoder != null) {
                return delimiterEncoder.encode("{") + encoder.encode(this.namespaceUri) + delimiterEncoder.encode("}")
                       + encoder.encode(this.localName);
            }
            return "{" + encoder.encode(this.namespaceUri) + "}" + encoder.encode(this.localName);

        }
        String prefix = namespaceRegistry.getPrefixForNamespaceUri(this.namespaceUri, true);
        if (prefix != null && prefix.length() != 0) {
            String delim = delimiterEncoder != null ? delimiterEncoder.encode(":") : ":";
            return encoder.encode(prefix) + delim + encoder.encode(this.localName);
        }
        return this.localName;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Name that ) {
        if (that == this) return 0;
        int diff = this.getLocalName().compareTo(that.getLocalName());
        if (diff != 0) return diff;
        diff = this.getNamespaceUri().compareTo(that.getNamespaceUri());
        return diff;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.hc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Name) {
            Name that = (Name)obj;
            if (!this.getLocalName().equals(that.getLocalName())) return false;
            if (!this.getNamespaceUri().equals(that.getNamespaceUri())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "{" + this.namespaceUri + "}" + this.localName;
    }

}
