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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.modeshape.common.database.DatabaseType;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;

/**
 * Class which manages the SQL statements used by the {@link RelationalDb} to interact with a particular DB.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public final class StatementsManager {
    private static final Logger LOGGER = Logger.getLogger(StatementsManager.class);

    private static final String CREATE_TABLE = "create_table";
    private static final String DELETE_TABLE = "delete_table";
    private static final String GET_ALL_IDS = "get_all_ids";
    private static final String GET_BY_ID = "get_by_id";
    private static final String CONTENT_EXISTS = "content_exists";
    private static final String INSERT_CONTENT = "insert_content";
    private static final String UPDATE_CONTENT = "update_content";
    private static final String REMOVE_CONTENT = "remove_content";
    private static final String REMOVE_ALL_CONTENT = "remove_all_content";
    
    private static final Map<DatabaseType.Name, List<Integer>> IGNORABLE_ERROR_CODES_BY_DB = new HashMap<>();

    private final Map<String, String> statements = new HashMap<>();
    private final String tableName;
                                                                                      
    static {
        // Oracle doesn't have an IF EXISTS clause for DROP or CREATE table, so in this case we want to ignore such exceptions
        IGNORABLE_ERROR_CODES_BY_DB.put(DatabaseType.Name.ORACLE, Arrays.asList(942, 955));    
    }
    
    protected StatementsManager(DatabaseType dbType, String tableName) {
        this.tableName = Objects.requireNonNull(tableName);
        loadStatementsResource(dbType);
    }
    
    protected boolean canIgnore(DatabaseType dbType, int errorCode) {
        return IGNORABLE_ERROR_CODES_BY_DB.getOrDefault(dbType.name(), Collections.emptyList()).contains(errorCode);
    }

    protected void createTable(Connection connection) throws SQLException {
        LOGGER.debug("Creating table {0}...", tableName);
        try (PreparedStatement createStmt = connection.prepareStatement(statements.get(CREATE_TABLE))) {
            if (createStmt.executeUpdate() > 0) {
                LOGGER.debug("Table {0} created", tableName);
            } else {
                LOGGER.debug("Table {0} already exists", tableName);
            }
        }
    }

    protected void dropTable(Connection connection) throws SQLException {
        LOGGER.debug("Dropping table {0}...", tableName);
        try (PreparedStatement createStmt = connection.prepareStatement(statements.get(DELETE_TABLE))) {
            if (createStmt.executeUpdate() > 0) {
                LOGGER.debug("Table {0} dropped", tableName);
            } else {
                LOGGER.debug("Table {0} does not exist", tableName);
            }
        }
    }

    protected <T> T getAllIds(Connection connection, int fetchSize, Function<ResultSet, T> function) throws SQLException {
        LOGGER.debug("Returning all ids from {0}", tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(GET_ALL_IDS))) {
            ps.setFetchSize(fetchSize);
            try (ResultSet rs = ps.executeQuery()) {
                return function.apply(rs);
            }
        }
    }

    protected <T> T getById(Connection connection, String id, Function<ResultSet, T> function) throws SQLException {
        LOGGER.debug("Searching for entry by id {0} in {1}", id, tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(GET_BY_ID))) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return function.apply(rs);
            }
        }
    }

    protected void insertOrUpdateContent(Connection connection, String id,
                                         StreamSupplier streamSupplier) throws SQLException {
        LOGGER.debug("Performing insert or update on {0} in {1}", id, tableName);
        if (contentExists(connection, id)) {
            try (PreparedStatement ps = connection.prepareStatement(statements.get(UPDATE_CONTENT))) {
                ps.setBinaryStream(1, streamSupplier.get(), streamSupplier.length());
                ps.setString(2, id);
                if (ps.executeUpdate() > 0) {
                    LOGGER.debug("Update successful on {0}", id);
                }
            }
        } else {
            LOGGER.debug("ID {0} not present in {1}. Attempting to insert...", id, tableName);
            // the update was not performed, so try an insert because the object is most likely missing
            try (PreparedStatement ps = connection.prepareStatement(statements.get(INSERT_CONTENT))) {
                ps.setString(1, id);
                ps.setBinaryStream(2, streamSupplier.get(), streamSupplier.length());
                if (ps.executeUpdate() > 0) {
                    LOGGER.debug("Insert successful on {0}");
                } else {
                    throw new RelationalProviderException(RelationalProviderI18n.insertOrUpdateFailed, id,
                                                          " cannot insert new entry");
                }
            }
        }
    }
    
    protected boolean contentExists(Connection connection, String id) throws SQLException {
        LOGGER.debug("Checking if the content with ID {0} exists in {1}", id, tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(CONTENT_EXISTS))) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();            
        }
    }

    protected boolean removeContent(Connection connection, String id) throws SQLException {
        LOGGER.debug("Removing entry {0} from {1}", id, tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(REMOVE_CONTENT))) {
            ps.setString(1, id);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                LOGGER.debug("Successfully removed {0} ", id);
            } else {
                LOGGER.debug("{0} not removed");
            }
            return success;
        }
    }

    protected void removeAllContent(Connection connection) throws SQLException {
        LOGGER.debug("Removing all content from {0}", tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(REMOVE_ALL_CONTENT))) {
            ps.executeUpdate();
        } 
    }

    private void loadStatementsResource(DatabaseType dbType) {
        try (InputStream fileStream = statementsFile(dbType)){
            Properties statements = new Properties();
            statements.load(fileStream);
            statements.entrySet().forEach(entry -> this.statements.put(entry.getKey().toString(),
                                                                       StringUtil.createString(entry.getValue().toString(),
                                                                                               tableName)));
        } catch (IOException e) {
            throw new RelationalProviderException(e);
        }
    }

    private InputStream statementsFile(DatabaseType dbType) {
        String filePrefix = StatementsManager.class.getPackage().getName().replaceAll("\\.", "/") + "/" + dbType.nameString().toLowerCase();
        // first search for a file matching the major.minor version....
        String majorMinorFile = filePrefix + String.format("%s.%s_database.properties", dbType.majorVersion(), dbType.minorVersion());
        // then a file matching just major version
        String majorFile = filePrefix + String.format("%s_database.properties", dbType.majorVersion());
        // the a default with just the db name
        String defaultFile = filePrefix + "_database.properties";
        return Stream.of(majorMinorFile, majorFile, defaultFile)
                     .map(fileName -> StatementsManager.class.getClassLoader().getResourceAsStream(fileName))
                     .filter(Objects::nonNull)
                     .findFirst()
                     .orElseThrow(() -> new RelationalProviderException(RelationalProviderI18n.unsupportedDBError, dbType));   
    }

    @Override
    public String toString() {
        return "Statements[tableName=" + tableName + ", statements=" + statements + ']';
    }
    
    protected interface StreamSupplier extends Supplier<InputStream> {
        long length();
    }
}
