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
package com.datastax.ai.agent.broadbandStats;

import java.util.Map;

import com.datastax.ai.agent.base.AiAgent;
import com.datastax.ai.agent.base.AiAgentDelegator;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;


/**
 * You will need to
 * `export TAVILY_API_KEY=…`
 *
 * Get a free API key at https://app.tavily.com/
 */
public class AiAgentFccBroadbandDataTool extends AiAgentDelegator<OpenAiChatOptions.Builder> {

    public static AiAgentFccBroadbandDataTool create(AiAgent agent) {
        return new AiAgentFccBroadbandDataTool(agent);
    }

    AiAgentFccBroadbandDataTool(AiAgent agent) {
        super(agent);
    }

    @Override
    public Prompt createPrompt(
            UserMessage message,
            Map<String,Object> promptProperties,
            OpenAiChatOptions.Builder chatOptionsBuilder) {

        return super.createPrompt(
                message,
                promptProperties(promptProperties),
                chatOptionsBuilder(chatOptionsBuilder));
    }

    @Override
    public OpenAiChatOptions.Builder chatOptionsBuilder(OpenAiChatOptions.Builder chatOptionsBuilder) {
        return chatOptionsBuilder.withFunction("fccBroadbandDataService");
    }

}
