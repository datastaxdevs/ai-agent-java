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
package com.datastax.ai.agent.history;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.datastax.ai.agent.base.AiAgent;
import com.datastax.oss.driver.api.core.CqlSession;

import org.springframework.ai.chat.history.ChatMemory;
import org.springframework.ai.chat.history.cassandra.CassandraChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageAggregator;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;


public final class AiAgentSession implements AiAgent<Object> {

    public static final String SESSION_ID = AiAgentSession.class.getSimpleName() + "_sessionId";

    private static final int CHAT_HISTORY_WINDOW_SIZE = 40;

    private final AiAgent agent;
    private final ChatMemory chatHistory;

    public static AiAgentSession create(AiAgent agent, CqlSession cqlSession) {
        return new AiAgentSession(agent, cqlSession);
    }

    AiAgentSession(AiAgent agent, CqlSession cqlSession) {
        this.agent = agent;
        this.chatHistory = CassandraChatMemory.create(cqlSession);
    }

    @Override
    public Prompt createPrompt(
            UserMessage message,
            Map<String,Object> promptProperties,
            Object chatOptionsBuilder) {

        String sessionId = message.getMetadata().get(SESSION_ID).toString();
        List<Message> history = chatHistory.get(sessionId, CHAT_HISTORY_WINDOW_SIZE);

        String conversationStr = history.reversed().stream()
                .map(msg -> msg.getMessageType().name().toLowerCase() + ": " + msg.getContent())
                .collect(Collectors.joining(System.lineSeparator()));

        promptProperties = new HashMap<>(promptProperties);
        promptProperties.put("conversation", conversationStr);
        message.getMetadata().put(CassandraChatMemory.CONVERSATION_TS, Instant.now());

        return agent.createPrompt(
                message,
                promptProperties(promptProperties),
                chatOptionsBuilder(chatOptionsBuilder));
    }

    @Override
    public Flux<ChatResponse> send(Prompt prompt) {

        UserMessage question = (UserMessage) prompt.getInstructions()
                .stream().filter((m) -> MessageType.USER == m.getMessageType()).findFirst().get();

        Flux<ChatResponse> responseFlux = agent.send(prompt);

        return MessageAggregator.aggregate(
                responseFlux,
                (answer) -> saveQuestionAnswer(question, answer));
    }

    private void saveQuestionAnswer(UserMessage question, Message answer) {
        String sessionId = question.getMetadata().get(SESSION_ID).toString();
        Instant instant = (Instant) question.getMetadata().get(CassandraChatMemory.CONVERSATION_TS);
        answer.getMetadata().put(CassandraChatMemory.CONVERSATION_TS, instant);
        List<Message> conversation = List.of(question, answer);
        chatHistory.add(sessionId, conversation);
    }

}
