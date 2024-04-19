/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 */
package org.springframework.ai.chat.history.cassandra;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.delete.DeleteSelection;
import com.datastax.oss.driver.api.querybuilder.insert.InsertInto;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.datastax.oss.driver.api.querybuilder.schema.AlterTableAddColumn;
import com.datastax.oss.driver.api.querybuilder.schema.AlterTableAddColumnEnd;
import com.datastax.oss.driver.api.querybuilder.schema.CreateTable;
import com.datastax.oss.driver.api.querybuilder.schema.CreateTableStart;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.datastax.oss.driver.shaded.guava.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// will be upsteamed to spring-ai
//  ref https://github.com/spring-projects/spring-ai/pull/858
//
// a very hardcoded config, but you get the point
class CassandraChatMemoryConfig {

    private static final Logger logger = LoggerFactory.getLogger(CassandraChatMemoryConfig.class);

    record Schema(
            String keyspace,
            String table,
            List<SchemaColumn> partitionKeys,
            List<SchemaColumn> clusteringKeys) {
    }

    record SchemaColumn(String name, DataType type) {}

    public interface ChatExchangeToPrimaryKeyTranslator extends Function<List<Object>, List<Object>> {}

    public static final String DEFAULT_KEYSPACE_NAME = "datastax_ai_agent";
    public static final String DEFAULT_TABLE_NAME = "agent_conversations";
    public static final String DEFAULT_SESSION_ID_NAME = "session_id";
    public static final String DEFAULT_EXCHANGE_ID_NAME = "message_timestamp";
    public static final String DEFAULT_ASSISTANT_COLUMN_NAME = "assistant";
    public static final String DEFAULT_QUESTION_COLUMN_NAME = "user";

    private final CqlSession session;

    private final Schema schema = new Schema(
            DEFAULT_KEYSPACE_NAME,
            DEFAULT_TABLE_NAME,
            List.of(new SchemaColumn(DEFAULT_SESSION_ID_NAME, DataTypes.TIMEUUID)),
            List.of(new SchemaColumn(DEFAULT_EXCHANGE_ID_NAME, DataTypes.TIMESTAMP)));

    private final boolean disallowSchemaChanges = false;

    final PreparedStatement addUserStmt, addAssistantStmt, getStmt, deleteStmt;

    CassandraChatMemoryConfig(CqlSession session) {
        this.session = session;
        ensureSchemaExists();
        addUserStmt = prepareAddStmt(DEFAULT_QUESTION_COLUMN_NAME);
        addAssistantStmt = prepareAddStmt(DEFAULT_ASSISTANT_COLUMN_NAME);
        getStmt = prepareGetStatement();
        deleteStmt = prepareDeleteStmt();
    }

    private void ensureSchemaExists() {
        if (!disallowSchemaChanges) {
            ensureKeyspaceExists();
            ensureTableExists();
            ensureTableColumnsExist();
            checkSchemaAgreement();
        } else {
            checkSchemaValid();
        }
    }

    private void checkSchemaAgreement() throws IllegalStateException {
        if (!session.checkSchemaAgreement()) {
            logger.warn("Waiting for cluster schema agreement, sleeping 10s…");
            try {
                Thread.sleep(Duration.ofSeconds(10).toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
            if (!session.checkSchemaAgreement()) {
                logger.error("no cluster schema agreement still, continuing, let's hope this works…");
            }
        }
    }

    void checkSchemaValid() {

        Preconditions.checkState(session.getMetadata().getKeyspace(schema.keyspace).isPresent(),
                "keyspace %s does not exist", schema.keyspace);

        Preconditions.checkState(session.getMetadata()
                .getKeyspace(schema.keyspace)
                .get()
                .getTable(schema.table)
                .isPresent(), "table %s does not exist");

        TableMetadata tableMetadata = session.getMetadata()
                .getKeyspace(schema.keyspace)
                .get()
                .getTable(schema.table)
                .get();

        Preconditions.checkState(
                tableMetadata.getColumn(DEFAULT_ASSISTANT_COLUMN_NAME).isPresent(),
                "column %s does not exist", DEFAULT_ASSISTANT_COLUMN_NAME);

        Preconditions.checkState(
                tableMetadata.getColumn(DEFAULT_QUESTION_COLUMN_NAME).isPresent(),
                "column %s does not exist", DEFAULT_QUESTION_COLUMN_NAME);

    }

    private void ensureKeyspaceExists() {
        if (session.getMetadata().getKeyspace(schema.keyspace).isEmpty()) {
            SimpleStatement keyspaceStmt = SchemaBuilder.createKeyspace(schema.keyspace)
                    .ifNotExists()
                    .withSimpleStrategy(1)
                    .build();

            logger.debug("Executing {}", keyspaceStmt.getQuery());
            session.execute(keyspaceStmt);
        }
    }

    private void ensureTableExists() {
        if (session.getMetadata().getKeyspace(schema.keyspace).get().getTable(schema.table).isEmpty()) {
            CreateTable createTable = null;

            CreateTableStart createTableStart = SchemaBuilder.createTable(schema.keyspace, schema.table)
                    .ifNotExists();

            for (SchemaColumn partitionKey : schema.partitionKeys) {
                
                createTable = (null != createTable ? createTable : createTableStart)
                        .withPartitionKey(partitionKey.name, partitionKey.type);
            }
            for (SchemaColumn clusteringKey : schema.clusteringKeys) {
                createTable = createTable.withClusteringColumn(clusteringKey.name, clusteringKey.type);
            }

            createTable = createTable.withColumn(DEFAULT_QUESTION_COLUMN_NAME, DataTypes.TEXT);

            session.execute(
                    createTable.withClusteringOrder(DEFAULT_EXCHANGE_ID_NAME, ClusteringOrder.DESC)
                            // set this if you want sessions to expire after a period of time
                            // TODO create option, and append TTL value to select queries (performance)
                            //.withDefaultTimeToLiveSeconds((int) Duration.ofDays(120).toSeconds())

                            // TODO replace when SchemaBuilder.unifiedCompactionStrategy() becomes available
                            .withOption("compaction", Map.of("class", "UnifiedCompactionStrategy"))
                            //.withCompaction(SchemaBuilder.unifiedCompactionStrategy()))
                            .build());
        }
    }

    private void ensureTableColumnsExist() {

        TableMetadata tableMetadata = session.getMetadata()
                .getKeyspace(schema.keyspace())
                .get()
                .getTable(schema.table())
                .get();

        boolean addAssistantColumn = tableMetadata.getColumn(DEFAULT_ASSISTANT_COLUMN_NAME).isEmpty();
        boolean addUserColumn = tableMetadata.getColumn(DEFAULT_QUESTION_COLUMN_NAME).isEmpty();

        if (addAssistantColumn || addUserColumn) {
            AlterTableAddColumn alterTable = SchemaBuilder.alterTable(schema.keyspace(), schema.table());
            if (addAssistantColumn) {
                alterTable = alterTable.addColumn(DEFAULT_ASSISTANT_COLUMN_NAME, DataTypes.TEXT);
            }
            if (addUserColumn) {
                alterTable = alterTable.addColumn(DEFAULT_QUESTION_COLUMN_NAME, DataTypes.TEXT);
            }
            SimpleStatement stmt = ((AlterTableAddColumnEnd)alterTable).build();
            logger.debug("Executing {}", stmt.getQuery());
            session.execute(stmt);
        }
    }

    private PreparedStatement prepareAddStmt(String column) {
        RegularInsert stmt = null;
        InsertInto stmtStart = QueryBuilder.insertInto(schema.keyspace(), schema.table());
        for (var c : schema.partitionKeys()) {
            stmt = (null != stmt ? stmt : stmtStart)
                    .value(c.name(), QueryBuilder.bindMarker(c.name()));
        }
        for (var c : schema.clusteringKeys()) {
            stmt = stmt.value(c.name(), QueryBuilder.bindMarker(c.name()));
        }
        stmt = stmt.value(column, QueryBuilder.bindMarker("message"));
        return session.prepare(stmt.build());
    }

    private PreparedStatement prepareGetStatement() {
        Select stmt = QueryBuilder.selectFrom(schema.keyspace, schema.table).all();
        // TODO make compatible with configurable ChatExchangeToPrimaryKeyTranslator
        // this assumes there's only one clustering key (for the message timestamp)
        for (var c : schema.partitionKeys()) {
            stmt = stmt.whereColumn(c.name()).isEqualTo(QueryBuilder.bindMarker(c.name()));
        }
        stmt = stmt.limit(QueryBuilder.bindMarker("lastN"));
        return session.prepare(stmt.build());
    }

    private PreparedStatement prepareDeleteStmt() {
        Delete stmt = null;
        DeleteSelection stmtStart = QueryBuilder.deleteFrom(schema.keyspace, schema.table);
        for (var c : schema.partitionKeys()) {
            stmt = (null != stmt ? stmt : stmtStart)
                    .whereColumn(c.name()).isEqualTo(QueryBuilder.bindMarker(c.name()));
        }
        return session.prepare(stmt.build());
    }

}
