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
package org.modeshape.persistence.relational;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.modeshape.common.database.DatabaseType;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.ParsingException;
import org.modeshape.schematic.internal.document.BasicDocument;

/**
 * Test for {@link RelationalProvider}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RelationalProviderTest {
    
    @Test
    public void shouldReturnDefaultDbWhenNoExplicitConfigurationGiven() {
        BasicDocument configDocument = new BasicDocument(Schematic.TYPE_FIELD, RelationalProvider.ALIAS1);
        RelationalDb db = Schematic.getDb(configDocument);
        assertNotNull(db);
        assertEquals(RelationalDbConfig.DEFAULT_CONNECTION_URL, db.id());
        
        RelationalDbConfig config = db.config();
        assertNotNull(config);
        assertTrue(config.createOnStart());
        assertFalse(config.dropOnExit());
        assertEquals(RelationalDbConfig.DEFAULT_TABLE_NAME, config.tableName());
        assertEquals(RelationalDbConfig.DEFAULT_FETCH_SIZE, config.fetchSize());
        assertTrue(config.compress());
        assertEquals(RelationalDbConfig.DEFAULT_CACHE_SIZE, config.cacheSize());
        
        DataSourceManager dsManager = db.dsManager();
        assertNotNull(dsManager);
        assertEquals(DatabaseType.Name.H2, dsManager.dbType().name());    
    }    
    
    @Test
    public void shouldReturnDbConfiguredFromDocument() throws ParsingException {
        RelationalDb db = Schematic.getDb(RelationalProviderTest.class.getClassLoader().getResourceAsStream("db-config-h2-full.json"));
        assertNotNull(db);
        assertEquals("jdbc:h2:mem:modeshape", db.id());

        RelationalDbConfig config = db.config();
        assertNotNull(config);
        assertFalse(config.createOnStart());
        assertTrue(config.dropOnExit());
        assertEquals("REPO", config.tableName());
        assertEquals(100, config.fetchSize());
        assertFalse(config.compress());
        assertEquals(100, config.cacheSize());


        DataSourceManager dsManager = db.dsManager();
        assertNotNull(dsManager);
        assertEquals(DatabaseType.Name.H2, dsManager.dbType().name());    
    }
}
