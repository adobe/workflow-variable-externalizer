# Forms UserMetadata External Persistence Provider
This repository hosts a sample implementation of the workflow API UserMetaDataPersistenceProvider that is used to store workflow variables in a customer owned/managed Azure blob storage.

The sample should be considered as a guide to store variables from workflow metadata map to any external storage of choice.

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install
    
To deploy the bundle and the content package to author, run
    
    mvn clean install -PautoInstallPackage
    
Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle
    
### How to use

To use the sample, it is required to initialize the properties like Azure storage account, account key, protocol, container name in the externalizer OSGI configuration file in the ui.config content package. 
    
### Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

