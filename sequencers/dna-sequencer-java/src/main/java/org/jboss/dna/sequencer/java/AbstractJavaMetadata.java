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
package org.jboss.dna.sequencer.java;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.jboss.dna.sequencer.java.metadata.ClassMetadata;
import org.jboss.dna.sequencer.java.metadata.ImportMetadata;
import org.jboss.dna.sequencer.java.metadata.ImportOnDemandMetadata;
import org.jboss.dna.sequencer.java.metadata.InterfaceMetadata;
import org.jboss.dna.sequencer.java.metadata.MarkerAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.NormalAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.PackageMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleImportMetadata;
import org.jboss.dna.sequencer.java.metadata.SingleMemberAnnotationMetadata;
import org.jboss.dna.sequencer.java.metadata.TypeMetadata;

/**
 * Abstract definition of a <tt>JavaMetadata<tt>. This class exposes some useful methods, that can
 * be used to create meta data of a compilation unit. Methods can also separately be used.
 *  
 * @author Serge Pagop
 */
public abstract class AbstractJavaMetadata {

    /**
     * Create a set of <code>ImportMetadata</code> of a compilation unit.
     * 
     * @param unit - the compilation unit.
     * @return all static import declarations from the compilation unit.
     */
    @SuppressWarnings( "unchecked" )
    protected List<ImportMetadata> createImportMetadata( CompilationUnit unit ) {
        List<ImportMetadata> metadata = new ArrayList<ImportMetadata>();
        List<ImportDeclaration> imports = unit.imports();
        if (!imports.isEmpty()) {
            for (ImportDeclaration importDeclaration : imports) {
                if (importDeclaration.isOnDemand()) {
                    // typeImportOnDemand and staticImportOnDemand
                    ImportOnDemandMetadata onDemandMetadata = new ImportOnDemandMetadata();
                    onDemandMetadata.setName(JavaMetadataUtil.getName(importDeclaration.getName()));
                    metadata.add(onDemandMetadata);
                } else {
                    // singleTypeImport and singleStaticImport
                    SingleImportMetadata singleImportMetadata = new SingleImportMetadata();
                    singleImportMetadata.setName(JavaMetadataUtil.getName(importDeclaration.getName()));
                    metadata.add(singleImportMetadata);
                }

            }
        }
        return metadata;
    }

    /**
     * Create a <code>PackageMetadata</code> of a compilation unit.
     * 
     * @param unit - the compilation unit.
     * @return the package meta data of a compilation unit.
     */
    @SuppressWarnings( "unchecked" )
    protected PackageMetadata createPackageMetadata( CompilationUnit unit ) {
        PackageMetadata packageMetadata = null;
        List<Annotation> annotations = null;
        PackageDeclaration packageDeclaration = unit.getPackage();
        if (packageDeclaration != null) {
            annotations = packageDeclaration.annotations();
            packageMetadata = new PackageMetadata();
            packageMetadata.setName(JavaMetadataUtil.getName(unit.getPackage().getName()));
            if (!annotations.isEmpty()) {
                for (Object object : annotations) {

                    if (object instanceof NormalAnnotation) {
                        NormalAnnotation normalAnnotation = (NormalAnnotation)object;
                        NormalAnnotationMetadata normalAnnotationMetadata = new NormalAnnotationMetadata();
                        normalAnnotationMetadata.setName(JavaMetadataUtil.getName(normalAnnotation.getTypeName()));
                        normalAnnotationMetadata.setNormal(Boolean.TRUE);
                        packageMetadata.getAnnotationMetada().add(normalAnnotationMetadata);
                    }
                    if (object instanceof MarkerAnnotation) {
                        MarkerAnnotation markerAnnotation = (MarkerAnnotation)object;
                        MarkerAnnotationMetadata markerAnnotationMetadata = new MarkerAnnotationMetadata();
                        markerAnnotationMetadata.setName(JavaMetadataUtil.getName(markerAnnotation.getTypeName()));
                        markerAnnotationMetadata.setMarker(Boolean.TRUE);
                        packageMetadata.getAnnotationMetada().add(markerAnnotationMetadata);
                    }
                    if (object instanceof SingleMemberAnnotation) {
                        SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation)object;
                        SingleMemberAnnotationMetadata singleMemberAnnotationMetadata = new SingleMemberAnnotationMetadata();
                        singleMemberAnnotationMetadata.setName(JavaMetadataUtil.getName(singleMemberAnnotation.getTypeName()));
                        singleMemberAnnotationMetadata.setSingle(Boolean.TRUE);
                        packageMetadata.getAnnotationMetada().add(singleMemberAnnotationMetadata);

                    }
                }
            }
        }
        return packageMetadata;
    }

    /**
     * Create a list with all top level types of a compilation unit.
     * 
     * @param unit - the compilation unit.
     * @return meta data for types in this compilation unit.
     */
    @SuppressWarnings( "unchecked" )
    protected List<TypeMetadata> createTypeMetadata( CompilationUnit unit ) {
        List<TypeMetadata> metadata = new ArrayList<TypeMetadata>();
        List<AbstractTypeDeclaration> topLevelType = unit.types();
        for (AbstractTypeDeclaration abstractTypeDeclaration : topLevelType) {

            // process TypeDeclaration (class, interface)
            if (abstractTypeDeclaration instanceof TypeDeclaration) {
                TypeDeclaration typeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
                if (typeDeclaration.isInterface()) {

                    // is an interface top level type
                    InterfaceMetadata interfaceMetadata = new InterfaceMetadata();
                    interfaceMetadata.setName(JavaMetadataUtil.getName(typeDeclaration.getName()));
                    metadata.add(interfaceMetadata);
                } else {
                    // is a class top level type
                    ClassMetadata classMetadata = new ClassMetadata();
                    classMetadata.setName(JavaMetadataUtil.getName(typeDeclaration.getName()));
                    List modifiers = typeDeclaration.modifiers();
                    for (Object object : modifiers) {
                        if (object instanceof Modifier) {
                            Modifier modifier = (Modifier)object;
                            if (modifier.isPublic()) {
                                classMetadata.getModifiers().put(TypeMetadata.PUBLIC_MODIFIER, modifier.getKeyword().toString());
                            }
                        }
                        if (object instanceof MarkerAnnotation) {
                            MarkerAnnotation marker = (MarkerAnnotation)object;
                            MarkerAnnotationMetadata markerAnnotationMetadata = new MarkerAnnotationMetadata();
                            markerAnnotationMetadata.setName(JavaMetadataUtil.getName(marker.getTypeName()));
                            classMetadata.getAnnotationMetadata().add(markerAnnotationMetadata);
                        }
                    }
                    metadata.add(classMetadata);
                }
            }

            // process EnumDeclaration
            if (abstractTypeDeclaration instanceof EnumDeclaration) {
                EnumDeclaration enumDeclaration = (EnumDeclaration)abstractTypeDeclaration;
                // TODO get infos from enum declaration and create a enum meta data object.
            }

            // process annotationTypeDeclaration
            if (abstractTypeDeclaration instanceof AnnotationTypeDeclaration) {
                AnnotationTypeDeclaration annotationTypeDeclaration = (AnnotationTypeDeclaration)abstractTypeDeclaration;
                // TODO get infos from annotation type declaration and create a annotation meta data object.
            }
        }
        return metadata;
    }
}
