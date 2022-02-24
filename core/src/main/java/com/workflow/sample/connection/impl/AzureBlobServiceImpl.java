package com.workflow.sample.connection.impl;

import com.adobe.aemfd.docmanager.Document;
import com.adobe.granite.workflow.WorkflowException;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.Constants;
import com.microsoft.azure.storage.blob.*;
import com.workflow.sample.connection.service.AzureBlobConfiguration;
import com.workflow.sample.connection.service.AzureBlobService;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.EnumSet;

@Component(
        immediate = true,
        service = AzureBlobService.class)
@Designate(
        ocd = AzureBlobConfiguration.class
)
public class AzureBlobServiceImpl implements AzureBlobService {

    private static Logger log = LoggerFactory.getLogger(AzureBlobServiceImpl.class);

    private static String storageConnectionString = "";

    private static String containerNameInService = "";

    private static String accountNameInService = "";

    private static String accountKeyInService = "";

    private static String endpointSuffixInService = "";

    private static String defaultProtocolInService = "";

    @Modified
    @Activate
    protected void activate(AzureBlobConfiguration azureBlobConfiguration) {
        this.accountNameInService = azureBlobConfiguration.accountName();
        this.accountKeyInService = azureBlobConfiguration.accountKey();
        this.defaultProtocolInService = azureBlobConfiguration.protocol();
        this.endpointSuffixInService = azureBlobConfiguration.endpointSuffix();
        this.containerNameInService = azureBlobConfiguration.containerName();
        this.storageConnectionString = String.join(";", "DefaultEndpointsProtocol=" + defaultProtocolInService, "AccountName=" + accountNameInService, "AccountKey=" + accountKeyInService, "EndpointSuffix=" + endpointSuffixInService);
    }

    public void postText(String blobId, String uploadText) throws WorkflowException {
        CloudStorageAccount storageAccount;
        CloudBlobClient blobClient = null;
        CloudBlobContainer container=null;
        try {
            if (StringUtils.isNotBlank(storageConnectionString)) {
                storageAccount = CloudStorageAccount.parse(storageConnectionString);
                blobClient = storageAccount.createCloudBlobClient();
                container = blobClient.getContainerReference(containerNameInService);
                CloudBlockBlob blob = container.getBlockBlobReference(blobId);
                blob.uploadText(uploadText, Constants.UTF8_CHARSET, null, null, null);
            } else {
                throw new WorkflowException("Cannot make connection to azure storage");
            }
        } catch (Exception e) {
            log.error("Unable to post text to blob, {}", blobId, e);
            throw new WorkflowException(e);
        }
    }

    public void postAttachment(String blobId, Document document) throws WorkflowException {
        try {
            CloudStorageAccount storageAccount;
            CloudBlobClient blobClient = null;
            CloudBlobContainer container=null;
            if(StringUtils.isNotBlank(storageConnectionString)) {
                storageAccount = CloudStorageAccount.parse(storageConnectionString);
                blobClient = storageAccount.createCloudBlobClient();
                container = blobClient.getContainerReference(containerNameInService);
                CloudBlockBlob blob = container.getBlockBlobReference(blobId);
                blob.upload(document.getInputStream(), -1, null, null, null);
            } else {
                throw new WorkflowException("Cannot make connection to azure storage");
            }
        } catch (Exception e) {
            log.error("Unable to post attachment, {}", blobId, e);
            throw new WorkflowException(e);
        }
    }

    public String getSaSUrl(String blobId) throws WorkflowException {
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference(containerNameInService);
            CloudBlockBlob blob = container.getBlockBlobReference(blobId);
            Date expirationTime = Date.from(LocalDateTime.now().plusDays(7).atZone(ZoneOffset.UTC).toInstant());
            SharedAccessBlobPolicy sharedAccessPolicy = new SharedAccessBlobPolicy();
            sharedAccessPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ,
                    SharedAccessBlobPermissions.WRITE, SharedAccessBlobPermissions.ADD));
            sharedAccessPolicy.setSharedAccessStartTime(new Date());
            sharedAccessPolicy.setSharedAccessExpiryTime(expirationTime);
            String token = blob.generateSharedAccessSignature(sharedAccessPolicy, null);
            return defaultProtocolInService + "://" + accountNameInService + ".blob." + endpointSuffixInService + "/" + containerNameInService + "/" + blobId + "?" + token;
        } catch (Exception e) {
            log.error("Unable to obtain SaS URL for blob, {}", blobId, e);
            throw new WorkflowException(e);
        }
    }

    public String downloadBlobJson(String blobId) throws WorkflowException {
        CloudStorageAccount storageAccount;
        CloudBlobClient blobClient = null;
        CloudBlobContainer container=null;
        String downloadedJson = null;
        try {
            if (StringUtils.isNotBlank(storageConnectionString)) {
                storageAccount = CloudStorageAccount.parse(storageConnectionString);
                blobClient = storageAccount.createCloudBlobClient();
                container = blobClient.getContainerReference(containerNameInService);
                CloudBlockBlob blob = container.getBlockBlobReference(blobId);
                downloadedJson = blob.downloadText(Constants.UTF8_CHARSET, null, null, null);
            }
        } catch (Exception e) {
            log.error("Unable to download blob json for blob {}", blobId, e);
            throw new WorkflowException(e);
        }
        return downloadedJson;
    }
}
