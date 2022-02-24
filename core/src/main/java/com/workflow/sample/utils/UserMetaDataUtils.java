package com.workflow.sample.utils;

import com.adobe.aemfd.docmanager.Document;
import com.adobe.granite.workflow.metadata.storage.UserMetaDataPersistenceContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.Map;

public class UserMetaDataUtils {

    public static String getBlobIdPrefix(UserMetaDataPersistenceContext userMetaDataPersistenceContext) {
        return getFolder(userMetaDataPersistenceContext);
    }

    private static String getFolder(UserMetaDataPersistenceContext userMetaDataPersistenceContext) {
        String workflowId = userMetaDataPersistenceContext.getWorkflowId();
        return getDate(userMetaDataPersistenceContext) + "/" + workflowId.replaceAll("/", "_");
    }

    private static String getDate(UserMetaDataPersistenceContext userMetaDataPersistenceContext) {
        Date timeStarted = userMetaDataPersistenceContext.getWorkflow().getTimeStarted();
        if (timeStarted == null) {
            timeStarted = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(timeStarted);
    }

    public static JsonObject getJson(String value) {
        return new JsonParser().parse(value).getAsJsonObject();
    }

    public static Object getXMLDocument(String value) throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(value)));
    }

    public static Document getDocument(JsonObject propertiesJson) throws MalformedURLException {
        URL url = new URL(propertiesJson.get(ExternalizerConstants.DOC_SOURCE_URL).getAsString());
        Document document = new Document(url);
        Iterator attributes = propertiesJson.entrySet().iterator();
        while(attributes.hasNext()) {
            Map.Entry<String, JsonElement> property = (Map.Entry)attributes.next();
            document.setAttribute(property.getKey(), property.getValue().getAsString());
        }
        return document;
    }

    public static Document[] getDocumentArray(JsonElement value) throws MalformedURLException {
        List<Document> docList = new ArrayList<>();
        JsonArray docsJsonArray = value.getAsJsonArray();
        if (docsJsonArray != null) {
            for (int i = 0; i < docsJsonArray.size(); i++) {
                Document document = null;
                JsonObject propertiesJson = new JsonParser().parse(docsJsonArray.get(i).getAsString()).getAsJsonObject();
                if (propertiesJson.has(ExternalizerConstants.DOC_SOURCE_URL)) {
                    document = UserMetaDataUtils.getDocument(propertiesJson);
                }
                docList.add(document);
            }
        }
        Document[] docArray = new Document[docList.size()];
        return docList.toArray(docArray);
    }

    public static String[] getStringArray(JsonElement value) {
        JsonArray jsonArray = value.getAsJsonArray();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            values.add(jsonArray.get(i).getAsString());
        }
        return values.toArray(new String[jsonArray.size()]);
    }
}
