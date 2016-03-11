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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.schematic.document.Bson;
import org.modeshape.schematic.document.Document;

/**
 * Default implementation for the {@link Statements} interface which applies to all databases.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class DefaultStatements implements Statements {
    
    protected final Logger logger = Logger.getLogger(getClass());
    
    private final Map<String, String> statements;
    private final RelationalDbConfig config;

    protected DefaultStatements( RelationalDbConfig config, Map<String, String> statements ) {
        this.statements = statements;
        this.config = config;
    }

    @Override
    public Void createTable( Connection connection ) throws SQLException {
        logTableInfo("Creating table {0}...");
        try (PreparedStatement createStmt = connection.prepareStatement(statements.get(CREATE_TABLE))) {
            if (createStmt.executeUpdate() > 0) {
                logTableInfo("Table {0} created");
            } else {
                logTableInfo("Table {0} already exists");
            }
        }
        return null;
    }

    @Override
    public Void dropTable( Connection connection ) throws SQLException {
        logTableInfo("Dropping table {0}...");
        try (PreparedStatement createStmt = connection.prepareStatement(statements.get(DELETE_TABLE))) {
            if (createStmt.executeUpdate() > 0) {
                logTableInfo("Table {0} dropped");
            } else {
                logTableInfo("Table {0} does not exist");
            }
        }
        return null;
    }

    @Override
    public Set<String> getAllIds( Connection connection ) throws SQLException {
        logTableInfo("Returning all ids from {0}");
        try (PreparedStatement ps = connection.prepareStatement(statements.get(GET_ALL_IDS))) {
            Set<String> result = new HashSet<>();
            ps.setFetchSize(config.fetchSize());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));                    
                }
            }
            return result;
        }
    }

    @Override
    public Document getById( Connection connection, String id ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Searching for entry by id {0} in {1}", id, config.tableName());
        }
        try (PreparedStatement ps = connection.prepareStatement(statements.get(GET_BY_ID))) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return readDocument(rs.getBinaryStream(1));
            }
        }
    }
    
    @Override
    public List<Document> load( Connection connection, List<String> ids ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Loading ids {0} from {1}", ids.toString(), config.tableName());
        }
        String getMultipleStatement = statements.get(GET_MULTIPLE);
        int batchLoadSize = batchLoadSize();
        List<Document> results = new ArrayList<>();
        runBatchOperation(connection, getMultipleStatement, ids, batchLoadSize,
                          ( dbConnection, statement, startIdx1, endIdx1, data ) -> results.addAll(loadIDs(dbConnection, statement,
                                                                                                         startIdx1, endIdx1,
                                                                                                          ids)));
        return results;
    }
                    
    private List<Document> loadIDs( Connection connection, String statement, int startIdx, int endIdx, List<String> ids ) throws SQLException {
        List<String> sublist = ids.subList(startIdx, endIdx);
        String params = sublist.stream().map(id -> "?").collect(Collectors.joining(","));
        String statementString = statement.replaceAll("#", params);
        try (PreparedStatement ps = connection.prepareStatement(statementString)) {
            AtomicInteger counter = new AtomicInteger(1);
            for (String id : sublist) {
                ps.setString(counter.getAndIncrement(), id);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                List<Document> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(readDocument(rs.getBinaryStream(1)));
                }
                return results;
            }
        }
    }
                    
    @Override
    public DefaultBatchUpdate batchUpdate( Connection connection ) {
        return new DefaultBatchUpdate(connection);
    }

    @Override
    public boolean exists( Connection connection, String id ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Checking if the content with ID {0} exists in {1}", id, config.tableName());
        }
        
        try (PreparedStatement ps = connection.prepareStatement(statements.get(CONTENT_EXISTS))) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    @Override
    public Void removeAll( Connection connection ) throws SQLException {
        logTableInfo("Removing all content from {0}");
        try (PreparedStatement ps = connection.prepareStatement(statements.get(REMOVE_ALL_CONTENT))) {
            ps.executeUpdate();
        }
        return null;
    }
    
    protected int batchLoadSize() {
        return 500;
    }
   
    protected void logTableInfo( String message ) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, config.tableName());
        }
    }
    
    protected Document readDocument(InputStream is) {
        try (InputStream contentStream = config.compress() ? new GZIPInputStream(is) : is) {
            return Bson.read(contentStream);
        } catch (IOException e) {
            throw new RelationalProviderException(e);
        }
    }

    protected byte[] writeDocument(Document content)  {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (OutputStream out = config.compress() ? new GZIPOutputStream(bos) : bos) {
                Bson.write(content, out);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RelationalProviderException(e);
        }
    }

    private <T> void runBatchOperation( Connection connection, String statement, List<T> data, int batchSize,
                                        BatchOperation<T> operation ) throws SQLException {
        if (data.isEmpty()) {
            return;
        }
        
        int dataSize = data.size();
        if (dataSize <= batchSize) {
            operation.run(connection, statement, 0, dataSize, data);
            return;
        }

        int startIdx = 0;
        while (startIdx < dataSize) {
            int endIdx = startIdx + batchSize > dataSize ? dataSize : startIdx + batchSize;
            operation.run(connection, statement, startIdx, endIdx, data);
            startIdx = endIdx;
        }    
    }
        
        
    @FunctionalInterface
    protected interface BatchOperation<T> {
        void run(Connection connection, String statement, int startIdx, int endIdx, List<T> data) throws SQLException;
    }    

    @NotThreadSafe
    protected class DefaultBatchUpdate implements BatchUpdate{
        private final Connection connection;
     
        protected DefaultBatchUpdate( Connection connection ) {
            this.connection = connection;
        }

        @Override
        public void insert( Map<String, Document> documentsById ) throws SQLException {
            String sql = statements.get(INSERT_CONTENT);
            PreparedStatement insert = connection.prepareStatement(sql);
            documentsById.forEach(( id, document ) -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("adding batch statement: {0}", sql.replaceFirst("\\?", id));
                }
                insertDocument(insert, id, document);
            });
            insert.executeBatch();
        }
        
        protected void insertDocument(PreparedStatement statement, String id, Document document) {
            try {
                statement.setString(1, id);
                byte[] content = writeDocument(document);
                statement.setBytes(2, content);
                statement.addBatch();
            } catch (SQLException e) {
                throw new RelationalProviderException(e);
            }    
        }

        @Override
        public void update( Map<String, Document> documentsById ) throws SQLException {
            String sql = statements.get(UPDATE_CONTENT);
            PreparedStatement update = connection.prepareStatement(sql);
            documentsById.forEach(( id, document ) -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("adding batch statement: {0}", sql.replaceFirst(" ID.*=.*\\?", " ID = " + id));
                }
                updateDocument(update, id, document);
            });
            update.executeBatch();
        }

        protected void updateDocument(PreparedStatement statement, String id, Document document) {
            try {
                byte[] content = writeDocument(document);
                statement.setBytes(1, content);
                statement.setString(2, id);
                statement.addBatch();
            } catch (SQLException e) {
                throw new RelationalProviderException(e);
            }
        }

        @Override
        public void remove( List<String> ids ) throws SQLException {
            String sql = statements.get(REMOVE_CONTENT);
            runBatchOperation(connection, sql, ids, batchLoadSize(), this::batchRemove);    
        }

        private void batchRemove( Connection connection, String statement, int startIdx, int endIdx, List<String> ids )
                throws SQLException {
            List<String> sublist = ids.subList(startIdx, endIdx);
            String params = sublist.stream().map(id -> "?").collect(Collectors.joining(","));
            String statementString = statement.replaceAll("#", params);
            if (logger.isDebugEnabled()) {
                logger.debug("running statement: {0}", statementString);
            }
            try (PreparedStatement remove = connection.prepareStatement(statementString)) {
                AtomicInteger counter = new AtomicInteger(1);
                for (String id : sublist) {
                    remove.setString(counter.getAndIncrement(), id);
                }
                remove.executeUpdate();
            }
        }
    }
}
