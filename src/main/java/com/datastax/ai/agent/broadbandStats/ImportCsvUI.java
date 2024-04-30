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

import java.io.IOException;

import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Route("import")
class ImportCsvUI extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ImportCsvUI.class);

    public ImportCsvUI(FccBroadbandDataService fccBroadbandDataService) {
        var messageInput = new MessageInput();
        messageInput.setWidthFull();
        //messageInput.getStyle().set("vaadin-button::part(label)", "Import");

        messageInput.addSubmitListener(e -> {
            String fileName = e.getValue();
            try {
                fccBroadbandDataService.importCsv(fileName);
            } catch (IOException | InterruptedException ex) {
                logger.error("failed to import file {}", fileName, ex);
            }
        });
        add(messageInput);
    }
}

