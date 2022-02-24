package com.workflow.sample;

import com.adobe.aemfd.docmanager.Document;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.metadata.storage.UserMetaDataPersistenceContext;
import com.adobe.granite.workflow.metadata.storage.service.UserMetaDataPersistenceProvider;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.VariableTemplate;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.workflow.sample.connection.service.AzureBlobService;
import com.workflow.sample.utils.ExternalizerConstants;
import com.workflow.sample.utils.UserMetaDataUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;


@Component(property = {
        "service.ranking:Integer=1000"
},service = UserMetaDataPersistenceProvider.class)
public class FormsUserMetaDataPersistenceProvider implements UserMetaDataPersistenceProvider {

    private static Logger log = LoggerFactory.getLogger(FormsUserMetaDataPersistenceProvider.class);

    @Reference
    protected AzureBlobService azureBlobService;

    @Override
    public void get(UserMetaDataPersistenceContext userMetaDataPersistenceContext, MetaDataMap metaDataMap) throws WorkflowException {
        try {
            String downloadedJson = azureBlobService.downloadBlobJson(userMetaDataPersistenceContext.getUserDataId());
            Workflow workflow = userMetaDataPersistenceContext.getWorkflow();
            WorkflowModel workflowModel = workflow.getWorkflowModel();
            populateMetaDataMap(downloadedJson, metaDataMap, workflowModel);
        } catch (WorkflowException e) {
            log.error("Unable to get metadata map", e);
            throw new WorkflowException(e);
        }
    }

    protected void populateMetaDataMap(String jsonString, MetaDataMap metaDataMap, WorkflowModel workflowModel) throws WorkflowException {
        try {
            if (jsonString != null && jsonString.length() > 0) {
                JsonObject response = new JsonParser().parse(jsonString).getAsJsonObject();
                Iterator entries = response.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, JsonElement> entry = (Map.Entry) entries.next();
                    String key = entry.getKey();
                    if (workflowModel != null && workflowModel.getVariableTemplates() != null) {
                        VariableTemplate varTemplate = workflowModel.getVariableTemplates().get(entry.getKey());
                        if (varTemplate != null) {
                            if (varTemplate.getType().equals(ExternalizerConstants.JSON_VARIABLE)) {
                                metaDataMap.put(entry.getKey(), UserMetaDataUtils.getJson(entry.getValue().getAsString()));
                            } else if (varTemplate.getType().equals(ExternalizerConstants.XML_VARIABLE)) {
                                metaDataMap.put(entry.getKey(), UserMetaDataUtils.getXMLDocument(entry.getValue().getAsString()));
                            } else if (varTemplate.getType().equals(ExternalizerConstants.DOCUMENT_VARIABLE)) {
                                String propertiesString = entry.getValue().getAsString();
                                JsonObject propertiesJson = new JsonParser().parse(propertiesString).getAsJsonObject();
                                if (propertiesJson.has(ExternalizerConstants.DOC_SOURCE_URL)) {
                                    metaDataMap.put(entry.getKey(), UserMetaDataUtils.getDocument(propertiesJson));
                                }
                            } else if (varTemplate.getType().equals(ExternalizerConstants.ARRAY_LIST)) {
                                if (varTemplate.getSubType().equals(ExternalizerConstants.DOCUMENT_VARIABLE)) {
                                    Document[] docArray = UserMetaDataUtils.getDocumentArray(entry.getValue());
                                    metaDataMap.put(key, docArray);
                                } else {
                                    metaDataMap.put(entry.getKey(), UserMetaDataUtils.getStringArray(entry.getValue()));
                                }
                            } else {
                                metaDataMap.put(entry.getKey(), entry.getValue().getAsString());
                            }
                        } else {
                            metaDataMap.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    } else {
                        metaDataMap.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("Unable to populate metadata map from downloaded blob json", e);
            throw new WorkflowException(e);
        }
    }

    protected String getUploadText(UserMetaDataPersistenceContext userMetaDataPersistenceContext, MetaDataMap metaDataMap, Map<String, Document> documentsToBeUploded) throws WorkflowException {
        String jsonString = null;
        try {
            JsonObject jsonObject = new JsonObject();
            Workflow workflow = userMetaDataPersistenceContext.getWorkflow();
            WorkflowModel workflowModel = workflow.getWorkflowModel();
            if (metaDataMap != null) {
                Iterator<String> iterator = metaDataMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Object value = metaDataMap.get(key);
                    if (value != null) {
                        if (workflowModel != null && workflowModel.getVariableTemplates() != null) {
                            VariableTemplate varTemplate = workflowModel.getVariableTemplates().get(key);
                            if (varTemplate != null) {
                                if (varTemplate.getType().equals(ExternalizerConstants.DOCUMENT_VARIABLE)) {
                                    String blobId = UserMetaDataUtils.getBlobIdPrefix(userMetaDataPersistenceContext) + "/" + key;
                                    handleDocument(metaDataMap, key, blobId, jsonObject, documentsToBeUploded);
                                } else if (varTemplate.getType().equals(ExternalizerConstants.ARRAY_LIST)) {
                                    if (varTemplate.getSubType().equals(ExternalizerConstants.DOCUMENT_VARIABLE)) {
                                        handleDocumentArrayList(userMetaDataPersistenceContext, metaDataMap, key, jsonObject, documentsToBeUploded);
                                    } else {
                                        handleStringArrayList(value, jsonObject, key);
                                    }
                                } else {
                                    jsonObject.addProperty(key, value.toString());
                                }
                            } else {
                                jsonObject.addProperty(key, value.toString());
                            }
                        } else {
                            jsonObject.addProperty(key, value.toString());
                        }
                    }
                }
                jsonString = jsonObject.toString();
            }
        } catch (Exception e) {
            throw new WorkflowException(e);
        } finally {

        }
        return jsonString;
    }

    @Override
    public String put(UserMetaDataPersistenceContext userMetaDataPersistenceContext, MetaDataMap metaDataMap) throws WorkflowException {
        String uniqueId = null;
        try {
            Map<String, Document> documentsToBeUploaded = new HashMap();
            String uploadText = getUploadText(userMetaDataPersistenceContext, metaDataMap, documentsToBeUploaded);
            if(documentsToBeUploaded.size() > 0) {
                Iterator<String> iterator = documentsToBeUploaded.keySet().iterator();
                while (iterator.hasNext()) {
                    String blobId = iterator.next();
                    Document document = documentsToBeUploaded.get(blobId);
                    azureBlobService.postAttachment(blobId, document);
                }
            }
            uniqueId = userMetaDataPersistenceContext.getUserDataId() != null? userMetaDataPersistenceContext.getUserDataId(): UserMetaDataUtils.getBlobIdPrefix(userMetaDataPersistenceContext) + "/" + UUID.randomUUID().toString();
            azureBlobService.postText(uniqueId, uploadText);
        } catch (Exception e) {
            throw new WorkflowException(e);
        } finally {

        }
        return uniqueId;
    }

    private void handleDocumentArrayList(UserMetaDataPersistenceContext userMetaDataPersistenceContext,  MetaDataMap metaDataMap, String key, JsonObject uploadJson, Map<String, Document> documentsToBeUploaded) throws Exception{
        Document[] docs = metaDataMap.get(key, Document[].class);
        JsonArray attachmentsArray = new JsonArray();
        for(int i =0; i<docs.length;i++) {
            JsonObject attachmentJsonObject = new JsonObject();
            if (docs[i].getAttribute(ExternalizerConstants.FILE_NAME_ATTRIBUTE_DOCUMENT) != null) {
                attachmentJsonObject.addProperty(ExternalizerConstants.FILE_NAME_ATTRIBUTE_DOCUMENT, (String) docs[i].getAttribute(ExternalizerConstants.FILE_NAME_ATTRIBUTE_DOCUMENT));
            }
            if (docs[i].getAttribute(ExternalizerConstants.FILE_ATTACHMENT_MAP) != null) {
                attachmentJsonObject.addProperty(ExternalizerConstants.FILE_ATTACHMENT_MAP, (String) docs[i].getAttribute(ExternalizerConstants.FILE_ATTACHMENT_MAP));
            }
            if(docs[i].getAttribute(ExternalizerConstants.DOC_SOURCE_URL) == null) {
                String blobId = UserMetaDataUtils.getBlobIdPrefix(userMetaDataPersistenceContext) + "/" + UUID.randomUUID().toString();
                documentsToBeUploaded.put(blobId, docs[i]);
                String sasURL = azureBlobService.getSaSUrl(blobId);
                if (sasURL != null) {
                    attachmentJsonObject.addProperty(ExternalizerConstants.DOC_SOURCE_URL, sasURL);
                }
            } else {
                attachmentJsonObject.addProperty(ExternalizerConstants.DOC_SOURCE_URL, (String) docs[i].getAttribute(ExternalizerConstants.DOC_SOURCE_URL));
            }
            attachmentsArray.add(attachmentJsonObject.toString());
        }
        uploadJson.add(key, attachmentsArray);
    }

    private void handleDocument(MetaDataMap metaDataMap, String key, String blobId, JsonObject uploadJson, Map<String, Document> documentsToBeUploded) throws Exception{
        Document document = metaDataMap.get(key, Document.class);
        JsonObject documentJsonObject = new JsonObject();
        if(document.getAttribute(ExternalizerConstants.DOC_SOURCE_URL) == null) {
            documentsToBeUploded.put(blobId, document);
            String sasURL = azureBlobService.getSaSUrl(blobId);
            if (sasURL != null) {
                documentJsonObject.addProperty(ExternalizerConstants.DOC_SOURCE_URL, sasURL);
            }
        } else {
            documentJsonObject.addProperty(ExternalizerConstants.DOC_SOURCE_URL, (String) document.getAttribute(ExternalizerConstants.DOC_SOURCE_URL));
        }
        uploadJson.addProperty(key, documentJsonObject.toString());
    }

    private void handleStringArrayList(Object value, JsonObject uploadJson, String key) {
        JsonArray jsonArray = new JsonArray();
        if (value instanceof Object[]) {
            Object[] values = (Object[]) value;
            if (values != null) {
                for (Object val : values) {
                    jsonArray.add(val.toString());
                }
            }
        }
        uploadJson.add(key, jsonArray);
    }
}
