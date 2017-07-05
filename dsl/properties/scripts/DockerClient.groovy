/**
 * Docker API client
 */

@Grab("de.gesellix:docker-client:2017-06-25T15-38-14")
@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')

import de.gesellix.docker.client.image.ManageImage
import de.gesellix.docker.client.DockerClientImpl

public class DockerClient extends BaseClient {

    def dockerClient
    def pluginConfig

    DockerClient(def pluginConfiguration){

        pluginConfig = pluginConfiguration
        
        def tempDir = System.getProperty("java.io.tmpdir")
        System.setProperty("docker.cert.path","${tempDir}/certs")

        if (pluginConfig.credential_key.password){
            // If docker client private key is provided in plugin config then enable TLS mode
            System.setProperty("docker.tls.verify", "1")
        }
        dockerClient = new DockerClientImpl(pluginConfig.endpoint)
        
    }


   /*	Function exits with return value 1 if docker endpoint specified
    *   in plugin configuration is not reachable. In case of swarm mode,
    *   whether the endpoint is swarm manager and is in active state or
	*   not is checked.
	*/
	def checkHealth(){

            def info = dockerClient.info().content
            println info

            if (pluginConfig.swarmMode=="true"){

                try{
                    def role = dockerClient.inspectNode(pluginConfig.swarmManagerHostname).content.Spec.Role
                    if (role != "manager"){
                        // Given node is  worker node in swarm cluster
                        logger ERROR, "${pluginConfig.endpoint} is not Swarm Manager. Exiting.."
                        exit 1
                    }

                    def availability = dockerClient.inspectNode(pluginConfig.swarmManagerHostname).content.Spec.Availability
                    if (availability!="active"){
                        // Given node is not active manager
                        logger ERROR, "${pluginConfig.endpoint} is not active Swarm Manager. Exiting.."
                        exit 1
                    }
                }catch(Exception e){
                    // Given node is not a swarm manager
                    logger ERROR, "${e}"
                    logger ERROR, "${pluginConfig.endpoint} is not Swarm Manager. Exiting.."
                    exit 1
                }

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

        String serviceName = formatName(serviceDetails.serviceName)
        //def deployedService = getService(clusterEndPoint, serviceName)
        def deployedService = null
        def serviceDefinition = buildServicePayload(serviceDetails, deployedService)
        println "service def:"
        println serviceDefinition
        if (OFFLINE) return null
        
        if(deployedService){
            logger INFO, "Updating deployed service $serviceName"
            
            doHttpRequest(PUT,
                    clusterEndPoint,
                    "/services/${serviceName}/update",
                    /* Headers*/  [:],
                    /*failOnErrorCode*/ true,
                    serviceDefinition)
            

        } else {

            logger INFO, "Creating service $serviceName"
            
            def response = dockerClient.createService(serviceDefinition)
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
        
        def response = doHttpGet(clusterEndPoint,
                "/services/${formatName(serviceName)}",
                null, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null
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
        def imageName = args.container.imageName[0]
        def hash=[
                    "name": serviceName,
                    "TaskTemplate": [
                        "ContainerSpec": [
                            "Image": imageName
                        ]
                    ],
                    "EndpointSpec": [
                        "ports" : args.port.collect { servicePort ->
                                    
                                    def targetPort

                                    for (container in args.container) {
                    
                                        if(container.containerName == servicePort.subcontainer){

                                            for (containerPort in container.port) {

                                                if (containerPort.portName == servicePort.subport) {
                                                    targetPort = containerPort.containerPort
                                                    break
                                                }
                                            }      
                                        }
                                    }
                                    def portMapping = [:]
                                    portMapping.PublishedPort=servicePort.listenerPort.toInteger()
                                    portMapping.TargetPort=targetPort.toInteger()
                                    portMapping
                            }          
                        ]
                ]
            def payload = hash
            return payload
    }

}