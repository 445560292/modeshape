/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.cache.document;

import static org.junit.Assert.assertNotNull;
import javax.transaction.TransactionManager;
import org.junit.After;
import org.junit.Before;
import org.modeshape.jcr.RepositoryEnvironment;
import org.modeshape.jcr.TestingUtil;
import org.modeshape.jcr.api.txn.TransactionManagerLookup;
import org.modeshape.jcr.txn.DefaultTransactionManagerLookup;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicDb;
import org.modeshape.schematic.internal.document.BasicDocument;

public abstract class AbstractDocumentStoreTest {

    protected RepositoryEnvironment repoEnv;
    protected LocalDocumentStore localStore;
    protected SchematicDb db;

    @Before
    public void beforeTest() throws Exception {
        // create a default in-memory db....
        db = Schematic.getDb(new BasicDocument(Schematic.TYPE_FIELD, "db"));
        db.start();
        TransactionManagerLookup txLookup = new DefaultTransactionManagerLookup();
        TransactionManager tm = txLookup.getTransactionManager();
        assertNotNull("Cannot find a transaction manager", tm);        
        repoEnv = new TestRepositoryEnvironment(tm, db);
        localStore = new LocalDocumentStore(db, repoEnv);
    }

    @After
    public void afterTest() {
        try {
            db.stop();
        } finally {
            try {
                TestingUtil.killTransaction(transactions().getTransactionManager());
            } finally {
                repoEnv = null;
            }
        }
    }

    protected Transactions transactions() {
        return repoEnv.getTransactions();
    }
    
    protected void runInTransaction(Runnable operation) {
       localStore.runInTransaction(() -> {
           operation.run();
           return null;
       }, 0);
    }
}
