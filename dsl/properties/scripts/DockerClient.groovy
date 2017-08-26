/**
 * Docker API client
 */

@Grab("de.gesellix:docker-client:2017-08-17T20-47-30")
@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')
@GrabExclude(group='org.codehaus.groovy', module='groovy', version='2.4.11')

import de.gesellix.docker.client.DockerClientImpl

public class DockerClient extends BaseClient {

    def dockerClient
    def pluginConfig
    def certificatesDir

    DockerClient(def pluginConfiguration){

        this.pluginConfig = pluginConfiguration

        def pathSeparator = File.separator
        def uniqueName = System.currentTimeMillis()
        def homeDir = System.getProperty('user.home')

        this.certificatesDir = "${homeDir}${pathSeparator}.docker${pathSeparator}cert${pathSeparator}${uniqueName}"
        File dir = new File(this.certificatesDir)
        dir.mkdirs()
        dir.deleteOnExit()

        if (pluginConfig.cacert) {
            File cacertFile = new File("${this.certificatesDir}${pathSeparator}ca.pem")
            cacertFile.text = pluginConfig.cacert
            cacertFile.deleteOnExit()
        }

        if (pluginConfig.cert) {
            File clientcertFile = new File("${this.certificatesDir}${pathSeparator}cert.pem")
            clientcertFile.text = pluginConfig.cert
            clientcertFile.deleteOnExit()
        }

        if (pluginConfig.credential?.password) {
            File clientkeyFile = new File("${this.certificatesDir}${pathSeparator}key.pem")
            clientkeyFile.text = pluginConfig.credential.password
            clientkeyFile.deleteOnExit()
        }

        System.setProperty("docker.cert.path","${this.certificatesDir}")

        if (pluginConfig.credential.password){
            // If docker client private key is provided in plugin config then enable TLS mode
            System.setProperty("docker.tls.verify", "1")
        }else{
            System.setProperty("docker.tls.verify", "")
        }
        
        dockerClient = new DockerClientImpl(pluginConfig.endpoint)
        
    }


    /**
     * Checks connection to the docker end-point, whether stand-alone
     * or Swarm.
     * Returns response object with following attributes:
     * o success: true|false
     * o code: success or error code from the DockerClient.
     * o text: success or error message.
     * Use the code and text for information/debugging purposes as
     * these may not be set by the DockerClient.
     */
    def checkConnection(){
        try{
            def status = dockerClient.info()?.status
            logger INFO, "DockerClient status: ${status}"
            if (status instanceof de.gesellix.docker.engine.EngineResponseStatus) {
                [
                        success: status.success,
                        code: status.code,
                        text: status.text
                ]
            } else {
                [
                        success: false,
                        text: "unknown reason"
                ]
            }
        } catch(Exception e){
            [
                    success: false,
                    text: "${e}"
            ]
        }
    }

    def static checkConnection(def pluginParams) {

        try {
            DockerClient dockerClient = new DockerClient(pluginParams)
            dockerClient.checkConnection()
        } catch(Exception e){
            [
                    success: false,
                    text: "${e}"
            ]
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

        if(serviceDetails.container.size() > 1){
            efClient.handleProcedureError("Services in Docker do not support multiple container images within one service. ${serviceDetails.container.size()} container definitions were found in the ElectricFlow service definition for '${serviceName}'.")
        }else{
            createOrUpdateService(clusterEndpoint, serviceDetails)
        }
        
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

    def undeployService(
            EFClient efClient,
            String clusterEndpoint,
            String serviceName,
            String serviceProjectName,
            String applicationName,
            String applicationRevisionId,
            String clusterName,
            String envProjectName,
            String environmentName){


        logger INFO, "Undeploying ElectricFlow service: ${serviceName} in service project:${serviceProjectName} application:${applicationName} cluster:${clusterName} environment project:${envProjectName} environment name:${environmentName} cluster endpoint:${clusterEndpoint}"

        def serviceDetails = efClient.getServiceDeploymentDetails(
                serviceName,
                serviceProjectName,
                applicationName,
                applicationRevisionId,
                clusterName,
                envProjectName,
                environmentName)

        if (OFFLINE) return null

        String deployedServiceName = getServiceNameToUseForDeployment(serviceDetails)
        undeployDockerService(deployedServiceName)

    }

    def undeployDockerService(def deployedServiceName) {
        if (standAloneDockerHost()) {
            logger INFO, "Removing container '$deployedServiceName' from standalone Docker host"
            //check if container exists, if found, stop and remove it
            def deployedContainerId = getContainerId(deployedServiceName)
            if (deployedContainerId) {
                dockerClient.stop(deployedContainerId)
                dockerClient.wait(deployedContainerId)
                dockerClient.rm(deployedContainerId)
            } else {
                logger INFO, "Nothing to do as no container named '$deployedServiceName' found on the standalone Docker host."
            }
        } else {
            //It is a Docker swarm cluster end-point.
            //Check if service exists
            logger INFO, "Undeploying service '$deployedServiceName' from Docker Swarm cluster"
            def deployedService = getService(deployedServiceName)
            if (deployedService) {
                deleteService(deployedServiceName)
            } else {
                logger INFO, "Nothing to do as no service named '$deployedServiceName' found on the Docker Swarm cluster."
            }
        }
    }

    boolean standAloneDockerHost() {
        dockerClient.info().content.Swarm.LocalNodeState == "inactive"
    }

    def createOrUpdateService(String clusterEndPoint,  def serviceDetails) {

        if (OFFLINE) return null

        String serviceName = getServiceNameToUseForDeployment(serviceDetails)

        if (standAloneDockerHost()){
            // Given endpoint is not a Swarm manager. Deploy Flow service as a container.
            def deployedContainer = getContainer(serviceName)

            if(deployedContainer){
                logger INFO, "Updating container $serviceName. Update of only following parameters is supported: \"Minimum CPU requested\", \"Maximum CPU allowed\", \"MemoryMemory\", \"Limit\"."
                def updateContainerDefinition = buildUpdateContainerPayload(serviceDetails)
                def response = dockerClient.updateContainer(serviceName, updateContainerDefinition)
                logger INFO, "Updated Container $serviceName. Response: $response"
            }else{
                def (containerDefinition,encodedAuthConfig) = buildContainerPayload(serviceDetails)
                def (imageName,tag) = getContainerImage(serviceDetails)
                def response = dockerClient.run(imageName, containerDefinition, tag, serviceName, encodedAuthConfig)
                logger INFO, "Created Container $serviceName. Response: $response"
            }
                  
        }else{
            // Given endpoint is a Swarm manager. Deploy Flow service as a swarm service.
            def deployedService = getService(serviceName)
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
                }
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
    def getService(String serviceName) {

        if (OFFLINE) return null
        
        try{
            def serviceSpec = dockerClient.inspectService(serviceName).content
            }catch(Exception e){
                 logger INFO, "Service $serviceName not found."
                 return null
            }

    }

    /**
     * Retrieves the container instance from docker engine.
     * Returns null if no container instance by the given name is found.
     */
    def getContainer(String serviceName) {

        if (OFFLINE) return null
        
        try{
            def containerSpec = dockerClient.inspectContainer(serviceName).content
            }catch(Exception e){
                 logger INFO, "Container $serviceName not found."
                 return null
            }

    }

    /**
     * Retrieves the container id from docker engine.
     * Returns null if no container instance by the given name is found.
     */
    def getContainerId(String serviceName) {

        def containers = dockerClient.ps([name:serviceName]).content
        containers?.find{it}?.Id
    }

     Object doHttpGet(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true) {

        doHttpRequest(GET,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode)
    }

    def getContainerImage(Map args){
        def container = args.container[0]

        def imageName = "${container.imageName}"
        def tag = "${container.imageVersion?:'latest'}"
        //Prepend the registry to the imageName
        //if it does not already include it.
        if (container.registryUri) {
            if (!imageName.startsWith("${container.registryUri}/")) {
                imageName = "${container.registryUri}/$imageName"
            }
        }
        return [imageName, tag]
    }

    def buildServicePayload(Map args, def deployedService){

        String serviceName = getServiceNameToUseForDeployment(args)

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

        def mounts = []
        for(mount in parseJsonToList(container.volumeMounts)){
            for(svcMount in parseJsonToList(args.volumes)){
                if(mount.name==svcMount.name){
                    if(svcMount.hostPath){
                        mounts << [
                                        Source: svcMount.hostPath,
                                        Target: mount.mountPath,
                                        Type: "bind"
                                  ]
                    }else{
                        mounts << [
                                        Source: formatName(mount.name),
                                        Target: mount.mountPath,
                                        Type: "volume"
                                  ]
                    }                 
                }
            }
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
 
        
        int replicas = args.defaultCapacity?args.defaultCapacity.toInteger():1
        
        int updateParallelism = args.minCapacity?args.minCapacity.toInteger():1
        
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
                    ],
                    "UpdateConfig": [
                        "Parallelism" : updateParallelism
                    ],
                    "Mode": [
                        "Replicated": [
                            "Replicas": replicas
                        ]
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

    def deleteService(def serviceName) {

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

    def buildUpdateContainerPayload(Map args){

        def container = args.container[0]

        def nanoCPUs
        if (container.cpuLimit) {
           nanoCPUs = convertCpuToNanoCpu(container.cpuLimit.toFloat())
        }

        def memoryLimit
        if (container.memoryLimit) {
           memoryLimit = convertMBsToBytes(container.memoryLimit.toFloat())
        }
           
        def cpuCount
        if (container.cpuCount) {
           cpuCount = container.cpuCount.toInteger()
        }

        def memoryReservation
        if (container.memorySize) {
           memoryReservation = convertMBsToBytes(container.memorySize.toFloat())
        }

        def hash=[
                    "Memory": memoryLimit,
                    "MemoryReservation": memoryReservation,
                    "NanoCPUs": nanoCPUs,
                    "cpuCount": cpuCount      
                ]
    }

    def buildContainerPayload(Map args){

        String serviceName = getServiceNameToUseForDeployment(args)

        def container = args.container[0]
        def encodedAuthConfig

        if(container.credentialName && container.registryUri){
            EFClient efClient = new EFClient()
            def cred = efClient.getCredentials(container.credentialName)
            def authConfig = "{\"username\":\"${cred.userName}\",\"password\": \"${cred.password}\",\"serveraddress\": \"${container.registryUri}\"}"
            encodedAuthConfig = authConfig.bytes.encodeBase64().toString()
        }

        def binds = []
        def volumes = [:]
        for(mount in parseJsonToList(container.volumeMounts)){
            for(svcMount in parseJsonToList(args.volumes)){
                if(mount.name==svcMount.name){
                    if(svcMount.hostPath){
                        binds << "${svcMount.hostPath}:${mount.mountPath}"
                    }else{
                        binds << "${formatName(mount.name)}:${mount.mountPath}"
                    }                 
                }
            }
            volumes[mount.mountPath] = [:]
        }

        def env = [:]
        env = container.environmentVariable?.collect { envVar ->
               
               "${envVar.environmentVariableName}=${envVar.value}"

            }

        def exposedPorts = [:]
        def port
        for (cPort in container.port){
            port = "${cPort.containerPort}/tcp"
            exposedPorts[port] = [:]
        }

        def portBindings = [:]
        def targetPort
        for (sPort in args.port ){
            for (cPort in container.port){      

                if (cPort.portName == sPort.subport) {
                    targetPort = "${cPort.containerPort}/tcp"
                    portBindings[targetPort] = [
                            ["HostPort": "${sPort.listenerPort}"]
                    ]      
                }
            }
        }      

        def nanoCPUs
        if (container.cpuLimit) {
           nanoCPUs = convertCpuToNanoCpu(container.cpuLimit.toFloat())
        }

        def memoryLimit
        if (container.memoryLimit) {
           memoryLimit = convertMBsToBytes(container.memoryLimit.toFloat())
        }
           
        def cpuCount
        if (container.cpuCount) {
           cpuCount = container.cpuCount.toInteger()
        }

        def memoryReservation
        if (container.memorySize) {
           memoryReservation = convertMBsToBytes(container.memorySize.toFloat())
        }

        def hash=[

                "Hostname": serviceName,
                "Entrypoint": container.entryPoint?.split(','),
                "Cmd": container.command?.split(','),
                "Env": env,
                "ExposedPorts": exposedPorts,
                "Volumes": volumes,
                "HostConfig": [
                        "Binds": binds,
                        "PortBindings": portBindings,
                        "Memory": memoryLimit,
                        "MemoryReservation": memoryReservation,
                        "NanoCPUs": nanoCPUs,
                        "cpuCount": cpuCount
                    ]
            ]
        
        return [hash,encodedAuthConfig]
       
    }

    def getServiceParameter(Map args, String parameterName, def defaultValue = null) {
        def result = args.parameterDetail?.find {
            it.parameterName == parameterName
        }?.parameterValue

        return result != null ? result : defaultValue
    }

    def getServiceNameToUseForDeployment (def serviceDetails) {
        formatName(getServiceParameter(serviceDetails, "serviceNameOverride", serviceDetails.serviceName))
    }
}
 