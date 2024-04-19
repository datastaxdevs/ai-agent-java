# Build your own Java RAG AI Agent

 ⬅ This is the next workshop step after the [step-0](../workshop-step-0).

## Code, moar code, MOAR CODE

 🤩 The step introduces a further basic concept of an AI Agent
- Short Term Memory

This is also know as Conversational Memory.

 ♻️ This step introduces the following technologies and techniques
- Apache Cassandra and/or AstraDB for persistence
- Spring AI's `.chat.history.` packages
- Spring Boot Autoconfigure's Cassandra package


This step introduces a new Decorating AI Agent `AiAgentSession` that adds conversation history capabilities.

The prompt adds some text to tell the LLM about the history being added.

The history is stored and received through Spring AI's `ChatMemory` interface.

Our implementation of this interface is `CassandraChatMemory`.  This requires a `CqlSession` which is autoconfigured with properties found in `application.properties`.  Additional autoconfiguration happens with the `AstraConfiguration` to get the the cassandra-java-driver to use our Secure Connect Bundle and make a connection to AstraDB.

The `CassandraChatMemory` automatically creates a default schema for itself.  This can be configured to use a different and/or existing table if you so desire.

The default schema looks like…
```
CREATE TABLE agent_conversations (
    session_id        timeuuid,
    message_timestamp timestamp,
    user              text,
    assistant         text,
    PRIMARY KEY (session_id, message_timestamp)
  ) WITH CLUSTERING ORDER BY (message_timestamp DESC);
```

 🔎 To see changes this step introduces use `git diff workshop-step-0..workshop-step-1`.

## Configure and Build


 🏃🏿 Run the project like:
```
./mvnw clean spring-boot:run
```

 🚧 It won't compile! There's an intentional error in the code for you to fix.


 ℹ️ In the following workshop steps there will appear springframework code under `org.springframework.` packages.  All such code is being upstreamed and will become part of the Spring AI framework.  During the workshop you don't need to pay attention to this code.



## Ask some questions…

 👩‍💻 Once fixed and running, open in a browser http://localhost:8080
 and ask your chatbot some questions.

Test the agents ability to remember the conversation.  The conversation is bound to the Vaadin session, so it will work over different browser tabs and limited periods of time as well.

The Agent is still limited though.  While is keeps memory of its interactions with you, it's only pulling knowledge its model has been trained on.

 🔍 Explore and test where these limitations are.

 🔍 Explore the data that's been created in AstraDB.
- Open the AstraDB console, go to the `CQL Console`
- Type the command cql commands
```
USE datastax_ai_agent ;
DESCRIBE agent_conversations ;
SELECT * FROM agent_conversations ;
```


## Next… 

 💪🏽 To move on to [step-2](../workshop-step-2) do the following:
```
git switch workshop-step-2
```



***
![java](./src/assets/java.png) ![vaadin](./src/assets/vaadin.png) ![spring](./src/assets/spring.png) ![tika](./src/assets/tika.jpeg) ![openai](./src/assets/openai.png) ![cassandra](./src/assets/cassandra.png) ![tavily](./src/assets/tavily.jpeg)

*** 
All work is copyrighted to DataStax, Inc
