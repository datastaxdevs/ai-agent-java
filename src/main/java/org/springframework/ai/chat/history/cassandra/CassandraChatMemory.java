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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;

import org.springframework.ai.chat.history.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;


// will be upsteamed to spring-ai
//  ref https://github.com/spring-projects/spring-ai/pull/858
public final class CassandraChatMemory implements ChatMemory {

    public static final String CONVERSATION_TS
            = CassandraChatMemory.class.getSimpleName() + "_message_timestamp";

    private static final int MAX_CONVERSATION_WORDS = 2000;

    private final CqlSession session;
    private final CassandraChatMemoryConfig config;

    public static CassandraChatMemory create(CqlSession session) {
        return new CassandraChatMemory(session);
    }

    public CassandraChatMemory(CqlSession session) {
        this.session = session;
        this.config = new CassandraChatMemoryConfig(session);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        messages.forEach((message) -> add(conversationId, message));
    }

    @Override
    public void add(String sessionId, Message message) {
        PreparedStatement stmt;
        switch (message.getMessageType()) {
            case USER -> stmt = config.addUserStmt;
            case ASSISTANT -> stmt = config.addAssistantStmt;
            default -> throw new IllegalArgumentException("Cant add type " + message);
        }
        Instant instant = (Instant) message.getMetadata().get(CONVERSATION_TS);

        BoundStatementBuilder builder = stmt.boundStatementBuilder()
                .setUuid(CassandraChatMemoryConfig.DEFAULT_SESSION_ID_NAME, UUID.fromString(sessionId))
                .setInstant(CassandraChatMemoryConfig.DEFAULT_EXCHANGE_ID_NAME, instant)
                .setString("message", message.getContent());

        session.execute(builder.build());
    }

    @Override
    public void clear(String sessionId) {
        BoundStatementBuilder builder = config.deleteStmt.boundStatementBuilder()
                .setUuid(CassandraChatMemoryConfig.DEFAULT_SESSION_ID_NAME, UUID.fromString(sessionId));

        session.execute(builder.build());
    }


    @Override
    public List<Message> get(String sessionId, int lastN) {

        BoundStatementBuilder builder = config.getStmt.boundStatementBuilder()
                .setUuid(CassandraChatMemoryConfig.DEFAULT_SESSION_ID_NAME, UUID.fromString(sessionId))
                .setInt("lastN", lastN);

        int words = 0;
        List<Message> messages = new ArrayList<>();
        for (Row r : session.execute(builder.build())) {
            String assistant = r.getString(CassandraChatMemoryConfig.DEFAULT_ASSISTANT_COLUMN_NAME);
            String user = r.getString(CassandraChatMemoryConfig.DEFAULT_QUESTION_COLUMN_NAME);
            words += new StringTokenizer(assistant + " " + user).countTokens();
            if (words > MAX_CONVERSATION_WORDS) {
                break;
            }
            if (null != assistant) {
                messages.add(new AssistantMessage(assistant));
            }
            if (null != user) {
                messages.add(new UserMessage(user));
            }
        }
        return messages;
    }

}
