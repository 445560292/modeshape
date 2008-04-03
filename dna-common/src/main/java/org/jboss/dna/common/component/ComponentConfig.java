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
package org.jboss.dna.common.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.CoreI18n;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.ClassUtil;

/**
 * @author Randall Hauch
 */
@Immutable
public class ComponentConfig implements Comparable<ComponentConfig> {

    private final String name;
    private final String description;
    private final String componentClassname;
    private final List<String> classpath;
    private final long timestamp;

    /**
     * Create a component configuration.
     * @param name the name of the configuration, which is considered to be a unique identifier
     * @param description the description
     * @param classname the name of the Java class used for the component
     * @param classpath the optional classpath (defined in a way compatible with a {@link ClassLoaderFactory}
     * @throws IllegalArgumentException if the name is null, empty or blank, or if the classname is null, empty or not a valid
     * Java classname
     */
    public ComponentConfig( String name, String description, String classname, String... classpath ) {
        this(name, description, System.currentTimeMillis(), classname, classpath);
    }

    /**
     * Create a component configuration.
     * @param name the name of the configuration, which is considered to be a unique identifier
     * @param description the description
     * @param timestamp the timestamp that this component was last changed
     * @param classname the name of the Java class used for the component
     * @param classpath the optional classpath (defined in a way compatible with a {@link ClassLoaderFactory}
     * @throws IllegalArgumentException if the name is null, empty or blank, or if the classname is null, empty or not a valid
     * Java classname
     */
    public ComponentConfig( String name, String description, long timestamp, String classname, String... classpath ) {
        ArgCheck.isNotEmpty(name, "name");
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";
        this.componentClassname = classname;
        this.classpath = buildList(classpath);
        this.timestamp = timestamp;
        // Check the classname is a valid classname ...
        if (!ClassUtil.isFullyQualifiedClassname(classname)) {
            throw new IllegalArgumentException(CoreI18n.componentClassnameNotValid.text(classname, name));
        }
    }

    /* package */static List<String> buildList( String... classpathElements ) {
        List<String> classpath = null;
        if (classpathElements != null) {
            classpath = new ArrayList<String>();
            for (String classpathElement : classpathElements) {
                if (!classpath.contains(classpathElement)) classpath.add(classpathElement);
            }
            classpath = Collections.unmodifiableList(classpath);
        } else {
            classpath = Collections.emptyList(); // already immutable
        }
        return classpath;
    }

    /**
     * Get the name of this component.
     * @return the component name; never null, empty or blank
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the description for this component
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Get the fully-qualified name of the Java class used for instances of this component
     * @return the Java class name of this component; never null or empty and always a valid Java class name
     */
    public String getComponentClassname() {
        return this.componentClassname;
    }

    /**
     * Get the classpath defined in terms of strings compatible with a {@link ClassLoaderFactory}.
     * @return the classpath; never null but possibly empty
     */
    public List<String> getComponentClasspath() {
        return this.classpath;
    }

    /**
     * Get the classpath defined as an array of strings compatible with a {@link ClassLoaderFactory}.
     * @return the classpath as an array; never null but possibly empty
     */
    public String[] getComponentClasspathArray() {
        return this.classpath.toArray(new String[this.classpath.size()]);
    }

    /**
     * Get the system timestamp when this configuration object was created.
     * @return the timestamp
     */
    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( ComponentConfig that ) {
        if (that == this) return 0;
        int diff = this.getName().compareToIgnoreCase(that.getName());
        if (diff != 0) return diff;
        diff = (int)(this.getTimestamp() - that.getTimestamp());
        return diff;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ComponentConfig) {
            ComponentConfig that = (ComponentConfig)obj;
            if (!this.getClass().equals(that.getClass())) return false;
            return this.getName().equalsIgnoreCase(that.getName());
        }
        return false;
    }

    /**
     * Determine whether this component has changed with respect to the supplied component. This method basically checks all
     * attributes, whereas {@link #equals(Object) equals} only checks the {@link #getClass() type} and {@link #getName()}.
     * @param component the component to be compared with this one
     * @return true if this componet and the supplied component have some changes, or false if they are exactly equivalent
     * @throws IllegalArgumentException if the supplied component reference is null or is not the same {@link #getClass() type} as
     * this object
     */
    public boolean hasChanged( ComponentConfig component ) {
        ArgCheck.isNotNull(component, "component");
        ArgCheck.isInstanceOf(component, this.getClass(), "component");
        if (!this.getName().equalsIgnoreCase(component.getName())) return true;
        if (!this.getDescription().equals(component.getDescription())) return true;
        if (!this.getComponentClassname().equals(component.getComponentClassname())) return true;
        if (!this.getComponentClasspath().equals(component.getComponentClasspath())) return true;
        return false;
    }

}
