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
package org.jboss.dna.common.collection;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.common.i18n.I18n;

/**
 * A list of problems for some execution context. The problems will be {@link #iterator() returned} in the order in which they
 * were encountered (although this cannot be guaranteed in contexts involving multiple threads or processes).
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
public abstract class AbstractProblems implements Problems {

    protected static final List<Problem> EMPTY_PROBLEMS = Collections.emptyList();

    public void addError( I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, null, null, null));
    }

    public void addError( Throwable throwable,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, null, null, throwable));
    }

    public void addError( I18n message,
                          String resource,
                          String location,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, resource, location, null));
    }

    public void addError( Throwable throwable,
                          I18n message,
                          String resource,
                          String location,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, Problem.DEFAULT_CODE, message, params, resource, location, throwable));
    }

    public void addError( int code,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, null, null, null));
    }

    public void addError( Throwable throwable,
                          int code,
                          I18n message,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, null, null, throwable));
    }

    public void addError( int code,
                          I18n message,
                          String resource,
                          String location,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, resource, location, null));
    }

    public void addError( Throwable throwable,
                          int code,
                          I18n message,
                          String resource,
                          String location,
                          Object... params ) {
        addProblem(new Problem(Problem.Status.ERROR, code, message, params, resource, location, throwable));
    }

    public void addWarning( I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, null, null, null));
    }

    public void addWarning( Throwable throwable,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, null, null, throwable));
    }

    public void addWarning( I18n message,
                            String resource,
                            String location,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, resource, location, null));
    }

    public void addWarning( Throwable throwable,
                            I18n message,
                            String resource,
                            String location,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, Problem.DEFAULT_CODE, message, params, resource, location, throwable));
    }

    public void addWarning( int code,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, null, null, null));
    }

    public void addWarning( Throwable throwable,
                            int code,
                            I18n message,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, null, null, throwable));
    }

    public void addWarning( int code,
                            I18n message,
                            String resource,
                            String location,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, resource, location, null));
    }

    public void addWarning( Throwable throwable,
                            int code,
                            I18n message,
                            String resource,
                            String location,
                            Object... params ) {
        addProblem(new Problem(Problem.Status.WARNING, code, message, params, resource, location, throwable));
    }

    public void addInfo( I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, null, null, null));
    }

    public void addInfo( Throwable throwable,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, null, null, throwable));
    }

    public void addInfo( I18n message,
                         String resource,
                         String location,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, resource, location, null));
    }

    public void addInfo( Throwable throwable,
                         I18n message,
                         String resource,
                         String location,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, Problem.DEFAULT_CODE, message, params, resource, location, throwable));
    }

    public void addInfo( int code,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, null, null, null));
    }

    public void addInfo( Throwable throwable,
                         int code,
                         I18n message,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, null, null, throwable));
    }

    public void addInfo( int code,
                         I18n message,
                         String resource,
                         String location,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, resource, location, null));
    }

    public void addInfo( Throwable throwable,
                         int code,
                         I18n message,
                         String resource,
                         String location,
                         Object... params ) {
        addProblem(new Problem(Problem.Status.INFO, code, message, params, resource, location, throwable));
    }

    public boolean hasProblems() {
        return getProblems().size() > 0;
    }

    public boolean hasErrors() {
        for (Problem problem : this.getProblems()) {
            if (problem.getStatus() == Problem.Status.ERROR) return true;
        }
        return false;
    }

    public boolean hasWarnings() {
        for (Problem problem : this.getProblems()) {
            if (problem.getStatus() == Problem.Status.WARNING) return true;
        }
        return false;
    }

    public boolean hasInfo() {
        for (Problem problem : this.getProblems()) {
            if (problem.getStatus() == Problem.Status.INFO) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return getProblems().isEmpty();
    }

    public int size() {
        return getProblems().size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.collection.Problems#iterator()
     */
    public Iterator<Problem> iterator() {
        return getProblems().iterator();
    }

    protected abstract void addProblem( Problem problem );

    protected abstract List<Problem> getProblems();
}
