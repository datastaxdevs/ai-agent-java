# Build your own Java RAG AI Agent

 ⬅ This is the next workshop step after the [step-2](../workshop-step-2).

## Code, moar code, MOAR CODE

 🤩 The step adds the concept often needed in production
- LLM Caching
- Post Request Processing
- Vector Stores on existing datamodels



This step introduces the decorating AI Agent `AiAgentSessionVector` that uses another Spring AI's `VectorStore` on our existing `agent_conversations` data.  Vector similarity searches against past prompts can be re-used, as "cache hits", preventing the cost and latency of further LLM requests.  This will be effective when user requests have hot patterns.

 👷‍♂️ Having confidence in returning cache hits is difficult.  Post-processing evaluation and quality monitoring is a typical requirement to achieve such confidence.  This example code requires a very high similarity score of 0.99 for any results to be considered reusable and a "cache hit".

An addition `CassandraVectorStore` is added.  It is configured to alter the existing `agent_conversations` table to add the columns it requires. This is an example of in-situ embeddings and vector search on your existing data, avoiding problem the of database sprawl.

The `agent_conversations` table will be changed to look like…
```
CREATE TABLE datastax_ai_agent.vector_store (
    id text PRIMARY KEY,
    content text,
    embedding vector<float, 1536>
);

CREATE CUSTOM INDEX vector_store_embedding_idx
   ON datastax_ai_agent.vector_store (embedding)
   USING 'StorageAttachedIndex';
```


 🔎 To see changes this step introduces use `git diff workshop-step-2..workshop-step-3`.

## Configure and Build


 🏃🏿 Run the project like:
```
./mvnw clean spring-boot:run
```

 🚧 It won't compile! There's an intentional error in the code for you to fix.


## Ask some questions…

 👩‍💻 Open in a browser http://localhost:8080
 and ask your chatbot some questions that requires specific information you know it doesn't have.


Ask the questions testing for answers that deliver the new information.


## Next…

 💪🏽 To move on to [step-4](../workshop-step-4), do the following:
```
git switch workshop-step-4
```



***
![java](./src/assets/java.png) ![vaadin](./src/assets/vaadin.png) ![spring](./src/assets/spring.png) ![tika](./src/assets/tika.jpeg) ![openai](./src/assets/openai.png) ![cassandra](./src/assets/cassandra.png) ![tavily](./src/assets/tavily.jpeg)

***
All work is copyrighted to DataStax, Inc