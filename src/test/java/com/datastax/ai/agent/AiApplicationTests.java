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

import org.junit.jupiter.api.Test;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest()
@Testcontainers
class AiApplicationTests {

    static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("cassandra");

    @Container
    static CassandraContainer cassandraContainer = new CassandraContainer(DEFAULT_IMAGE_NAME.withTag("5.0"));

    @Bean
    EmbeddingModel getEmbeddingClient() {
        return new TransformersEmbeddingModel();
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void test() {
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.cassandra.contactPoints", () -> cassandraContainer.getContactPoint().getHostString());
        registry.add("spring.cassandra.port", () -> String.valueOf(cassandraContainer.getContactPoint().getPort()));
        registry.add("spring.cassandra.localDatacenter", () -> cassandraContainer.getLocalDatacenter());
    }
}
