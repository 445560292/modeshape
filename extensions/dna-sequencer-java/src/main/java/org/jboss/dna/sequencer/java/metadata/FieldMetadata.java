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
package org.jboss.dna.sequencer.java.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Serge Pagop
 */
public class FieldMetadata {
    
    /** The type of the field */
    private String type;
    
    /** The variables */
    private List<Variable> variables = new ArrayList<Variable>();
    
    private List<ModifierMetadata> modifierMetadatas = new ArrayList<ModifierMetadata>();

    /**
     * @return variables
     */
    public List<Variable> getVariables() {
        return variables;
    }

    /**
     * @param variables Sets variables to the specified value.
     */
    public void setVariables( List<Variable> variables ) {
        this.variables = variables;
    }

    /**
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type Sets type to the specified value.
     */
    public void setType( String type ) {
        this.type = type;
    }

    /**
     * @return modifierMetadatas
     */
    public List<ModifierMetadata> getModifiers() {
        return modifierMetadatas;
    }

    /**
     * @param modifierMetadatas Sets modifierMetadatas to the specified value.
     */
    public void setModifiers( List<ModifierMetadata> modifierMetadatas ) {
        this.modifierMetadatas = modifierMetadatas;
    }
}
