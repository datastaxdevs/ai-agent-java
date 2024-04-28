# Build your own Java RAG AI Agent

 ⬅ This is the next workshop step after the [step-4](../workshop-step-4).

## Code, moar code, MOAR CODE

 🤩 The step adds the concepts
- Hybrid Search
- including online search results

♻️ And introduces the following technologies and techniques
- Tavily.com online search service
- Spring REST Clients


This step introduces the decorating AI Agent `AiAgentTavily`.  With the use of the `TAVILY_API_KEY` env var is uses the api.tavily.com service to fetch online search results.

This further enriches the prompt and the LLM's response, especially when information retrieved is recently published and unique.  It also allows the LLM's response to include reference links to sources and suggested further readings.


 👷‍♂️ Tavily also has a news search which can be particularly useful for including recent information models are not trained on.

 ⚠️ Online search context is of arbitary length.  We want to include multiple results for diversity but have to keep the prompt within its window size, so the contents within each search results is trimmed as needed.

## Build

 🔎 To see changes this step introduces use `git diff workshop-step-4..workshop-step-5`.


 🏃🏿 Run the project like:
```
./mvnw clean spring-boot:run
```


## Ask some questions…

 👩‍💻 Open in a browser http://localhost:8080
 and ask your chatbot some questions that are about recent affairs only available with online searches.

Compare what is put into the prompt and how the LLM uses it.

 🧐 What are the limitations of these results ? How could the queries be made more specific to a) what is really be asked at the current point in the conversation, and b) what the LLM actually needs in complimenting/missing information compared to what it has access to already ?


## Next…

 💪🏽 To move on to [step-6](../workshop-step-6), do the following:
```
git switch workshop-step-6
```



***
![java](./src/assets/java.png) ![vaadin](./src/assets/vaadin.png) ![spring](./src/assets/spring.png) ![tika](./src/assets/tika.jpeg) ![openai](./src/assets/openai.png) ![cassandra](./src/assets/cassandra.png) ![tavily](./src/assets/tavily.jpeg)

***
All work is copyrighted to DataStax, Inc