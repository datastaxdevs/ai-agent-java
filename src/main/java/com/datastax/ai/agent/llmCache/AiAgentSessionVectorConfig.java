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
package com.datastax.ai.agent.llmCache;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.shaded.guava.common.base.Preconditions;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.CassandraVectorStore;
import org.springframework.ai.vectorstore.CassandraVectorStoreConfig;
import org.springframework.ai.vectorstore.CassandraVectorStoreConfig.SchemaColumn;

final class AiAgentSessionVectorConfig {

    static final CassandraVectorStoreConfig.PrimaryKeyTranslator PRIMARY_KEY_TRANSLATOR
        = (pKeyColumns) -> {
            if (pKeyColumns.isEmpty()) {
                return UUID.randomUUID().toString() + "§¶0";
            }
            Preconditions.checkArgument(2 == pKeyColumns.size());

            String sessionId = pKeyColumns.get(0) instanceof UUID
                    ? ((UUID) pKeyColumns.get(0)).toString()
                    : (String) pKeyColumns.get(0);

            String conversationTs = pKeyColumns.get(1) instanceof Instant
                    ? String.valueOf(((Instant) pKeyColumns.get(1)).toEpochMilli())
                    : (String) pKeyColumns.get(1);

            return sessionId + "§¶" + conversationTs;
        };

    static final CassandraVectorStoreConfig.DocumentIdTranslator DOCUMENT_ID_TRANSLATOR
            = (id) -> {
                String[] parts = id.split("§¶");
                Preconditions.checkArgument(2 == parts.length);
                UUID sessionId = UUID.fromString(parts[0]);
                Instant conversationTs = Instant.ofEpochMilli(Long.parseLong(parts[1]));
                return List.of(sessionId, conversationTs);
            };


    static CassandraVectorStore configureAndCreateStore(CqlSession cqlSession, EmbeddingModel embeddingModel) {
        try {
            // TODO remove when https://github.com/spring-projects/spring-ai/pull/640 is merged
            cqlSession.execute(
                    String.format(
                            "ALTER TABLE datastax_ai_agent.agent_conversations ADD (prompt_request text,embedding vector<float,%s>)",
                            String.valueOf(embeddingModel.dimensions())));

        } catch (InvalidQueryException ex) {}


        CassandraVectorStoreConfig config = CassandraVectorStoreConfig.builder()
                .withCqlSession(cqlSession)
                .withKeyspaceName("datastax_ai_agent")
                .withTableName("agent_conversations")
                .withPartitionKeys(List.of(new SchemaColumn("session_id", DataTypes.TIMEUUID)))
                .withClusteringKeys(List.of(new SchemaColumn("message_timestamp", DataTypes.TIMESTAMP)))
                .withContentColumnName("prompt_request")
                .addMetadataColumn(new SchemaColumn("assistant", DataTypes.TEXT))
                .withIndexName("agent_conversations_embedding_idx") // TODO remove when XXX is merged
                .withPrimaryKeyTranslator(PRIMARY_KEY_TRANSLATOR)
                .withDocumentIdTranslator(DOCUMENT_ID_TRANSLATOR)
                .build();

        return new CassandraVectorStore(config, embeddingModel);
    }

    private AiAgentSessionVectorConfig() {}
}
