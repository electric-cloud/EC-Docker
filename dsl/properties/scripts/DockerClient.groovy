/**
 * Docker API client
 */

@Grab("de.gesellix:docker-client:2017-07-02T23-30-20")
@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')

import de.gesellix.docker.client.image.ManageImage
import de.gesellix.docker.client.DockerClientImpl

public class DockerClient extends BaseClient {

    def dockerClient
    def pluginConfig

    DockerClient(def pluginConfiguration){

        pluginConfig = pluginConfiguration
        def homeDir = System.getProperty('user.home')
        def pathSeparator = File.separator
        def certDir = "${homeDir}${pathSeparator}.docker${pathSeparator}cert"
        System.setProperty("docker.cert.path","${certDir}")

        if (pluginConfig.credential_key.password){
            // If docker client private key is provided in plugin config then enable TLS mode
            System.setProperty("docker.tls.verify", "1")
        }
        dockerClient = new DockerClientImpl(pluginConfig.endpoint)
        
    }


   /*   Function exits with return value 1 if docker endpoint specified
    *   in plugin configuration is not reachable. In case of swarm mode,
    *   whether the endpoint is swarm manager and is in active state or
    *   not is checked.
    */
    def checkHealth(){


            try{
                def info = dockerClient.info().content
                logger DEBUG, "${info}"

            }catch(Exception e){
                    // Given node is not a swarm manager
                    logger ERROR, "${e}"
                    logger ERROR, "${pluginConfig.endpoint} is not Swarm Manager. Exiting.."
                    exit 1
            }
    }

     def getPluginConfig(EFClient efClient, String clusterName, String clusterOrEnvProjectName, String environmentName) {

        def clusterParameters = efClient.getProvisionClusterParameters(
                clusterName,
                clusterOrEnvProjectName,
                environmentName)

        def configName = clusterParameters.config
        def pluginProjectName = '$[/myProject/projectName]'
        efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)
    }

    def deployService(
            EFClient efClient,
            String clusterEndpoint,
            String serviceName,
            String serviceProjectName,
            String applicationName,
            String applicationRevisionId,
            String clusterName,
            String clusterOrEnvProjectName,
            String environmentName,
            String resultsPropertySheet){

       
        def serviceDetails = efClient.getServiceDeploymentDetails(
                serviceName,
                serviceProjectName,
                applicationName,
                applicationRevisionId,
                clusterName,
                clusterOrEnvProjectName,
                environmentName)

        createOrUpdateService(clusterEndpoint, serviceDetails)

        /*
        
        def serviceEndpoint = getDeployedServiceEndpoint(clusterEndpoint, namespace, serviceDetails, accessToken)

        if (serviceEndpoint) {
            serviceDetails.port?.each { port ->
                String portName = port.portName
                String url = "${serviceEndpoint}:${port.listenerPort}"
                efClient.createProperty("${resultsPropertySheet}/${serviceName}/${portName}/url", url)
            }
        }
        */
    }

    def createOrUpdateService(String clusterEndPoint,  def serviceDetails) {

        if (OFFLINE) return null

        String serviceName = formatName(serviceDetails.serviceName)

        def deployedService = getService(clusterEndPoint, serviceName)
        def deployedServiceSpec = deployedService?.Spec
        def deployedServiceVersion = deployedService?.Version?.Index
        
        def (serviceDefinition,encodedAuthConfig) = buildServicePayload(serviceDetails, deployedServiceSpec)

        if(deployedService){
            logger INFO, "Updating deployed service $serviceName"

            def response
            if(encodedAuthConfig){
                // For private docker registries 
                // encodedAuthConfig will be passed as "X-Registry-Auth" header
                response = dockerClient.updateService(serviceName, [version: deployedServiceVersion], serviceDefinition, [EncodedRegistryAuth: encodedAuthConfig])
            } else {
                response = dockerClient.updateService(serviceName, [version: deployedServiceVersion], serviceDefinition)
            }

            logger INFO, "Created Service $serviceName. Response: $response\nWaiting for service to start..."
            def service = awaitServiceStarted(serviceName, 5000)
            if(service){
                logger INFO, "Service $serviceName started successfully."
            }else{
                logger ERROR, "Service start timed out."
                exit 1
            }
            

        } else {

            logger INFO, "Creating service $serviceName"
            
            def response
            if(encodedAuthConfig){
                // For private docker registries 
                // encodedAuthConfig will be passed as "X-Registry-Auth" header
                response = dockerClient.createService(serviceDefinition, [EncodedRegistryAuth: encodedAuthConfig])
            } else {
                response = dockerClient.createService(serviceDefinition)
            }

            logger INFO, "Created Service $serviceName. Response: $response\nWaiting for service to start..."
            def service = awaitServiceStarted(serviceName, 5000)
            if(service){
                logger INFO, "Service $serviceName started successfully."
            }else{
                logger ERROR, "Service start timed out."
                exit 1
            }
        }
    }

    def awaitServiceStarted(def name,def timeout) {
        def service = null
        def timespent = 0 
        while (service == null && timespent<timeout) {
            service = findService(name)
            if (service) {
                break
            }
            else {
                sleep(1000)
                timespent += 1000
            }
        }  
        return service
    }

    def findService(name) {
        def services = dockerClient.services().content
        return services.find { it.Spec.Name == name }
    }

    /**
     * Retrieves the Service instance from docker swarm cluster.
     * Returns null if no Service instance by the given name is found.
     */
    def getService(String clusterEndPoint, String serviceName) {

        if (OFFLINE) return null
        
        try{
            def serviceSpec = dockerClient.inspectService(serviceName).content
            }catch(Exception e){
                 logger INFO, "Service $serviceName not found."
                 return null
            }

    }

     Object doHttpGet(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true) {

        doHttpRequest(GET,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode)
    }

    def buildServicePayload(Map args, def deployedService){

        def serviceName = formatName(args.serviceName)
        def container = args.container[0]
        def imageName = "${container.imageName}:${container.imageVersion?:'latest'}"
        def encodedAuthConfig

        //Prepend the registry to the imageName
        //if it does not already include it.
        if (container.registryUri) {
            if (!imageName.startsWith("${container.registryUri}/")) {
                imageName = "${container.registryUri}/$imageName"
            }
        }

        if(container.credentialName && container.registryUri){
            EFClient efClient = new EFClient()
            def cred = efClient.getCredentials(container.credentialName)
            def authConfig = "{\"username\":\"${cred.userName}\",\"password\": \"${cred.password}\",\"serveraddress\": \"${container.registryUri}\"}"
            encodedAuthConfig = authConfig.bytes.encodeBase64().toString()
        }

        def mounts = [:]
        mounts= (parseJsonToList(container.volumeMounts)).collect { mount ->
                                    [
                                        Source: formatName(mount.name),
                                        Target: mount.mountPath
                                    ]

                    }

        def env = [:]
        env = container.environmentVariable?.collect { envVar ->
                   
                   "${envVar.environmentVariableName}=${envVar.value}"

                }
        def limits = [:]
        if (container.cpuLimit) {
           limits.NanoCPUs= convertCpuToNanoCpu(container.cpuLimit.toFloat())
        }
        if (container.memoryLimit) {
           limits.MemoryBytes= convertMBsToBytes(container.memoryLimit.toFloat())
        }
           
        def reservation = [:]
        if (container.cpuCount) {
           reservation.NanoCPUs= convertCpuToNanoCpu(container.cpuCount.toFloat())
        }
        if (container.memorySize) {
           reservation.MemoryBytes= convertMBsToBytes(container.memorySize.toFloat())
        }

        def hash=[
                    "name": serviceName,
                    "TaskTemplate": [
                        "ContainerSpec": [

                            "Image": imageName,
                            "Command":container.entryPoint?.split(','),
                            "Args":container.command?.split(','),
                            "Mounts": mounts,
                            "Env": env

                        ],
                        "Resources":[
           
                            "Limits":limits,
                            "Reservation":reservation
                        ]
                    ],
                    "EndpointSpec": [
                        "ports" : args.port.collect { servicePort ->
                                    
                                    def targetPort

                                    for (containerPort in container.port) {

                                        if (containerPort.portName == servicePort.subport) {
                                            targetPort = containerPort.containerPort
                                            break
                                        }
                                    }      
                                    
                                    def portMapping = [:]
                                    portMapping.PublishedPort=servicePort.listenerPort.toInteger()
                                    portMapping.TargetPort=targetPort.toInteger()
                                    portMapping
                            }          
                        ]
                ]
            
            def payload = deployedService
            if (payload) {
                payload = mergeObjs(payload, hash)
            } else {
                payload = hash
            }
            return [payload,encodedAuthConfig]
    }

    def convertCpuToNanoCpu(float cpu) {
        return cpu * 1000000000 as int
    }

    def convertMBsToBytes(float mbs) {
        return mbs * 1048576 as int
    }

    def deleteService(String clusterEndPoint,  def serviceName) {
        serviceName = formatName(serviceName)
        def response = dockerClient.rmService(serviceName)
        logger INFO, "Deleted Service $serviceName. Response: $response\nWaiting for service cleanup..."
        def service = awaitServiceRemoved(serviceName, 5000)
        if(service == null ){
            logger INFO, "Service $serviceName cleaned up successfully."
        }else{
            logger ERROR, "Service clean up timed out."
            exit 1
        }
    }

    def awaitServiceRemoved(def name,def timeout) {
        def service = findService(name)
        def timespent = 0 
        while (service != null && timespent<timeout) {
            service = findService(name)
            if (service == null) {
                break
            }
            else {
                sleep(1000)
                timespent += 1000
            }
        }  
        return service
    }

}
 