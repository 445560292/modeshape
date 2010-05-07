package org.modeshape.connector.infinispan;

import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.connector.infinispan*</code> packages.
 */
public final class InfinispanConnectorI18n {

    public static I18n connectorName;
    public static I18n propertyIsRequired;
    public static I18n errorSerializingCachePolicyInSource;
    public static I18n objectFoundInJndiWasNotCacheManager;
    public static I18n unableToCreateWorkspace;

    static {
        try {
            I18n.initialize(InfinispanConnectorI18n.class);
        } catch (final Exception err) {
            System.err.println(err);
        }
    }

    public static Set<Locale> getLocalizationProblemLocales() {
        return I18n.getLocalizationProblemLocales(InfinispanConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems() {
        return I18n.getLocalizationProblems(InfinispanConnectorI18n.class);
    }

    public static Set<String> getLocalizationProblems( Locale locale ) {
        return I18n.getLocalizationProblems(InfinispanConnectorI18n.class, locale);
    }

}
