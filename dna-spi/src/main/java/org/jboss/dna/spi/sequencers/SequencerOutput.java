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
package org.jboss.dna.spi.sequencers;

import java.io.InputStream;
import java.util.Calendar;

/**
 * Interface for sequencers to use to generate their output.
 *
 * @author Randall Hauch
 */
public interface SequencerOutput {

	/**
	 * Set the supplied property on the supplied node.
	 * <p>
	 * The allowable values are any of the following:
	 * <ul>
	 * <li>primitives (which will be autoboxed)</li>
	 * <li>{@link String} instances</li>
	 * <li>{@link String} arrays</li>
	 * <li>byte arrays</li>
	 * <li>{@link InputStream} instances</li>
	 * <li>{@link Calendar} instances</li>
	 * <li></li>
	 * </ul>
	 * </p>
	 *
	 * @param nodePath the path to the node containing the property; may not be null
	 * @param property the name of the property to be set
	 * @param values the value(s) for the property; may be empty if any existing property is to be removed
	 */
	void setProperty( String nodePath,
	                  String property,
	                  Object... values );

	/**
	 * Set the supplied reference on the supplied node.
	 *
	 * @param nodePath the path to the node containing the property; may not be null
	 * @param property the name of the property to be set
	 * @param paths the paths to the referenced property, which may be absolute paths or relative to the sequencer output node;
	 *        may be empty if any existing property is to be removed
	 */
	void setReference( String nodePath,
	                   String property,
	                   String... paths );
}
