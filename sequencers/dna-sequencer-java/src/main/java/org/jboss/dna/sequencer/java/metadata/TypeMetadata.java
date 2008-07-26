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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes meta data of a top level type.
 * 
 * @author Serge Pagop
 */
public class TypeMetadata {

    public static final int PUBLIC_MODIFIER = 0;

    /** The name. */
    private String name;

    /** All modifiers of a top level type */
    private Map<Integer, String> modifiers = new HashMap<Integer, String>();

    /** All annotations of a top level type */
    private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

    /** All fields of a top level type */
    private List<FieldMetadata> fields = new ArrayList<FieldMetadata>();

    /** All methods of a top level type */
    private List<MethodMetadata> methods = new ArrayList<MethodMetadata>();

    /**
     * Get the name.
     * 
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name.
     * 
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return annotations
     */
    public List<AnnotationMetadata> getAnnotations() {
        return annotations;
    }

    /**
     * @param annotations Sets annotations to the specified value.
     */
    public void setAnnotations( List<AnnotationMetadata> annotations ) {
        this.annotations = annotations;
    }

    /**
     * 
     * @return modifiers
     */
    public Map<Integer, String> getModifiers() {
        return modifiers;
    }

    /**
     * @param modifiers Sets modifiers to the specified value.
     */
    public void setModifiers( Map<Integer, String> modifiers ) {
        this.modifiers = modifiers;
    }

    /**
     * Gets a ordered lists of {@link FieldMetadata} from the unit.
     * 
     * @return all fields of this unit if there is one.
     */
    public List<FieldMetadata> getFields() {
        return this.fields;
    }

    /**
     * @param fields Sets fields to the specified value.
     */
    public void setFields( List<FieldMetadata> fields ) {
        this.fields = fields;
    }

    /**
     * Gets all {@link MethodMetadata} from the unit.
     * 
     * @return all methods from the units.
     */
    public List<MethodMetadata> getMethods() {
        return methods;
    }

    /**
     * @param methods Sets methods to the specified value.
     */
    public void setMethods( List<MethodMetadata> methods ) {
        this.methods = methods;
    }

}
