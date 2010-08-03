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
package org.modeshape.jboss.managed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Repository;

import org.jboss.managed.api.ManagedOperation.Impact;
import org.jboss.managed.api.annotation.ManagementOperation;
import org.jboss.managed.api.annotation.ManagementParameter;
import org.jboss.managed.api.annotation.ManagementProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.metatype.api.annotations.MetaMapping;
import org.joda.time.DateTime;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * The <code>ManagedRepository</code> is a JBoss managed object for a {@link JcrRepository repository}.
 */
@MetaMapping(ManagedRepositoryMapper.class)
public class ManagedRepository implements ModeShapeManagedObject {

    // TODO get rid of this constructor
    public ManagedRepository() {
        this.repository = null;
        this.options = null;
        this.descriptors = null;
    }

    /****************************** above for temporary testing only ***********************************************************/

    /**
     * A JBoss managed property for the the configured descriptors of this repository. This map is unmodifiable and never
     * <code>null</code>.
     */
    private final Map<String, String> descriptors;

    /**
     * A JBoss managed property for configured options of this repository. This map is unmodifiable and never <code>null</code>.
     */
    @SuppressWarnings("unused")
	private final Map<String, String> options;

    /**
     * The ModeShape object being managed and delegated to (never <code>null</code>).
     */
    private final JcrRepository repository;
    
    /**
     * The name of this repository. 
     */
    private String name;
    
    /**
     * The JCR version support for this repository. 
     */
    private String version;

    /**
     * Creates a JBoss managed object for the specified repository.
     * 
     * @param repository the repository being managed (never <code>null</code>)
     */
    public ManagedRepository( JcrRepository repository ) {
        CheckArg.isNotNull(repository, "repository");
        this.repository = repository;

        // construct options
        Map<String, String> tempOptions = new HashMap<String, String>();

        for (Entry<Option, String> entry : this.repository.getOptions().entrySet()) {
            tempOptions.put(entry.getKey().toString(), entry.getValue());
        }

        this.options = Collections.unmodifiableMap(tempOptions);

        // construct descriptors
        Map<String, String> tempDescriptors = new HashMap<String, String>();

        for (String descriptorName : this.repository.getDescriptorKeys()) {
            tempDescriptors.put(descriptorName, this.repository.getDescriptor(descriptorName));
        }

        this.descriptors = Collections.unmodifiableMap(tempDescriptors);
    }

    /**
     * Obtains the options configured for this repository. This is a JBoss managed readonly property.
     * 
     * @return String name (can be <code>null</code>)
     */
    @ManagementProperty( name = "Name", description = "The name of this repository", readOnly = true, use = ViewUse.CONFIGURATION )
    public String getName() {
    	if (descriptors!=null){
    		return this.descriptors.get(org.modeshape.jcr.api.Repository.REPOSITORY_NAME);
    	}
        return name;
    }
    
    /**
     * The name for this repository. 
     * @param name 
     */
    public void setName(String name) {
		this.name = name;
    }

    /**
     * Obtains the JCR version supported by this repository. This is a JBoss managed readonly property.
     * 
     * @return String version (never <code>null</code>)
     */
    @ManagementProperty( name = "Version", description = "The JCR version supported by this repository", readOnly = true, use = ViewUse.CONFIGURATION )
    public String getVersion() {
    	if (descriptors!=null){
    		return this.descriptors.get(Repository.SPEC_VERSION_DESC) + " " + this.descriptors.get(Repository.SPEC_NAME_DESC);
    	}
        return version;
    }
    
    /**
     * The version support for this repository. 
     * @param version
     */
    public void setVersion(String version) {
		this.version = version;
    }

    
    @ManagementProperty( name = "Query Activity", description = "The number of queries executed", use = ViewUse.STATISTIC )
    public int getQueryActivity() {
        // TODO implement getQueryActivity() and define in doc what the return value actually represents
        return 0;
    }

    @ManagementProperty( name = "Save Activity", description = "The number of nodes saved", use = ViewUse.STATISTIC )
    public int getSaveActivity() {
        // TODO implement getSaveActivity() and define in doc what the return value actually represents
        return 0;
    }

    @ManagementProperty( name = "Session Activity", description = "The number of sessions created", use = ViewUse.STATISTIC )
    public Object getSessionActivity() {
        // TODO implement getSaveActivity() and define in doc what the return value actually represents
        return 0;
    }

    /**
     * Obtains all the repository locks sorted by owner. This is a JBoss managed operation.
     * 
     * @return a list of sessions sorted by owner (never <code>null</code>)
     */
    @ManagementOperation( description = "Obtains all the managed locks sorted by the owner", impact = Impact.ReadOnly )
    public List<ManagedLock> listLocks() {
        return listLocks(ManagedLock.SORT_BY_OWNER);
    }

    /**
     * Obtains all the repository locks sorted by the specified sorter.
     * 
     * @param lockSorter the lock sorter (never <code>null</code>)
     * @return a list of locks sorted by the specified lock sorter (never <code>null</code>)
     */
    public List<ManagedLock> listLocks( Comparator<ManagedLock> lockSorter ) {
        CheckArg.isNotNull(lockSorter, "lockSorter");

        // TODO implement listLocks(Comparator)
        List<ManagedLock> locks = new ArrayList<ManagedLock>();

        // create temporary date
        for (int i = 0; i < 5; ++i) {
            locks.add(new ManagedLock("workspace-" + i, true, "sessionId-1", new DateTime(), "id-" + i, "owner-" + i, true));
        }

        // sort
        Collections.sort(locks, lockSorter);

        return locks;
    }

    /**
     * Obtains all the repository sessions sorted by user name. This is a JBoss managed operation.
     * 
     * @return a list of sessions sorted by user name (never <code>null</code>)
     */
    @ManagementOperation( description = "Obtains the managed sessions sorted by user name", impact = Impact.ReadOnly )
    public List<ManagedSession> listSessions() {
        return listSessions(ManagedSession.SORT_BY_USER);
    }

    /**
     * Obtains all the repository sessions sorted by the specified sorter.
     * 
     * @param sessionSorter the session sorter (never <code>null</code>)
     * @return a list of locks sorted by the specified session sorter (never <code>null</code>)
     */
    public List<ManagedSession> listSessions( Comparator<ManagedSession> sessionSorter ) {
        CheckArg.isNotNull(sessionSorter, "sessionSorter");

        // TODO implement listSessions(Comparator)
        List<ManagedSession> sessions = new ArrayList<ManagedSession>();

        // create temporary date
        for (int i = 0; i < 5; ++i) {
            sessions.add(new ManagedSession("workspace-" + i, "userName-" + i, "sessionId-1", new DateTime()));
        }

        // sort
        Collections.sort(sessions, sessionSorter);

        return sessions;
    }

    /**
     * Removes the lock with the specified identifier. This is a JBoss managed operation.
     * 
     * @param lockId the lock's identifier
     * @return <code>true</code> if the lock was removed
     */
    @ManagementOperation( description = "Removes the lock with the specified ID", impact = Impact.WriteOnly, params = {@ManagementParameter( name = "lockId", description = "The lock identifier" )} )
    public boolean removeLock( String lockId ) {
        // TODO implement removeLockWithLockToken()
        return false;
    }

    /**
     * Terminates the session with the specified identifier. This is a JBoss managed operation.
     * 
     * @param sessionId the session's identifier
     * @return <code>true</code> if the session was terminated
     */
    @ManagementOperation( description = "Terminates the session with the specified ID", impact = Impact.WriteOnly, params = {@ManagementParameter( name = "sessionId", description = "The session identifier" )} )
    public boolean terminateSession( String sessionId ) {
        // TODO implement terminateSessionBySessionId()
        return false;
    }

}
