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
package org.jboss.dna.services;

import net.jcip.annotations.ThreadSafe;

/**
 * Contract defining an administrative interface for controlling the running state of a service.
 * @author Randall Hauch
 */
@ThreadSafe
public interface ServiceAdministrator {

    /**
     * The available states.
     * @author Randall Hauch
     */
    public static enum State {
        STARTED,
        PAUSED,
        SHUTDOWN;
    }

    /**
     * Return the current state of this system.
     * @return the current state
     */
    public State getState();

    /**
     * Set the state of the system. This method does nothing if the desired state matches the current state.
     * @param state the desired state
     * @return this object for method chaining purposes
     * @see #setState(String)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    public ServiceAdministrator setState( State state );

    /**
     * Set the state of the system. This method does nothing if the desired state matches the current state.
     * @param state the desired state in string form
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if the specified state string is null or does not match one of the predefined
     * {@link State predefined enumerated values}
     * @see #setState(State)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    public ServiceAdministrator setState( String state );

    /**
     * Start monitoring and sequence the events. This method can be called multiple times, including after the system is
     * {@link #pause() paused}. However, once the system is {@link #shutdown() shutdown}, it cannot be started or paused.
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the system has been {@link #shutdown() shutdown}.
     * @see #pause()
     * @see #shutdown()
     * @see #isStarted()
     */
    public ServiceAdministrator start();

    /**
     * Temporarily stop monitoring and sequencing events. This method can be called multiple times, including after the system is
     * {@link #start() started}. However, once the system is {@link #shutdown() shutdown}, it cannot be started or paused.
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the system has been {@link #shutdown() shutdown}.
     * @see #start()
     * @see #shutdown()
     * @see #isPaused()
     */
    public ServiceAdministrator pause();

    /**
     * Permanently stop monitoring and sequencing events. This method can be called multiple times, but only the first call has an
     * effect. Once the system has been shutdown, it may not be {@link #start() restarted} or {@link #pause() paused}.
     * @return this object for method chaining purposes
     * @see #start()
     * @see #pause()
     * @see #isShutdown()
     */
    public ServiceAdministrator shutdown();

    /**
     * Return whether this system has been started and is currently running.
     * @return true if started and currently running, or false otherwise
     * @see #start()
     * @see #pause()
     * @see #isPaused()
     * @see #isShutdown()
     */
    public boolean isStarted();

    /**
     * Return whether this system is currently paused.
     * @return true if currently paused, or false otherwise
     * @see #pause()
     * @see #start()
     * @see #isStarted()
     * @see #isShutdown()
     */
    public boolean isPaused();

    /**
     * Return whether this system is stopped and unable to be restarted.
     * @return true if currently shutdown, or false otherwise
     * @see #shutdown()
     * @see #isPaused()
     * @see #isStarted()
     */
    public boolean isShutdown();
}
