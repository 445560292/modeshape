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

import java.io.OutputStream;
import java.util.Collections;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.ValueFactories;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Implementation of {@link AbstractJcrExporter} that implements the document view mapping described in section 6.4.2 of the JCR
 * 1.0 specification.
 * 
 * @see JcrSession#exportDocumentView(String, ContentHandler, boolean, boolean)
 * @see JcrSession#exportDocumentView(String, OutputStream, boolean, boolean)
 */
@NotThreadSafe
class JcrDocumentViewExporter extends AbstractJcrExporter {

    JcrDocumentViewExporter( JcrSession session ) {
        super(session, Collections.<String>emptyList());
    }

    /**
     * Exports <code>node</code> (or the subtree rooted at <code>node</code>) into an XML document by invoking SAX events on
     * <code>contentHandler</code>.
     * 
     * @param node the node which should be exported. If <code>noRecursion</code> was set to <code>false</code> in the
     *        constructor, the entire subtree rooted at <code>node</code> will be exported.
     * @param contentHandler the SAX content handler for which SAX events will be invoked as the XML document is created.
     * @param skipBinary if <code>true</code>, indicates that binary properties should not be exported
     * @param noRecurse if<code>true</code>, indicates that only the given node should be exported, otherwise a recursive export
     *        and not any of its child nodes.
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    @Override
    public void exportNode( Node node,
                            ContentHandler contentHandler,
                            boolean skipBinary,
                            boolean noRecurse ) throws RepositoryException, SAXException {
        ExecutionContext executionContext = session.getExecutionContext();

        // If this node is a special xmltext node, output it as raw content (see JCR 1.0 spec - section 6.4.2.3
        if (node.getParent() != null && isXmlTextNode(node)) {

            String xmlCharacters = getXmlCharacters(node);
            contentHandler.characters(xmlCharacters.toCharArray(), 0, xmlCharacters.length());

            return;
        }

        ValueFactories valueFactories = executionContext.getValueFactories();
        AttributesImpl atts = new AttributesImpl();

        // Build the attributes for this node's element
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property prop = properties.nextProperty();

            if (skipBinary && PropertyType.BINARY == prop.getType()) {
                continue;
            }

            Name propName = ((AbstractJcrProperty)prop).getDnaProperty().getName();

            String localPropName = getPrefixedName(propName);

            Value value;
            if (prop instanceof JcrSingleValueProperty) {
                value = prop.getValue();
            } else {
                // Only output the first value of the multi-valued property. This is acceptable as per JCR 1.0 Spec - section
                // 6.4.2.5
                value = prop.getValues()[0];
            }
            atts.addAttribute(propName.getNamespaceUri(),
                              propName.getLocalName(),
                              localPropName,
                              PropertyType.nameFromValue(prop.getType()),
                              value.getString());
        }

        Name name;

        // Special case to stub in name for root node as per JCR 1.0 Spec - 6.4.2.2
        if ("/".equals(node.getPath())) {
            name = JcrLexicon.ROOT;
        } else {
            name = valueFactories.getNameFactory().create(node.getName());
        }

        startElement(contentHandler, name, atts);

        if (!noRecurse) {
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                exportNode(nodes.nextNode(), contentHandler, skipBinary, noRecurse);
            }
        }

        endElement(contentHandler, name);
    
    }

    /**
     * Indicates whether the current node is an XML text node as per section 6.4.2.3 of the JCR 1.0 specification.
     * XML text nodes are nodes that have the name &quot;jcr:xmltext&quot; and only one property (besides the mandatory
     * &quot;jcr:primaryType&quot;).  The property must have a property name of &quot;jcr:xmlcharacters&quot;, a type of <code>String</code>,
     * and does not have multiple values.<p/>
     * In practice, this is handled in DNA by making XML text nodes have a type of &quot;dna:xmltext&quot;, which
     * enforces these property characteristics.
     * 
     * @param node the node to test
     * @return whether this node is a special xml text node
     * @throws RepositoryException if there is an error accessing the repository
     */
    private boolean isXmlTextNode( Node node ) throws RepositoryException {
        // ./xmltext/xmlcharacters exception (see JSR-170 Spec 6.4.2.3)

        if (getPrefixedName(JcrLexicon.XMLTEXT).equals(node.getName())) {
            if (node.getNodes().getSize() == 0) {

                PropertyIterator properties = node.getProperties();
                boolean xmlCharactersFound = false;

                while (properties.hasNext()) {
                    Property property = properties.nextProperty();

                    if (getPrefixedName(JcrLexicon.PRIMARY_TYPE).equals(property.getName())) {
                        continue;
                    }

                    if (getPrefixedName(JcrLexicon.XMLCHARACTERS).equals(property.getName())) {
                        xmlCharactersFound = true;
                        continue;
                    }

                    // If the xmltext node has any properties other than primaryType or xmlcharacters, return false;
                    return false;
                }

                return xmlCharactersFound;
            }
        }

        return false;

    }

    /**
     * Returns the XML characters for the given node. 
     * The node must be an XML text node, as defined in {@link #isXmlTextNode(Node)}.
     * 
     * @param node the node for which XML characters will be retrieved.
     * @return the xml characters for this node
     * @throws RepositoryException if there is an error accessing this node
     */
    private String getXmlCharacters( Node node ) throws RepositoryException {
        // ./xmltext/xmlcharacters exception (see JSR-170 Spec 6.4.2.3)

        assert isXmlTextNode(node);
        
        Property xmlCharacters = node.getProperty(getPrefixedName(JcrLexicon.XMLCHARACTERS));

        assert xmlCharacters != null;

        if (xmlCharacters.getDefinition().isMultiple()) {
            StringBuffer buff = new StringBuffer();

            for (Value value : xmlCharacters.getValues()) {
                buff.append(value.getString());
            }

            return buff.toString();
        }

        return xmlCharacters.getValue().getString();
    }

}
