# Build your own Java RAG AI Agent

 ⬅ This is the next workshop step after the [step-3](../workshop-step-3).

## Code, moar code, MOAR CODE

 🤩 The step adds the concepts of
- Reranking
- Vector calculations

♻️ And introduces the following technologies and techniques
- JVector


This step introduces the decorating AI Agent `AiAgentReranker`.  Reranking is a post-retrieval processing step that re-orders, filters, and blends search results from one or more sources before augmenting the prompt with them.

Imagine the situation where vector similarity search results are all too similiar to each other, and beyond the first search result they don't offer much additional value.  This can be common in larger vector indexes.  The answer can be to retrieve a larger list of search results, and find the most diverse smaller set of results within a similarity score limit.  This is just one example of the need or benefit from reranking.


 👷‍♂️ This step introduces the use of JVector to do manual similarity distance calculations between vectors.  Such operations can be used for more advanced techniques like live-tracking vector clusters and their centroids.

 🔎 To see changes this step introduces use `git diff workshop-step-3..workshop-step-4`.


## Experiment

 🚧 Experiment increase the vector search size and code your own reranking approach.


## Next…

 💪🏽 To move on to [step-5](../workshop-step-5), do the following:
```
git switch workshop-step-5
```



***
![java](./src/assets/java.png) ![vaadin](./src/assets/vaadin.png) ![spring](./src/assets/spring.png) ![tika](./src/assets/tika.jpeg) ![openai](./src/assets/openai.png) ![cassandra](./src/assets/cassandra.png) ![tavily](./src/assets/tavily.jpeg)

***
All work is copyrighted to DataStax, Inc