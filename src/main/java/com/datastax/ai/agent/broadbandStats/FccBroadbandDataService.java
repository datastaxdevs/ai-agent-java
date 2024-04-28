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

import java.time.Instant;
import java.util.function.Function;

import com.datastax.ai.agent.broadbandStats.FccBroadbandDataService.Request;
import com.datastax.ai.agent.broadbandStats.FccBroadbandDataService.Response;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;

final class FccBroadbandDataService implements Function<Request, Response> {

    public record TimeRange(Instant from, Instant to) {}

    public record Request(int device_id, TimeRange range) {}

    public record Response(
            long sk_tx_bytes,
            long sk_rx_bytes,
            long cust_wired_tx_bytes,
            long cust_wired_rx_bytes,
            long cust_wifi_tx_bytes,
            long cust_wifi_rx_bytes) {}

    private final CqlSession cqlSession;

    FccBroadbandDataService(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    @Override
    public Response apply(Request t) {
        
        SimpleStatement stmt = QueryBuilder.selectFrom("datastax_ai_agent", "network_traffic").all()
                .whereColumn("unit_id").isEqualTo(QueryBuilder.literal(t.device_id))
                .whereColumn("dtime").isGreaterThanOrEqualTo(QueryBuilder.literal(t.range.from))
                .whereColumn("dtime").isLessThanOrEqualTo(QueryBuilder.literal(t.range.to))
                .build();


        Response sum = new Response(0, 0, 0, 0, 0, 0);
        for (Row r : cqlSession.execute(stmt)) {
            sum = new Response(
                    sum.sk_tx_bytes + r.getLong("sk_tx_bytes"),
                    sum.sk_rx_bytes + r.getLong("sk_rx_bytes"),
                    sum.cust_wired_tx_bytes + r.getLong("cust_wired_tx_bytes"),
                    sum.cust_wired_rx_bytes + r.getLong("cust_wired_rx_bytes"),
                    sum.cust_wifi_tx_bytes + r.getLong("cust_wifi_tx_bytes"),
                    sum.cust_wifi_rx_bytes + r.getLong("cust_wifi_rx_bytes")
            );
        }
        return sum;
    }

}
