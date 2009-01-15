/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.ReadPropertyRequest;
import org.jboss.dna.graph.request.RemovePropertiesRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.xml.sax.SAXException;

/**
 * A graph representation of the content within a {@link RepositorySource}, including mechanisms to interact and manipulate that
 * content. The graph is designed to be an <i><a href="http://en.wikipedia.org/wiki/Domain_Specific_Language">embedded domain
 * specific language</a></i>, meaning calls to it are designed to read like sentences even though they are really just Java
 * methods. And to be more readable, methods can be chained together.
 * 
 * @author Randall Hauch
 */
@NotThreadSafe
public class Graph {

    /**
     * Create a graph instance that uses the supplied repository and {@link ExecutionContext context}.
     * 
     * @param sourceName the name of the source that should be used
     * @param connectionFactory the factory of repository connections
     * @param context the context in which all executions should be performed
     * @return the new graph
     * @throws IllegalArgumentException if the source or context parameters are null
     */
    public static Graph create( String sourceName,
                                RepositoryConnectionFactory connectionFactory,
                                ExecutionContext context ) {
        return new Graph(sourceName, connectionFactory, context);
    }

    /**
     * Create a graph instance that uses the supplied {@link RepositoryConnection} and {@link ExecutionContext context}.
     * 
     * @param connection the connection that should be used
     * @param context the context in which all executions should be performed
     * @return the new graph
     * @throws IllegalArgumentException if the connection or context parameters are null
     */
    public static Graph create( final RepositoryConnection connection,
                                ExecutionContext context ) {
        CheckArg.isNotNull(connection, "connection");
        final String connectorSourceName = connection.getSourceName();
        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                if (connectorSourceName.equals(sourceName)) return connection;
                return null;
            }
        };
        return new Graph(connectorSourceName, connectionFactory, context);
    }

    /**
     * Create a graph instance that uses the supplied {@link RepositoryConnection} and {@link ExecutionContext context}.
     * 
     * @param source the source that should be used
     * @param context the context in which all executions should be performed
     * @return the new graph
     * @throws IllegalArgumentException if the connection or context parameters are null
     */
    public static Graph create( final RepositorySource source,
                                ExecutionContext context ) {
        CheckArg.isNotNull(source, "source");
        final String connectorSourceName = source.getName();
        RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                if (connectorSourceName.equals(sourceName)) return source.getConnection();
                return null;
            }
        };
        return new Graph(connectorSourceName, connectionFactory, context);
    }

    private final String sourceName;
    private final RepositoryConnectionFactory connectionFactory;
    private final ExecutionContext context;
    private final RequestQueue requestQueue;
    private final Conjunction<Graph> nextGraph;

    protected Graph( String sourceName,
                     RepositoryConnectionFactory connectionFactory,
                     ExecutionContext context ) {
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isNotNull(context, "context");
        this.sourceName = sourceName;
        this.connectionFactory = connectionFactory;
        this.context = context;
        this.requestQueue = new GraphRequestQueue();
        this.nextGraph = new Conjunction<Graph>() {
            public Graph and() {
                return Graph.this;
            }
        };
    }

    /**
     * Get the RepositoryConnectionFactory that this graph uses to create {@link RepositoryConnection repository connections}.
     * 
     * @return the factory repository connections used by this graph; never null
     */
    public RepositoryConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * The name of the repository that will be used by this graph. This name is passed to the {@link #getConnectionFactory()
     * connection factory} when this graph needs to {@link RepositoryConnectionFactory#createConnection(String) obtain} a
     * {@link RepositoryConnection repository connection}.
     * 
     * @return the name of the source
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Get the context of execution within which operations on this graph are performed.
     * 
     * @return the execution context; never null
     */
    public ExecutionContext getContext() {
        return context;
    }

    /*package*/RequestQueue queue() {
        return this.requestQueue;
    }

    /**
     * Get the default cache policy for this graph. May be null if such a policy has not been defined for thie
     * {@link #getSourceName() source}.
     * 
     * @return the default cache policy, or null if no such policy has been defined for the source
     * @throws RepositorySourceException if no repository source with the {@link #getSourceName() name} could be found
     */
    public CachePolicy getDefaultCachePolicy() {
        RepositoryConnection connection = this.connectionFactory.createConnection(getSourceName());
        if (connection == null) {
            throw new RepositorySourceException(GraphI18n.unableToFindRepositorySourceWithName.text(getSourceName()));
        }
        try {
            return connection.getDefaultCachePolicy();
        } finally {
            connection.close();
        }
    }

    /**
     * Begin the request to move the specified node into a parent node at a different location, which is specified via the
     * <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Node from ) {
        return new MoveAction<Conjunction<Graph>>(this.nextGraph, this.requestQueue, from.getLocation());
    }

    /**
     * Begin the request to move a node at the specified location into a parent node at a different location, which is specified
     * via the <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the location of the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Location from ) {
        return new MoveAction<Conjunction<Graph>>(this.nextGraph, this.requestQueue, from);
    }

    /**
     * Begin the request to move a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param fromPath the path to the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( String fromPath ) {
        return new MoveAction<Conjunction<Graph>>(this.nextGraph, this.requestQueue, new Location(createPath(fromPath)));
    }

    /**
     * Begin the request to move a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the path to the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Path from ) {
        return new MoveAction<Conjunction<Graph>>(this.nextGraph, this.requestQueue, new Location(from));
    }

    /**
     * Begin the request to move a node with the specified unique identifier into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Move} object.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the UUID of the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( UUID from ) {
        return new MoveAction<Conjunction<Graph>>(this.nextGraph, this.requestQueue, new Location(from));
    }

    /**
     * Begin the request to move a node with the specified unique identification property into a parent node at a different
     * location, which is specified via the <code>into(...)</code> method on the returned {@link Move} object. The identification
     * property should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param idProperty the unique identification property of the node that is to be moved.
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Property idProperty ) {
        return new MoveAction<Conjunction<Graph>>(this.nextGraph, this.requestQueue, new Location(idProperty));
    }

    /**
     * Begin the request to move a node with the specified identification properties into a parent node at a different location,
     * which is specified via the <code>into(...)</code> method on the returned {@link Move} object. The identification properties
     * should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the move request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param firstIdProperty the first identification property of the node that is to be moved
     * @param additionalIdProperties the remaining idenficiation properties of the node that is to be moved
     * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is to
     *         be moved
     */
    public Move<Conjunction<Graph>> move( Property firstIdProperty,
                                          Property... additionalIdProperties ) {
        return new MoveAction<Conjunction<Graph>>(this.nextGraph, this.requestQueue, new Location(firstIdProperty,
                                                                                                  additionalIdProperties));
    }

    /**
     * Begin the request to copy the specified node into a parent node at a different location, which is specified via the
     * <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Node from ) {
        return new CopyAction<Graph>(this, this.requestQueue, from.getLocation());
    }

    /**
     * Begin the request to copy a node at the specified location into a parent node at a different location, which is specified
     * via the <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the location of the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Location from ) {
        return new CopyAction<Graph>(this, this.requestQueue, from);
    }

    /**
     * Begin the request to copy a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param fromPath the path to the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( String fromPath ) {
        return new CopyAction<Graph>(this, this.requestQueue, new Location(createPath(fromPath)));
    }

    /**
     * Begin the request to copy a node located at the supplied path into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the path to the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Path from ) {
        return new CopyAction<Graph>(this, this.requestQueue, new Location(from));
    }

    /**
     * Begin the request to copy a node with the specified unique identifier into a parent node at a different location, which is
     * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param from the UUID of the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( UUID from ) {
        return new CopyAction<Graph>(this, this.requestQueue, new Location(from));
    }

    /**
     * Begin the request to copy a node with the specified unique identification property into a parent node at a different
     * location, which is specified via the <code>into(...)</code> method on the returned {@link Copy} object. The identification
     * property should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param idProperty the unique identification property of the node that is to be copied.
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Property idProperty ) {
        return new CopyAction<Graph>(this, this.requestQueue, new Location(idProperty));
    }

    /**
     * Begin the request to copy a node with the specified identification properties into a parent node at a different location,
     * which is specified via the <code>into(...)</code> method on the returned {@link Copy} object. The identification properties
     * should uniquely identify a single node.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>into(...)</code>
     * method is called.
     * </p>
     * 
     * @param firstIdProperty the first identification property of the node that is to be copied
     * @param additionalIdProperties the remaining idenficiation properties of the node that is to be copied
     * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node is to
     *         be copied
     */
    public Copy<Graph> copy( Property firstIdProperty,
                             Property... additionalIdProperties ) {
        return new CopyAction<Graph>(this, this.requestQueue, new Location(firstIdProperty, additionalIdProperties));
    }

    /**
     * Request to delete the specified node. This request is submitted to the repository immediately.
     * 
     * @param at the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Node at ) {
        this.requestQueue.submit(new DeleteBranchRequest(at.getLocation()));
        return nextGraph;
    }

    /**
     * Request to delete the node at the given location. This request is submitted to the repository immediately.
     * 
     * @param at the location of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Location at ) {
        this.requestQueue.submit(new DeleteBranchRequest(at));
        return nextGraph;
    }

    /**
     * Request to delete the node at the given path. This request is submitted to the repository immediately.
     * 
     * @param atPath the path of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( String atPath ) {
        this.requestQueue.submit(new DeleteBranchRequest(new Location(createPath(atPath))));
        return nextGraph;
    }

    /**
     * Request to delete the node at the given path. This request is submitted to the repository immediately.
     * 
     * @param at the path of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Path at ) {
        this.requestQueue.submit(new DeleteBranchRequest(new Location(at)));
        return nextGraph;
    }

    /**
     * Request to delete the node with the given UUID. This request is submitted to the repository immediately.
     * 
     * @param at the UUID of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( UUID at ) {
        this.requestQueue.submit(new DeleteBranchRequest(new Location(at)));
        return nextGraph;
    }

    /**
     * Request to delete the node with the given unique identification property. This request is submitted to the repository
     * immediately.
     * 
     * @param idProperty the unique identifying property of the node that is to be deleted
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Property idProperty ) {
        this.requestQueue.submit(new DeleteBranchRequest(new Location(idProperty)));
        return nextGraph;
    }

    /**
     * Request to delete the node with the given identification properties. The identification properties should uniquely identify
     * a single node. This request is submitted to the repository immediately.
     * 
     * @param firstIdProperty the first identification property of the node that is to be copied
     * @param additionalIdProperties the remaining idenficiation properties of the node that is to be copied
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> delete( Property firstIdProperty,
                                      Property... additionalIdProperties ) {
        this.requestQueue.submit(new DeleteBranchRequest(new Location(firstIdProperty, additionalIdProperties)));
        return nextGraph;
    }

    /**
     * Begin the request to create a node located at the supplied path. This request is submitted to the repository immediately.
     * <p>
     * If you have the {@link Location} of the parent (for the new node) from a previous request, it is better and more efficient
     * to use {@link #createUnder(Location)}. However, this method work just as well if all you have is the {@link Path} to the
     * parent or new node.
     * </p>
     * 
     * @param atPath the path to the node that is to be created.
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> create( String atPath ) {
        Path at = createPath(atPath);
        Path parent = at.getParent();
        Name child = at.getLastSegment().getName();
        this.requestQueue.submit(new CreateNodeRequest(new Location(parent), child));
        return nextGraph;
    }

    /**
     * Begin the request to create a node located at the supplied path. This request is submitted to the repository immediately.
     * <p>
     * If you have the {@link Location} of the parent (for the new node) from a previous request, it is better and more efficient
     * to use {@link #createUnder(Location)}. However, this method work just as well if all you have is the {@link Path} to the
     * parent or new node.
     * </p>
     * 
     * @param atPath the path to the node that is to be created.
     * @param properties the properties for the new node
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> create( String atPath,
                                      Property... properties ) {
        Path at = createPath(atPath);
        Path parent = at.getParent();
        Name child = at.getLastSegment().getName();
        this.requestQueue.submit(new CreateNodeRequest(new Location(parent), child, properties));
        return nextGraph;
    }

    /**
     * Begin the request to create a node located at the supplied path. This request is submitted to the repository immediately.
     * <p>
     * If you have the {@link Location} of the parent (for the new node) from a previous request, it is better and more efficient
     * to use {@link #createUnder(Location)}. However, this method work just as well if all you have is the {@link Path} to the
     * parent or new node.
     * </p>
     * 
     * @param at the path to the node that is to be created.
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> create( Path at ) {
        CheckArg.isNotNull(at, "at");
        Path parent = at.getParent();
        Name child = at.getLastSegment().getName();
        this.requestQueue.submit(new CreateNodeRequest(new Location(parent), child));
        return nextGraph;
    }

    /**
     * Begin the request to create a node located at the supplied path. This request is submitted to the repository immediately.
     * <p>
     * If you have the {@link Location} of the parent (for the new node) from a previous request, it is better and more efficient
     * to use {@link #createUnder(Location)}. However, this method work just as well if all you have is the {@link Path} to the
     * parent or new node.
     * </p>
     * 
     * @param at the path to the node that is to be created.
     * @param properties the properties for the new node
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> create( Path at,
                                      Property... properties ) {
        CheckArg.isNotNull(at, "at");
        Path parent = at.getParent();
        Name child = at.getLastSegment().getName();
        this.requestQueue.submit(new CreateNodeRequest(new Location(parent), child, properties));
        return nextGraph;
    }

    /**
     * Begin the request to create a node located at the supplied path. This request is submitted to the repository immediately.
     * <p>
     * If you have the {@link Location} of the parent (for the new node) from a previous request, it is better and more efficient
     * to use {@link #createUnder(Location)}. However, this method work just as well if all you have is the {@link Path} to the
     * parent or new node.
     * </p>
     * 
     * @param at the path to the node that is to be created.
     * @param properties the properties for the new node
     * @return an object that may be used to start another request
     */
    public Conjunction<Graph> create( Path at,
                                      Iterable<Property> properties ) {
        CheckArg.isNotNull(at, "at");
        Path parent = at.getParent();
        Name child = at.getLastSegment().getName();
        this.requestQueue.submit(new CreateNodeRequest(new Location(parent), child, properties));
        return nextGraph;
    }

    /**
     * Begin the request to create a node under the existing parent node at the supplied location. Use this method if you are
     * creating a node when you have the {@link Location} of a parent from a previous request.
     * <p>
     * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the <code>node(...)</code>
     * method is called on the returned object
     * </p>
     * 
     * @param parent the location of the parent
     * @return the object used to start creating a node
     */
    public CreateNode<Conjunction<Graph>> createUnder( final Location parent ) {
        final NameFactory nameFactory = getContext().getValueFactories().getNameFactory();
        CheckArg.isNotNull(parent, "parent");
        return new CreateNode<Conjunction<Graph>>() {
            @SuppressWarnings( "synthetic-access" )
            public Conjunction<Graph> node( String name,
                                            Property... properties ) {
                Name child = nameFactory.create(name);
                requestQueue.submit(new CreateNodeRequest(parent, child, properties));
                return nextGraph;
            }

            @SuppressWarnings( "synthetic-access" )
            public Conjunction<Graph> node( String name,
                                            Iterator<Property> properties ) {
                Name child = nameFactory.create(name);
                requestQueue.submit(new CreateNodeRequest(parent, child, properties));
                return nextGraph;
            }

            @SuppressWarnings( "synthetic-access" )
            public Conjunction<Graph> node( String name,
                                            Iterable<Property> properties ) {
                Name child = nameFactory.create(name);
                requestQueue.submit(new CreateNodeRequest(parent, child, properties));
                return nextGraph;
            }
        };
    }

    /**
     * Set the properties on a node.
     * 
     * @param properties the properties to set
     * @return the remove request object that should be used to specify the node on which the properties are to be set.
     */
    public On<Conjunction<Graph>> set( final Property... properties ) {
        return new On<Conjunction<Graph>>() {
            @SuppressWarnings( "synthetic-access" )
            public Conjunction<Graph> on( Location location ) {
                UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, properties);
                queue().submit(request);
                return nextGraph;
            }

            public Conjunction<Graph> on( String path ) {
                return on(new Location(createPath(path)));
            }

            public Conjunction<Graph> on( Path path ) {
                return on(new Location(path));
            }

            public Conjunction<Graph> on( Property idProperty ) {
                return on(new Location(idProperty));
            }

            public Conjunction<Graph> on( Property firstIdProperty,
                                          Property... additionalIdProperties ) {
                return on(new Location(firstIdProperty, additionalIdProperties));
            }

            public Conjunction<Graph> on( UUID uuid ) {
                return on(new Location(uuid));
            }
        };
    }

    /**
     * Set a property on a node, starting with the name. The interface returned from this method should be used to specify the
     * value(s) and the location of the node onto which the property should be set.
     * 
     * @param propertyName the property name
     * @return the interface used to specify the values
     */
    public SetValues<Conjunction<Graph>> set( String propertyName ) {
        Name name = getContext().getValueFactories().getNameFactory().create(propertyName);
        return set(name);
    }

    /**
     * Set a property on a node, starting with the name. The interface returned from this method should be used to specify the
     * value(s) and the location of the node onto which the property should be set.
     * 
     * @param propertyName the property name
     * @return the interface used to specify the values
     */
    public SetValues<Conjunction<Graph>> set( final Name propertyName ) {
        return new SetValues<Conjunction<Graph>>() {
            @SuppressWarnings( "synthetic-access" )
            public SetValuesTo<Conjunction<Graph>> on( final Location location ) {
                return new SetValuesTo<Conjunction<Graph>>() {
                    public Conjunction<Graph> to( Node value ) {
                        return to(value.getLocation());
                    }

                    public Conjunction<Graph> to( Location value ) {
                        Reference ref = (Reference)convertReferenceValue(value);
                        Property property = getContext().getPropertyFactory().create(propertyName, ref);
                        UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                        queue().submit(request);
                        return nextGraph;
                    }

                    protected Conjunction<Graph> toValue( Object value ) {
                        Property property = getContext().getPropertyFactory().create(propertyName, value);
                        UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                        queue().submit(request);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( String value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( int value ) {
                        return toValue(Integer.valueOf(value));
                    }

                    public Conjunction<Graph> to( long value ) {
                        return toValue(Long.valueOf(value));
                    }

                    public Conjunction<Graph> to( boolean value ) {
                        return toValue(Boolean.valueOf(value));
                    }

                    public Conjunction<Graph> to( float value ) {
                        return toValue(Float.valueOf(value));
                    }

                    public Conjunction<Graph> to( double value ) {
                        return toValue(Double.valueOf(value));
                    }

                    public Conjunction<Graph> to( BigDecimal value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Calendar value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Date value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( DateTime value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Name value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Path value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Reference value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( URI value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( UUID value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Binary value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( byte[] value ) {
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( InputStream stream,
                                                  long approximateLength ) {
                        Binary value = getContext().getValueFactories().getBinaryFactory().create(stream, approximateLength);
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Reader reader,
                                                  long approximateLength ) {
                        Binary value = getContext().getValueFactories().getBinaryFactory().create(reader, approximateLength);
                        return toValue(value);
                    }

                    public Conjunction<Graph> to( Object value ) {
                        value = convertReferenceValue(value);
                        Property property = getContext().getPropertyFactory().create(propertyName, value);
                        UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                        queue().submit(request);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( Object firstValue,
                                                  Object... otherValues ) {
                        firstValue = convertReferenceValue(firstValue);
                        for (int i = 0, len = otherValues.length; i != len; ++i) {
                            otherValues[i] = convertReferenceValue(otherValues[i]);
                        }
                        Property property = getContext().getPropertyFactory().create(propertyName, firstValue, otherValues);
                        UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                        queue().submit(request);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( Iterable<?> values ) {
                        List<Object> valueList = new LinkedList<Object>();
                        for (Object value : values) {
                            value = convertReferenceValue(value);
                            valueList.add(value);
                        }
                        Property property = getContext().getPropertyFactory().create(propertyName, valueList);
                        UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                        queue().submit(request);
                        return nextGraph;
                    }

                    public Conjunction<Graph> to( Iterator<?> values ) {
                        List<Object> valueList = new LinkedList<Object>();
                        while (values.hasNext()) {
                            Object value = values.next();
                            valueList.add(value);
                        }
                        Property property = getContext().getPropertyFactory().create(propertyName, valueList);
                        UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                        queue().submit(request);
                        return nextGraph;
                    }
                };
            }

            public SetValuesTo<Conjunction<Graph>> on( String path ) {
                return on(new Location(createPath(path)));
            }

            public SetValuesTo<Conjunction<Graph>> on( Path path ) {
                return on(new Location(path));
            }

            public SetValuesTo<Conjunction<Graph>> on( Property idProperty ) {
                return on(new Location(idProperty));
            }

            public SetValuesTo<Conjunction<Graph>> on( Property firstIdProperty,
                                                       Property... additionalIdProperties ) {
                return on(new Location(firstIdProperty, additionalIdProperties));
            }

            public SetValuesTo<Conjunction<Graph>> on( UUID uuid ) {
                return on(new Location(uuid));
            }

            public On<Conjunction<Graph>> to( Node node ) {
                Reference value = (Reference)convertReferenceValue(node);
                return set(getContext().getPropertyFactory().create(propertyName, value));
            }

            public On<Conjunction<Graph>> to( Location location ) {
                Reference value = (Reference)convertReferenceValue(location);
                return set(getContext().getPropertyFactory().create(propertyName, value));
            }

            protected On<Conjunction<Graph>> toValue( Object value ) {
                return set(getContext().getPropertyFactory().create(propertyName, value));
            }

            public On<Conjunction<Graph>> to( String value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( int value ) {
                return toValue(Integer.valueOf(value));
            }

            public On<Conjunction<Graph>> to( long value ) {
                return toValue(Long.valueOf(value));
            }

            public On<Conjunction<Graph>> to( boolean value ) {
                return toValue(Boolean.valueOf(value));
            }

            public On<Conjunction<Graph>> to( float value ) {
                return toValue(Float.valueOf(value));
            }

            public On<Conjunction<Graph>> to( double value ) {
                return toValue(Double.valueOf(value));
            }

            public On<Conjunction<Graph>> to( BigDecimal value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Calendar value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Date value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( DateTime value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Name value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Path value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Reference value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( URI value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( UUID value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Binary value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( byte[] value ) {
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( InputStream stream,
                                              long approximateLength ) {
                Binary value = getContext().getValueFactories().getBinaryFactory().create(stream, approximateLength);
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Reader reader,
                                              long approximateLength ) {
                Binary value = getContext().getValueFactories().getBinaryFactory().create(reader, approximateLength);
                return toValue(value);
            }

            public On<Conjunction<Graph>> to( Object value ) {
                value = convertReferenceValue(value);
                return set(getContext().getPropertyFactory().create(propertyName, value));
            }

            public On<Conjunction<Graph>> to( Object firstValue,
                                              Object... otherValues ) {
                firstValue = convertReferenceValue(firstValue);
                for (int i = 0, len = otherValues.length; i != len; ++i) {
                    otherValues[i] = convertReferenceValue(otherValues[i]);
                }
                return set(getContext().getPropertyFactory().create(propertyName, firstValue, otherValues));
            }

            public On<Conjunction<Graph>> to( Iterable<?> values ) {
                List<Object> valueList = new LinkedList<Object>();
                for (Object value : values) {
                    value = convertReferenceValue(value);
                    valueList.add(value);
                }
                return set(getContext().getPropertyFactory().create(propertyName, valueList));
            }

            public On<Conjunction<Graph>> to( Iterator<?> values ) {
                List<Object> valueList = new LinkedList<Object>();
                while (values.hasNext()) {
                    Object value = values.next();
                    valueList.add(value);
                }
                return set(getContext().getPropertyFactory().create(propertyName, valueList));
            }
        };
    }

    /**
     * Remove properties from the node at the given location.
     * 
     * @param propertyNames the names of the properties to be removed
     * @return the remove request object that should be used to specify the node from which the properties are to be removed.
     */
    public On<Conjunction<Graph>> remove( final Name... propertyNames ) {
        return new On<Conjunction<Graph>>() {
            @SuppressWarnings( "synthetic-access" )
            public Conjunction<Graph> on( Location location ) {
                RemovePropertiesRequest request = new RemovePropertiesRequest(location, propertyNames);
                queue().submit(request);
                return nextGraph;
            }

            public Conjunction<Graph> on( String path ) {
                return on(new Location(createPath(path)));
            }

            public Conjunction<Graph> on( Path path ) {
                return on(new Location(path));
            }

            public Conjunction<Graph> on( Property idProperty ) {
                return on(new Location(idProperty));
            }

            public Conjunction<Graph> on( Property firstIdProperty,
                                          Property... additionalIdProperties ) {
                return on(new Location(firstIdProperty, additionalIdProperties));
            }

            public Conjunction<Graph> on( UUID uuid ) {
                return on(new Location(uuid));
            }
        };
    }

    /**
     * Remove properties from the node at the given location.
     * 
     * @param propertyNames the names of the properties to be removed
     * @return the remove request object that should be used to specify the node from which the properties are to be removed.
     */
    public On<Conjunction<Graph>> remove( String... propertyNames ) {
        NameFactory nameFactory = getContext().getValueFactories().getNameFactory();
        final List<Name> names = new LinkedList<Name>();
        for (String propertyName : propertyNames) {
            names.add(nameFactory.create(propertyName));
        }
        return new On<Conjunction<Graph>>() {
            @SuppressWarnings( "synthetic-access" )
            public Conjunction<Graph> on( Location location ) {
                RemovePropertiesRequest request = new RemovePropertiesRequest(location, names);
                queue().submit(request);
                return nextGraph;
            }

            public Conjunction<Graph> on( String path ) {
                return on(new Location(createPath(path)));
            }

            public Conjunction<Graph> on( Path path ) {
                return on(new Location(path));
            }

            public Conjunction<Graph> on( Property idProperty ) {
                return on(new Location(idProperty));
            }

            public Conjunction<Graph> on( Property firstIdProperty,
                                          Property... additionalIdProperties ) {
                return on(new Location(firstIdProperty, additionalIdProperties));
            }

            public Conjunction<Graph> on( UUID uuid ) {
                return on(new Location(uuid));
            }
        };
    }

    /**
     * Request that the properties be read on the node defined via the <code>on(...)</code> method on the returned {@link On}
     * object. Once the location is specified, the {@link Collection collection of properties} are read and then returned.
     * 
     * @return the object that is used to specified the node whose properties are to be read, and which will return the properties
     */
    public On<Collection<Property>> getProperties() {
        return new On<Collection<Property>>() {
            public Collection<Property> on( Location location ) {
                ReadAllPropertiesRequest request = new ReadAllPropertiesRequest(location);
                queue().submit(request);
                return request.getProperties();
            }

            public Collection<Property> on( String path ) {
                return on(new Location(createPath(path)));
            }

            public Collection<Property> on( Path path ) {
                return on(new Location(path));
            }

            public Collection<Property> on( Property idProperty ) {
                return on(new Location(idProperty));
            }

            public Collection<Property> on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                return on(new Location(firstIdProperty, additionalIdProperties));
            }

            public Collection<Property> on( UUID uuid ) {
                return on(new Location(uuid));
            }
        };
    }

    /**
     * Request that the properties be read on the node defined via the <code>on(...)</code> method on the returned {@link On}
     * object. Once the location is specified, the {@link Map map of properties} are read and then returned.
     * 
     * @return the object that is used to specified the node whose properties are to be read, and which will return the properties
     *         as a map keyed by their name
     */
    public On<Map<Name, Property>> getPropertiesByName() {
        return new On<Map<Name, Property>>() {
            public Map<Name, Property> on( Location location ) {
                ReadAllPropertiesRequest request = new ReadAllPropertiesRequest(location);
                queue().submit(request);
                return request.getPropertiesByName();
            }

            public Map<Name, Property> on( String path ) {
                return on(new Location(createPath(path)));
            }

            public Map<Name, Property> on( Path path ) {
                return on(new Location(path));
            }

            public Map<Name, Property> on( Property idProperty ) {
                return on(new Location(idProperty));
            }

            public Map<Name, Property> on( Property firstIdProperty,
                                           Property... additionalIdProperties ) {
                return on(new Location(firstIdProperty, additionalIdProperties));
            }

            public Map<Name, Property> on( UUID uuid ) {
                return on(new Location(uuid));
            }
        };
    }

    /**
     * Request that the children be read on the node defined via the <code>of(...)</code> method on the returned {@link Of}
     * object. The returned object is used to supply the remaining information, including either the {@link Children#of(Location)
     * location of the parent}, or that a subset of the children should be retrieved {@link Children#inBlockOf(int) in a block}.
     * 
     * @return the object that is used to specify the remaining inputs for the request, and which will return the children
     */
    public Children<List<Location>> getChildren() {
        return new Children<List<Location>>() {
            public List<Location> of( String path ) {
                return of(new Location(createPath(path)));
            }

            public List<Location> of( Path path ) {
                return of(new Location(path));
            }

            public List<Location> of( Property idProperty ) {
                return of(new Location(idProperty));
            }

            public List<Location> of( Property firstIdProperty,
                                      Property... additionalIdProperties ) {
                return of(new Location(firstIdProperty, additionalIdProperties));
            }

            public List<Location> of( UUID uuid ) {
                return of(new Location(uuid));
            }

            public List<Location> of( Location at ) {
                ReadAllChildrenRequest request = new ReadAllChildrenRequest(at);
                queue().submit(request);
                return request.getChildren();
            }

            public BlockOfChildren<List<Location>> inBlockOf( final int blockSize ) {
                return new BlockOfChildren<List<Location>>() {
                    public Under<List<Location>> startingAt( final int startingIndex ) {
                        return new Under<List<Location>>() {
                            public List<Location> under( String path ) {
                                return under(new Location(createPath(path)));
                            }

                            public List<Location> under( Path path ) {
                                return under(new Location(path));
                            }

                            public List<Location> under( Property idProperty ) {
                                return under(new Location(idProperty));
                            }

                            public List<Location> under( Property firstIdProperty,
                                                         Property... additionalIdProperties ) {
                                return under(new Location(firstIdProperty, additionalIdProperties));
                            }

                            public List<Location> under( UUID uuid ) {
                                return under(new Location(uuid));
                            }

                            public List<Location> under( Location at ) {
                                ReadBlockOfChildrenRequest request = new ReadBlockOfChildrenRequest(at, startingIndex, blockSize);
                                queue().submit(request);
                                return request.getChildren();
                            }
                        };
                    }

                    public List<Location> startingAfter( final Location previousSibling ) {
                        ReadNextBlockOfChildrenRequest request = new ReadNextBlockOfChildrenRequest(previousSibling, blockSize);
                        queue().submit(request);
                        return request.getChildren();
                    }

                    public List<Location> startingAfter( String pathOfPreviousSibling ) {
                        return startingAfter(new Location(createPath(pathOfPreviousSibling)));
                    }

                    public List<Location> startingAfter( Path pathOfPreviousSibling ) {
                        return startingAfter(new Location(pathOfPreviousSibling));
                    }

                    public List<Location> startingAfter( UUID uuidOfPreviousSibling ) {
                        return startingAfter(new Location(uuidOfPreviousSibling));
                    }

                    public List<Location> startingAfter( Property idPropertyOfPreviousSibling ) {
                        return startingAfter(new Location(idPropertyOfPreviousSibling));
                    }

                    public List<Location> startingAfter( Property firstIdProperyOfPreviousSibling,
                                                         Property... additionalIdPropertiesOfPreviousSibling ) {
                        return startingAfter(new Location(firstIdProperyOfPreviousSibling,
                                                          additionalIdPropertiesOfPreviousSibling));
                    }
                };
            }
        };
    }

    /**
     * Request that the property with the given name be read on the node defined via the <code>on(...)</code> method on the
     * returned {@link On} object. Once the location is specified, the {@link Property property} is read and then returned.
     * 
     * @param name the name of the property that is to be read
     * @return the object that is used to specified the node whose property is to be read, and which will return the property
     */
    public On<Property> getProperty( final String name ) {
        Name nameObj = context.getValueFactories().getNameFactory().create(name);
        return getProperty(nameObj);
    }

    /**
     * Request that the property with the given name be read on the node defined via the <code>on(...)</code> method on the
     * returned {@link On} object. Once the location is specified, the {@link Property property} is read and then returned.
     * 
     * @param name the name of the property that is to be read
     * @return the object that is used to specified the node whose property is to be read, and which will return the property
     */
    public On<Property> getProperty( final Name name ) {
        return new On<Property>() {
            public Property on( String path ) {
                return on(new Location(createPath(path)));
            }

            public Property on( Path path ) {
                return on(new Location(path));
            }

            public Property on( Property idProperty ) {
                return on(new Location(idProperty));
            }

            public Property on( Property firstIdProperty,
                                Property... additionalIdProperties ) {
                return on(new Location(firstIdProperty, additionalIdProperties));
            }

            public Property on( UUID uuid ) {
                return on(new Location(uuid));
            }

            public Property on( Location at ) {
                ReadPropertyRequest request = new ReadPropertyRequest(at, name);
                queue().submit(request);
                return request.getProperty();
            }
        };
    }

    /**
     * Request to read the node with the supplied UUID.
     * 
     * @param uuid the UUID of the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( UUID uuid ) {
        return getNodeAt(new Location(uuid));
    }

    /**
     * Request to read the node at the supplied location.
     * 
     * @param location the location of the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( Location location ) {
        ReadNodeRequest request = new ReadNodeRequest(location);
        this.requestQueue.submit(request);
        return new GraphNode(request);
    }

    /**
     * Request to read the node at the supplied path.
     * 
     * @param path the path of the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( String path ) {
        return getNodeAt(new Location(createPath(path)));
    }

    /**
     * Request to read the node at the supplied path.
     * 
     * @param path the path of the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( Path path ) {
        return getNodeAt(new Location(path));
    }

    /**
     * Request to read the node with the supplied unique identifier property.
     * 
     * @param idProperty the identification property that is unique to the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( Property idProperty ) {
        return getNodeAt(new Location(idProperty));
    }

    /**
     * Request to read the node with the supplied unique identifier properties.
     * 
     * @param firstIdProperty the first of the identification properties that uniquely identify the node that is to be read
     * @param additionalIdProperties the remaining identification properties that uniquely identify the node that is to be read
     * @return the node that is read from the repository
     */
    public Node getNodeAt( Property firstIdProperty,
                           Property... additionalIdProperties ) {
        return getNodeAt(new Location(firstIdProperty, additionalIdProperties));
    }

    /**
     * Request to read the node given by the supplied reference value.
     * 
     * @param reference the reference property value that is to be resolved into a node
     * @return the node that is read from the repository
     * @throws ValueFormatException if the supplied reference could not be converted to an identifier property value
     */
    public Node resolve( Reference reference ) {
        CheckArg.isNotNull(reference, "reference");
        UUID uuid = context.getValueFactories().getUuidFactory().create(reference);
        return getNodeAt(uuid);
    }

    /**
     * Request to read a subgraph of the specified depth, rooted at a location that will be specified via <code>at(...)</code> in
     * the resulting {@link At} object. All properties and children of every node in the subgraph will be read and returned in the
     * {@link Subgraph} object returned from the <code>at(...)</code> methods.
     * 
     * @param depth the maximum depth of the subgraph that should be read
     * @return the component that should be used to specify the location of the node that is the top of the subgraph, and which
     *         will return the {@link Subgraph} containing the results
     */
    public At<Subgraph> getSubgraphOfDepth( final int depth ) {
        return new At<Subgraph>() {
            public Subgraph at( Location location ) {
                ReadBranchRequest request = new ReadBranchRequest(location, depth);
                queue().submit(request);
                return new SubgraphResults(request);
            }

            public Subgraph at( String path ) {
                return at(new Location(createPath(path)));
            }

            public Subgraph at( Path path ) {
                return at(new Location(path));
            }

            public Subgraph at( UUID uuid ) {
                return at(new Location(uuid));
            }

            public Subgraph at( Property idProperty ) {
                return at(new Location(idProperty));
            }

            public Subgraph at( Property firstIdProperty,
                                Property... additionalIdProperties ) {
                return at(new Location(firstIdProperty, additionalIdProperties));
            }
        };
    }

    /**
     * Import the content from the XML file at the supplied URI, specifying via the returned {@link ImportInto object} where the
     * content is to be imported.
     * 
     * @param uri the URI where the importer can read the content that is to be imported
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     */
    public ImportInto<Conjunction<Graph>> importXmlFrom( final URI uri ) {
        return new ImportInto<Conjunction<Graph>>() {
            private boolean skipRootElement = false;

            public ImportInto<Conjunction<Graph>> skippingRootElement( boolean skipRootElement ) {
                this.skipRootElement = skipRootElement;
                return this;
            }

            public Conjunction<Graph> into( String path ) throws IOException, SAXException {
                return into(new Location(createPath(path)));
            }

            public Conjunction<Graph> into( Path path ) throws IOException, SAXException {
                return into(new Location(path));
            }

            public Conjunction<Graph> into( Property idProperty ) throws IOException, SAXException {
                return into(new Location(idProperty));
            }

            public Conjunction<Graph> into( Property firstIdProperty,
                                            Property... additionalIdProperties ) throws IOException, SAXException {
                return into(new Location(firstIdProperty, additionalIdProperties));
            }

            public Conjunction<Graph> into( UUID uuid ) throws IOException, SAXException {
                return into(new Location(uuid));
            }

            @SuppressWarnings( "synthetic-access" )
            public Conjunction<Graph> into( Location at ) throws IOException, SAXException {
                GraphImporter importer = new GraphImporter(Graph.this);
                importer.importXml(uri, at, skipRootElement).execute(); // 'importXml' creates and uses a new batch
                return Graph.this.nextGraph;
            }
        };
    }

    /**
     * Import the content from the XML file at the supplied file location, specifying via the returned {@link ImportInto object}
     * where the content is to be imported.
     * 
     * @param pathToFile the path to the XML file that should be imported.
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     */
    public ImportInto<Conjunction<Graph>> importXmlFrom( String pathToFile ) {
        CheckArg.isNotNull(pathToFile, "pathToFile");
        return importXmlFrom(new File(pathToFile).toURI());
    }

    /**
     * Import the content from the XML file at the supplied file, specifying via the returned {@link ImportInto object} where the
     * content is to be imported.
     * 
     * @param file the XML file that should be imported.
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     */
    public ImportInto<Conjunction<Graph>> importXmlFrom( File file ) {
        CheckArg.isNotNull(file, "file");
        return importXmlFrom(file.toURI());
    }

    /*package*/Path createPath( String path ) {
        return getContext().getValueFactories().getPathFactory().create(path);
    }

    /*package*/void execute( Request request ) {
        RepositoryConnection connection = Graph.this.getConnectionFactory().createConnection(getSourceName());
        if (connection == null) {
            throw new RepositorySourceException(GraphI18n.unableToFindRepositorySourceWithName.text(getSourceName()));
        }
        try {
            connection.execute(Graph.this.getContext(), request);
        } finally {
            connection.close();
        }
        if (request.hasError()) {
            Throwable error = request.getError();
            if (error instanceof RuntimeException) throw (RuntimeException)error;
            throw new RepositorySourceException(getSourceName(), error);
        }
    }

    /*package*/List<Segment> getSegments( List<Location> locations ) {
        List<Segment> segments = new ArrayList<Segment>(locations.size());
        for (Location location : locations) {
            segments.add(location.getPath().getLastSegment());
        }
        return segments;
    }

    /**
     * Begin a batch of requests to perform various operations. Use this approach when multiple operations are to be built and
     * then executed with one submission to the underlying {@link #getSourceName() repository source}. The {@link Results results}
     * are not available until the {@link Batch#execute()} method is invoked.
     * 
     * @return the batch object used to build and accumulate multiple requests and to submit them all for processing at once.
     * @see Batch#execute()
     * @see Results
     */
    public Batch batch() {
        return new Batch();
    }

    /**
     * Interface for creating multiple requests to perform various operations. Note that all the requests are accumulated until
     * the {@link #execute()} method is called. The results of all the operations are then available in the {@link Results} object
     * returned by the {@link #execute()}.
     * 
     * @author Randall Hauch
     */
    @Immutable
    public final class Batch implements Executable {
        protected final CompositingRequestQueue requestQueue = new CompositingRequestQueue();
        protected final BatchConjunction nextRequests;
        protected boolean executed = false;

        /*package*/Batch() {
            this.nextRequests = new BatchConjunction() {
                public Batch and() {
                    return Batch.this;
                }

                public Results execute() {
                    executed = true;
                    return Batch.this.requestQueue.execute();
                }
            };
        }

        /**
         * Obtain the graph that this batch uses.
         * 
         * @return the graph; never null
         */
        public Graph getGraph() {
            return Graph.this;
        }

        protected final void assertNotExecuted() {
            if (executed) {
                throw new IllegalStateException(GraphI18n.unableToAddMoreRequestsToAlreadyExecutedBatch.text());
            }
        }

        /**
         * Begin the request to move the specified node into a parent node at a different location, which is specified via the
         * <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Node from ) {
            assertNotExecuted();
            return new MoveAction<BatchConjunction>(this.nextRequests, this.requestQueue, from.getLocation());
        }

        /**
         * Begin the request to move a node at the specified location into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the location of the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Location from ) {
            assertNotExecuted();
            return new MoveAction<BatchConjunction>(this.nextRequests, this.requestQueue, from);
        }

        /**
         * Begin the request to move a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param fromPath the path to the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( String fromPath ) {
            assertNotExecuted();
            return new MoveAction<BatchConjunction>(this.nextRequests, this.requestQueue, new Location(createPath(fromPath)));
        }

        /**
         * Begin the request to move a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the path to the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Path from ) {
            assertNotExecuted();
            return new MoveAction<BatchConjunction>(this.nextRequests, this.requestQueue, new Location(from));
        }

        /**
         * Begin the request to move a node with the specified unique identifier into a parent node at a different location, which
         * is specified via the <code>into(...)</code> method on the returned {@link Move} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the UUID of the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( UUID from ) {
            assertNotExecuted();
            return new MoveAction<BatchConjunction>(this.nextRequests, this.requestQueue, new Location(from));
        }

        /**
         * Begin the request to move a node with the specified unique identification property into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Move} object. The
         * identification property should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperty the unique identification property of the node that is to be moved.
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Property idProperty ) {
            assertNotExecuted();
            return new MoveAction<BatchConjunction>(this.nextRequests, this.requestQueue, new Location(idProperty));
        }

        /**
         * Begin the request to move a node with the specified identification properties into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Move} object. The
         * identification properties should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param firstIdProperty the first identification property of the node that is to be moved
         * @param additionalIdProperties the remaining idenficiation properties of the node that is to be moved
         * @return the object that can be used to specify addition nodes to be moved or the location of the node where the node is
         *         to be moved
         */
        public Move<BatchConjunction> move( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
            assertNotExecuted();
            return new MoveAction<BatchConjunction>(this.nextRequests, this.requestQueue, new Location(firstIdProperty,
                                                                                                       additionalIdProperties));
        }

        /**
         * Begin the request to copy the specified node into a parent node at a different location, which is specified via the
         * <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Node from ) {
            assertNotExecuted();
            return new CopyAction<BatchConjunction>(nextRequests, this.requestQueue, from.getLocation());
        }

        /**
         * Begin the request to copy a node at the specified location into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the location of the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Location from ) {
            assertNotExecuted();
            return new CopyAction<BatchConjunction>(nextRequests, this.requestQueue, from);
        }

        /**
         * Begin the request to copy a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param fromPath the path to the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( String fromPath ) {
            assertNotExecuted();
            return new CopyAction<BatchConjunction>(nextRequests, this.requestQueue, new Location(createPath(fromPath)));
        }

        /**
         * Begin the request to copy a node located at the supplied path into a parent node at a different location, which is
         * specified via the <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Graph}, the copy request will be performed immediately when the
         * <code>into(...)</code> method is called.
         * </p>
         * 
         * @param from the path to the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Path from ) {
            assertNotExecuted();
            return new CopyAction<BatchConjunction>(nextRequests, this.requestQueue, new Location(from));
        }

        /**
         * Begin the request to copy a node with the specified unique identifier into a parent node at a different location, which
         * is specified via the <code>into(...)</code> method on the returned {@link Copy} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param from the UUID of the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( UUID from ) {
            assertNotExecuted();
            return new CopyAction<BatchConjunction>(nextRequests, this.requestQueue, new Location(from));
        }

        /**
         * Begin the request to copy a node with the specified unique identification property into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Copy} object. The
         * identification property should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperty the unique identification property of the node that is to be copied.
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Property idProperty ) {
            assertNotExecuted();
            return new CopyAction<BatchConjunction>(nextRequests, this.requestQueue, new Location(idProperty));
        }

        /**
         * Begin the request to copy a node with the specified identification properties into a parent node at a different
         * location, which is specified via the <code>into(...)</code> method on the returned {@link Copy} object. The
         * identification properties should uniquely identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param firstIdProperty the first identification property of the node that is to be copied
         * @param additionalIdProperties the remaining idenficiation properties of the node that is to be copied
         * @return the object that can be used to specify addition nodes to be copied or the location of the node where the node
         *         is to be copied
         */
        public Copy<BatchConjunction> copy( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
            assertNotExecuted();
            return new CopyAction<BatchConjunction>(nextRequests, this.requestQueue, new Location(firstIdProperty,
                                                                                                  additionalIdProperties));
        }

        /**
         * Request to delete the specified node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Node at ) {
            assertNotExecuted();
            this.requestQueue.submit(new DeleteBranchRequest(at.getLocation()));
            return nextRequests;
        }

        /**
         * Request to delete the node at the given location.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the location of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Location at ) {
            assertNotExecuted();
            this.requestQueue.submit(new DeleteBranchRequest(at));
            return nextRequests;
        }

        /**
         * Request to delete the node at the given path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param atPath the path of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( String atPath ) {
            assertNotExecuted();
            this.requestQueue.submit(new DeleteBranchRequest(new Location(createPath(atPath))));
            return nextRequests;
        }

        /**
         * Request to delete the node at the given path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the path of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Path at ) {
            assertNotExecuted();
            this.requestQueue.submit(new DeleteBranchRequest(new Location(at)));
            return nextRequests;
        }

        /**
         * Request to delete the node with the given UUID.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the UUID of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( UUID at ) {
            assertNotExecuted();
            this.requestQueue.submit(new DeleteBranchRequest(new Location(at)));
            return nextRequests;
        }

        /**
         * Request to delete the node with the given unique identification property.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperty the unique identifying property of the node that is to be deleted
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Property idProperty ) {
            assertNotExecuted();
            this.requestQueue.submit(new DeleteBranchRequest(new Location(idProperty)));
            return nextRequests;
        }

        /**
         * Request to delete the node with the given identification properties. The identification properties should uniquely
         * identify a single node.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param firstIdProperty the first identification property of the node that is to be copied
         * @param additionalIdProperties the remaining idenficiation properties of the node that is to be copied
         * @return an object that may be used to start another request
         */
        public BatchConjunction delete( Property firstIdProperty,
                                        Property... additionalIdProperties ) {
            assertNotExecuted();
            this.requestQueue.submit(new DeleteBranchRequest(new Location(firstIdProperty, additionalIdProperties)));
            return nextRequests;
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param atPath the path to the node that is to be created.
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( String atPath ) {
            assertNotExecuted();
            Path at = createPath(atPath);
            Path parent = at.getParent();
            Name name = at.getLastSegment().getName();
            return new CreateAction<Batch>(this, requestQueue, new Location(parent), name);
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param atPath the path to the node that is to be created.
         * @param property a property for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( String atPath,
                                     Property property ) {
            assertNotExecuted();
            Path at = createPath(atPath);
            Path parent = at.getParent();
            Name name = at.getLastSegment().getName();
            return new CreateAction<Batch>(this, requestQueue, new Location(parent), name).with(property);
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param atPath the path to the node that is to be created.
         * @param firstProperty a property for the new node
         * @param additionalProperties additional properties for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( String atPath,
                                     Property firstProperty,
                                     Property... additionalProperties ) {
            assertNotExecuted();
            Path at = createPath(atPath);
            Path parent = at.getParent();
            Name name = at.getLastSegment().getName();
            return new CreateAction<Batch>(this, requestQueue, new Location(parent), name).with(firstProperty,
                                                                                                additionalProperties);
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the path to the node that is to be created.
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( Path at ) {
            assertNotExecuted();
            CheckArg.isNotNull(at, "at");
            Path parent = at.getParent();
            Name name = at.getLastSegment().getName();
            return new CreateAction<Batch>(this, requestQueue, new Location(parent), name);
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the path to the node that is to be created.
         * @param properties the iterator over the properties for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( Path at,
                                     Iterable<Property> properties ) {
            assertNotExecuted();
            CheckArg.isNotNull(at, "at");
            Path parent = at.getParent();
            Name name = at.getLastSegment().getName();
            CreateAction<Batch> action = new CreateAction<Batch>(this, requestQueue, new Location(parent), name);
            for (Property property : properties) {
                action.and(property);
            }
            return action;
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the path to the node that is to be created.
         * @param property a property for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( Path at,
                                     Property property ) {
            assertNotExecuted();
            CheckArg.isNotNull(at, "at");
            Path parent = at.getParent();
            Name name = at.getLastSegment().getName();
            return new CreateAction<Batch>(this, requestQueue, new Location(parent), name).with(property);
        }

        /**
         * Begin the request to create a node located at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param at the path to the node that is to be created.
         * @param firstProperty a property for the new node
         * @param additionalProperties additional properties for the new node
         * @return the object that can be used to specify addition properties for the new node to be copied or the location of the
         *         node where the node is to be created
         */
        public Create<Batch> create( Path at,
                                     Property firstProperty,
                                     Property... additionalProperties ) {
            assertNotExecuted();
            CheckArg.isNotNull(at, "at");
            Path parent = at.getParent();
            Name name = at.getLastSegment().getName();
            return new CreateAction<Batch>(this, requestQueue, new Location(parent), name).with(firstProperty,
                                                                                                additionalProperties);
        }

        /**
         * Begin the request to create a node under the existing parent node at the supplied location. This request is submitted
         * to the repository after the returned components are completed.
         * 
         * @param parent the location of the parent
         * @return the object used to start creating a node
         */
        public CreateNodeNamed<Batch> createUnder( Location parent ) {
            CheckArg.isNotNull(parent, "parent");
            return new CreateNodeNamedAction<Batch>(this, requestQueue, parent);
        }

        /**
         * Set the properties on a node.
         * 
         * @param properties the properties to set
         * @return the interface that should be used to specify the node on which the properties are to be set.
         */
        public On<BatchConjunction> set( final Property... properties ) {
            return new On<BatchConjunction>() {
                public BatchConjunction on( Location location ) {
                    UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, properties);
                    requestQueue.submit(request);
                    return nextRequests;
                }

                public BatchConjunction on( String path ) {
                    return on(new Location(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(new Location(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(new Location(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(new Location(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(new Location(uuid));
                }
            };
        }

        /**
         * Set a property on a node, starting with the name. The interface returned from this method should be used to specify the
         * value(s) and the location of the node onto which the property should be set.
         * 
         * @param propertyName the property name
         * @return the interface used to specify the values
         */
        public SetValues<BatchConjunction> set( String propertyName ) {
            Name name = getContext().getValueFactories().getNameFactory().create(propertyName);
            return set(name);
        }

        /**
         * Set a property on a node, starting with the name. The interface returned from this method should be used to specify the
         * value(s) and the location of the node onto which the property should be set.
         * 
         * @param propertyName the property name
         * @return the interface used to specify the values
         */
        public SetValues<BatchConjunction> set( final Name propertyName ) {
            return new SetValues<BatchConjunction>() {
                public SetValuesTo<BatchConjunction> on( final Location location ) {
                    return new SetValuesTo<BatchConjunction>() {
                        public BatchConjunction to( Node value ) {
                            return to(value.getLocation());
                        }

                        public BatchConjunction to( Location value ) {
                            Reference ref = (Reference)convertReferenceValue(value);
                            Property property = getContext().getPropertyFactory().create(propertyName, ref);
                            UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                            requestQueue.submit(request);
                            return nextRequests;
                        }

                        protected BatchConjunction toValue( Object value ) {
                            Property property = getContext().getPropertyFactory().create(propertyName, value);
                            UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                            requestQueue.submit(request);
                            return nextRequests;
                        }

                        public BatchConjunction to( String value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( int value ) {
                            return toValue(Integer.valueOf(value));
                        }

                        public BatchConjunction to( long value ) {
                            return toValue(Long.valueOf(value));
                        }

                        public BatchConjunction to( boolean value ) {
                            return toValue(Boolean.valueOf(value));
                        }

                        public BatchConjunction to( float value ) {
                            return toValue(Float.valueOf(value));
                        }

                        public BatchConjunction to( double value ) {
                            return toValue(Double.valueOf(value));
                        }

                        public BatchConjunction to( BigDecimal value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Calendar value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Date value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( DateTime value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Name value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Path value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Reference value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( URI value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( UUID value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( Binary value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( byte[] value ) {
                            return toValue(value);
                        }

                        public BatchConjunction to( InputStream stream,
                                                    long approximateLength ) {
                            Binary value = getContext().getValueFactories().getBinaryFactory().create(stream, approximateLength);
                            return toValue(value);
                        }

                        public BatchConjunction to( Reader reader,
                                                    long approximateLength ) {
                            Binary value = getContext().getValueFactories().getBinaryFactory().create(reader, approximateLength);
                            return toValue(value);
                        }

                        public BatchConjunction to( Object value ) {
                            value = convertReferenceValue(value);
                            Property property = getContext().getPropertyFactory().create(propertyName, value);
                            UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                            requestQueue.submit(request);
                            return nextRequests;
                        }

                        public BatchConjunction to( Object firstValue,
                                                    Object... otherValues ) {
                            firstValue = convertReferenceValue(firstValue);
                            for (int i = 0, len = otherValues.length; i != len; ++i) {
                                otherValues[i] = convertReferenceValue(otherValues[i]);
                            }
                            Property property = getContext().getPropertyFactory().create(propertyName, firstValue, otherValues);
                            UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                            requestQueue.submit(request);
                            return nextRequests;
                        }

                        public BatchConjunction to( Iterable<?> values ) {
                            List<Object> valueList = new LinkedList<Object>();
                            for (Object value : values) {
                                value = convertReferenceValue(value);
                                valueList.add(value);
                            }
                            Property property = getContext().getPropertyFactory().create(propertyName, valueList);
                            UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                            requestQueue.submit(request);
                            return nextRequests;
                        }

                        public BatchConjunction to( Iterator<?> values ) {
                            List<Object> valueList = new LinkedList<Object>();
                            while (values.hasNext()) {
                                Object value = values.next();
                                valueList.add(value);
                            }
                            Property property = getContext().getPropertyFactory().create(propertyName, valueList);
                            UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, property);
                            requestQueue.submit(request);
                            return nextRequests;
                        }

                    };
                }

                public SetValuesTo<BatchConjunction> on( String path ) {
                    return on(new Location(createPath(path)));
                }

                public SetValuesTo<BatchConjunction> on( Path path ) {
                    return on(new Location(path));
                }

                public SetValuesTo<BatchConjunction> on( Property idProperty ) {
                    return on(new Location(idProperty));
                }

                public SetValuesTo<BatchConjunction> on( Property firstIdProperty,
                                                         Property... additionalIdProperties ) {
                    return on(new Location(firstIdProperty, additionalIdProperties));
                }

                public SetValuesTo<BatchConjunction> on( UUID uuid ) {
                    return on(new Location(uuid));
                }

                public On<BatchConjunction> to( Node value ) {
                    Object reference = convertReferenceValue(value);
                    return set(getContext().getPropertyFactory().create(propertyName, reference));
                }

                public On<BatchConjunction> to( Location value ) {
                    Object reference = convertReferenceValue(value);
                    return set(getContext().getPropertyFactory().create(propertyName, reference));
                }

                protected On<BatchConjunction> toValue( Object value ) {
                    return set(getContext().getPropertyFactory().create(propertyName, value));
                }

                public On<BatchConjunction> to( String value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( int value ) {
                    return toValue(Integer.valueOf(value));
                }

                public On<BatchConjunction> to( long value ) {
                    return toValue(Long.valueOf(value));
                }

                public On<BatchConjunction> to( boolean value ) {
                    return toValue(Boolean.valueOf(value));
                }

                public On<BatchConjunction> to( float value ) {
                    return toValue(Float.valueOf(value));
                }

                public On<BatchConjunction> to( double value ) {
                    return toValue(Double.valueOf(value));
                }

                public On<BatchConjunction> to( BigDecimal value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Calendar value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Date value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( DateTime value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Name value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Path value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Reference value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( URI value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( UUID value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( Binary value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( byte[] value ) {
                    return toValue(value);
                }

                public On<BatchConjunction> to( InputStream stream,
                                                long approximateLength ) {
                    Binary value = getContext().getValueFactories().getBinaryFactory().create(stream, approximateLength);
                    return toValue(value);
                }

                public On<BatchConjunction> to( Reader reader,
                                                long approximateLength ) {
                    Binary value = getContext().getValueFactories().getBinaryFactory().create(reader, approximateLength);
                    return toValue(value);
                }

                public On<BatchConjunction> to( Object value ) {
                    value = convertReferenceValue(value);
                    return set(getContext().getPropertyFactory().create(propertyName, value));
                }

                public On<BatchConjunction> to( Object firstValue,
                                                Object... otherValues ) {
                    firstValue = convertReferenceValue(firstValue);
                    for (int i = 0, len = otherValues.length; i != len; ++i) {
                        otherValues[i] = convertReferenceValue(otherValues[i]);
                    }
                    return set(getContext().getPropertyFactory().create(propertyName, firstValue, otherValues));
                }

                public On<BatchConjunction> to( Iterable<?> values ) {
                    List<Object> valueList = new LinkedList<Object>();
                    for (Object value : values) {
                        value = convertReferenceValue(value);
                        valueList.add(value);
                    }
                    return set(getContext().getPropertyFactory().create(propertyName, valueList));
                }

                public On<BatchConjunction> to( Iterator<?> values ) {
                    List<Object> valueList = new LinkedList<Object>();
                    while (values.hasNext()) {
                        Object value = values.next();
                        valueList.add(value);
                    }
                    return set(getContext().getPropertyFactory().create(propertyName, valueList));
                }
            };
        }

        /**
         * Remove properties from the node at the given location.
         * 
         * @param propertyNames the names of the properties to be removed
         * @return the remove request object that should be used to specify the node from which the properties are to be removed.
         */
        public On<BatchConjunction> remove( final Name... propertyNames ) {
            return new On<BatchConjunction>() {
                public BatchConjunction on( Location location ) {
                    RemovePropertiesRequest request = new RemovePropertiesRequest(location, propertyNames);
                    requestQueue.submit(request);
                    return nextRequests;
                }

                public BatchConjunction on( String path ) {
                    return on(new Location(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(new Location(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(new Location(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(new Location(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(new Location(uuid));
                }
            };
        }

        /**
         * Remove properties from the node at the given location.
         * 
         * @param propertyNames the names of the properties to be removed
         * @return the remove request object that should be used to specify the node from which the properties are to be removed.
         */
        public On<BatchConjunction> remove( String... propertyNames ) {
            NameFactory nameFactory = getContext().getValueFactories().getNameFactory();
            final List<Name> names = new LinkedList<Name>();
            for (String propertyName : propertyNames) {
                names.add(nameFactory.create(propertyName));
            }
            return new On<BatchConjunction>() {
                public BatchConjunction on( Location location ) {
                    RemovePropertiesRequest request = new RemovePropertiesRequest(location, names);
                    requestQueue.submit(request);
                    return nextRequests;
                }

                public BatchConjunction on( String path ) {
                    return on(new Location(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(new Location(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(new Location(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(new Location(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(new Location(uuid));
                }
            };
        }

        /**
         * Request to read the node with the supplied UUID.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param uuid the UUID of the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( UUID uuid ) {
            return read(new Location(uuid));
        }

        /**
         * Request to read the node at the supplied location.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param location the location of the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( Location location ) {
            assertNotExecuted();
            ReadNodeRequest request = new ReadNodeRequest(location);
            requestQueue.submit(request);
            return nextRequests;
        }

        /**
         * Request to read the node at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param path the path of the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( String path ) {
            return read(new Location(createPath(path)));
        }

        /**
         * Request to read the node at the supplied path.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param path the path of the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( Path path ) {
            return read(new Location(path));
        }

        /**
         * Request to read the node with the supplied unique identifier property.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param idProperty the identification property that is unique to the node that is to be read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( Property idProperty ) {
            return read(new Location(idProperty));
        }

        /**
         * Request to read the node with the supplied unique identifier properties.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param firstIdProperty the first of the identification properties that uniquely identify the node that is to be read
         * @param additionalIdProperties the remaining identification properties that uniquely identify the node that is to be
         *        read
         * @return the interface that can either execute the batched requests or continue to add additional requests to the batch
         */
        public BatchConjunction read( Property firstIdProperty,
                                      Property... additionalIdProperties ) {
            return read(new Location(firstIdProperty, additionalIdProperties));
        }

        /**
         * Request that the property with the given name be read on the node defined via the <code>on(...)</code> method on the
         * returned {@link On} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param propertyName the name of the property that is to be read
         * @return the object that is used to specified the node whose property is to be read
         */
        public On<BatchConjunction> readProperty( String propertyName ) {
            assertNotExecuted();
            Name name = Graph.this.getContext().getValueFactories().getNameFactory().create(propertyName);
            return readProperty(name);
        }

        /**
         * Request that the property with the given name be read on the node defined via the <code>on(...)</code> method on the
         * returned {@link On} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param name the name of the property that is to be read
         * @return the object that is used to specified the node whose property is to be read
         */
        public On<BatchConjunction> readProperty( final Name name ) {
            assertNotExecuted();
            return new On<BatchConjunction>() {
                public BatchConjunction on( String path ) {
                    return on(new Location(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(new Location(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(new Location(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(new Location(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(new Location(uuid));
                }

                public BatchConjunction on( Location at ) {
                    ReadPropertyRequest request = new ReadPropertyRequest(at, name);
                    requestQueue.submit(request);
                    return Batch.this.nextRequests;
                }
            };
        }

        /**
         * Request that the properties be read on the node defined via the <code>on(...)</code> method on the returned {@link On}
         * object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @return the object that is used to specified the node whose properties are to be read,
         */
        public On<BatchConjunction> readProperties() {
            assertNotExecuted();
            return new On<BatchConjunction>() {
                public BatchConjunction on( Location location ) {
                    ReadAllPropertiesRequest request = new ReadAllPropertiesRequest(location);
                    requestQueue.submit(request);
                    return Batch.this.nextRequests;
                }

                public BatchConjunction on( String path ) {
                    return on(new Location(createPath(path)));
                }

                public BatchConjunction on( Path path ) {
                    return on(new Location(path));
                }

                public BatchConjunction on( Property idProperty ) {
                    return on(new Location(idProperty));
                }

                public BatchConjunction on( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return on(new Location(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction on( UUID uuid ) {
                    return on(new Location(uuid));
                }
            };
        }

        /**
         * Request that the children be read on the node defined via the <code>of(...)</code> method on the returned {@link Of}
         * object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @return the object that is used to specified the node whose children are to be read
         */
        public Of<BatchConjunction> readChildren() {
            assertNotExecuted();
            return new Of<BatchConjunction>() {
                public BatchConjunction of( String path ) {
                    return of(new Location(createPath(path)));
                }

                public BatchConjunction of( Path path ) {
                    return of(new Location(path));
                }

                public BatchConjunction of( Property idProperty ) {
                    return of(new Location(idProperty));
                }

                public BatchConjunction of( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return of(new Location(firstIdProperty, additionalIdProperties));
                }

                public BatchConjunction of( UUID uuid ) {
                    return of(new Location(uuid));
                }

                public BatchConjunction of( Location at ) {
                    ReadAllChildrenRequest request = new ReadAllChildrenRequest(at);
                    requestQueue.submit(request);
                    return Batch.this.nextRequests;
                }
            };
        }

        /**
         * Request to read a subgraph of the specified depth, rooted at a location that will be specified via <code>at(...)</code>
         * in the resulting {@link At} object.
         * <p>
         * Like all other methods on the {@link Batch}, the request will be performed when the {@link #execute()} method is
         * called.
         * </p>
         * 
         * @param depth the maximum depth of the subgraph that should be read
         * @return the component that should be used to specify the location of the node that is the top of the subgraph
         */
        public At<BatchConjunction> readSubgraphOfDepth( final int depth ) {
            assertNotExecuted();
            return new At<BatchConjunction>() {
                public BatchConjunction at( Location location ) {
                    ReadBranchRequest request = new ReadBranchRequest(location, depth);
                    requestQueue.submit(request);
                    return Batch.this.nextRequests;
                }

                public BatchConjunction at( String path ) {
                    return at(new Location(createPath(path)));
                }

                public BatchConjunction at( Path path ) {
                    return at(new Location(path));
                }

                public BatchConjunction at( UUID uuid ) {
                    return at(new Location(uuid));
                }

                public BatchConjunction at( Property idProperty ) {
                    return at(new Location(idProperty));
                }

                public BatchConjunction at( Property firstIdProperty,
                                            Property... additionalIdProperties ) {
                    return at(new Location(firstIdProperty, additionalIdProperties));
                }
            };
        }

        public Results execute() {
            return this.requestQueue.execute();
        }
    }

    /**
     * Utility method for checking a property value. If the value is a {@link Node} or {@link Location}, a {@link Reference} value
     * is created (if the node/location has a UUID); otherwise, the value is returned as is.
     * 
     * @param value the property value
     * @return the property value, which may be a {@link Reference} if the input value is a Node or Location
     */
    protected Object convertReferenceValue( Object value ) {
        if (value instanceof Node) {
            Node node = (Node)value;
            UUID uuid = node.getLocation().getUuid();
            if (uuid == null) {
                String nodeString = node.getLocation().getString(getContext().getNamespaceRegistry());
                String msg = GraphI18n.unableToCreateReferenceToNodeWithoutUuid.text(nodeString);
                throw new IllegalArgumentException(msg);
            }
            return getContext().getValueFactories().getReferenceFactory().create(uuid);
        }
        if (value instanceof Location) {
            Location location = (Location)value;
            UUID uuid = location.getUuid();
            if (uuid == null) {
                String nodeString = location.getString(getContext().getNamespaceRegistry());
                String msg = GraphI18n.unableToCreateReferenceToNodeWithoutUuid.text(nodeString);
                throw new IllegalArgumentException(msg);
            }
            return getContext().getValueFactories().getReferenceFactory().create(uuid);
        }
        return value;
    }

    /**
     * A interface used to execute the accumulated {@link Batch requests}.
     * 
     * @author Randall Hauch
     */
    public interface Executable {
        /**
         * Stop accumulating the requests, submit them to the repository source, and return the results.
         * 
         * @return the results containing the requested information from the repository.
         */
        Results execute();
    }

    /**
     * A interface that can be used to finish the current request and start another.
     * 
     * @param <Next> the interface that will be used to start another request
     * @author Randall Hauch
     */
    public interface Conjunction<Next> {
        /**
         * Finish the request and prepare to start another.
         * 
         * @return the interface that can be used to start another request; never null
         */
        Next and();
    }

    /**
     * A component that defines the location into which a node should be copied or moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface Into<Next> {
        /**
         * Finish the request by specifying the new location into which the node should be copied/moved.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         */
        Next into( Location to );

        /**
         * Finish the request by specifying the new location into which the node should be copied/moved.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next into( String toPath );

        /**
         * Finish the request by specifying the new location into which the node should be copied/moved.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next into( Path to );

        /**
         * Finish the request by specifying the new location into which the node should be copied/moved.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next into( UUID to );

        /**
         * Finish the request by specifying the new location into which the node should be copied/moved.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next into( Property idProperty );

        /**
         * Finish the request by specifying the new location into which the node should be copied/moved.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next into( Property firstIdProperty,
                   Property... additionalIdProperties );
    }

    /**
     * A interface that is used to add more locations that are to be copied/moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface And<Next> {
        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param from the location of the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( Location from );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param fromPath the path of the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( String fromPath );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param from the path of the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( Path from );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param from the UUID of the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( UUID from );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param idProperty the property that uniquely identifies the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( Property idProperty );

        /**
         * Specify that another node should also be copied or moved.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        node to be copied or moved
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the node to be copied or moved
         * @return the interface for finishing the request
         */
        Next and( Property firstIdProperty,
                  Property... additionalIdProperties );
    }

    /**
     * The interface for defining additional nodes to be moved and the parent into which the node(s) are to be moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface Move<Next> extends Into<Next>, And<Move<Next>> {
    }

    /**
     * The interface for defining additional nodes to be copied and the parent into which the node(s) are to be copied. where the
     * node(s) are to be moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface Copy<Next> extends Into<Next>, And<Copy<Next>> {
    }

    /**
     * The interface for defining additional properties on a new node.
     * 
     * @param <Next> The interface that is to be returned when this create request is completed
     * @author Randall Hauch
     */
    public interface Create<Next> extends Conjunction<Next>, Executable {
        /**
         * Specify the UUID that should the new node should have. This is an alias for {@link #and(UUID)}.
         * 
         * @param uuid the UUID
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( UUID uuid );

        /**
         * Specify a property that should the new node should have. This is an alias for {@link #and(Property)}.
         * 
         * @param property the property
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( Property property );

        /**
         * Specify a property that should the new node should have. This is an alias for {@link #and(String, Object...)}.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( String propertyName,
                           Object... values );

        /**
         * Specify a property that should the new node should have. This is an alias for {@link #and(Name, Object...)}.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( Name propertyName,
                           Object... values );

        /**
         * Specify properties that should the new node should have. This is an alias for {@link #and(Property, Property...)}.
         * 
         * @param firstProperty the first property
         * @param additionalProperties the additional property
         * @return this same interface so additional properties may be added
         */
        Create<Next> with( Property firstProperty,
                           Property... additionalProperties );

        /**
         * Specify the UUID that should the new node should have.
         * 
         * @param uuid the UUID
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( UUID uuid );

        /**
         * Specify a property that should the new node should have.
         * 
         * @param property the property
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( Property property );

        /**
         * Specify a property that should the new node should have.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( String propertyName,
                          Object... values );

        /**
         * Specify a property that should the new node should have.
         * 
         * @param propertyName the name of the property
         * @param values the property values
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( Name propertyName,
                          Object... values );

        /**
         * Specify properties that should the new node should have.
         * 
         * @param firstProperty the first property
         * @param additionalProperties the additional property
         * @return this same interface so additional properties may be added
         */
        Create<Next> and( Property firstProperty,
                          Property... additionalProperties );
    }

    /**
     * The interface for defining the node upon which a request operates.
     * 
     * @param <Next> The interface that is to be returned when the request is completed
     * @author Randall Hauch
     */
    public interface On<Next> {
        /**
         * Specify the location of the node upon which the request is to operate.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         */
        Next on( Location to );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next on( String toPath );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next on( Path to );

        /**
         * Specify the UUID of the node upon which the request is to operate.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next on( UUID to );

        /**
         * Specify the unique identification property that identifies the node upon which the request is to operate.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next on( Property idProperty );

        /**
         * Specify the unique identification properties that identify the node upon which the request is to operate.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next on( Property firstIdProperty,
                 Property... additionalIdProperties );
    }

    /**
     * The interface for defining the node upon which a request operates.
     * 
     * @param <Next> The interface that is to be returned when the request is completed
     * @author Randall Hauch
     */
    public interface Of<Next> {
        /**
         * Specify the location of the node upon which the request is to operate.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         */
        Next of( Location to );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next of( String toPath );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next of( Path to );

        /**
         * Specify the UUID of the node upon which the request is to operate.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next of( UUID to );

        /**
         * Specify the unique identification property that identifies the node upon which the request is to operate.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next of( Property idProperty );

        /**
         * Specify the unique identification properties that identify the node upon which the request is to operate.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next of( Property firstIdProperty,
                 Property... additionalIdProperties );
    }

    /**
     * The interface for defining the node upon which which a request operates.
     * 
     * @param <Next> The interface that is to be returned when the request is completed
     * @author Randall Hauch
     */
    public interface At<Next> {
        /**
         * Specify the location of the node upon which the request is to operate.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         */
        Next at( Location to );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next at( String toPath );

        /**
         * Specify the path of the node upon which the request is to operate.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next at( Path to );

        /**
         * Specify the UUID of the node upon which the request is to operate.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next at( UUID to );

        /**
         * Specify the unique identification property that identifies the node upon which the request is to operate.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next at( Property idProperty );

        /**
         * Specify the unique identification properties that identify the node upon which the request is to operate.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next at( Property firstIdProperty,
                 Property... additionalIdProperties );
    }

    /**
     * A component used to supply the details for getting children of another node. If all of the children are to be obtained,
     * then the parent can be specified using one of the <code>of(...)</code> methods on this component. If, however, only some of
     * the nodes are to be returned (e.g., a "block" of children), then specify the {@link #inBlockOf(int) block size} followed by
     * the {@link BlockOfChildren block size and parent}.
     * 
     * @param <Next>
     * @author Randall Hauch
     */
    public interface Children<Next> extends Of<Next> {
        /**
         * Specify that a block of children are to be retreived, and in particular the number of children that are to be returned.
         * 
         * @param blockSize the number of children that are to be retrieved in the block; must be positive
         * @return the interface used to specify the starting point for the block and the parent
         */
        BlockOfChildren<Next> inBlockOf( int blockSize );
    }

    /**
     * A component used to specify a block of children starting either {@link #startingAt(int) at a particular index} or
     * {@link #startingAfter(Location) after a previous sibling}.
     * 
     * @param <Next>
     * @author Randall Hauch
     */
    public interface BlockOfChildren<Next> {
        /**
         * Specify the block of children is to start at the supplied index.
         * 
         * @param startingIndex the zero-based index of the first child to be returned in the block
         * @return interface used to specify the parent of the children; never null
         */
        Under<Next> startingAt( int startingIndex );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param previousSibling the location of the sibling node that is before the first node in the block
         * @return the children; never null
         */
        Next startingAfter( Location previousSibling );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param pathToPreviousSiblingName the path of the sibling node that is before the first node in the block
         * @return the children; never null
         */
        Next startingAfter( String pathToPreviousSiblingName );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param previousSibling the path of the sibling node that is before the first node in the block
         * @return the children; never null
         */
        Next startingAfter( Path previousSibling );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param previousSiblingUuid the UUID of the sibling node that is before the first node in the block
         * @return the children; never null
         */
        Next startingAfter( UUID previousSiblingUuid );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param idPropertyOfPreviousSibling the property that uniquely identifies the previous sibling
         * @return the children; never null
         */
        Next startingAfter( Property idPropertyOfPreviousSibling );

        /**
         * Specify the block of children is to start with the child immediately following the supplied node. This method is
         * typically used when a previous block of children has already been retrieved and this request is retrieving the next
         * block.
         * 
         * @param firstIdPropertyOfPreviousSibling the first property that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the previous sibling
         * @param additionalIdPropertiesOfPreviousSibling the additional properties that, with the
         *        <code>additionalIdProperties</code>, uniquely identifies the previous sibling
         * @return the children; never null
         */
        Next startingAfter( Property firstIdPropertyOfPreviousSibling,
                            Property... additionalIdPropertiesOfPreviousSibling );
    }

    /**
     * The interface for defining the node under which which a request operates.
     * 
     * @param <Next> The interface that is to be returned when the request is completed
     * @author Randall Hauch
     */
    public interface Under<Next> {
        /**
         * Specify the location of the node under which the request is to operate.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         */
        Next under( Location to );

        /**
         * Specify the path of the node under which the request is to operate.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next under( String toPath );

        /**
         * Specify the path of the node under which the request is to operate.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         */
        Next under( Path to );

        /**
         * Specify the UUID of the node under which the request is to operate.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         */
        Next under( UUID to );

        /**
         * Specify the unique identification property that identifies the node under which the request is to operate.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next under( Property idProperty );

        /**
         * Specify the unique identification properties that identify the node under which the request is to operate.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         */
        Next under( Property firstIdProperty,
                    Property... additionalIdProperties );
    }

    /**
     * A component used to set the values on a property.
     * 
     * @param <Next> the next command
     * @author Randall Hauch
     */
    public interface SetValues<Next> extends On<SetValuesTo<Next>>, SetValuesTo<On<Next>> {
    }

    /**
     * A component used to set the values on a property.
     * 
     * @param <Next>
     * @author Randall Hauch
     */
    public interface SetValuesTo<Next> {

        /**
         * Set the property value to be a reference to the given node. Note that it is an error if the Node does not have a
         * {@link Location#getUuid() UUID}.
         * 
         * @param node the node to which a reference should be set
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the value is a Node that has no {@link Location#getUuid() UUID}
         */
        Next to( Node node );

        /**
         * Set the property value to be a reference to the given location. Note that it is an error if the Location does not have
         * a {@link Location#getUuid() UUID}.
         * 
         * @param location the location to which a reference should be set
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the value is a Location that has no {@link Location#getUuid() UUID}
         */
        Next to( Location location );

        /**
         * Set the property value to the given string.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( String value );

        /**
         * Set the property value to the given integer value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( int value );

        /**
         * Set the property value to the given long value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( long value );

        /**
         * Set the property value to the given boolean value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( boolean value );

        /**
         * Set the property value to the given float value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( float value );

        /**
         * Set the property value to the given double value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( double value );

        /**
         * Set the property value to the given decimal value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( BigDecimal value );

        /**
         * Set the property value to the date given by the supplied calendar.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Calendar value );

        /**
         * Set the property value to the given date.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Date value );

        /**
         * Set the property value to the given date-time instant.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( DateTime value );

        /**
         * Set the property value to the given Name.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Name value );

        /**
         * Set the property value to the given Path.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Path value );

        /**
         * Set the property value to the given Reference. See also {@link #to(Node)}.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Reference value );

        /**
         * Set the property value to the given URI.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( URI value );

        /**
         * Set the property value to the given UUID.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( UUID value );

        /**
         * Set the property value to the given binary value.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( Binary value );

        /**
         * Set the property value to the given byte array.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         */
        Next to( byte[] value );

        /**
         * Set the property value to the given string.
         * 
         * @param stream the stream containing the content to be used for the property value
         * @param approximateLength the approximate length of the content (in bytes)
         * @return the interface for additional requests or actions
         */
        Next to( InputStream stream,
                 long approximateLength );

        /**
         * Set the property value to the given string.
         * 
         * @param reader the reader containing the content to be used for the property value
         * @param approximateLength the approximate length of the content (in bytes)
         * @return the interface for additional requests or actions
         */
        Next to( Reader reader,
                 long approximateLength );

        /**
         * Set the property value to the given object. The supplied <code>value</code> should be a valid property value, or a
         * {@link Node} (or {@link Location}) if the property value is to be a reference to that node (or location). Note that it
         * is an error if the Node (or Location) does not have a {@link Location#getUuid() UUID}.
         * 
         * @param value the property value
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the value is a Node or Location that has no {@link Location#getUuid() UUID}
         */
        Next to( Object value );

        /**
         * Set the property value to the given objects. Each of the supplied values should be a valid property value, or a
         * {@link Node} (or {@link Location}) if the property value is to be a reference to that node (or location). Note that it
         * is an error if the Node (or Location) does not have a {@link Location#getUuid() UUID}.
         * 
         * @param firstValue the first property value
         * @param otherValues the remaining property values
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the any of the values is a Node or Location that has no {@link Location#getUuid()
         *         UUID}
         */
        Next to( Object firstValue,
                 Object... otherValues );

        /**
         * Set the property value to the given object. Each of the supplied values should be a valid property value, or a
         * {@link Node} (or {@link Location}) if the property value is to be a reference to that node (or location). Note that it
         * is an error if the Node (or Location) does not have a {@link Location#getUuid() UUID}.
         * 
         * @param values the container for the property values
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the any of the values is a Node or Location that has no {@link Location#getUuid()
         *         UUID}
         */
        Next to( Iterable<?> values );

        /**
         * Set the property value to the given object. Each of the supplied values should be a valid property value, or a
         * {@link Node} (or {@link Location}) if the property value is to be a reference to that node (or location). Note that it
         * is an error if the Node (or Location) does not have a {@link Location#getUuid() UUID}.
         * 
         * @param values the iterator over the property values
         * @return the interface for additional requests or actions
         * @throws IllegalArgumentException if the any of the values is a Node or Location that has no {@link Location#getUuid()
         *         UUID}
         */
        Next to( Iterator<?> values );
    }

    /**
     * A component that defines a node that is to be created.
     * 
     * @param <Next> The interface that is to be returned to complete the create request
     * @author Randall Hauch
     */
    public interface CreateNode<Next> {
        /**
         * Specify the name of the node that is to be created.
         * 
         * @param nodeName the name of the new node
         * @param properties the properties for the new node
         * @return the next component for making additional requests.
         */
        Next node( String nodeName,
                   Property... properties );

        /**
         * Specify the name of the node that is to be created.
         * 
         * @param nodeName the name of the new node
         * @param properties the properties for the new node
         * @return the next component for making additional requests.
         */
        Next node( String nodeName,
                   Iterator<Property> properties );

        /**
         * Specify the name of the node that is to be created.
         * 
         * @param nodeName the name of the new node
         * @param properties the properties for the new node
         * @return the next component for making additional requests.
         */
        Next node( String nodeName,
                   Iterable<Property> properties );
    }

    /**
     * A component that defines a node that is to be created.
     * 
     * @param <Next> The interface that is to be returned to complete the create request
     * @author Randall Hauch
     */
    public interface CreateNodeNamed<Next> {
        /**
         * Specify the name of the node that is to be created.
         * 
         * @param nodeName the name of the new node
         * @return the interface used to complete the request
         */
        CreateAction<Next> nodeNamed( String nodeName );
    }

    /**
     * A component that defines the location into which a node should be copied or moved.
     * 
     * @param <Next> The interface that is to be returned when this request is completed
     * @author Randall Hauch
     */
    public interface ImportInto<Next> {
        /**
         * Specify whether the root element in the XML document should be skipped (that is, not be represented by a node). By
         * default, the root element is not skipped.
         * 
         * @param skip true if the root element should be skipped, or false if a node should be created for the root XML element
         * @return the interface used to specify the location where the content should be placed
         */
        ImportInto<Next> skippingRootElement( boolean skip );

        /**
         * Finish the import by specifying the new location into which the node should be copied/moved.
         * 
         * @param to the location of the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( Location to ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the new location into which the node should be copied/moved.
         * 
         * @param toPath the path of the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( String toPath ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the new location into which the node should be copied/moved.
         * 
         * @param to the path of the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( Path to ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the new location into which the node should be copied/moved.
         * 
         * @param to the UUID of the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( UUID to ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the new location into which the node should be copied/moved.
         * 
         * @param idProperty the property that uniquely identifies the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( Property idProperty ) throws IOException, SAXException;

        /**
         * Finish the import by specifying the new location into which the node should be copied/moved.
         * 
         * @param firstIdProperty the first property that, with the <code>additionalIdProperties</code>, uniquely identifies the
         *        new parent
         * @param additionalIdProperties the additional properties that, with the <code>additionalIdProperties</code>, uniquely
         *        identifies the new parent
         * @return the interface for additional requests or actions
         * @throws IOException if there is a problem reading the content being imported
         * @throws SAXException if there is a problem with the SAX Parser
         */
        Next into( Property firstIdProperty,
                   Property... additionalIdProperties ) throws IOException, SAXException;
    }

    public interface BatchConjunction extends Conjunction<Batch>, Executable {
    }

    // ----------------------------------------------------------------------------------------------------------------
    // RequestQueue and the different implementations
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * A queue to which each each {@link AbstractAction} can submit its {@link Request} objects, either in single or multiple.
     * This interface abstracts away from the {@link AbstractAction} what it is to do with its {@link Request} objects, allowing
     * the same <code>AbstractAction</code> classes to be used by the {@link Graph} and {@link Graph.Batch} components.
     * 
     * @author Randall Hauch
     */
    /*package*/interface RequestQueue extends Executable {
        Graph getGraph();

        void submit( Request request );

        void submit( List<Request> requests );
    }

    /**
     * A RequestQueue that is used by the Graph instance to immediately execute the submitted requests.
     * 
     * @author Randall Hauch
     */
    @NotThreadSafe
    /*package*/class GraphRequestQueue implements RequestQueue {
        public Graph getGraph() {
            return Graph.this;
        }

        public void submit( Request request ) {
            // Execute the request immediately ...
            Graph.this.execute(request);
        }

        public void submit( List<Request> requests ) {
            Request request = CompositeRequest.with(requests);
            // Execute the request immediately ...
            Graph.this.execute(request);
        }

        public Results execute() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A RequestQueue that is used by the {@link Graph.Batch} component to enqueue {@link Request}s until they are to be submitted
     * to the repository connections.
     * 
     * @author Randall Hauch
     */
    @NotThreadSafe
    /*package*/class CompositingRequestQueue implements RequestQueue {
        private final LinkedList<Request> requests = new LinkedList<Request>();

        public Graph getGraph() {
            return Graph.this;
        }

        public List<Request> getRequests() {
            return this.requests;
        }

        public void submit( Request request ) {
            if (!requests.isEmpty() && request instanceof UpdatePropertiesRequest) {
                // If the previous request was also an update, then maybe they can be merged ...
                Request previous = requests.getLast();
                if (previous instanceof UpdatePropertiesRequest) {
                    // They can be merged if the have the same location ...
                    UpdatePropertiesRequest next = (UpdatePropertiesRequest)request;
                    UpdatePropertiesRequest prev = (UpdatePropertiesRequest)previous;
                    if (next.on().equals(prev.on())) {
                        requests.removeLast();
                        requests.add(prev.mergeWith(next));
                    }
                }
            }
            this.requests.add(request);
        }

        public void submit( List<Request> requests ) {
            this.requests.addAll(requests);
        }

        public Results execute() {
            if (!requests.isEmpty()) {
                // Execute the requests ...
                Request request = CompositeRequest.with(requests);
                Graph.this.execute(request);
            }
            return new BatchResults(requests);
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Node Implementation
    // ----------------------------------------------------------------------------------------------------------------
    @Immutable
    protected class GraphNode implements Node {
        private final ReadNodeRequest request;

        /*package*/GraphNode( ReadNodeRequest request ) {
            this.request = request;
        }

        public Location getLocation() {
            return request.getActualLocationOfNode();
        }

        public Graph getGraph() {
            return Graph.this;
        }

        public Collection<Property> getProperties() {
            return request.getProperties();
        }

        public Property getProperty( Name name ) {
            return getPropertiesByName().get(name);
        }

        public Property getProperty( String nameStr ) {
            Name name = getContext().getValueFactories().getNameFactory().create(nameStr);
            return getPropertiesByName().get(name);
        }

        public Map<Name, Property> getPropertiesByName() {
            return request.getPropertiesByName();
        }

        public List<Location> getChildren() {
            return request.getChildren();
        }

        public boolean hasChildren() {
            return request.getChildren().size() > 0;
        }

        public List<Segment> getChildrenSegments() {
            return getSegments(getChildren());
        }

        public Iterator<Location> iterator() {
            return request.getChildren().iterator();
        }

        @Override
        public int hashCode() {
            return getLocation().hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof Node) {
                Node that = (Node)obj;
                return this.getLocation().equals(that.getLocation());
            }
            return false;
        }

        @Override
        public String toString() {
            return "Node " + getLocation().toString();
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Results implementation for the batched requests
    // ----------------------------------------------------------------------------------------------------------------
    @Immutable
    class BatchResults implements Results {
        private final Map<Path, BatchResultsNode> nodes = new HashMap<Path, BatchResultsNode>();

        /*package*/BatchResults( List<Request> requests ) {
            for (Request request : requests) {
                if (request instanceof ReadAllPropertiesRequest) {
                    ReadAllPropertiesRequest read = (ReadAllPropertiesRequest)request;
                    getOrCreateNode(read.getActualLocationOfNode()).setProperties(read.getPropertiesByName());
                } else if (request instanceof ReadPropertyRequest) {
                    ReadPropertyRequest read = (ReadPropertyRequest)request;
                    getOrCreateNode(read.getActualLocationOfNode()).addProperty(read.getProperty());
                } else if (request instanceof ReadNodeRequest) {
                    ReadNodeRequest read = (ReadNodeRequest)request;
                    BatchResultsNode node = getOrCreateNode(read.getActualLocationOfNode());
                    node.setProperties(read.getPropertiesByName());
                    node.setChildren(read.getChildren());
                } else if (request instanceof ReadBlockOfChildrenRequest) {
                    throw new IllegalStateException();
                } else if (request instanceof ReadAllChildrenRequest) {
                    ReadAllChildrenRequest read = (ReadAllChildrenRequest)request;
                    getOrCreateNode(read.getActualLocationOfNode()).setChildren(read.getChildren());
                } else if (request instanceof ReadBranchRequest) {
                    ReadBranchRequest read = (ReadBranchRequest)request;
                    for (Location location : read) {
                        BatchResultsNode node = getOrCreateNode(location);
                        node.setProperties(read.getPropertiesFor(location));
                        node.setChildren(read.getChildren(location));
                    }
                }
            }
            for (Map.Entry<Path, BatchResultsNode> entry : nodes.entrySet()) {
                entry.getValue().freeze();
            }
        }

        private BatchResultsNode getOrCreateNode( Location location ) {
            BatchResultsNode node = nodes.get(location);
            if (node == null) {
                node = new BatchResultsNode(location);
                assert location.getPath() != null;
                nodes.put(location.getPath(), node);
            }
            return node;
        }

        public Graph getGraph() {
            return Graph.this;
        }

        protected void checkIsAbsolute( Path path ) {
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException(GraphI18n.pathIsNotAbsolute.text(path));
            }
        }

        public Node getNode( String pathStr ) {
            Path path = createPath(pathStr);
            checkIsAbsolute(path);
            return nodes.get(path);
        }

        public Node getNode( Path path ) {
            CheckArg.isNotNull(path, "path");
            checkIsAbsolute(path);
            return nodes.get(path);
        }

        public Node getNode( Location location ) {
            CheckArg.isNotNull(location, "location");
            CheckArg.isNotNull(location.getPath(), "location.getPath()");
            return nodes.get(location.getPath());
        }

        public boolean includes( String path ) {
            return getNode(path) != null;
        }

        public boolean includes( Path path ) {
            return getNode(path) != null;
        }

        public boolean includes( Location location ) {
            return getNode(location) != null;
        }

        public Iterator<Node> iterator() {
            List<Path> paths = new ArrayList<Path>(nodes.keySet());
            Collections.sort(paths);
            final Iterator<Path> pathIter = paths.iterator();
            return new Iterator<Node>() {
                public boolean hasNext() {
                    return pathIter.hasNext();
                }

                public Node next() {
                    Path nextPath = pathIter.next();
                    return getNode(nextPath);
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    @Immutable
    class BatchResultsNode implements Node {
        private final Location location;
        private Map<Name, Property> properties;
        private List<Location> children;

        BatchResultsNode( Location location ) {
            this.location = location;
        }

        void addProperty( Property property ) {
            if (this.properties == null) this.properties = new HashMap<Name, Property>();
            this.properties.put(property.getName(), property);
        }

        void setProperties( Map<Name, Property> properties ) {
            this.properties = properties;
        }

        void setChildren( List<Location> children ) {
            this.children = children;
        }

        void freeze() {
            if (properties != null) properties = Collections.unmodifiableMap(properties);
            else properties = Collections.emptyMap();
            if (children != null) children = Collections.unmodifiableList(children);
            else children = Collections.emptyList();
        }

        public List<Segment> getChildrenSegments() {
            return getSegments(getChildren());
        }

        public Graph getGraph() {
            return Graph.this;
        }

        public Location getLocation() {
            return location;
        }

        public Collection<Property> getProperties() {
            return properties.values();
        }

        public Map<Name, Property> getPropertiesByName() {
            return properties;
        }

        public Property getProperty( Name name ) {
            return properties.get(name);
        }

        public Property getProperty( String nameStr ) {
            Name name = getContext().getValueFactories().getNameFactory().create(nameStr);
            return properties.get(name);
        }

        public List<Location> getChildren() {
            return children;
        }

        public boolean hasChildren() {
            return children.size() != 0;
        }

        public Iterator<Location> iterator() {
            return children.iterator();
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof Node) {
                Node that = (Node)obj;
                return this.location.equals(that.getLocation());
            }
            return false;
        }

        @Override
        public String toString() {
            return "Node " + getLocation().toString();
        }

    }

    // ----------------------------------------------------------------------------------------------------------------
    // Subgraph and SubgraphNode implementations
    // ----------------------------------------------------------------------------------------------------------------
    @Immutable
    class SubgraphResults implements Subgraph {
        private final ReadBranchRequest request;

        SubgraphResults( ReadBranchRequest request ) {
            this.request = request;
        }

        public Graph getGraph() {
            return Graph.this;
        }

        public Location getLocation() {
            return request.getActualLocationOfNode();
        }

        public Node getRoot() {
            return getNode(getLocation());
        }

        public int getMaximumDepth() {
            return request.maximumDepth();
        }

        public Iterator<Node> iterator() {
            final Iterator<Location> iter = request.iterator();
            return new Iterator<Node>() {
                public boolean hasNext() {
                    return iter.hasNext();
                }

                public Node next() {
                    return getNode(iter.next());
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public boolean includes( Path path ) {
            CheckArg.isNotNull(path, "path");
            path = getAbsolutePath(path);
            return request.includes(path);
        }

        public boolean includes( Location location ) {
            CheckArg.isNotNull(location, "location");
            return request.includes(location);
        }

        public boolean includes( String pathStr ) {
            Path path = createPath(pathStr);
            path = getAbsolutePath(path);
            return includes(path);
        }

        public Node getNode( Location location ) {
            if (!location.hasPath()) return null;
            Location actualLocation = request.getLocationFor(location.getPath());
            if (actualLocation == null) return null;
            return new SubgraphNode(actualLocation, request);
        }

        public Node getNode( Path path ) {
            path = getAbsolutePath(path);
            if (!includes(path)) return null;
            Location location = request.getLocationFor(path);
            if (location == null) return null;
            return new SubgraphNode(location, request);
        }

        public Node getNode( String pathStr ) {
            CheckArg.isNotEmpty(pathStr, "path");
            Path path = createPath(pathStr);
            path = getAbsolutePath(path);
            return getNode(path);
        }

        protected Path getAbsolutePath( Path absoluteOrRelative ) {
            Path result = absoluteOrRelative;
            if (!result.isAbsolute()) {
                result = getGraph().getContext().getValueFactories().getPathFactory().create(getLocation().getPath(), result);
                result = result.getNormalizedPath();
            }
            return result;
        }

        @Override
        public int hashCode() {
            return getLocation().hashCode();
        }

        @Override
        public String toString() {
            return "Subgraph " + getLocation().toString();
        }
    }

    protected static final List<Location> NO_CHILDREN = Collections.emptyList();

    @Immutable
    class SubgraphNode implements Node {
        private final Location location;
        private final ReadBranchRequest request;

        SubgraphNode( Location location,
                      ReadBranchRequest request ) {
            this.location = location;
            this.request = request;
        }

        public List<Location> getChildren() {
            List<Location> children = request.getChildren(location);
            if (children == null) children = NO_CHILDREN;
            return children;
        }

        public Graph getGraph() {
            return Graph.this;
        }

        public Location getLocation() {
            return location;
        }

        public Collection<Property> getProperties() {
            return getPropertiesByName().values();
        }

        public Map<Name, Property> getPropertiesByName() {
            return request.getPropertiesFor(location);
        }

        public Property getProperty( Name name ) {
            return getPropertiesByName().get(name);
        }

        public Property getProperty( String nameStr ) {
            Name name = getContext().getValueFactories().getNameFactory().create(nameStr);
            return getPropertiesByName().get(name);
        }

        public boolean hasChildren() {
            return getChildren().size() != 0;
        }

        public List<Segment> getChildrenSegments() {
            return getSegments(getChildren());
        }

        public Iterator<Location> iterator() {
            return getChildren().iterator();
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj instanceof Node) {
                Node that = (Node)obj;
                return this.location.equals(that.getLocation());
            }
            return false;
        }

        @Override
        public String toString() {
            return "Node " + getLocation().toString();
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Action Implementations
    // ----------------------------------------------------------------------------------------------------------------
    @Immutable
    static abstract class AbstractAction<T> implements Conjunction<T>, Executable {
        private final RequestQueue queue;
        private final T afterConjunction;

        /*package*/AbstractAction( T afterConjunction,
                                    RequestQueue queue ) {
            this.queue = queue;
            this.afterConjunction = afterConjunction;
        }

        /*package*/RequestQueue queue() {
            return this.queue;
        }

        /*package*/T afterConjunction() {
            return this.afterConjunction;
        }

        public T and() {
            return this.afterConjunction;
        }

        /*package*/Path createPath( String path ) {
            return queue.getGraph().getContext().getValueFactories().getPathFactory().create(path);
        }

        public Results execute() {
            return queue.execute();
        }
    }

    @NotThreadSafe
    static class MoveAction<T> extends AbstractAction<T> implements Move<T> {
        private final Locations from;

        /*package*/MoveAction( T afterConjunction,
                                RequestQueue queue,
                                Location from ) {
            super(afterConjunction, queue);
            this.from = new Locations(from);
        }

        public Move<T> and( Location from ) {
            this.from.add(from);
            return this;
        }

        public Move<T> and( String from ) {
            this.from.add(new Location(createPath(from)));
            return this;
        }

        public Move<T> and( Path from ) {
            this.from.add(new Location(from));
            return this;
        }

        public Move<T> and( Property firstFrom,
                            Property... additionalFroms ) {
            this.from.add(new Location(firstFrom, additionalFroms));
            return this;
        }

        public Move<T> and( Property from ) {
            this.from.add(new Location(from));
            return this;
        }

        public Move<T> and( UUID from ) {
            this.from.add(new Location(from));
            return this;
        }

        /**
         * Submit any requests to move the targets into the supplied parent location
         * 
         * @param into the parent location
         * @return this object, for method chaining
         */
        private T submit( Location into ) {
            if (this.from.hasNext()) {
                List<Request> requests = new LinkedList<Request>();
                Locations locations = this.from;
                while (locations.hasNext()) {
                    Location location = locations.getLocation();
                    requests.add(new MoveBranchRequest(location, into));
                    locations = locations.next();
                }
                queue().submit(requests);
            } else {
                queue().submit(new MoveBranchRequest(this.from.getLocation(), into));
            }
            return and();
        }

        public T into( Location into ) {
            return submit(into);
        }

        public T into( Path into ) {
            return submit(new Location(into));
        }

        public T into( UUID into ) {
            return submit(new Location(into));
        }

        public T into( Property firstIdProperty,
                       Property... additionalIdProperties ) {
            return submit(new Location(firstIdProperty, additionalIdProperties));
        }

        public T into( Property into ) {
            return submit(new Location(into));
        }

        public T into( String into ) {
            return submit(new Location(createPath(into)));
        }

        @Override
        public Results execute() {
            return queue().execute();
        }
    }

    @NotThreadSafe
    static class CopyAction<T> extends AbstractAction<T> implements Copy<T> {
        private final Locations from;

        /*package*/CopyAction( T afterConjunction,
                                RequestQueue queue,
                                Location from ) {
            super(afterConjunction, queue);
            this.from = new Locations(from);
        }

        public Copy<T> and( Location from ) {
            this.from.add(from);
            return this;
        }

        public Copy<T> and( String from ) {
            this.from.add(new Location(createPath(from)));
            return this;
        }

        public Copy<T> and( Path from ) {
            this.from.add(new Location(from));
            return this;
        }

        public Copy<T> and( Property firstFrom,
                            Property... additionalFroms ) {
            this.from.add(new Location(firstFrom, additionalFroms));
            return this;
        }

        public Copy<T> and( Property from ) {
            this.from.add(new Location(from));
            return this;
        }

        public Copy<T> and( UUID from ) {
            this.from.add(new Location(from));
            return this;
        }

        /**
         * Submit any requests to move the targets into the supplied parent location
         * 
         * @param into the parent location
         * @return this object, for method chaining
         */
        private T submit( Location into ) {
            if (this.from.hasNext()) {
                List<Request> requests = new LinkedList<Request>();
                Locations locations = this.from;
                while (locations.hasNext()) {
                    Location location = locations.getLocation();
                    requests.add(new CopyBranchRequest(location, into));
                    locations = locations.next();
                }
                queue().submit(requests);
            } else {
                queue().submit(new CopyBranchRequest(this.from.getLocation(), into));
            }
            return and();
        }

        public T into( Location into ) {
            return submit(into);
        }

        public T into( Path into ) {
            return submit(new Location(into));
        }

        public T into( UUID into ) {
            return submit(new Location(into));
        }

        public T into( Property firstIdProperty,
                       Property... additionalIdProperties ) {
            return submit(new Location(firstIdProperty, additionalIdProperties));
        }

        public T into( Property into ) {
            return submit(new Location(into));
        }

        public T into( String into ) {
            return submit(new Location(createPath(into)));
        }

        @Override
        public Results execute() {
            return queue().execute();
        }
    }

    @NotThreadSafe
    static class CreateAction<T> extends AbstractAction<T> implements Create<T> {
        private final Location parent;
        private final Name childName;
        private final List<Property> properties = new LinkedList<Property>();

        /*package*/CreateAction( T afterConjunction,
                                  RequestQueue queue,
                                  Location parent,
                                  Name childName ) {
            super(afterConjunction, queue);
            this.parent = parent;
            this.childName = childName;
        }

        public Create<T> and( UUID uuid ) {
            PropertyFactory factory = queue().getGraph().getContext().getPropertyFactory();
            properties.add(factory.create(DnaLexicon.UUID, uuid));
            return this;
        }

        public Create<T> and( Property property ) {
            properties.add(property);
            return this;
        }

        public Create<T> and( String name,
                              Object... values ) {
            ExecutionContext context = queue().getGraph().getContext();
            PropertyFactory factory = context.getPropertyFactory();
            NameFactory nameFactory = context.getValueFactories().getNameFactory();
            properties.add(factory.create(nameFactory.create(name), values));
            return this;
        }

        public Create<T> and( Name name,
                              Object... values ) {
            ExecutionContext context = queue().getGraph().getContext();
            PropertyFactory factory = context.getPropertyFactory();
            properties.add(factory.create(name, values));
            return this;
        }

        public Create<T> and( Property property,
                              Property... additionalProperties ) {
            properties.add(property);
            for (Property additionalProperty : additionalProperties) {
                properties.add(additionalProperty);
            }
            return this;
        }

        public Create<T> with( UUID uuid ) {
            return and(uuid);
        }

        public Create<T> with( Property property ) {
            return and(property);
        }

        public Create<T> with( Property property,
                               Property... additionalProperties ) {
            return and(property, additionalProperties);
        }

        public Create<T> with( String name,
                               Object... values ) {
            return and(name, values);
        }

        public Create<T> with( Name name,
                               Object... values ) {
            return and(name, values);
        }

        @Override
        public T and() {
            this.queue().submit(new CreateNodeRequest(parent, childName, this.properties));
            return super.and();
        }

        @Override
        public Results execute() {
            this.queue().submit(new CreateNodeRequest(parent, childName, this.properties));
            return queue().execute();
        }
    }

    @NotThreadSafe
    static class CreateNodeNamedAction<T> extends AbstractAction<T> implements CreateNodeNamed<T> {
        private final Location parent;

        /*package*/CreateNodeNamedAction( T afterConjunction,
                                           RequestQueue queue,
                                           Location parent ) {
            super(afterConjunction, queue);
            this.parent = parent;
        }

        public CreateAction<T> nodeNamed( String name ) {
            ExecutionContext context = queue().getGraph().getContext();
            NameFactory factory = context.getValueFactories().getNameFactory();
            Name nameObj = factory.create(name);
            return new CreateAction<T>(afterConjunction(), queue(), parent, nameObj);
        }
    }
}
