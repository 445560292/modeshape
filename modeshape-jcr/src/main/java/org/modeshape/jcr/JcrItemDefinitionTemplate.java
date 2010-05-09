/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;

/**
 * ModeShape convenience implementation to support the JCR 2 NodeDefinitionTemplate and PropertyDefinitionTemplate classes.
 */
@NotThreadSafe
abstract class JcrItemDefinitionTemplate implements ItemDefinition {

    private final ExecutionContext context;
    private boolean autoCreated = false;
    private boolean mandatory = false;
    private boolean isProtected = false;
    private Name name;
    private int onParentVersion = OnParentVersionAction.COPY;

    JcrItemDefinitionTemplate( ExecutionContext context ) {
        assert context != null;

        this.context = context;
    }

    ExecutionContext getExecutionContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
     */
    public NodeType getDeclaringNodeType() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getName()
     */
    public String getName() {
        if (name == null) return null;
        return name.getString(context.getNamespaceRegistry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getOnParentVersion()
     */
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isAutoCreated()
     */
    public boolean isAutoCreated() {
        return autoCreated;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isMandatory()
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isProtected()
     */
    public boolean isProtected() {
        return isProtected;
    }

    public ExecutionContext getContext() {
        return context;
    }

    public void setAutoCreated( boolean autoCreated ) {
        this.autoCreated = autoCreated;
    }

    public void setMandatory( boolean mandatory ) {
        this.mandatory = mandatory;
    }

    public void setProtected( boolean isProtected ) {
        this.isProtected = isProtected;
    }

    public void setName( String name ) {
        CheckArg.isNotEmpty(name, "name");
        this.name = context.getValueFactories().getNameFactory().create(name);
    }

    public void setOnParentVersion( int onParentVersion ) {
        assert onParentVersion == OnParentVersionAction.ABORT || onParentVersion == OnParentVersionAction.COMPUTE
               || onParentVersion == OnParentVersionAction.COPY || onParentVersion == OnParentVersionAction.IGNORE
               || onParentVersion == OnParentVersionAction.INITIALIZE || onParentVersion == OnParentVersionAction.VERSION;
        this.onParentVersion = onParentVersion;
    }
}
