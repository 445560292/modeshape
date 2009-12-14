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
package org.jboss.dna.graph;

import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.jboss.dna.graph*</code> packages.
 */
public final class GraphI18n {

    public static I18n closedConnectionMayNotBeUsed;
    public static I18n errorConvertingIo;
    public static I18n errorConvertingType;
    public static I18n errorReadingPropertyValueBytes;
    public static I18n invalidIndexInSegmentName;
    public static I18n invalidQualifiedNameString;
    public static I18n maximumPoolSizeMayNotBeSmallerThanCorePoolSize;
    public static I18n missingEndBracketInSegmentName;
    public static I18n noNamespaceRegisteredForPrefix;
    public static I18n pathAncestorDegreeIsInvalid;
    public static I18n pathCannotBeNormalized;
    public static I18n pathIsAlreadyAbsolute;
    public static I18n pathIsNotAbsolute;
    public static I18n pathIsNotRelative;
    public static I18n repositoryConnectionPoolIsNotRunning;
    public static I18n unableToCreateSubpathBeginIndexGreaterThanOrEqualToEndingIndex;
    public static I18n unableToCreateSubpathBeginIndexGreaterThanOrEqualToSize;
    public static I18n unableToCreateValue;
    public static I18n unableToDiscoverPropertyTypeForNullValue;
    public static I18n unableToObtainValidRepositoryAfterAttempts;
    public static I18n validPathMayNotContainEmptySegment;
    public static I18n valueJavaTypeNotCompatibleWithPropertyType;
    public static I18n pathExpressionMayNotBeBlank;
    public static I18n pathExpressionIsInvalid;
    public static I18n pathExpressionHasInvalidSelect;
    public static I18n pathExpressionHasInvalidMatch;
    public static I18n messageDigestNotFound;
    public static I18n pathNotFoundExceptionLowestExistingLocationFound;

    public static I18n executingRequest;
    public static I18n executedRequest;
    public static I18n closingRequestProcessor;
    public static I18n closedRequestProcessor;
    public static I18n multipleErrorsWhileExecutingManyRequests;
    public static I18n multipleErrorsWhileExecutingRequests;
    public static I18n errorWhilePerformingAccessQuery;
    public static I18n unsupportedRequestType;
    public static I18n unableToAddMoreRequestsToAlreadyExecutedBatch;
    public static I18n unableToCreateReferenceToNodeWithoutUuid;
    public static I18n unableToCopyToLocationWithoutAPath;
    public static I18n unableToCopyToTheRoot;
    public static I18n actualLocationNotEqualToInputLocation;
    public static I18n actualLocationIsNotChildOfInputLocation;
    public static I18n actualLocationIsNotAtCorrectChildSegment;
    public static I18n actualLocationDoesNotHaveCorrectChildName;
    public static I18n actualLocationMustHavePath;
    public static I18n actualNewLocationIsNotSameAsInputLocation;
    public static I18n actualNewLocationMustHavePath;
    public static I18n actualOldLocationIsNotSameAsInputLocation;
    public static I18n actualOldLocationMustHavePath;
    public static I18n actualNewLocationMustHaveSameParentAsOldLocation;
    public static I18n actualNewLocationMustHaveSameNameAsRequest;
    public static I18n requestIsFrozenAndMayNotBeChanged;
    public static I18n propertyIsNotPartOfRequest;

    public static I18n errorImportingContent;
    public static I18n unableToFindRepositorySourceWithName;
    public static I18n nodeAlreadyExistsWithUuid;
    public static I18n couldNotAcquireLock;

    /* In-Memory Connector */
    public static I18n inMemoryNodeDoesNotExist;
    public static I18n errorSerializingInMemoryCachePolicyInSource;
    public static I18n inMemoryConnectorRequestsMustHavePathOrUuid;
    public static I18n workspaceDoesNotExistInRepository;
    public static I18n workspaceAlreadyExistsInRepository;

    /* Federation Connection */
    public static I18n namePropertyIsRequiredForFederatedRepositorySource;
    public static I18n propertyIsRequiredForFederatedRepositorySource;
    public static I18n federatedRepositorySourceMustBeInitialized;
    public static I18n errorReadingConfigurationForFederatedRepositorySource;
    public static I18n errorAddingProjectionRuleParseMethod;
    public static I18n requiredNodeDoesNotExistRelativeToNode;
    public static I18n unableToObtainConnectionToFederatedSource;
    public static I18n workspaceDoesNotExistInFederatedRepository;
    public static I18n locationCannotBeProjectedIntoWorkspaceAndSource;
    public static I18n unableToAddRequestToChannelThatIsDone;
    public static I18n federatedSourceDoesNotSupportCreatingWorkspaces;
    public static I18n federatedSourceDoesNotSupportCloningWorkspaces;
    public static I18n federatedSourceDoesNotSupportDestroyingWorkspaces;
    public static I18n unableToProjectSourceInformationIntoWorkspace;
    public static I18n unableToCreateNodeUnderPlaceholder;
    public static I18n unableToUpdatePlaceholder;
    public static I18n unableToDeletePlaceholder;
    public static I18n copyLimitedToBeWithinSingleSource;
    public static I18n moveLimitedToBeWithinSingleSource;
    public static I18n cloneLimitedToBeWithinSingleSource;

    /* Session */
    public static I18n unableToRefreshBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
    public static I18n unableToSaveBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
    public static I18n unableToSaveNodeThatWasCreatedSincePreviousSave;
    public static I18n nodeHasAlreadyBeenRemovedFromThisSession;
    public static I18n unableToMoveNodeToBeChildOfDecendent;
    public static I18n childNotFound;

    /* Query */
    public static I18n unknownQueryLanguage;
    public static I18n tableDoesNotExist;
    public static I18n columnDoesNotExistOnTable;
    public static I18n columnDoesNotExistInQuery;
    public static I18n columnIsNotFullTextSearchable;
    public static I18n tableIsNotFullTextSearchable;
    public static I18n selectorDoesNotExistInQuery;
    public static I18n propertyOnSelectorIsNotUsedInQuery;
    public static I18n errorResolvingNodesFromLocationsUsingSourceAndWorkspace;
    public static I18n queryHasNoResults;
    public static I18n schemataKeyReferencesNonExistingColumn;
    public static I18n nextMethodMustBeCalledBeforeGettingValue;
    public static I18n expectingValidName;
    public static I18n expectingValidPath;
    public static I18n columnMustBeScoped;
    public static I18n expectingValidNameAtLineAndColumn;
    public static I18n expectingValidPathAtLineAndColumn;
    public static I18n mustBeScopedAtLineAndColumn;
    public static I18n unexpectedToken;
    public static I18n secondValueInLimitRangeCannotBeLessThanFirst;
    public static I18n expectingComparisonOperator;
    public static I18n expectingConstraintCondition;
    public static I18n functionIsAmbiguous;
    public static I18n bindVariableMustConformToNcName;
    public static I18n invalidPropertyType;
    public static I18n valueCannotBeCastToSpecifiedType;
    public static I18n noMatchingBracketFound;
    public static I18n expectingLiteralAndUnableToParseAsLong;
    public static I18n expectingLiteralAndUnableToParseAsDouble;
    public static I18n expectingLiteralAndUnableToParseAsDate;

    /* Search */
    public static I18n interruptedWhileClosingChannel;
    public static I18n errorWhilePerformingQuery;
    public static I18n errorShuttingDownExecutorServiceInSearchEngineIndexer;
    public static I18n searchEngineIndexerForSourceHasAlreadyBeenClosed;

    static {
        try {
            I18n.initialize(GraphI18n.class);
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
