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

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * Simple abstract implementation of the service administrator interface that can be easily subclassed by services that require an
 * administrative interface.
 * @author Randall Hauch
 */
@ThreadSafe
public abstract class AbstractServiceAdministrator implements ServiceAdministrator {

    private volatile State state;

    protected AbstractServiceAdministrator( State initialState ) {
        assert initialState != null;
        this.state = initialState;
    }

    /**
     * Return the current state of this service.
     * @return the current state
     */
    public State getState() {
        return this.state;
    }

    /**
     * Set the state of the service. This method does nothing if the desired state matches the current state.
     * @param state the desired state
     * @return this object for method chaining purposes
     * @see #setState(String)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    @GuardedBy( "this" )
    public synchronized ServiceAdministrator setState( State state ) {
        switch (state) {
            case STARTED:
                start();
                break;
            case PAUSED:
                pause();
                break;
            case SHUTDOWN:
                shutdown();
                break;
        }
        return this;
    }

    /**
     * Set the state of the service. This method does nothing if the desired state matches the current state.
     * @param state the desired state in string form
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if the specified state string is null or does not match one of the predefined
     * {@link ServiceAdministrator.State predefined enumerated values}
     * @see #setState(State)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    public ServiceAdministrator setState( String state ) {
        State newState = state == null ? null : State.valueOf(state.toUpperCase());
        if (newState == null) {
            throw new IllegalArgumentException("Invalid state parameter");
        }
        return setState(newState);
    }

    /**
     * Start monitoring and sequence the events. This method can be called multiple times, including after the service is
     * {@link #pause() paused}. However, once the service is {@link #shutdown() shutdown}, it cannot be started or paused.
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the service has been {@link #shutdown() shutdown}.
     * @see #pause()
     * @see #shutdown()
     * @see #isStarted()
     */
    public synchronized ServiceAdministrator start() {
        if (isShutdown()) throw new IllegalStateException("The " + serviceName() + " has been shutdown and may not be (re)started");
        if (this.state != State.STARTED) {
            doStart(this.state);
            this.state = State.STARTED;
        }
        return this;
    }

    /**
     * Implementation of the functionality to switch to the started state. This method is only called if the state from which the
     * service is transitioning is appropriate ({@link State#PAUSED}). This method does nothing by default, and should be
     * overridden if needed.
     * @param fromState the state from which this service is transitioning; never null
     * @throws IllegalStateException if the service is such that it cannot be transitioned from the supplied state
     */
    @GuardedBy( "this" )
    protected void doStart( State fromState ) {
    }

    /**
     * Temporarily stop monitoring and sequencing events. This method can be called multiple times, including after the service is
     * {@link #start() started}. However, once the service is {@link #shutdown() shutdown}, it cannot be started or paused.
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the service has been {@link #shutdown() shutdown}.
     * @see #start()
     * @see #shutdown()
     * @see #isPaused()
     */
    public synchronized ServiceAdministrator pause() {
        if (isShutdown()) throw new IllegalStateException("The " + serviceName() + " has been shutdown and may not be paused");
        if (this.state != State.PAUSED) {
            doPause(this.state);
            this.state = State.PAUSED;
        }
        return this;
    }

    /**
     * Implementation of the functionality to switch to the paused state. This method is only called if the state from which the
     * service is transitioning is appropriate ({@link State#STARTED}). This method does nothing by default, and should be
     * overridden if needed.
     * @param fromState the state from which this service is transitioning; never null
     * @throws IllegalStateException if the service is such that it cannot be transitioned from the supplied state
     */
    @GuardedBy( "this" )
    protected void doPause( State fromState ) {
    }

    /**
     * Permanently stop monitoring and sequencing events. This method can be called multiple times, but only the first call has an
     * effect. Once the service has been shutdown, it may not be {@link #start() restarted} or {@link #pause() paused}.
     * @return this object for method chaining purposes
     * @see #start()
     * @see #pause()
     * @see #isShutdown()
     */
    public synchronized ServiceAdministrator shutdown() {
        if (this.state != State.SHUTDOWN) {
            doShutdown(this.state);
            this.state = State.SHUTDOWN;
        }
        return this;
    }

    /**
     * Implementation of the functionality to switch to the shutdown state. This method is only called if the state from which the
     * service is transitioning is appropriate ({@link State#STARTED} or {@link State#PAUSED}). This method does nothing by
     * default, and should be overridden if needed.
     * @param fromState the state from which this service is transitioning; never null
     * @throws IllegalStateException if the service is such that it cannot be transitioned from the supplied state
     */
    @GuardedBy( "this" )
    protected void doShutdown( State fromState ) {
    }

    /**
     * Return whether this service has been started and is currently running.
     * @return true if started and currently running, or false otherwise
     * @see #start()
     * @see #pause()
     * @see #isPaused()
     * @see #isShutdown()
     */
    public boolean isStarted() {
        return this.state == State.STARTED;
    }

    /**
     * Return whether this service is currently paused.
     * @return true if currently paused, or false otherwise
     * @see #pause()
     * @see #start()
     * @see #isStarted()
     * @see #isShutdown()
     */
    public boolean isPaused() {
        return this.state == State.PAUSED;
    }

    /**
     * Return whether this service is stopped and unable to be restarted.
     * @return true if currently shutdown, or false otherwise
     * @see #shutdown()
     * @see #isPaused()
     * @see #isStarted()
     */
    public boolean isShutdown() {
        return this.state == State.SHUTDOWN;
    }

    protected abstract String serviceName();
}
