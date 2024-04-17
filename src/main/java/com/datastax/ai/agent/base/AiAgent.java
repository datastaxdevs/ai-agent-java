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

import java.util.Map;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;


public interface AiAgent<T extends Object/*ChatOptionsBuilder*/> {
    
    Prompt createPrompt(
            UserMessage userMessage,
            Map<String,Object> promptProperties,
            T chatOptionsBuilder);
    
    Flux<ChatResponse> send(Prompt prompt);

    default Map<String,Object> promptProperties(Map<String,Object> promptProperties) {
        return promptProperties;
    }

    default T chatOptionsBuilder(T chatOptionsBuilder) {
        return chatOptionsBuilder;
    }
}
