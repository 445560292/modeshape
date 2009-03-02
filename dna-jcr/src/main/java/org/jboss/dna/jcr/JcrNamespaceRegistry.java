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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.xml.XMLConstants;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.xml.XmlCharacters;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrMixLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.NamespaceRegistry.Namespace;

/**
 * A thread-safe JCR {@link javax.jcr.NamespaceRegistry} implementation that has the standard JCR namespaces pre-registered and
 * enforces the JCR semantics for {@link #registerNamespace(String, String) registering} and {@link #unregisterNamespace(String)
 * unregistering} namespaces.
 * <p>
 * Note that this implementation is {@link NotThreadSafe not thread safe}, since it is used within a single {@link JcrWorkspace}
 * and single {@link JcrSession}, and according to the JCR specification these interfaces are not thread safe.
 * </p>
 */
@NotThreadSafe
class JcrNamespaceRegistry implements javax.jcr.NamespaceRegistry {

    public static enum Behavior {
        JSR170_SESSION,
        JSR283_SESSION,
        WORKSPACE;
    }

    static final String DEFAULT_NAMESPACE_PREFIX = "";
    static final String DEFAULT_NAMESPACE_URI = "";

    static final String XML_NAMESPACE_PREFIX = XMLConstants.XML_NS_PREFIX;
    static final String XML_NAMESPACE_URI = XMLConstants.XML_NS_URI;
    static final String XMLNS_NAMESPACE_PREFIX = XMLConstants.XMLNS_ATTRIBUTE;
    static final String XMLNS_NAMESPACE_URI = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

    static final Set<String> STANDARD_BUILT_IN_PREFIXES;
    static final Set<String> STANDARD_BUILT_IN_URIS;
    static final Map<String, String> STANDARD_BUILT_IN_NAMESPACES_BY_PREFIX;
    static final Map<String, String> STANDARD_BUILT_IN_PREFIXES_BY_NAMESPACE;

    static {
        // Set up the standard namespaces ...
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put(DEFAULT_NAMESPACE_PREFIX, DEFAULT_NAMESPACE_URI);
        namespaces.put(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        namespaces.put(JcrNtLexicon.Namespace.PREFIX, JcrNtLexicon.Namespace.URI);
        namespaces.put(JcrMixLexicon.Namespace.PREFIX, JcrMixLexicon.Namespace.URI);
        namespaces.put(JcrSvLexicon.Namespace.PREFIX, JcrSvLexicon.Namespace.URI);
        namespaces.put(XML_NAMESPACE_PREFIX, XML_NAMESPACE_URI);
        namespaces.put(XMLNS_NAMESPACE_PREFIX, XMLNS_NAMESPACE_URI);
        namespaces.put(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        // Set up the reverse map for the standard namespaces ...
        Map<String, String> prefixes = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            String uri = entry.getValue();
            String prefix = entry.getKey();
            prefixes.put(uri, prefix);
        }
        // Now set up the unmodifiable static collections ...
        STANDARD_BUILT_IN_NAMESPACES_BY_PREFIX = Collections.unmodifiableMap(namespaces);
        STANDARD_BUILT_IN_PREFIXES_BY_NAMESPACE = Collections.unmodifiableMap(prefixes);
        STANDARD_BUILT_IN_PREFIXES = Collections.unmodifiableSet(namespaces.keySet());
        STANDARD_BUILT_IN_URIS = Collections.unmodifiableSet(new HashSet<String>(namespaces.values()));
    }

    private final Behavior behavior;
    private final NamespaceRegistry registry;
    private final NamespaceRegistry workspaceRegistry;

    JcrNamespaceRegistry( NamespaceRegistry workspaceRegistry ) {
        this(Behavior.WORKSPACE, null, workspaceRegistry);
    }

    JcrNamespaceRegistry( Behavior behavior,
                          NamespaceRegistry localRegistry,
                          NamespaceRegistry workspaceRegistry ) {
        this.behavior = behavior;
        this.registry = localRegistry != null ? localRegistry : workspaceRegistry;
        this.workspaceRegistry = workspaceRegistry;
        // Add the built-ins, ensuring we overwrite any badly-initialized values ...
        for (Map.Entry<String, String> builtIn : STANDARD_BUILT_IN_NAMESPACES_BY_PREFIX.entrySet()) {
            this.registry.register(builtIn.getKey(), builtIn.getValue());
        }
        assert this.behavior != null;
        assert this.registry != null;
        assert this.workspaceRegistry != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#getPrefix(java.lang.String)
     */
    public String getPrefix( String uri ) throws NamespaceException, RepositoryException {
        if (behavior == Behavior.WORKSPACE) {
            // Check the standard ones first, ensuring that invalid changes to the persistent storage don't matter ...
            String prefix = STANDARD_BUILT_IN_PREFIXES_BY_NAMESPACE.get(uri);
            if (prefix != null) return prefix;
        }
        // Now check the underlying registry ...
        String prefix = registry.getPrefixForNamespaceUri(uri, false);
        if (prefix == null) {
            throw new NamespaceException(JcrI18n.noNamespaceWithUri.text(uri));
        }
        return prefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     */
    public String[] getPrefixes() {
        Set<Namespace> namespaces = registry.getNamespaces();
        String[] prefixes = new String[namespaces.size()];
        int i = 0;
        for (Namespace namespace : namespaces) {
            prefixes[i++] = namespace.getPrefix();
        }
        return prefixes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#getURI(java.lang.String)
     */
    public String getURI( String prefix ) throws NamespaceException, RepositoryException {
        if (behavior == Behavior.WORKSPACE) {
            // Check the standard ones first, ensuring that invalid changes to the persistent storage don't matter ...
            String uri = STANDARD_BUILT_IN_NAMESPACES_BY_PREFIX.get(prefix);
            if (uri != null) return uri;
        }
        // Now check the underlying registry ...
        String uri = registry.getNamespaceForPrefix(prefix);
        if (uri == null) {
            throw new NamespaceException(JcrI18n.noNamespaceWithPrefix.text(prefix));
        }
        return uri;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#getURIs()
     */
    public String[] getURIs() {
        Set<Namespace> namespaces = registry.getNamespaces();
        String[] uris = new String[namespaces.size()];
        int i = 0;
        for (Namespace namespace : namespaces) {
            uris[i++] = namespace.getNamespaceUri();
        }
        return uris;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#registerNamespace(java.lang.String, java.lang.String)
     */
    public synchronized void registerNamespace( String prefix,
                                                String uri ) throws NamespaceException, RepositoryException {
        CheckArg.isNotNull(prefix, "prefix");
        CheckArg.isNotNull(uri, "uri");

        switch (behavior) {
            case JSR170_SESSION:
                // ----------------------------------------------------------
                // JSR-170 Session remapping behavior (see Section 6.3.3) ...
                // ----------------------------------------------------------
                // Section 6.3.3:
                // "If existingUri is not registered in the NamespaceRegistry a NamespaceException will be thrown.
                //
                // If newPrefix is already locally mapped to existingUri (i.e., within this Session, by virtue
                // of an earlier setNamespaceRegistry call) then this method returns silently and has no effect.
                //
                // If newPrefix is already locally mapped to a URI other than existingUri, then that URI reverts to its
                // globally mapped prefix (as set in the NamespaceRegistry) and newPrefix is locally mapped to existingUri.
                //
                // If newPrefix is already assigned in the global NamespaceRegistry to otheruri (which differs from
                // existingUri) and otherUri has not been locally mapped to another prefix which differs from newPrefix,
                // then a NamespaceException will be thrown. In order to successfully locally map newPrefix to existingUri,
                // otherUri must first be locally mapped to another prefix."

                // The URI must already be registered ...
                String existingPrefix = registry.getPrefixForNamespaceUri(uri, false);
                if (existingPrefix == null) {
                    // Paragraph 1 ...
                    throw new NamespaceException(JcrI18n.unableToRemapUriNotRegisteredInNamespaceRegistry.text(prefix, uri));
                }
                if (existingPrefix.equals(prefix)) return; // Paragraph 2

                // Is the prefix already used in a mapping ...
                String existingUri = registry.getNamespaceForPrefix(prefix);
                if (existingUri != null) {
                    // Is this existing mapping local, or is it in the global (workspace) registry?
                    String globalPrefix = workspaceRegistry.getPrefixForNamespaceUri(existingUri, false);
                    if (!prefix.equals(globalPrefix)) {
                        // Paragraph 3: The mapping is local to the session, so this local mapping should just be reverted ...
                        registry.unregister(existingUri);
                    }

                    // Paragraph 4: The mapping is global, so this is not allowed; the existing ...
                    String msg = JcrI18n.unableToRemapUriUsingPrefixUsedInNamespaceRegistry.text(prefix, uri, existingUri);
                    throw new NamespaceException(msg);
                }

                // Otherwise, the prefix is not already used in a mapping, so we can continue ...

                break;

            case JSR283_SESSION:
                // --------------------------------------
                // JSR-283 Session remapping behavior ...
                // --------------------------------------
                // Section 4.3.3 (of the Draft specification):
                // "All local mappings already present in the Session that include either the specified prefix
                // or the specified uri are removed and the new mapping is added."
                String existingUriForPrefix = registry.getNamespaceForPrefix(prefix);
                if (existingUriForPrefix != null) {
                    registry.unregister(existingUriForPrefix);
                }
                registry.unregister(uri);

                break;

            case WORKSPACE:
                // --------------------------------------------------
                // JSR-170 & JSR-283 Workspace namespace registry ...
                // --------------------------------------------------

                // Check the zero-length prefix and zero-length URI ...
                if (DEFAULT_NAMESPACE_PREFIX.equals(prefix) || DEFAULT_NAMESPACE_URI.equals(uri)) {
                    throw new NamespaceException(JcrI18n.unableToChangeTheDefaultNamespace.text());
                }
                // Check whether the prefix or URI are reserved (case-sensitive) ...
                if (STANDARD_BUILT_IN_PREFIXES.contains(prefix)) {
                    throw new NamespaceException(JcrI18n.unableToRegisterReservedNamespacePrefix.text(prefix, uri));
                }
                if (STANDARD_BUILT_IN_URIS.contains(uri)) {
                    throw new NamespaceException(JcrI18n.unableToRegisterReservedNamespaceUri.text(prefix, uri));
                }
                break;
            default:
                assert false; // should never happen
        }

        // Check the zero-length prefix and zero-length URI ...
        if (DEFAULT_NAMESPACE_PREFIX.equals(prefix) || DEFAULT_NAMESPACE_URI.equals(uri)) {
            throw new NamespaceException(JcrI18n.unableToChangeTheDefaultNamespace.text());
        }

        // Check whether the prefix begins with 'xml' (in any case) ...
        if (prefix.toLowerCase().startsWith(XML_NAMESPACE_PREFIX)) {
            throw new NamespaceException(JcrI18n.unableToRegisterNamespaceUsingXmlPrefix.text(prefix, uri));
        }

        // The prefix must be a valid XML Namespace prefix (i.e., a valid NCName) ...
        if (!XmlCharacters.isValidName(prefix)) {
            throw new NamespaceException(JcrI18n.unableToRegisterNamespaceWithInvalidPrefix.text(prefix, uri));
        }

        // Now we're sure the prefix and URI are valid and okay for a custom mapping ...
        try {
            registry.register(prefix, uri);
        } catch (RuntimeException e) {
            throw new RepositoryException(e.getMessage(), e.getCause());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#unregisterNamespace(java.lang.String)
     */
    public synchronized void unregisterNamespace( String prefix )
        throws NamespaceException, AccessDeniedException, RepositoryException {
        CheckArg.isNotNull(prefix, "prefix");
        // Look to see whether the prefix is registered ...
        String uri = registry.getNamespaceForPrefix(prefix);
        // It is an error to unregister a namespace that is not registered ...
        if (uri == null) {
            throw new NamespaceException(JcrI18n.unableToUnregisterPrefixForNamespaceThatIsNotRegistered.text(prefix));
        }
        // Unregistering a built-in prefix or URI is invalid ...
        if (STANDARD_BUILT_IN_PREFIXES.contains(prefix)) {
            throw new NamespaceException(JcrI18n.unableToUnregisterReservedNamespacePrefix.text(prefix, uri));
        }
        if (STANDARD_BUILT_IN_URIS.contains(uri)) {
            throw new NamespaceException(JcrI18n.unableToUnregisterReservedNamespaceUri.text(prefix, uri));
        }

        // Now we're sure the prefix is valid and is actually used in a mapping ...
        try {
            registry.unregister(uri);
        } catch (RuntimeException e) {
            throw new RepositoryException(e.getMessage(), e.getCause());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return registry.toString();
    }
}
