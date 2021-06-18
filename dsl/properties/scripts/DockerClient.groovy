/**
 * Docker API client
 */

@Grab(group='ch.qos.logback', module='logback-classic', version='1.2.0')
@Grab(group='org.apache.commons', module='commons-compress', version='1.20')
@Grab(group='com.squareup.okhttp3', module='okhttp', version='3.10.0')
@Grab("de.gesellix:docker-client:2018-01-26T21-28-05")
// @Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')
// @GrabExclude(group='org.codehaus.groovy', module='groovy', version='2.4.11')
// @GrabExclude('org.apache.commons:commons-compress')
@GrabExclude('net.sf.json-lib:json-lib')
@GrabExclude('commons-beanutils:commons-beanutils')
@GrabExclude('commons-collections:commons-collections')
@GrabExclude('org.codehaus.groovy.modules.http-builder:http-builder')
@GrabExclude(group='org.codehaus.groovy', module='groovy')
@GrabExclude(group='org.codehaus.groovy', module='groovy-json')
@GrabExclude(group='org.slf4j', module='slf4j-api')

import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.DockerClientException
import de.gesellix.docker.client.image.BuildConfig
import de.gesellix.docker.compose.ComposeFileReader
import de.gesellix.docker.compose.types.ComposeConfig
import groovy.json.JsonBuilder

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

        if (pluginConfig.credential?.password){
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

    def createNetwork(def serviceDetails){

        def networkList = getNetworkList(serviceDetails)
        def subnetList = getServiceParameter(serviceDetails, "subnetList", "").split(",")
        def gatewayList = getServiceParameter(serviceDetails, "gatewayList", "").split(",")

        // For each network in list
        for(int i=0; i<networkList.size(); i++){

            def networkName = networkList[i]

            if(!findNetwork(networkName)){

                // If network does not exists already, create one.
                logger INFO, "Creating $networkName network."

                def config = []
                def payload
                try{
                    def subnets = subnetList[i].split("\\|")
                    def gateways = gatewayList[i].split("\\|")

                    for(int j=0;j<subnets.size();j++){

                        def networkConfig = [:]
                        try{
                            def subnet = subnets[j].trim()
                            def gateway = gateways[j].trim()

                            if(subnet != ""){
                                networkConfig["Subnet"] = subnet
                            }

                            if(gateway != ""){
                                networkConfig["Gateway"] = gateway
                            }

                            // If atleast one of the config parameter is given
                            // then add them to network config. Adding an empty
                            // value causes docker to pick empty values and
                            // prevents it from supplying default values.
                            if(subnet != "" || gateway != ""){
                                config << networkConfig
                            }
                        }catch(ArrayIndexOutOfBoundsException e){

                            // If gateway not defined for a given subnet
                            config << [
                                "Subnet":subnets[j].trim()
                            ]
                        }
                    }

                    }catch(ArrayIndexOutOfBoundsException e){

                        // if no subnet and gateway provided for last network in list
                        // no action required.
                    }

                if (standAloneDockerHost()){

                    // Create user defined bridge network
                    payload = [
                            Driver: "bridge",
                            "IPAM": [
                                    "Driver": "default"
                            ],
                            "Scope": "local"
                    ]

                    // Add config parameter only if it is defined
                    if(config.size()!=0){
                        payload["IPAM"]["Config"] = config
                    }
                }else{

                    // Create user defined overlay network
                    payload = [
                            Driver: "overlay",
                            "IPAM": [
                                    "Driver": "default"
                            ],
                            "Scope": "swarm"
                    ]

                    // Add config parameter only if it is defined
                    if(config.size()!=0){
                        payload["IPAM"]["Config"] = config
                    }
                }

                def response = dockerClient.createNetwork(networkName, payload)
                logger INFO, "Created network $networkName."
            } else {

                logger INFO, "Network $networkName already exists."
            }
        }
    }

    def createOrUpdateService(String clusterEndPoint,  def serviceDetails) {

        if (OFFLINE) return null

        String serviceName = getServiceNameToUseForDeployment(serviceDetails)

        // Create network if does not exists
        createNetwork(serviceDetails)

        if (standAloneDockerHost()){
            // Given endpoint is not a Swarm manager. Deploy Flow service as a container.
            def deployedContainer = getContainer(serviceName)

            if(deployedContainer){
               // Remove the container if it is already deployed, in order to update it.
                undeployDockerService(serviceName)
            }

            def (containerDefinition,encodedAuthConfig) = buildContainerPayload(serviceDetails)
            def (imageName,tag) = getContainerImage(serviceDetails)
            logger INFO, "Payload to create container $serviceName:\n $containerDefinition \n with image: $imageName, tag: $tag"
            def response = dockerClient.run(imageName, containerDefinition, tag, serviceName, encodedAuthConfig)
            logger INFO, "Created Container $serviceName. Response: $response"
            attachAdditionalNetworks(serviceName, serviceDetails)

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

    def findNetwork(name){
        return dockerClient.networks().content.find { it.Name == name }
    }

    /**
     * Retrieves the Service instance from docker swarm cluster.
     * Returns null if no Service instance by the given name is found.
     */
    def getService(String serviceName) {

        if (OFFLINE) return null

        try{
            def serviceSpec = dockerClient.inspectService(serviceName).content
            if (serviceSpec) {
                logger DEBUG, "Service information obtained for '$serviceName' from the Docker swarm cluster: " + JsonOutput.prettyPrint(JsonOutput.toJson(serviceSpec))
            }
            serviceSpec
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
            def containerSpec = dockerClient.inspectContainer(serviceName)?.content
            if (containerSpec) {
                logger DEBUG, "Container information obtained for '$serviceName' from the Docker engine: " + JsonOutput.prettyPrint(JsonOutput.toJson(containerSpec))
            }
            containerSpec
        } catch(Exception e){
             logger INFO, "Container '$serviceName' not found."
             return null
        }

    }

    /**
     * Retrieves the container id from docker engine.
     * Returns null if no container instance by the given name is found.
     */
    def getContainerId(String serviceName) {

        // do not rely on docker ps name:<name> as it does not do an exact match
        // so all containers with names containing the <name> will be retrieved
        //def containers = dockerClient.ps([name:serviceName]).content
        def containerInfo = getContainer(serviceName)
        containerInfo?.Id
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

        if(container.credentialName){
            encodedAuthConfig = buildAuthConfig(container)
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
        int updateParallelism = args.minCapacity? replicas - args.minCapacity.toInteger() : replicas - 1

        def networkList = getNetworkList(args)

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

            if(networkList.size()>0){
                def networks = []
                for(network in networkList){
                    networks << [
                                    "Target": network
                                ]
                }
                hash["TaskTemplate"]["Networks"] = networks
            }

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


    def buildImage(String tag, InputStream tar) {
        def response = dockerClient.build(tar, new BuildConfig(query: [t: tag]))
        logger INFO, "Created image $tag.\n"
        return response
    }

    def pushImage(String name, String auth, String registry) {
        def response = dockerClient.push(name, auth, registry)
        logger INFO, "Pushed image $name. Response $response\n"
        return response
    }

    def removeImage(String imageId) {
        def response = dockerClient.rmi(imageId)
        logger INFO, "Removed image ${imageId}. Response ${response}."
        return response
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

    def buildAuthConfig(def container) {
        EFClient efClient = new EFClient()
        def cred = efClient.getCredentials(container.credentialName)
        def authConfig = [ username: cred.userName,
                           password: cred.password]
        if (container.registryUri) {
            authConfig << [serveraddress: container.registryUri]
        }
        JsonBuilder authConfigJson = new JsonBuilder(authConfig)
        authConfigJson.toString().bytes.encodeBase64().toString()
    }

    def buildContainerPayload(Map args){

        String serviceName = getServiceNameToUseForDeployment(args)

        def container = args.container[0]
        def encodedAuthConfig

        if(container.credentialName){
            encodedAuthConfig = buildAuthConfig(container)
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
           cpuCount = convertCpuToNanoCpu(container.cpuCount.toFloat())
        }

        def memoryReservation
        if (container.memorySize) {
           memoryReservation = convertMBsToBytes(container.memorySize.toFloat())
        }

        def networkList = getNetworkList(args)

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

        // Deploy the container in first network in the list
        // Later connect it to rest of the networks.
        // Refer: https://github.com/moby/moby/issues/29265
        if(networkList.size()>0){

            hash["NetworkingConfig"] = [
                "EndpointsConfig": [
                    (networkList[0]): [:]
                ]
            ]
        }

        return [hash,encodedAuthConfig]
    }

    def getServiceParameter(Map args, String parameterName, def defaultValue = null) {
        def result = args.parameterDetail?.find {
            it.parameterName == parameterName
        }?.parameterValue

        // expand any property references in the service parameter if required
        if (result && result.toString().contains('$[')) {
            EFClient efClient = new EFClient()
            result = efClient.expandString(result.toString())
        }

        // not using groovy truthiness so that we can account for 0
        return result != null && result.toString().trim() != '' ? result.toString().trim() : defaultValue
    }

    def getServiceNameToUseForDeployment (def serviceDetails) {
        formatName(getServiceParameter(serviceDetails, "serviceNameOverride", serviceDetails.serviceName))
    }

    static def readCompose(File composeFile) {
        def composeStream = new FileInputStream(composeFile)
        String workingDir = composeFile.parent
        ComposeFileReader composeFileReader = new ComposeFileReader()
        ComposeConfig composeConfig = composeFileReader.load(composeStream, workingDir, System.getenv())
        composeConfig
    }

    def getNetworkList(def serviceDetails){
        def formattedNetworkList = []
        def networkList = getServiceParameter(serviceDetails, "networkList", "").split(",")
        for(network in networkList){
            if(network != ""){
                formattedNetworkList << formatName(network)
            }
        }
        formattedNetworkList
    }

    def attachAdditionalNetworks(def container, def serviceDetails){
        def networkList = getNetworkList(serviceDetails)

        for(int i=0;i<networkList.size();i++){
            try{
                   dockerClient.connectNetwork(networkList[i], container)
                }catch(DockerClientException e){
                    // If network is already attached, move on to next network.
                    if(e.toString() =~ /docker network connect failed/){
                        logger INFO, "${container} already attached to ${networkList[i]}"
                    }else{
                        throw e
                    }
                }
        }
    }

    def createIngress(def networkName,
                         def subnetList,
                         def gatewayList,
                         def enableIpv6,
                         def mtu,
                         def labels){

        if(!standAloneDockerHost()){

            if(!findNetwork(networkName)){

                def payload = buildNetworkPayload(subnetList,
                                  gatewayList,
                                  enableIpv6,
                                  mtu,
                                  labels)
                dockerClient.createNetwork(networkName, payload)
            }else{
                logger ERROR, "${networkName} network already exists"
            }

        }else{
            logger ERROR, "Can not create ingress network on stand-alone Docker engine."
        }
    }

    def buildNetworkPayload( def subnetList,
                             def gatewayList,
                             def enableIpv6,
                             def mtu,
                             def labels){

        def config = []

        for(int i=0;i<subnetList.size();i++){

            def networkConfig = [:]
            try{

                if(subnetList[i] != ""){
                    networkConfig["Subnet"] = subnetList[i]
                }

                if(gatewayList[i] != ""){
                    networkConfig["Gateway"] = gatewayList[i]
                }

                // If atleast one of the config parameter is given
                // then add them to network config. Adding an empty
                // value causes docker to pick empty values and
                // prevents it from supplying default values.
                if(subnetList[i] != "" || gatewayList[i] != ""){
                    config << networkConfig
                }
            }catch(ArrayIndexOutOfBoundsException e){

                // If gateway not defined for a given subnet
                config << [
                    "Subnet":subnetList[i]
                ]
            }
        }

        // Parse labels string to map
        def labelsMap = [:]
        def labelsList = labels.split(",")

        for(label in labelsList){
            if(label!=""){
                 def key = label.split("=")[0]
                 def value = label.split("=")[1]
                 labelsMap[(key)] = value
            }
        }

         def payload  = [
                    Driver: "overlay",
                    "IPAM": [
                            "Driver": "default"
                    ],
                    "Scope": "swarm",
                    "Ingress": true,
                    "Internal": "false".toBoolean(),
                    "EnableIPv6": enableIpv6.toBoolean(),
                    "Options":[
                        "com.docker.network.mtu": mtu
                    ],
                    "Labels":labelsMap
                ]

        // Add config parameter only if it is defined
        if(config.size()!=0){
            payload["IPAM"]["Config"] = config
        }

        payload
    }

    def deleteNetwork(def networkName){
        dockerClient.rmNetwork(networkName)
    }
}
