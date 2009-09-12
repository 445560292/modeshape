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
package org.jboss.example.dna.sequencer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.jboss.dna.common.collection.Problem;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.repository.RepositoryI18n;

/**
 * Utility methods for working with JCR.
 */
public class JcrTools {

    public Map<String, Object> loadProperties( Node propertyContainer,
                                               Problems problems ) {
        return loadProperties(propertyContainer, null, problems);
    }

    public Map<String, Object> loadProperties( Node propertyContainer,
                                               Map<String, Object> properties,
                                               Problems problems ) {
        if (properties == null) properties = new HashMap<String, Object>();
        if (propertyContainer != null) {
            try {
                NodeIterator iter = propertyContainer.getNodes();
                while (iter.hasNext()) {
                    Node propertyNode = iter.nextNode();
                    if (propertyNode != null && propertyNode.getPrimaryNodeType().isNodeType("dna:property")) {
                        String propertyName = propertyNode.getName();
                        Object propertyValue = getPropertyValue(propertyNode, "dna:propertyValue", true, problems);
                        properties.put(propertyName, propertyValue);
                    }
                }
            } catch (RepositoryException e) {
                problems.addError(e, RepositoryI18n.errorReadingPropertiesFromContainerNode, getReadable(propertyContainer));
            }
        }

        return properties;
    }

    public boolean removeProblems( Node parent ) throws RepositoryException {
        Node problemsNode = null;
        if (parent.hasNode("dna:problems")) {
            problemsNode = parent.getNode("dna:problems");
            problemsNode.remove();
            return true;
        }
        return false;
    }

    public boolean storeProblems( Node parent,
                                  Problems problems ) throws RepositoryException {
        Node problemsNode = null;
        if (parent.hasNode("dna:problems")) {
            problemsNode = parent.getNode("dna:problems");
            // Delete all problems ...
            removeAllChildren(problemsNode);
        }
        if (problems.isEmpty()) {
            return false;
        }
        if (problemsNode == null) {
            problemsNode = parent.addNode("dna:problems"); // primary type dictated by child definition
        }

        // Add a child for each problem ...
        for (Problem problem : problems) {
            Node problemNode = problemsNode.addNode("problem", "dna:problem");
            // - dna:status (string) mandatory copy
            // < 'ERROR', 'WARNING', 'INFO'
            // - dna:message (string) mandatory copy
            // - dna:code (string) copy
            // - dna:type (string) copy
            // - dna:resource (string) copy
            // - dna:location (string) copy
            // - dna:trace (string) copy
            problemNode.setProperty("dna:status", problem.getStatus().name());
            problemNode.setProperty("dna:message", problem.getMessageString());
            if (problem.getCode() != Problem.DEFAULT_CODE) {
                problemNode.setProperty("dna:code", Integer.toString(problem.getCode()));
            }
            String resource = problem.getResource();
            if (resource != null) {
                problemNode.setProperty("dna:resource", resource);
            }
            String location = problem.getLocation();
            if (location != null) {
                problemNode.setProperty("dna:location", location);
            }
            Throwable t = problem.getThrowable();
            if (t != null) {
                String trace = StringUtil.getStackTrace(t);
                problemNode.setProperty("dna:trace", trace);
            }
        }
        return true;
    }

    public int removeAllChildren( Node node ) throws RepositoryException {
        int childrenRemoved = 0;
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            child.remove();
            ++childrenRemoved;
        }
        return childrenRemoved;
    }

    public String getPropertyAsString( Node node,
                                       String propertyName,
                                       boolean required,
                                       Problems problems ) {
        return getPropertyAsString(node, propertyName, required, null);
    }

    public String getPropertyAsString( Node node,
                                       String propertyName,
                                       boolean required,
                                       String defaultValue,
                                       Problems problems ) {
        try {
            Property property = node.getProperty(propertyName);
            return property.getString();
        } catch (ValueFormatException e) {
            if (required) {
                problems.addError(e,
                                  RepositoryI18n.requiredPropertyOnNodeWasExpectedToBeStringValue,
                                  propertyName,
                                  getReadable(node));
            } else {
                problems.addError(e,
                                  RepositoryI18n.optionalPropertyOnNodeWasExpectedToBeStringValue,
                                  propertyName,
                                  getReadable(node));
            }
        } catch (PathNotFoundException e) {
            if (required) {
                problems.addError(e, RepositoryI18n.requiredPropertyIsMissingFromNode, propertyName, getReadable(node));
            }
            if (!required) return defaultValue;
        } catch (RepositoryException err) {
            if (required) {
                problems.addError(err, RepositoryI18n.errorGettingRequiredPropertyFromNode, propertyName, getReadable(node));
            } else {
                problems.addError(err, RepositoryI18n.errorGettingOptionalPropertyFromNode, propertyName, getReadable(node));
            }
        }
        return null;
    }

    public Object getPropertyValue( Node node,
                                    String propertyName,
                                    boolean required,
                                    Problems problems ) {
        try {
            Property property = node.getProperty(propertyName);
            switch (property.getType()) {
                case PropertyType.BINARY: {
                    InputStream stream = property.getStream();
                    try {
                        stream = property.getStream();
                        return IoUtil.readBytes(stream);
                    } finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException e) {
                                // Log ...
                                Logger.getLogger(this.getClass())
                                      .error(e,
                                             RepositoryI18n.errorClosingBinaryStreamForPropertyFromNode,
                                             propertyName,
                                             node.getPath());
                            }
                        }
                    }
                }
                default: {
                    return property.getString();
                }
            }
        } catch (IOException e) {
            if (required) {
                problems.addError(e, RepositoryI18n.requiredPropertyOnNodeCouldNotBeRead, propertyName, getReadable(node));
            } else {
                problems.addError(e, RepositoryI18n.optionalPropertyOnNodeCouldNotBeRead, propertyName, getReadable(node));
            }
        } catch (PathNotFoundException e) {
            if (required) {
                problems.addError(e, RepositoryI18n.requiredPropertyIsMissingFromNode, propertyName, getReadable(node));
            }
        } catch (RepositoryException err) {
            if (required) {
                problems.addError(err, RepositoryI18n.errorGettingRequiredPropertyFromNode, propertyName, getReadable(node));
            } else {
                problems.addError(err, RepositoryI18n.errorGettingOptionalPropertyFromNode, propertyName, getReadable(node));
            }
        }
        return null;
    }

    public String[] getPropertyAsStringArray( Node node,
                                              String propertyName,
                                              boolean required,
                                              Problems problems,
                                              String... defaultValues ) {
        String[] result = defaultValues;
        try {
            Property property = node.getProperty(propertyName);
            if (property.getDefinition().isMultiple()) {
                Value[] values = property.getValues();
                result = new String[values.length];
                int i = 0;
                for (Value value : values) {
                    result[i++] = value.getString();
                }
            } else {
                result = new String[] {property.getString()};
            }
        } catch (ValueFormatException e) {
            if (required) {
                problems.addError(e,
                                  RepositoryI18n.requiredPropertyOnNodeWasExpectedToBeStringArrayValue,
                                  propertyName,
                                  getReadable(node));
            } else {
                problems.addError(e,
                                  RepositoryI18n.optionalPropertyOnNodeWasExpectedToBeStringArrayValue,
                                  propertyName,
                                  getReadable(node));
            }
        } catch (PathNotFoundException e) {
            if (required) {
                problems.addError(e, RepositoryI18n.requiredPropertyIsMissingFromNode, propertyName, getReadable(node));
            }
        } catch (RepositoryException err) {
            if (required) {
                problems.addError(err, RepositoryI18n.errorGettingRequiredPropertyFromNode, propertyName, getReadable(node));
            } else {
                problems.addError(err, RepositoryI18n.errorGettingOptionalPropertyFromNode, propertyName, getReadable(node));
            }
        }
        return result;
    }

    public Node getNode( Node node,
                         String relativePath,
                         boolean required,
                         Problems problems ) {
        Node result = null;
        try {
            result = node.getNode(relativePath);
        } catch (PathNotFoundException e) {
            if (required) problems.addError(e,
                                            RepositoryI18n.requiredNodeDoesNotExistRelativeToNode,
                                            relativePath,
                                            getReadable(node));
        } catch (RepositoryException err) {
            problems.addError(err, RepositoryI18n.errorGettingNodeRelativeToNode, relativePath, getReadable(node));
        }
        return result;
    }

    public String getReadable( Node node ) {
        if (node == null) return "";
        try {
            return node.getPath();
        } catch (RepositoryException err) {
            return node.toString();
        }
    }

    public Node findOrCreateNode( Session session,
                                  String path ) throws RepositoryException {
        return findOrCreateNode(session, path, null, null);
    }

    public Node findOrCreateNode( Session session,
                                  String path,
                                  String nodeType ) throws RepositoryException {
        return findOrCreateNode(session, path, nodeType, nodeType);
    }

    public Node findOrCreateNode( Session session,
                                  String path,
                                  String defaultNodeType,
                                  String finalNodeType ) throws RepositoryException {
        Node root = session.getRootNode();
        return findOrCreateNode(session, root, path, defaultNodeType, finalNodeType);
    }

    public Node findOrCreateNode( Session session,
                                  Node parentNode,
                                  String path,
                                  String defaultNodeType,
                                  String finalNodeType ) throws RepositoryException {
        // Remove leading and trailing slashes ...
        String relPath = path.replaceAll("^/+", "").replaceAll("/+$", "");

        // Look for the node first ...
        try {
            return parentNode.getNode(relPath);
        } catch (PathNotFoundException e) {
            // continue
        }
        // Create the node, which has to be done segment by segment ...
        String[] pathSegments = relPath.split("/");
        Node node = parentNode;
        for (int i = 0, len = pathSegments.length; i != len; ++i) {
            String pathSegment = pathSegments[i];
            pathSegment = pathSegment.trim();
            if (pathSegment.length() == 0) continue;
            if (node.hasNode(pathSegment)) {
                // Find the existing node ...
                node = node.getNode(pathSegment);
            } else {
                // Make sure there is no index on the final segment ...
                String pathSegmentWithNoIndex = pathSegment.replaceAll("(\\[\\d+\\])+$", "");
                // Create the node ...
                String nodeType = defaultNodeType;
                if (i == len - 1 && finalNodeType != null) nodeType = finalNodeType;
                if (nodeType != null) {
                    node = node.addNode(pathSegmentWithNoIndex, nodeType);
                } else {
                    node = node.addNode(pathSegmentWithNoIndex);
                }
            }
        }
        return node;
    }

    public Node findOrCreateChild( Session session,
                                   Node parent,
                                   String name ) throws RepositoryException {
        return findOrCreateChild(session, parent, name, null);
    }

    public Node findOrCreateChild( Session session,
                                   Node parent,
                                   String name,
                                   String nodeType ) throws RepositoryException {
        return findOrCreateNode(session, parent, name, nodeType, nodeType);
    }

}
