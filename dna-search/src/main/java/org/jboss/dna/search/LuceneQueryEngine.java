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

import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;
import org.apache.lucene.queryParser.ParseException;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryEngine;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.optimize.Optimizer;
import org.jboss.dna.graph.query.optimize.OptimizerRule;
import org.jboss.dna.graph.query.optimize.RuleBasedOptimizer;
import org.jboss.dna.graph.query.parse.InvalidQueryException;
import org.jboss.dna.graph.query.parse.QueryParser;
import org.jboss.dna.graph.query.plan.CanonicalPlanner;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.process.ProcessingComponent;
import org.jboss.dna.graph.query.process.QueryProcessor;
import org.jboss.dna.graph.query.process.SelectComponent.Analyzer;
import org.jboss.dna.graph.query.validate.Schemata;

/**
 * 
 */
class LuceneQueryEngine {

    private QueryEngine engine;

    public LuceneQueryEngine( Schemata schemata ) {
        engine = new QueryEngine(new CanonicalPlanner(), new LuceneOptimizer(), new LuceneProcessor(), schemata);
    }

    public LuceneQueryEngine( Schemata schemata,
                              QueryParser... languages ) {
        engine = new QueryEngine(new CanonicalPlanner(), new LuceneOptimizer(), new LuceneProcessor(), schemata, languages);
    }

    /**
     * Add a language to this engine by supplying its parser.
     * 
     * @param languageParser the query parser for the language
     * @throws IllegalArgumentException if the language parser is null
     */
    public void addLanguage( QueryParser languageParser ) {
        this.engine.addLanguage(languageParser);
    }

    /**
     * Remove from this engine the language with the given name.
     * 
     * @param language the name of the language, which is to match the {@link QueryParser#getLanguage() language} of the parser
     * @return the parser for the language, or null if the engine had no support for the named language
     * @throws IllegalArgumentException if the language is null
     */
    public QueryParser removeLanguage( String language ) {
        return this.engine.removeLanguage(language);
    }

    /**
     * Get the set of languages that this engine is capable of parsing.
     * 
     * @return the unmodifiable copy of the set of languages; never null but possibly empty
     */
    public Set<String> getLanguages() {
        return this.engine.getLanguages();
    }

    /**
     * Execute the supplied query by planning, optimizing, and then processing it.
     * 
     * @param indexes the context in which the query should be executed
     * @param language the language in which the query is expressed; must be one of the supported {@link #getLanguages()
     *        languages}
     * @param query the query that is to be executed
     * @return the query results; never null
     * @throws IllegalArgumentException if the language, context or query references are null, or if the language is not know
     * @throws ParsingException if there is an error parsing the supplied query
     * @throws InvalidQueryException if the supplied query can be parsed but is invalid
     */
    public QueryResults execute( IndexContext indexes,
                                 String language,
                                 String query ) {
        CheckArg.isNotNull(language, "language");
        CheckArg.isNotNull(indexes, "indexes");
        CheckArg.isNotNull(query, "query");
        return engine.execute(indexes.context(), language, query);
    }

    /**
     * Execute the supplied query by planning, optimizing, and then processing it.
     * 
     * @param query the query that is to be executed
     * @param indexes the indexes that should be used to execute the query; never null
     * @return the query results; never null
     * @throws IllegalArgumentException if the context or query references are null
     * @throws IOException if there is a problem indexing or using the writers
     * @throws ParseException if there is a problem parsing the query
     */
    public QueryResults execute( QueryCommand query,
                                 IndexContext indexes ) throws IOException, ParseException {
        return engine.execute(indexes.context(), query, new PlanHints());
    }

    /**
     * An {@link Optimizer} implementation that specializes the {@link RuleBasedOptimizer} by using custom rules.
     */
    protected static class LuceneOptimizer extends RuleBasedOptimizer {

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.optimize.RuleBasedOptimizer#populateRuleStack(java.util.LinkedList,
         *      org.jboss.dna.graph.query.plan.PlanHints)
         */
        @Override
        protected void populateRuleStack( LinkedList<OptimizerRule> ruleStack,
                                          PlanHints hints ) {
            super.populateRuleStack(ruleStack, hints);
            // Add any custom rules here, either at the front of the stack or at the end
        }
    }

    /**
     * A query processor that operates against Lucene indexes. All functionality is inherited from the {@link QueryProcessor},
     * except for the creation of the {@link ProcessingComponent} that does the low-level atomic queries (against the Lucene
     * indexes).
     */
    protected static class LuceneProcessor extends QueryProcessor {

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.process.QueryProcessor#createAccessComponent(org.jboss.dna.graph.query.QueryContext,
         *      org.jboss.dna.graph.query.plan.PlanNode, org.jboss.dna.graph.query.QueryResults.Columns,
         *      org.jboss.dna.graph.query.process.SelectComponent.Analyzer)
         */
        @Override
        protected ProcessingComponent createAccessComponent( QueryContext context,
                                                             PlanNode accessNode,
                                                             Columns resultColumns,
                                                             Analyzer analyzer ) {
            return new LuceneQueryComponent(context, resultColumns, accessNode);
        }
    }
}
