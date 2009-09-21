/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.search;

import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.i18n.I18n;

/**
 * 
 */
public class SearchI18n {

    public static I18n locationForIndexesIsNotDirectory;
    public static I18n locationForIndexesCannotBeRead;
    public static I18n locationForIndexesCannotBeWritten;
    public static I18n errorWhileIndexingContentAtPath;
    public static I18n errorWhileRemovingContentAtPath;
    public static I18n errorWhileUpdatingContent;
    public static I18n errorWhileCommittingIndexChanges;
    public static I18n errorCreatingIndexWriter;
    public static I18n errorWhileOptimizingIndexes;
    public static I18n errorWhilePerformingSearch;
    public static I18n errorWhilePerformingQuery;
    public static I18n errorWhileInitializingSearchEngine;

    static {
        try {
            I18n.initialize(SearchI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(CommonI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(CommonI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(CommonI18n.class, locale);
    }
}
