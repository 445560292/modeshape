/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.rhq.plugin;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.metatype.api.values.CollectionValueSupport;
import org.jboss.metatype.api.values.CompositeValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.modeshape.rhq.plugin.util.ModeShapeManagementView;
import org.modeshape.rhq.plugin.util.PluginConstants;
import org.modeshape.rhq.plugin.util.ProfileServiceUtil;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * 
 */
public class ConnectorDiscoveryComponent implements
		ResourceDiscoveryComponent<ConnectorComponent> {

	private final Log log = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);
		
	/**
	 * {@inheritDoc}
	 *
	 * @see org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent#discoverResources(org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext)
	 */
	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext<ConnectorComponent> discoveryContext)
			throws InvalidPluginConfigurationException, Exception {

		Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

		ManagedComponent mc = ProfileServiceUtil
				.getManagedComponent(
						new ComponentType(
								PluginConstants.ComponentType.Engine.MODESHAPE_TYPE,
								PluginConstants.ComponentType.Engine.MODESHAPE_SUB_TYPE),
						PluginConstants.ComponentType.Engine.MODESHAPE_ENGINE);

		ModeShapeManagementView mmv = new ModeShapeManagementView();

		String operation = "getConnectors";

		MetaValue connectors = mmv.executeManagedOperation(mc, operation, null);

		if (connectors == null) {
			return discoveredResources;
		}

		MetaValue[] mvConnectorArray = ((CollectionValueSupport) connectors)
				.getElements();

		for (MetaValue value : mvConnectorArray) {

			CompositeValueSupport cvs = (CompositeValueSupport) value;
			String name = ProfileServiceUtil.stringValue(cvs.get("name"));
			String retryLimit = ProfileServiceUtil.stringValue(cvs.get("retryLimit"));
			String supportingCreatingWorkspaces = ProfileServiceUtil.stringValue(cvs.get("supportingCreatingWorkspaces"));
			String supportingEvents = ProfileServiceUtil.stringValue(cvs.get("supportingEvents"));
			String supportingLocks = ProfileServiceUtil.stringValue(cvs.get("supportingLocks"));
			String supportingQueries = ProfileServiceUtil.stringValue(cvs.get("supportingQueries"));
			String supportingReferences = ProfileServiceUtil.stringValue(cvs.get("supportingReferences"));
			String supportingSameNameSiblings = ProfileServiceUtil.stringValue(cvs.get("supportingSameNameSiblings"));
			String supportingSearches = ProfileServiceUtil.stringValue(cvs.get("supportingSearches"));
			String supportingUpdates = ProfileServiceUtil.stringValue(cvs.get("supportingUpdates"));
			/**
			 * 
			 * A discovered resource must have a unique key, that must stay the
			 * same when the resource is discovered the next time
			 */
			DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
					discoveryContext.getResourceType(), // ResourceType
					name, // Resource Key
					name, // Resource name
					null,
					PluginConstants.ComponentType.Repository.MODESHAPE_REPOSITORY_DESC, // Description
					discoveryContext.getDefaultPluginConfiguration(), // Plugin
																		// Config
					null // Process info from a process scan
			);

			Configuration c = detail.getPluginConfiguration();
			
			c.put(new PropertySimple("name", name));
			c.put(new PropertySimple("retryLimit", retryLimit));
			c.put(new PropertySimple("supportingCreatingWorkspaces", supportingCreatingWorkspaces));
			c.put(new PropertySimple("supportingEvents", supportingEvents));
			c.put(new PropertySimple("supportingLocks", supportingLocks));
			c.put(new PropertySimple("supportingQueries", supportingQueries));
			c.put(new PropertySimple("supportingReferences", supportingReferences));
			c.put(new PropertySimple("supportingSameNameSiblings", supportingSameNameSiblings));
			c.put(new PropertySimple("supportingSearches", supportingSearches));
			c.put(new PropertySimple("supportingUpdates", supportingUpdates));
			
			detail.setPluginConfiguration(c);
			
			// Add to return values
			discoveredResources.add(detail);
			log.info("Discovered ModeShape repositories: " + mc.getName());
		}

		return discoveredResources;

	}
}