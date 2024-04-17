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
package com.datastax.ai.agent.base;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import reactor.core.publisher.Flux;


@Configuration
public class AiAgentBase implements AiAgent<OpenAiChatOptions.Builder> {

    private static final Logger logger = LoggerFactory.getLogger(AiAgentBase.class);

    @Value("classpath:/prompt-templates/system-prompt-qa.txt")
    private Resource systemPrompt;

    private final StreamingChatModel chatClient;

    public static AiAgentBase create(StreamingChatModel chatClient) {
        return new AiAgentBase(chatClient);
    }

    AiAgentBase(StreamingChatModel chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Prompt createPrompt(
            UserMessage userMessage,
            Map<String,Object> promptProperties,
            OpenAiChatOptions.Builder chatOptionsBuilder) {

        Message systemMessage
                = new SystemPromptTemplate(this.systemPrompt)
                        .createMessage(promptProperties(promptProperties));

        return new Prompt(
                List.of(systemMessage, userMessage),
                chatOptionsBuilder(chatOptionsBuilder).build());
    }

    @Override
    public Flux<ChatResponse> send(Prompt prompt) {

        logger.info("prompt (length {}): {} ",
                String.valueOf(new StringTokenizer(prompt.getContents()).countTokens()), prompt.getContents());

        return chatClient.stream(prompt);
    }

    @Override
    public Map<String,Object> promptProperties(Map<String,Object> promptProperties) {
        return new HashMap<>() {{
            putAll(promptProperties);
            put("current_date", java.time.LocalDate.now());
        }};
    }

}