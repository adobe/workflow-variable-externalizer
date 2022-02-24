package com.workflow.sample.connection.service;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Forms Workflow Externalization Azure Configuration",
        description = "Forms Workflow Externalization Azure Configuration."
)
public @interface AzureBlobConfiguration {

    @AttributeDefinition(
            name = "accountName",
            description = "Azure storage account name",
            type = AttributeType.STRING
    )
    String accountName();

    @AttributeDefinition(
            name = "accountKey",
            description = "Azure storage account key",
            type = AttributeType.PASSWORD
    )
    String accountKey();

    @AttributeDefinition(
            name = "protocol",
            description = "Azure storage account protocol",
            type = AttributeType.STRING
    )
    String protocol();

    @AttributeDefinition(
            name = "endpointSuffix",
            description = "Azure storage account endpoint suffix",
            type = AttributeType.STRING
    )
    String endpointSuffix();

    @AttributeDefinition(
            name = "containerName",
            description = "Azure storage container name",
            type = AttributeType.STRING
    )
    String containerName();
}
