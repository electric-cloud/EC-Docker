/**
 * Docker API client
 */
public class DockerClient extends BaseClient {


   /*	Returns string "reachable" or "unreachable" based on availability of
    *   docker engine (if swarmMode is false) 
   	*   docker swarm manager (if swarmMode is true)
	*
	*/
	def checkHealth(String endpoint, boolean swarmMode){

			if (OFFLINE) return null
			def response 
			def reachability
			if (swarmMode) {
				response = doHttpRequest(GET, endpoint, "/nodes", [:], /*failOnErrorCode*/ true, /*requestBody*/ null)
				reachability = response.data[0].ManagerStatus.Reachability

			} else {
				response = doHttpRequest(GET, endpoint, "/info", [:], /*failOnErrorCode*/ true, /*requestBody*/ null)
				if(response.status == 200){
					reachability = "reachable"
				}else{
					reachability = "unreachable"
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
            String resultsPropertySheet) {

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
        def deployedService = getService(clusterEndPoint, serviceName)

        def serviceDefinition = buildServicePayload(serviceDetails, deployedService)

        if (OFFLINE) return null

        if(deployedService){
            logger INFO, "Updating deployed service $serviceName"
            /*
            doHttpRequest(PUT,
                    clusterEndPoint,
                    "/services/${serviceName}/update",
                    /* Headers */ [:],
                    /*failOnErrorCode*//* true,
                    serviceDefinition)
            */

        } else {
            logger INFO, "Creating service $serviceName"
             
            doHttpRequest(POST,
                    clusterEndPoint,
                    "/services/create",
                    /* Headers */ [:],
                    /*failOnErrorCode*/ true,
                    serviceDefinition)
        }
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

    String buildServicePayload(Map args, def deployedService){

        def serviceName = formatName(args.serviceName)
        def json = new JsonBuilder()
        def result = json {
            name serviceName
            TaskTemplate {
                ContainerSpec {
                    Image args.container.imageName[0]
                }
            }
            EndpointSpec {
                ports(args.port.collect { servicePort ->
                            
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
                )
            }
        }
           

        def payload = deployedService
        if (payload) {
            payload = mergeObjs(payload, result)
        } else {
            payload = result
        }
        return (new JsonBuilder(payload)).toPrettyString()
    }

}