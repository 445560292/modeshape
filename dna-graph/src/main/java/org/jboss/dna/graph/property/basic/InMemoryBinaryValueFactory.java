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
package org.jboss.dna.graph.property.basic;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactory;

/**
 * The {@link ValueFactory} for in-memory {@link PropertyType#BINARY} values.
 * <p>
 * This factory does not {@link #find(byte[]) reuse} any instances.
 * </p>
 * 
 * @author Randall Hauch
 */
@Immutable
public class InMemoryBinaryValueFactory extends AbstractBinaryValueFactory {

    public InMemoryBinaryValueFactory( TextDecoder decoder,
                                       ValueFactory<String> stringValueFactory ) {
        super(decoder, stringValueFactory);
    }

    /**
     * {@inheritDoc}
     */
    public Binary create( byte[] value ) {
        return new InMemoryBinary(value);
    }

}
