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
package com.datastax.ai.agent.tavily;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.datastax.ai.agent.base.AiAgent;
import com.datastax.ai.agent.base.AiAgentDelegator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;


/**
 * You will need to
 * `export TAVILY_API_KEY=…`
 *
 * Get a free API key at https://app.tavily.com/
 */
public class AiAgentTavily extends AiAgentDelegator<Object> {

    private static final String TAVILY_URL = "https://api.tavily.com/search";
    private static final String TAVILY_API_KEY = System.getenv("TAVILY_API_KEY");
    private static final int MAX_SEARCH_WORDS = 2000;
    private static final int SEARCH_RESULTS = 3;

    private static void assertTavilyApiKeySet() {
        if (null == TAVILY_API_KEY) {
            throw new IllegalStateException("Please set the environment variable TAVILY_API_KEY");
        }
    }

    public static AiAgentTavily create(AiAgent agent) {
        return new AiAgentTavily(agent);
    }

    AiAgentTavily(AiAgent agent) {
        super(agent);
    }

    @Override
    public Prompt createPrompt(
            UserMessage message,
            Map<String,Object> promptProperties,
            Object chatOptionsBuilder) {

        assertTavilyApiKeySet();
        promptProperties = new HashMap<>(promptProperties(promptProperties));
        if ( message.getContent().length() >= 5 ) {
            JSONObject post = new JSONObject();
            post.put("api_key", TAVILY_API_KEY);
            post.put("query", message.getContent()); // maybe the conversation is needed context
            post.put("include_answer", true);
            post.put("max_results", SEARCH_RESULTS);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(post.toString(), headers);

            JSONObject json
                    = new JSONObject(new RestTemplate().postForObject(TAVILY_URL, request, String.class));

            promptProperties.put("search_results", truncateResults(json.getJSONArray("results")).toString());
        } else {
            promptProperties.put("search_results", "[]");
        }
        return super.createPrompt(message, promptProperties, chatOptionsBuilder);
    }

    private static JSONArray truncateResults(JSONArray results) throws JSONException {
        // truncate each content to 1000 chars if word count is over 400
        for (var result : results) {
            if (result instanceof JSONObject jsonObject) {
                String content = jsonObject.getString("content");
                if (new StringTokenizer(content).countTokens()
                        > (MAX_SEARCH_WORDS / SEARCH_RESULTS)) {

                    // hack, just truncate to 1000 characters.
                    jsonObject.put("content", content.substring(0, 1000));
                }
            }
        }
        return results;
    }

}
