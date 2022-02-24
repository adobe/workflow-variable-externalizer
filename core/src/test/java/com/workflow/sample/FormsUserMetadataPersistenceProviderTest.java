/*************************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Adobe and its suppliers, if any. The intellectual
 * and technical concepts contained herein are proprietary to Adobe
 * and its suppliers and are protected by all applicable intellectual
 * property laws, including trade secret and copyright laws.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe.
 **************************************************************************/
package com.workflow.sample;

import com.adobe.aemfd.docmanager.Document;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.metadata.storage.UserMetaDataPersistenceContext;
import com.adobe.granite.workflow.model.VariableTemplate;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.workflow.sample.connection.service.AzureBlobService;
import org.junit.Test;
import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;



public class FormsUserMetadataPersistenceProviderTest {


    private ModifiedFormsUserMetadataPersistenceProvider fixture = new ModifiedFormsUserMetadataPersistenceProvider();

    private TestLogger logger = TestLoggerFactory.getTestLogger(fixture.getClass());


    private UserMetaDataPersistenceContext mockMetaDataPersistenceContext() {
        return new UserMetaDataPersistenceContext() {
            @Override
            public Workflow getWorkflow() {
                Workflow workflow = Mockito.mock(Workflow.class);
                WorkflowModel workflowModel = Mockito.mock(WorkflowModel.class);
                Mockito.when(workflow.getWorkflowModel()).thenReturn(workflowModel);

                VariableTemplate variableTemplateFormDataXML = Mockito.mock(VariableTemplate.class);
                Mockito.when(variableTemplateFormDataXML.getType()).thenReturn("org.w3c.dom.Document");
                VariableTemplate variableTemplateFormDataJson = Mockito.mock(VariableTemplate.class);
                Mockito.when(variableTemplateFormDataJson.getType()).thenReturn("com.google.gson.JsonObject");
                VariableTemplate variableTemplateFormDataDocument = Mockito.mock(VariableTemplate.class);
                Mockito.when(variableTemplateFormDataDocument.getType()).thenReturn("com.adobe.aemfd.docmanager.Document");
                VariableTemplate variableTemplateAttachments = Mockito.mock(VariableTemplate.class);
                Mockito.when(variableTemplateAttachments.getSubType()).thenReturn("com.adobe.aemfd.docmanager.Document");
                Mockito.when(variableTemplateAttachments.getType()).thenReturn("java.util.ArrayList");

                Map<String, VariableTemplate> variableTemplateMap = new HashMap<>();
                variableTemplateMap.put("formDataXml", variableTemplateFormDataXML);
                variableTemplateMap.put("formDataJson", variableTemplateFormDataJson);
                variableTemplateMap.put("formDataDocument", variableTemplateFormDataDocument);
                variableTemplateMap.put("attachments", variableTemplateAttachments);

                Mockito.when(workflowModel.getVariableTemplates()).thenReturn(variableTemplateMap);

                return workflow;
            }

            @Override
            public String getWorkflowId() {
                return "/var/workflow/instances/workflowId";
            }

            @Override
            public String getUserDataId() {
                return getWorkflowId();
            }
        };
    }

    private MetaDataMap mockMetaDataMap() throws IOException {
        Set<String> keys = new HashSet<>();
        keys.add("formDataXml");
        keys.add("formDataJson");

        String dataXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<afData><afUnboundData><data><radiobutton>0</radiobutton><radiobutton_0>0</radiobutton_0><checkbox>0</checkbox><textbox>startpoint Data</textbox></data></afUnboundData><afSubmissionInfo><lastFocusItem>guide[0].guide1[0].guideRootPanel[0].panel1[0]</lastFocusItem><computedMetaInfo/><signers/><afPath>/content/dam/formsanddocuments/basic</afPath><afSubmissionTime>20170509051316</afSubmissionTime></afSubmissionInfo></afData>";
        String dataJson = "{\"afData\":{\"afUnboundData\":{\"data\":{\"fileupload1617856940171\":{\"fileAttachment\":\"CustomForm.png\"}}},\"afBoundData\":{\"data\":{\"eligible_offer1\":{\"Offer_Type\":\"10 Sec\",\"roi\":\"15\"}}},\"afSubmissionInfo\":{\"lastFocusItem\":\"guide[0].guide1[0].guideRootPanel[0].page[0]\",\"stateOverrides\":{},\"signers\":{},\"afPath\":\"/content/dam/formsanddocuments/output/personal-loan2\",\"afSubmissionTime\":\"20210409124149\"}}}";

        Map<String, Object> map = new HashMap<>();
        map.put("formDataXml", dataXML);
        map.put("formDataJson", dataJson);

        Map<String, Object> localMap = new HashMap<>();

        MetaDataMap metaDataMap = Mockito.mock(MetaDataMap.class);
        Mockito.when(metaDataMap.keySet()).thenReturn(keys);
        Mockito.when(metaDataMap.get("formDataXml")).thenReturn(map.get("formDataXml"));
        Mockito.when(metaDataMap.get("formDataXml", String.class)).thenReturn(map.get("formDataXml").toString());
        Mockito.when(metaDataMap.get("formDataJson")).thenReturn(map.get("formDataJson"));
        Mockito.when(metaDataMap.get("formDataJson", String.class)).thenReturn(map.get("formDataJson").toString());

        Mockito.doAnswer(invocation -> {
            String arg0 = invocation.getArgument(0);
            Object arg1 = invocation.getArgument(1);
            localMap.put(arg0, arg1);
            return arg0;
        }).when(metaDataMap).put(Mockito.any(String.class), Mockito.any());
        return metaDataMap;
    }


    @Test
    public void testGetUploadText() throws Exception{
        UserMetaDataPersistenceContext userMetaDataPersistenceContext = mockMetaDataPersistenceContext();
        MetaDataMap metaDataMap = mockMetaDataMap();
        Map<String, Document> documentsToBeUploaded = new HashMap();
        String uploadText = fixture.getUploadText(userMetaDataPersistenceContext, metaDataMap, documentsToBeUploaded);
        Assert.assertNotNull(uploadText);
    }

    @Test
    public void testPut() throws Exception{
        UserMetaDataPersistenceContext userMetaDataPersistenceContext = mockMetaDataPersistenceContext();
        MetaDataMap metaDataMap = mockMetaDataMap();
        fixture.put(userMetaDataPersistenceContext, metaDataMap);
    }

    @Test
    public void testGet() throws Exception{
        UserMetaDataPersistenceContext userMetaDataPersistenceContext = mockMetaDataPersistenceContext();
        MetaDataMap metaDataMap = mockMetaDataMap();
        fixture.get(userMetaDataPersistenceContext, metaDataMap);
    }

    class ModifiedFormsUserMetadataPersistenceProvider extends FormsUserMetaDataPersistenceProvider {
        ModifiedFormsUserMetadataPersistenceProvider() {
            super();
            azureBlobService = Mockito.mock(AzureBlobService.class);
            try {
                Mockito.when(azureBlobService.getSaSUrl(Mockito.anyString())).thenReturn("sasUrl");
                Mockito.doNothing().when(azureBlobService).postText(Mockito.anyString(), Mockito.anyString());
                Mockito.when(azureBlobService.downloadBlobJson(Mockito.anyString())).thenReturn("{\"attachments\":[],\"actionTaken\":\"Submit\",\"formDataXml\":\"<afData><afUnboundData><data><textbox1621842431438>hey syama</textbox1621842431438><switch1621842465582>0</switch1621842465582><textbox_10744701141621842449570>you</textbox_10744701141621842449570></data></afUnboundData><afBoundData><data xmlns:xfa=\\\"http://www.xfa.org/schema/xfa-data/1.0/\\\"/></afBoundData><afSubmissionInfo><lastFocusItem>guide[0].guide1[0].guideRootPanel[0].textbox1621842431438[0]</lastFocusItem><computedMetaInfo/><stateOverrides/><signers/></afSubmissionInfo></afData>\"}");
            } catch (WorkflowException e) {

            }
        }

        @Override
        protected String getUploadText(UserMetaDataPersistenceContext userMetaDataPersistenceContext, MetaDataMap metaDataMap, Map<String, Document> documentsToBeUploded) throws WorkflowException {
            return super.getUploadText(userMetaDataPersistenceContext, metaDataMap, documentsToBeUploded);
        }
    }
}
