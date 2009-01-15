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
package org.jboss.dna.common.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;

/**
 * @author Randall Hauch
 */
public class SimpleNamespaceContext implements NamespaceContext {

    private final Map<String, String> prefixToNamespace = new HashMap<String, String>();

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI( String prefix ) {
        return this.prefixToNamespace.get(prefix);
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix( String namespaceURI ) {
        for (Map.Entry<String, String> entry : this.prefixToNamespace.entrySet()) {
            if (entry.getValue().equals(namespaceURI)) return entry.getKey();
        }
        return null;
    }

    public SimpleNamespaceContext setNamespace( String prefix,
                                                String namespaceUri ) {
        this.prefixToNamespace.put(prefix, namespaceUri);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<String> getPrefixes( String namespaceURI ) {
        return this.prefixToNamespace.keySet().iterator();
    }

}
