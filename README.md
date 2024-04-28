# Build your own Java RAG AI Agent

 ⬅ This is the next workshop step after the [step-5](../workshop-step-5).

## Code, moar code, MOAR CODE

 🤩 The step adds the concept
- LLM Function Calling
- Using ChatGPT to explain unknown datasets

♻️ And introduces the following technologies, techniques and data
- ChatGPT for schema and CQL assist
- dsbulk
- FCC's Measuring Broadband dataset


This step introduces the service `FccBroadbandDataService`, a function wrapper around it `FccBroadbandDataTool`, and a decorating Agent `AiAgentFccBroadbandDataTool` that tells the LLM of the function it can take advantage of.

 👩‍💻  The LLM uses the `@Description` from `FccBroadbandDataTool` to know when to use the function.


 🔎 To see changes this step introduces use `git diff workshop-step-5..workshop-step-6`.


## Download and Insert the FCC’s Broadband dataset

1. Download FCC’s “Broadband data”
https://www.fcc.gov/oet/mba/raw-data-releases
 → https://data.fcc.gov/download/measuring-broadband-america/2023/data-raw-2023-jul.tar.gz
2. Unzip it
3. With `curr_datausage.csv` ask ChatGPT to
    a. Explain the schema – ”Tell me what the following csv dataset is”
    b. Create a CQL table – ”Create a Cassandra CQL schema for this data, explain your choices”
        i. Make sure `unit_id` is of type ` INT ` and dtime is of type ` TIMESTAMP `
    c. Ask how to load it into AstraDB – ”What's the quickest way to load that data into Cassandra”
        i. Use the dsbulk approach

## Schema and Upload

- Using the informtion learnt from ChatGPT above, create a table `datastax_ai_agent.network_traffic`.
- Adjust `FccBroadbandDataService` to write and read to columns as the schema you have created.
- Use dsbulk to upload the whole csv file into AstraDB. It shouldn't take more than a few seconds.

## Build


 🏃🏿 Run the project like:
```
./mvnw clean spring-boot:run
```


## Ask some questions…

 👩‍💻 Open in a browser http://localhost:8080
 and ask your chatbot some questions about a given unit_id, over a given period of time.  This unit_id and the time period needs to exist in the data you uploaded.

Look into the prompt and how the LLM puts the results of the function call into it.


## Next…

 💪🏽 To move on to [step-7](../workshop-step-7), do the following:
```
git switch workshop-step-7
```



***
![java](./src/assets/java.png) ![vaadin](./src/assets/vaadin.png) ![spring](./src/assets/spring.png) ![tika](./src/assets/tika.jpeg) ![openai](./src/assets/openai.png) ![cassandra](./src/assets/cassandra.png) ![tavily](./src/assets/tavily.jpeg)

***
All work is copyrighted to DataStax, Inc
