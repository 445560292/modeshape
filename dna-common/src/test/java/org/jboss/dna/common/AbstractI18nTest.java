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
package org.jboss.dna.common;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;
import org.junit.Test;

/**
 * @author John Verhaeg
 */
public abstract class AbstractI18nTest {

    private Class<?> i18nClass;

    protected AbstractI18nTest( Class<?> i18nClass ) {
        this.i18nClass = i18nClass;
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void shouldNotHaveProblems() throws Exception {
        for (Field fld : i18nClass.getDeclaredFields()) {
            if (fld.getType() == I18n.class && (fld.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC
                && (fld.getModifiers() & Modifier.STATIC) == Modifier.STATIC
                && (fld.getModifiers() & Modifier.FINAL) != Modifier.FINAL) {
                I18n i18n = (I18n)fld.get(null);
                if (i18n.hasProblem()) {
                    fail(i18n.problem());
                }
            }
        }
        // Check for global problems after checking field problems since global problems are detected lazily upon field usage
        Method method = i18nClass.getDeclaredMethod("getLocalizationProblemLocales", (Class[])null);
        Set<Locale> locales = (Set<Locale>)method.invoke(null, (Object[])null);
        if (!locales.isEmpty()) {
            method = i18nClass.getDeclaredMethod("getLocalizationProblems", Locale.class);
            for (Locale locale : locales) {
                Set<String> problems = (Set<String>)method.invoke(null, locale);
                try {
                    assertThat(problems.isEmpty(), is(true));
                } catch (AssertionError error) {
                    fail(problems.iterator().next());
                }
            }
        }
    }
}
