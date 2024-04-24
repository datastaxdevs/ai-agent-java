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

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.datastax.ai.agent.base.AiAgent;
import com.datastax.ai.agent.base.AiAgentDelegator;
import com.datastax.ai.agent.history.AiAgentSession;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.shaded.guava.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.history.cassandra.CassandraChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.CassandraVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;

import reactor.core.publisher.Flux;

public class AiAgentSessionVector extends AiAgentDelegator {

    private static final double SIMILARITY_CACHE_HIT_SCORE = 0.99;

    // our embedding window is smaller than prompt, any prompt larger than the embedding window won't be cached
    private static final int MAX_DOCUMENT_WORDS = 8000;

    private static final Logger logger = LoggerFactory.getLogger(AiAgentSessionVector.class);

    private final AiAgent agent;
    private final CassandraVectorStore store;

    public static AiAgentSessionVector create(AiAgent agent, CqlSession cqlSession, EmbeddingModel embeddingModel) {
        return new AiAgentSessionVector(agent, cqlSession, embeddingModel);
    }

    AiAgentSessionVector(AiAgent agent, CqlSession cqlSession, EmbeddingModel embeddingModel) {
        super(agent);
        this.agent = agent;
        this.store = AiAgentSessionVectorConfig.configureAndCreateStore(cqlSession, embeddingModel);
    }

    @Override
    public Flux<ChatResponse> send(Prompt prompt) {

        UserMessage userMsg = (UserMessage) prompt.getInstructions()
                .stream().filter(m -> m instanceof UserMessage).findFirst().get();

        Preconditions.checkState(
                userMsg.getMetadata().containsKey(AiAgentSession.SESSION_ID),
                "AiAgentSession is expected to have put the AiAgentSession.SESSION_ID property into the UserMessage");

        // don't cache anything bigger than the embedding window
        if (new StringTokenizer(prompt.getContents()).countTokens() < MAX_DOCUMENT_WORDS) {
            // in production you would need to
            //  a) monitor and censor any sensitive/private data
            //  b) add metadata on a user id if you only want cache hits on the user's own history
            SearchRequest request = SearchRequest
                    .query(prompt.getContents())
                    .withSimilarityThreshold(SIMILARITY_CACHE_HIT_SCORE);


            for (Document cacheHit : store.similaritySearch(request)) {
                String cachedAnswer = (String) cacheHit.getMetadata().get("assistant");
                if (null != cachedAnswer && !cachedAnswer.isBlank()) {
                    logger.warn("CACHE HIT id {} question {}", cacheHit.getId(), userMsg.getContent());
                    return Flux.just(new ChatResponse(List.of(new Generation(cachedAnswer))));
                }
            }

            Document promptDoc = new Document(
                    AiAgentSessionVectorConfig.PRIMARY_KEY_TRANSLATOR.apply(List.of(
                            userMsg.getMetadata().get(AiAgentSession.SESSION_ID),
                            userMsg.getMetadata().get(CassandraChatMemory.CONVERSATION_TS))),
                    prompt.getContents(),
                    Map.of());

            store.add(List.of(promptDoc));
        }
        return agent.send(prompt);
    }

}
