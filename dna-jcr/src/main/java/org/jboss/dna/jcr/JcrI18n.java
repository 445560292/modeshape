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
package org.jboss.dna.jcr;

import org.jboss.dna.common.i18n.I18n;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
public final class JcrI18n {

    public static I18n cannotConvertValue;
    public static I18n credentialsMustProvideJaasMethod;
    public static I18n credentialsMustReturnAccessControlContext;
    public static I18n credentialsMustReturnLoginContext;
    public static I18n defaultWorkspaceName;
    public static I18n inputStreamConsumed;
    public static I18n nonInputStreamConsumed;
    public static I18n pathNotFound;
    public static I18n permissionDenied;
    public static I18n repositoryMustBeConfigured;
    public static I18n sourceInUse;

    public static I18n REP_NAME_DESC;
    public static I18n REP_VENDOR_DESC;
    public static I18n SPEC_NAME_DESC;

    static {
        try {
            I18n.initialize(JcrI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }
}
