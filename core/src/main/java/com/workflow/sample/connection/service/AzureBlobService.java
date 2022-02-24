package com.workflow.sample.connection.service;

import com.adobe.aemfd.docmanager.Document;
import com.adobe.granite.workflow.WorkflowException;

public interface AzureBlobService {

    void postText(String blobId, String uploadText) throws WorkflowException;

    void postAttachment(String blobId, Document document) throws WorkflowException;

    String getSaSUrl(String blobId) throws WorkflowException;

    String downloadBlobJson(String blobId) throws WorkflowException;
}
