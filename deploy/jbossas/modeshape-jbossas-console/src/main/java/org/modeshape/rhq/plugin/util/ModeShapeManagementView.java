package org.modeshape.rhq.plugin.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.values.CollectionValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.MetaValueFactory;
import org.modeshape.jboss.managed.ManagedRepository;
import org.modeshape.jboss.managed.ManagedSequencerConfig;
import org.modeshape.rhq.plugin.objects.ExecutedResult;
import org.modeshape.rhq.plugin.util.PluginConstants.ComponentType.Engine;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;

import com.sun.istack.Nullable;

public class ModeShapeManagementView implements PluginConstants {

	private static final Log LOG = LogFactory
			.getLog(PluginConstants.DEFAULT_LOGGER_CATEGORY);

	public ModeShapeManagementView() {

	}

	/*
	 * Metric methods
	 */
	public Object getMetric(ProfileServiceConnection connection,
			String componentType, String identifier, String metric,
			Map<String, Object> valueMap) {
		Object resultObject = new Object();

		// if
		// (componentType.equals(PluginConstants.ComponentType.Platform.NAME)) {
		// resultObject = getPlatformMetric(connection, componentType, metric,
		// valueMap);
		// } else if
		// (componentType.equals(PluginConstants.ComponentType.VDB.NAME)) {
		// resultObject = getVdbMetric(connection, componentType, identifier,
		// metric, valueMap);
		// }

		return resultObject;
	}

	private Object getPlatformMetric(ProfileServiceConnection connection,
			String componentType, String metric, Map<String, Object> valueMap) {

		Object resultObject = new Object();

		// if (metric
		// .equals(PluginConstants.ComponentType.Platform.Metrics.QUERY_COUNT))
		// {
		// resultObject = new Double(getQueryCount(connection).doubleValue());
		// } else {
		// if (metric
		// .equals(PluginConstants.ComponentType.Platform.Metrics.SESSION_COUNT))
		// {
		// resultObject = new Double(getSessionCount(connection).doubleValue());
		// } else {
		// if (metric
		// .equals(PluginConstants.ComponentType.Platform.Metrics.LONG_RUNNING_QUERIES))
		// {
		// Collection<Request> longRunningQueries = new ArrayList<Request>();
		// getRequestCollectionValue(getLongRunningQueries(connection),
		// longRunningQueries);
		// resultObject = new Double(longRunningQueries.size());
		// }
		// }
		// }

		return resultObject;
	}

	private Object getVdbMetric(ProfileServiceConnection connection,
			String componentType, String identifier, String metric,
			Map<String, Object> valueMap) {

		Object resultObject = new Object();

		// if (metric
		// .equals(PluginConstants.ComponentType.VDB.Metrics.ERROR_COUNT)) {
		// // TODO remove version parameter after AdminAPI is changed
		// resultObject = getErrorCount(connection, (String)
		// valueMap.get(VDB.NAME));
		// } else if (metric
		// .equals(PluginConstants.ComponentType.VDB.Metrics.STATUS)) {
		// // TODO remove version parameter after AdminAPI is changed
		// resultObject = getVDBStatus(connection, (String) valueMap
		// .get(VDB.NAME), 1);
		// } else if (metric
		// .equals(PluginConstants.ComponentType.VDB.Metrics.QUERY_COUNT)) {
		// resultObject = new Double(getQueryCount(connection).doubleValue());
		// } else if (metric
		// .equals(PluginConstants.ComponentType.VDB.Metrics.SESSION_COUNT)) {
		// resultObject = new Double(getSessionCount(connection).doubleValue());
		// } else if (metric
		// .equals(PluginConstants.ComponentType.VDB.Metrics.LONG_RUNNING_QUERIES))
		// {
		// Collection<Request> longRunningQueries = new ArrayList<Request>();
		// getRequestCollectionValue(getLongRunningQueries(connection),
		// longRunningQueries);
		// resultObject = new Double(longRunningQueries.size());
		//
		// }

		return resultObject;
	}

	/*
	 * Operation methods
	 */

	public void executeOperation(ProfileServiceConnection connection,
			ExecutedResult operationResult, final Map<String, Object> valueMap) {

		if (operationResult.getComponentType().equals(
				ComponentType.Engine.MODESHAPE_ENGINE)) {
			executeEngineOperation(connection, operationResult, operationResult
					.getOperationName(), valueMap);
		} else if (operationResult.getComponentType().equals(
				ComponentType.Repository.NAME)) {
			// TODO Implement repo ops
		}

	}

	private void executeEngineOperation(ProfileServiceConnection connection,
			ExecutedResult operationResult, final String operationName,
			final Map<String, Object> valueMap) {

		if (operationName.equals(Engine.Operations.RESTART)) {
			try {
				executeManagedOperation(ProfileServiceUtil
						.getManagedEngine(connection), operationName,
						new MetaValue[] { null });
			} catch (Exception e) {
				final String msg = "Exception executing operation: " + Engine.Operations.RESTART; //$NON-NLS-1$
				LOG.error(msg, e);
			}
		} else if (operationName.equals(Engine.Operations.SHUTDOWN)) {
			try {
				executeManagedOperation(ProfileServiceUtil
						.getManagedEngine(connection), operationName,
						new MetaValue[] { null });
			} catch (Exception e) {
				final String msg = "Exception executing operation: " + Engine.Operations.SHUTDOWN; //$NON-NLS-1$
				LOG.error(msg, e);
			}
		}
	}

	// private void executeVdbOperation(ProfileServiceConnection connection,
	// ExecutedResult operationResult,
	// final String operationName, final Map<String, Object> valueMap) {
	// Collection<Request> resultObject = new ArrayList<Request>();
	// Collection<Session> activeSessionsCollection = new ArrayList<Session>();
	// String vdbName = (String) valueMap
	// .get(PluginConstants.ComponentType.VDB.NAME);
	// String vdbVersion = (String) valueMap
	// .get(PluginConstants.ComponentType.VDB.VERSION);
	//
	// if (operationName.equals(VDB.Operations.GET_PROPERTIES)) {
	// List<String> fieldNameList = operationResult.getFieldNameList();
	// getProperties(connection, PluginConstants.ComponentType.VDB.NAME);
	// operationResult.setContent(createReportResultList(fieldNameList,
	// resultObject.iterator()));
	// } else if (operationName.equals(VDB.Operations.GET_SESSIONS)) {
	// List<String> fieldNameList = operationResult.getFieldNameList();
	// MetaValue sessionMetaValue = getSessions(connection);
	// getSessionCollectionValueForVDB(sessionMetaValue,
	// activeSessionsCollection, vdbName);
	// operationResult.setContent(createReportResultList(fieldNameList,
	// activeSessionsCollection.iterator()));
	// } else if (operationName.equals(VDB.Operations.GET_REQUESTS)) {
	// List<String> fieldNameList = operationResult.getFieldNameList();
	// MetaValue requestMetaValue = getRequestsForVDB(connection, vdbName,
	// Integer
	// .parseInt(vdbVersion));
	// getRequestCollectionValue(requestMetaValue, resultObject);
	// operationResult.setContent(createReportResultList(fieldNameList,
	// resultObject.iterator()));
	// }
	//
	// }
	//
	// /*
	// * Helper methods
	// */
	//
	// public MetaValue getProperties(ProfileServiceConnection connection, final
	// String component) {
	//
	// MetaValue propertyValue = null;
	// MetaValue args = null;
	//
	// try {
	// propertyValue = executeManagedOperation(connection, mc,
	// PluginConstants.Operation.GET_PROPERTIES, args);
	// } catch (Exception e) {
	//			final String msg = "Exception executing operation: " + Platform.Operations.GET_PROPERTIES; //$NON-NLS-1$
	// LOG.error(msg, e);
	// }
	//
	// return propertyValue;
	//
	// }
	//
	// protected MetaValue getRequests(ProfileServiceConnection connection) {
	//
	// MetaValue requestsCollection = null;
	// MetaValue args = null;
	//
	// try {
	// requestsCollection = executeManagedOperation(connection, mc,
	//					
	// PluginConstants.Operation.GET_REQUESTS, args);
	// } catch (Exception e) {
	//			final String msg = "Exception executing operation: " + Platform.Operations.GET_REQUESTS; //$NON-NLS-1$
	// LOG.error(msg, e);
	// }
	//
	// return requestsCollection;
	//
	// }
	//
	// protected MetaValue getRequestsForVDB(ProfileServiceConnection
	// connection, String vdbName, int vdbVersion) {
	//
	// MetaValue requestsCollection = null;
	// MetaValue[] args = new MetaValue[] {
	// MetaValueFactory.getInstance().create(vdbName),
	// MetaValueFactory.getInstance().create(vdbVersion) };
	//
	// try {
	// requestsCollection = executeManagedOperation(connection, mc,
	// PluginConstants.ComponentType.VDB.Operations.GET_REQUESTS,
	// args);
	// } catch (Exception e) {
	//			final String msg = "Exception executing operation: " + Platform.Operations.GET_REQUESTS; //$NON-NLS-1$
	// LOG.error(msg, e);
	// }
	//
	// return requestsCollection;
	//
	// }
	//
	// protected MetaValue getTransactions(ProfileServiceConnection connection)
	// {
	//
	// MetaValue transactionsCollection = null;
	// MetaValue args = null;
	//
	// try {
	// transactionsCollection = executeManagedOperation(connection, mc,
	// Platform.Operations.GET_TRANSACTIONS, args);
	// } catch (Exception e) {
	//			final String msg = "Exception executing operation: " + Platform.Operations.GET_TRANSACTIONS; //$NON-NLS-1$
	// LOG.error(msg, e);
	// }
	//
	// return transactionsCollection;
	//
	// }
	//
	// public MetaValue getSessions(ProfileServiceConnection connection) {
	//
	// MetaValue sessionCollection = null;
	// MetaValue args = null;
	//
	// try {
	// sessionCollection = executeManagedOperation(connection, mc,
	// PluginConstants.Operation.GET_SESSIONS, args);
	// } catch (Exception e) {
	//			final String msg = "Exception executing operation: " + Platform.Operations.GET_SESSIONS; //$NON-NLS-1$
	// LOG.error(msg, e);
	// }
	// return sessionCollection;
	//
	// }
	//
	// public static String getVDBStatus(ProfileServiceConnection connection,
	// String vdbName, int version) {
	//
	// ManagedComponent mcVdb = null;
	// try {
	// mcVdb = ProfileServiceUtil
	// .getManagedComponent(connection,
	// new org.jboss.managed.api.ComponentType(
	// PluginConstants.ComponentType.VDB.TYPE,
	// PluginConstants.ComponentType.VDB.SUBTYPE),
	// vdbName);
	// } catch (NamingException e) {
	//			final String msg = "NamingException in getVDBStatus(): " + e.getExplanation(); //$NON-NLS-1$
	// LOG.error(msg, e);
	// } catch (Exception e) {
	//			final String msg = "Exception in getVDBStatus(): " + e.getMessage(); //$NON-NLS-1$
	// LOG.error(msg, e);
	// }
	//
	//		return ProfileServiceUtil.getSimpleValue(mcVdb, "status", String.class); //$NON-NLS-1$
	// }

	/**
	 * @param mc
	 * @param operation
	 * @param args
	 * @return {@link MetaValue}
	 * @throws Exception
	 */
	public static MetaValue executeManagedOperation(ManagedComponent mc,
			String operation, @Nullable MetaValue... args) throws Exception {

		for (ManagedOperation mo : mc.getOperations()) {
			String opName = mo.getName();
			if (opName.equals(operation)) {
				try {
					if (args == null || (args.length == 1 && args[0] == null)) {
						return mo.invoke();
					}
					return mo.invoke(args);
				} catch (Exception e) {
					final String msg = "Exception invoking " + operation; //$NON-NLS-1$
					LOG.error(msg, e);
					throw e;
				}
			}
		}
		throw new Exception("No operation found with given name =" + operation); //$NON-NLS-1$

	}

	// public static MetaValue getManagedProperty(
	// ProfileServiceConnection connection, ManagedComponent mc,
	// String property, MetaValue... args) throws Exception {
	//
	// mc = getDQPManagementView(connection, mc);
	//
	// try {
	// mc.getProperty(property);
	// } catch (Exception e) {
	//			final String msg = "Exception getting the AdminApi in " + property; //$NON-NLS-1$
	// LOG.error(msg, e);
	// }
	//
	//		throw new Exception("No property found with given name =" + property); //$NON-NLS-1$
	// }
	//
	// private Integer getQueryCount(ProfileServiceConnection connection) {
	//
	// Integer count = new Integer(0);
	//
	// MetaValue requests = null;
	// Collection<Request> requestsCollection = new ArrayList<Request>();
	//
	// requests = getRequests(connection);
	//
	// getRequestCollectionValue(requests, requestsCollection);
	//
	// if (requestsCollection != null && !requestsCollection.isEmpty()) {
	// count = requestsCollection.size();
	// }
	//
	// return count;
	// }
	//
	// private Integer getSessionCount(ProfileServiceConnection connection) {
	//
	// Collection<Session> activeSessionsCollection = new ArrayList<Session>();
	// MetaValue sessionMetaValue = getSessions(connection);
	// getSessionCollectionValue(sessionMetaValue, activeSessionsCollection);
	// return activeSessionsCollection.size();
	// }
	//
	// /**
	// * @param mcVdb
	// * @return count
	// * @throws Exception
	// */
	// private int getErrorCount(ProfileServiceConnection connection, String
	// vdbName) {
	//
	// ManagedComponent mcVdb = null;
	// try {
	// mcVdb = ProfileServiceUtil
	// .getManagedComponent(connection,
	// new org.jboss.managed.api.ComponentType(
	// PluginConstants.ComponentType.VDB.TYPE,
	// PluginConstants.ComponentType.VDB.SUBTYPE),
	// vdbName);
	// } catch (NamingException e) {
	//			final String msg = "NamingException in getVDBStatus(): " + e.getExplanation(); //$NON-NLS-1$
	// LOG.error(msg, e);
	// } catch (Exception e) {
	//			final String msg = "Exception in getVDBStatus(): " + e.getMessage(); //$NON-NLS-1$
	// LOG.error(msg, e);
	// }
	//
	// // Get models from VDB
	// int count = 0;
	//		ManagedProperty property = mcVdb.getProperty("models"); //$NON-NLS-1$
	// CollectionValueSupport valueSupport = (CollectionValueSupport) property
	// .getValue();
	// MetaValue[] metaValues = valueSupport.getElements();
	//
	// for (MetaValue value : metaValues) {
	// GenericValueSupport genValueSupport = (GenericValueSupport) value;
	// ManagedObjectImpl managedObject = (ManagedObjectImpl) genValueSupport
	// .getValue();
	//
	// // Get any model errors/warnings
	//			MetaValue errors = managedObject.getProperty("errors").getValue(); //$NON-NLS-1$
	// if (errors != null) {
	// CollectionValueSupport errorValueSupport = (CollectionValueSupport)
	// errors;
	// MetaValue[] errorArray = errorValueSupport.getElements();
	// count += errorArray.length;
	// }
	// }
	// return count;
	// }
	//
	// protected MetaValue getLongRunningQueries(ProfileServiceConnection
	// connection) {
	//
	// MetaValue requestsCollection = null;
	// MetaValue args = null;
	//
	// try {
	// requestsCollection = executeManagedOperation(connection, mc,
	// Platform.Operations.GET_LONGRUNNINGQUERIES, args);
	// } catch (Exception e) {
	//			final String msg = "Exception executing operation: " + Platform.Operations.GET_LONGRUNNINGQUERIES; //$NON-NLS-1$
	// LOG.error(msg, e);
	// }
	//
	// return requestsCollection;
	// }
	//
	// private void getRequestCollectionValue(MetaValue pValue,
	// Collection<Request> list) {
	// MetaType metaType = pValue.getMetaType();
	// if (metaType.isCollection()) {
	// for (MetaValue value : ((CollectionValueSupport) pValue)
	// .getElements()) {
	// if (value.getMetaType().isComposite()) {
	// RequestMetadataMapper rmm = new RequestMetadataMapper();
	// RequestMetadata request = (RequestMetadata) rmm
	// .unwrapMetaValue(value);
	// list.add(request);
	// } else {
	// throw new IllegalStateException(pValue
	//							+ " is not a Composite type"); //$NON-NLS-1$
	// }
	// }
	// }
	// }
	//
	// private Collection<Session> getSessionsForVDB(ProfileServiceConnection
	// connection, String vdbName) {
	// Collection<Session> activeSessionsCollection = Collections.emptyList();
	// MetaValue sessionMetaValue = getSessions(connection);
	// getSessionCollectionValueForVDB(sessionMetaValue,
	// activeSessionsCollection, vdbName);
	// return activeSessionsCollection;
	// }
	//
	// public static <T> void getTransactionCollectionValue(MetaValue pValue,
	// Collection<Transaction> list) {
	// MetaType metaType = pValue.getMetaType();
	// if (metaType.isCollection()) {
	// for (MetaValue value : ((CollectionValueSupport) pValue)
	// .getElements()) {
	// if (value.getMetaType().isComposite()) {
	// Transaction transaction = (Transaction) MetaValueFactory
	// .getInstance().unwrap(value);
	// list.add(transaction);
	// } else {
	// throw new IllegalStateException(pValue
	//							+ " is not a Composite type"); //$NON-NLS-1$
	// }
	// }
	// }
	// }
	//
	// public static <T> void getSessionCollectionValue(MetaValue pValue,
	// Collection<Session> list) {
	// MetaType metaType = pValue.getMetaType();
	// if (metaType.isCollection()) {
	// for (MetaValue value : ((CollectionValueSupport) pValue)
	// .getElements()) {
	// if (value.getMetaType().isComposite()) {
	// Session Session = (Session) MetaValueFactory.getInstance()
	// .unwrap(value);
	// list.add(Session);
	// } else {
	// throw new IllegalStateException(pValue
	//							+ " is not a Composite type"); //$NON-NLS-1$
	// }
	// }
	// }
	// }
	//
	public static Collection<ManagedRepository> getRepositoryCollectionValue(
			MetaValue pValue) {
		Collection<ManagedRepository> list = new ArrayList<ManagedRepository>();
		MetaType metaType = pValue.getMetaType();
		if (metaType.isCollection()) {
			for (MetaValue value : ((CollectionValueSupport) pValue)
					.getElements()) {
				if (value.getMetaType().isComposite()) {
					ManagedRepository repository = (ManagedRepository) MetaValueFactory
							.getInstance().unwrap(value);
					list.add(repository);
				} else {
					throw new IllegalStateException(pValue
							+ " is not a Composite type"); //$NON-NLS-1$
				}
			}
		}
		return list;
	}

	public static Collection<ManagedSequencerConfig> getSequencerCollectionValue(
			MetaValue pValue) {
		Collection<ManagedSequencerConfig> list = new ArrayList<ManagedSequencerConfig>();
		MetaType metaType = pValue.getMetaType();
		if (metaType.isCollection()) {
			for (MetaValue value : ((CollectionValueSupport) pValue)
					.getElements()) {
				if (value.getMetaType().isComposite()) {
					ManagedSequencerConfig sequencer = (ManagedSequencerConfig) MetaValueFactory
							.getInstance().unwrap(value);
					list.add(sequencer);
				} else {
					throw new IllegalStateException(pValue
							+ " is not a Composite type"); //$NON-NLS-1$
				}
			}
		}
		return list;
	}

	private Collection createReportResultList(List fieldNameList,
			Iterator objectIter) {
		Collection reportResultList = new ArrayList();

		while (objectIter.hasNext()) {
			Object object = objectIter.next();

			Class cls = null;
			try {
				cls = object.getClass();
				Iterator methodIter = fieldNameList.iterator();
				Map reportValueMap = new HashMap<String, String>();
				while (methodIter.hasNext()) {
					String fieldName = (String) methodIter.next();
					String methodName = fieldName;
					Method meth = cls.getMethod(methodName, (Class[]) null);
					Object retObj = meth.invoke(object, (Object[]) null);
					reportValueMap.put(fieldName, retObj);
				}
				reportResultList.add(reportValueMap);
			} catch (Throwable e) {
				System.err.println(e);
			}
		}
		return reportResultList;
	}

}
