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
package com.datastax.ai.agent.vector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;

import org.apache.commons.lang3.StringUtils;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.txt.TXTParser;
import org.apache.tika.sax.BodyContentHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.CassandraVectorStore;

import org.xml.sax.SAXException;



@Route("upload")
class AiUploadUI extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(AiUploadUI.class);

    // chunk overlap is this.  chunk size is double it.
    private static final int DOCUMENT_CHUNK_TOKENS = 150;

    AiUploadUI(CassandraVectorStore store) {
        add(addFileUploader(store));
    }

    private static Upload addFileUploader(CassandraVectorStore store) {
        MemoryBuffer memoryBuffer = new MemoryBuffer();
        Upload upload = new Upload(memoryBuffer);
        upload.setAcceptedFileTypes("application/pdf", "text/plain");
        upload.setDropLabel(new Span("Upload document (.txt or .pdf)"));
        upload.addFinishedListener(e -> {
            // Embed uploaded document
            try {
                // read the contents of the buffered memory inputStream
                parseInputFile(store, e.getFileName(), memoryBuffer.getInputStream(), e.getMIMEType());
                // Update grid with parsed items and show dashboard.
                Notification.show("File uploaded").setPosition(Notification.Position.MIDDLE);
            } catch (IOException ex) {
                logger.warn("Exception parsing input file ", ex);
            }
        });
        // Add listeners to various actions (Optional)
        upload.addFailedListener( event -> {
            Notification.show(event.getReason().getMessage()).setPosition(Notification.Position.MIDDLE);
        });
        upload.addFileRejectedListener( event -> {
            Notification.show(event.getErrorMessage()).setPosition(Notification.Position.MIDDLE);
        });
        upload.getElement().addEventListener("file-remove", event -> {
            // Listener for user doc remove action
        }).addEventData("event.detail.file.name");
        return upload;
    }

    private static void parseInputFile(
            CassandraVectorStore store,
            String filename,
            InputStream stream,
            String mediaType) throws IOException {

        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try {
            switch (mediaType) {
                case "text/plain" -> new TXTParser().parse(stream, handler, metadata, new ParseContext());
                case "application/pdf" -> new PDFParser().parse(stream, handler, metadata, new ParseContext());
            }
            String content = clean(handler.toString());
            if (logger.isDebugEnabled()) {
                logger.debug("Contents of the document:" + content);
                logger.debug("Metadata of the document:");
                for(String name : metadata.names()) {
                    logger.debug(name + " : " + metadata.get(name));
                }
            }
            List<Document> docs = chunkInputText(content, filename, metadata);
            store.add(docs);
            logger.info("Uploaded, parsed, chunked ({}), vectorised and stored {}", docs.size(), filename);
        } catch (SAXException | TikaException e) {
            throw new IOException(e);
        }
    }

    private static List<Document> chunkInputText(String contents, String filename, Metadata metadata) {
        Map<String,Object> md = new HashMap<>();
        for(String name : metadata.names()) {
            md.put(name, metadata.get(name));
        }
        List<Document> documents = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        StringBuilder next = new StringBuilder();
        int tokens = 0;
        StringTokenizer tokenizer = new StringTokenizer(contents, " \t\n\r\f", true);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            current.append(token);
            next.append(token);
            if (++tokens > DOCUMENT_CHUNK_TOKENS) {

                documents.add(new Document(filename + "§¶" + documents.size(), current.toString(), md));
                tokens = 0;
                current = new StringBuilder(next.toString());
                next.setLength(0);
            }
        }
        documents.add(new Document(filename + "§¶" + documents.size(), current.toString(), md));
        return documents;
    }

    private static String clean(String content) {
        return StringUtils.normalizeSpace(content).replaceAll("(?m)^[ \t]*\r?\n", "");
    }
}
