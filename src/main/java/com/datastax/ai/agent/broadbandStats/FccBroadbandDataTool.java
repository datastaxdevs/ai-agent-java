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
package com.datastax.ai.agent.broadbandStats;

import java.util.function.Function;

import com.datastax.ai.agent.broadbandStats.FccBroadbandDataService.Request;
import com.datastax.ai.agent.broadbandStats.FccBroadbandDataService.Response;
import com.datastax.oss.driver.api.core.CqlSession;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
public class FccBroadbandDataTool {

    @Bean
    @Description("Get device data usage over a period of time")
    public Function<Request, Response> fccBroadbandDataService(CqlSession cqlSession) {
        return new FccBroadbandDataService(cqlSession);
    }
}
