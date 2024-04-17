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
package com.datastax.ai.agent;

import java.util.Map;

import com.datastax.ai.agent.base.AiAgent;
import com.datastax.ai.agent.base.AiAgentBase;

import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import org.vaadin.firitin.components.messagelist.MarkdownMessage;
import org.vaadin.firitin.components.messagelist.MarkdownMessage.Color;


@Push
@SpringBootApplication
public class AiApplication implements AppShellConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(AiApplication.class);

    @Bean
    public AiAgentBase agent(AiAgent baseAgent) {
        return (AiAgentBase) baseAgent;
    }

    @Route("")
    static class AiChatUI extends VerticalLayout {

        public AiChatUI(AiAgentBase agent) {
            var messageList = new VerticalLayout();
            var messageInput = new MessageInput();

            messageInput.addSubmitListener(e -> {
                var question = e.getValue();
                var userUI = new MarkdownMessage(question, "You", Color.AVATAR_PRESETS[1]);
                var assistantUI = new MarkdownMessage("Assistant", Color.AVATAR_PRESETS[2]);

                messageList.add(userUI, assistantUI);

                UserMessage message = new UserMessage(question);
                Prompt prompt = agent.createPrompt(message, Map.of(), OpenAiChatOptions.builder());

                agent.send(prompt)
                        .subscribe((response) -> {
                            if (isValidResponse(response)) {

                                String output = response.getResult().getOutput().getContent();
                                if (null != output) {

                                    getUI().ifPresent(ui -> ui.access(
                                            () -> assistantUI.appendMarkdown(output)));

                                }
                            } else {
                                logger.warn("ChatResponse is/contains null! {}", response);
                            }
                        });
            });
            add(messageList, messageInput);
        }

        private static boolean isValidResponse(ChatResponse chatResponse) {
            return null != chatResponse
                    && null != chatResponse.getResult()
                    && null != chatResponse.getResult().getOutput();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
