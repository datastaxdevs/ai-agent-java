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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

import com.datastax.ai.agent.broadbandStats.FccBroadbandDataService.DeviceTimeRange;
import com.datastax.ai.agent.broadbandStats.FccBroadbandDataService.BroadbandData;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.data.CqlVector;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;

import org.json.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.embedding.EmbeddingModel;


public final class FccBroadbandDataService implements Function<DeviceTimeRange, BroadbandData> {

    private static final Logger logger = LoggerFactory.getLogger(FccBroadbandDataService.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public record TimeRange(Instant from, Instant to) {}

    public record DeviceTimeRange(int device_id, TimeRange range) {}

    public record DeviceTimestamp(int device_id, Instant timestamp) {}

    public record BroadbandData(
            long sk_tx_bytes,
            long sk_rx_bytes,
            long cust_wired_tx_bytes,
            long cust_wired_rx_bytes,
            long cust_wifi_tx_bytes,
            long cust_wifi_rx_bytes) {}

    private final CqlSession cqlSession;
    private final EmbeddingModel embeddingClient;

    FccBroadbandDataService(CqlSession cqlSession, EmbeddingModel embeddingModel) {
        this.cqlSession = cqlSession;
        this.embeddingClient = embeddingModel;
        addEmbeddingColumnAndIndex(cqlSession, embeddingModel);
    }

    @Override
    public BroadbandData apply(DeviceTimeRange deviceTimeRange) {
        return read(deviceTimeRange);
    }

    public BroadbandData read(DeviceTimeRange deviceTimeRange) {
        
        SimpleStatement stmt = QueryBuilder.selectFrom("datastax_ai_agent", "network_traffic").all()
                .whereColumn("unit_id").isEqualTo(QueryBuilder.literal(deviceTimeRange.device_id))
                .whereColumn("dtime").isGreaterThanOrEqualTo(QueryBuilder.literal(deviceTimeRange.range.from))
                .whereColumn("dtime").isLessThanOrEqualTo(QueryBuilder.literal(deviceTimeRange.range.to))
                .build();


        BroadbandData sum = new BroadbandData(0, 0, 0, 0, 0, 0);
        for (Row r : cqlSession.execute(stmt)) {
            sum = new BroadbandData(
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

    // TODO write a script that reads the csv row-by-row and calls this
    public void write(DeviceTimestamp deviceTimestamp, BroadbandData broadbandData) {

        // write the data
        SimpleStatement stmt = QueryBuilder.insertInto("datastax_ai_agent", "network_traffic")
                .value("unit_id", QueryBuilder.literal(deviceTimestamp.device_id))
                .value("unit_id_", QueryBuilder.literal(deviceTimestamp.device_id))
                .value("dtime", QueryBuilder.literal(deviceTimestamp.timestamp))
                .value("sk_tx_bytes", QueryBuilder.literal(broadbandData.sk_tx_bytes))
                .value("sk_rx_bytes", QueryBuilder.literal(broadbandData.sk_rx_bytes))
                .value("cust_wired_tx_bytes", QueryBuilder.literal(broadbandData.cust_wired_tx_bytes))
                .value("cust_wired_rx_bytes", QueryBuilder.literal(broadbandData.cust_wired_rx_bytes))
                .value("cust_wifi_tx_bytes", QueryBuilder.literal(broadbandData.cust_wifi_tx_bytes))
                .value("cust_wifi_rx_bytes", QueryBuilder.literal(broadbandData.cust_wifi_rx_bytes))
                .build();

        cqlSession.execute(stmt);

        // write the rolling-time-window's embedding
        stmt = QueryBuilder.selectFrom("datastax_ai_agent", "network_traffic").all()
                .whereColumn("unit_id").isEqualTo(QueryBuilder.literal(deviceTimestamp.device_id))
                .whereColumn("dtime").isGreaterThanOrEqualTo(QueryBuilder.literal(deviceTimestamp.timestamp.minus(1, ChronoUnit.DAYS)))
                .whereColumn("dtime").isLessThanOrEqualTo(QueryBuilder.literal(deviceTimestamp.timestamp))
                .limit(10)
                .build();

        JSONArray jsonArr = new JSONArray();
        for (Row r : cqlSession.execute(stmt)) {
            jsonArr.put(new BroadbandData(
                    r.getLong("sk_tx_bytes"),
                    r.getLong("sk_rx_bytes"),
                    r.getLong("cust_wired_tx_bytes"),
                    r.getLong("cust_wired_rx_bytes"),
                    r.getLong("cust_wifi_tx_bytes"),
                    r.getLong("cust_wifi_rx_bytes")
            ));
        }

        List<Float> embedding = embeddingClient.embed(jsonArr.toString())
                .stream().map(Double::floatValue).toList();

        stmt = QueryBuilder.insertInto("datastax_ai_agent", "network_traffic")
                .value("unit_id", QueryBuilder.literal(deviceTimestamp.device_id))
                .value("dtime", QueryBuilder.literal(deviceTimestamp.timestamp))
                .value("embedding", QueryBuilder.literal(CqlVector.newInstance(embedding)))
                .build();

        cqlSession.execute(stmt);
    }

    public void importCsv(String csvFilePath) throws IOException, InterruptedException {

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line = br.readLine(); //skip header
            while ((line = br.readLine()) != null) {
                // Use comma as separator
                String[] columns = line.split(",");

                LocalDateTime dateTime = LocalDateTime.parse(columns[1], FORMATTER);
                Instant timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant();

                FccBroadbandDataService.DeviceTimestamp device
                        = new FccBroadbandDataService.DeviceTimestamp(
                            Integer.parseInt(columns[0]),
                            Instant.from(timestamp));

                FccBroadbandDataService.BroadbandData data = new FccBroadbandDataService.BroadbandData(
                        Long.parseLong(columns[2]),
                        Long.parseLong(columns[3]),
                        Long.parseLong(columns[4]),
                        Long.parseLong(columns[5]),
                        Long.parseLong(columns[6]),
                        Long.parseLong(columns[7]));

                logger.warn("importing {}", device);
                write(device, data);
                Thread.sleep(200);
            }
        }
    }

    private static void addEmbeddingColumnAndIndex(CqlSession cqlSession, EmbeddingModel embeddingModel) {

        // hacky schema changes
        try {
            cqlSession.execute(
                    String.format(
                            "ALTER TABLE datastax_ai_agent.network_traffic ADD (unit_id_ int,embedding vector<float,%s>)",
                            String.valueOf(embeddingModel.dimensions())));

        } catch (InvalidQueryException ex) {
            logger.info(ex.getLocalizedMessage());
        }
        try {
            cqlSession.execute(
                            "CREATE CUSTOM INDEX IF NOT EXISTS network_traffic_embedding_idx ON datastax_ai_agent.network_traffic (embedding) USING 'StorageAttachedIndex'");

            // copy this column and put a SAI on it so we can `WHERE !=` on it
            cqlSession.execute(
                            "CREATE CUSTOM INDEX IF NOT EXISTS network_traffic_unit_id_idx ON datastax_ai_agent.network_traffic (unit_id_) USING 'StorageAttachedIndex'");

        } catch (InvalidQueryException ex) {
            logger.info(ex.getLocalizedMessage());
        }
    }
}
