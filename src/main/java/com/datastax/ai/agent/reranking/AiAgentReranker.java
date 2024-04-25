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
package com.datastax.ai.agent.reranking;

import java.util.List;
import java.util.Map;

import com.datastax.ai.agent.base.AiAgent;
import com.datastax.ai.agent.base.AiAgentDelegator;

import io.github.jbellis.jvector.vector.VectorUtil;
import io.github.jbellis.jvector.vector.VectorizationProvider;
import io.github.jbellis.jvector.vector.types.VectorFloat;
import io.github.jbellis.jvector.vector.types.VectorTypeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;

import static org.springframework.ai.vectorstore.CassandraVectorStore.SIMILARITY_FIELD_NAME;

public class AiAgentReranker extends AiAgentDelegator<Object> {

    private static final Logger logger = LoggerFactory.getLogger(AiAgentReranker.class);

    private static final VectorTypeSupport VTS = VectorizationProvider.getInstance().getVectorTypeSupport();

    public static AiAgentReranker create(AiAgent agent) {
        return new AiAgentReranker(agent);
    }

    AiAgentReranker(AiAgent agent) {
        super(agent);
    }

    @Override
    public Map<String, Object> promptProperties(Map<String, Object> promptProperties) {

        List<Document> similarDocuments = (List<Document>) promptProperties.get("documents");

        // any re-ranking happens here
        if (!similarDocuments.isEmpty()) {
            for (int i = 0; i + 1 < similarDocuments.size(); ++i) {

                Document doc0 = similarDocuments.get(i);
                Document doc1 = similarDocuments.get(i + 1);

                VectorFloat v0 = VTS.createFloatVector(toFloatArray(doc0.getEmbedding()));
                VectorFloat v1 = VTS.createFloatVector(toFloatArray(doc1.getEmbedding()));

                float cosine = VectorUtil.cosine(v0, v1);

                logger.info("");
                logger.info("Result {}", i);
                logger.info("  similarity score to search {}", doc0.getMetadata().get(SIMILARITY_FIELD_NAME));
                logger.info("  similarity score to next   {}", cosine);
                logger.info("");
            }
            logger.info("");
            logger.info("Result {}", similarDocuments.size() - 1);
            logger.info("  similarity score to search {}",
                    similarDocuments.get(similarDocuments.size() - 1).getMetadata().get(SIMILARITY_FIELD_NAME));
        }

        // for example, use just the top 3 documents
        promptProperties.put("documents", similarDocuments.subList(0, 3));
        return promptProperties;
    }

    private float[] toFloatArray(List<Double> embeddingDouble) {
        float[] embeddingFloat = new float[embeddingDouble.size()];
        int i = 0;
        for (Double d : embeddingDouble) {
            embeddingFloat[i++] = d.floatValue();
        }
        return embeddingFloat;
    }

}
