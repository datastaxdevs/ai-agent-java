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
package com.datastax.ai.agent.vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.datastax.ai.agent.base.AiAgent;
import com.datastax.ai.agent.base.AiAgentDelegator;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.CassandraVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;



public class AiAgentVector extends AiAgentDelegator<Object> {

    private static final int CHAT_DOCUMENTS_SIZE = 10;
    private static final int MAX_DOCUMENT_WORDS = 2000;

    private final CassandraVectorStore store;

    public static AiAgentVector create(AiAgent agent, CassandraVectorStore store) {
        return new AiAgentVector(agent, store);
    }

    AiAgentVector(AiAgent agent, CassandraVectorStore store) {
        super(agent);
        this.store = store;
    }

    @Override
    public Prompt createPrompt(
            UserMessage message,
            Map<String,Object> promptProperties,
            Object chatOptionsBuilder) {

        SearchRequest request = SearchRequest.query(message.getContent()).withTopK(CHAT_DOCUMENTS_SIZE);
        List<Document> similarDocuments = store.similaritySearch(request);
        
        promptProperties = new HashMap<>(promptProperties);
        promptProperties.put("documents", limitToMaxWords(similarDocuments));

        // any re-ranking happens here

        return super.createPrompt(message, promptProperties, chatOptionsBuilder);
    }

    private static List<Document> limitToMaxWords(List<Document> docs) {
        int i = 0, words = 0;

        for (   ; i < docs.size() && words < MAX_DOCUMENT_WORDS
                ; words += new StringTokenizer(docs.get(i).getContent()).countTokens(), ++i) {}

        return docs.subList(0, i);
    }
}
