/**
 * ElectricFlow API client
 */

public class EFClient extends BaseClient {
    static final String REST_VERSION = 'v1.0'

    def getServerUrl() {
        def commanderServer = System.getenv('COMMANDER_SERVER')
        def secure = Integer.getInteger("COMMANDER_SECURE", 0).intValue()
        def protocol = secure ? "https" : "http"
        def commanderPort = secure ? System.getenv("COMMANDER_HTTPS_PORT") : System.getenv("COMMANDER_PORT")
        def url = "$protocol://$commanderServer:$commanderPort"
        logger DEBUG, "Using ElectricFlow server url: $url"
        url
    }

    Object doHttpGet(String requestUri, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(GET, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"],
                failOnErrorCode, /*requestBody*/ null, query)
    }

    Object doHttpPost(String requestUri, Object requestBody, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(POST, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"], failOnErrorCode, requestBody, query)
    }

    Object doHttpPut(String requestUri, Object requestBody, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(PUT, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"], failOnErrorCode, requestBody, query)
    }

    Object doRestPost(String requestUri, Map payload, boolean failOnErrorCode = true, def query = null) {
        def json = payloadToJson(payload)
        doHttpPost(requestUri, json, failOnErrorCode, query)
    }

    private def payloadToJson(payload) {
        def refinedPayload = [:]
        payload.each {k, v ->
            if (v != null) {
                refinedPayload[k] = v
            }
        }
        JsonOutput.toJson(refinedPayload)
    }

    def getApplication(def projectName, def applicationName) {
        def result = doHttpGet("/rest/v1.0/projects/$projectName/applications/$applicationName", /*failOnErrorCode*/ false)
        result.data
    }

    def getApplications(projName){
        def result = doHttpGet("/rest/${REST_VERSION}/projects/${projName}/applications")
        result?.data?.application
    }

    def createApplication(projName, appName){
        def payload = [
                applicationName: appName
        ]
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/applications", payload, false)
        result?.data
    }

    def getClusters(projName, envName) {
        def result = doHttpGet("/rest/${REST_VERSION}/projects/${projName}/environments/${envName}/clusters")
        result?.data?.cluster
    }

    def deleteApplication(def projectName, def applicationName) {

        doHttpDelete("/rest/v1.0/projects/$projectName/applications/$applicationName")
    }

    Object doRestPut(String requestUri, Map payload, boolean failOnErrorCode = true, def query = null) {
        def json = payloadToJson(payload)
        doHttpPut(requestUri, json, failOnErrorCode, query)
    }

    def getConfigValues(def configPropertySheet, def config, def pluginProjectName) {

        // Get configs property sheet
        def result = doHttpGet("/rest/v1.0/projects/$pluginProjectName/$configPropertySheet", /*failOnErrorCode*/ false)
        def configPropSheetId = result.data?.property?.propertySheetId
        if (!configPropSheetId) {
            handleProcedureError("No plugin configurations exist!")
        }

        result = doHttpGet("/rest/v1.0/propertySheets/$configPropSheetId", /*failOnErrorCode*/ false)
        // Get the property sheet id of the config from the result
        def configProp = result.data.propertySheet.property.find{
            it.propertyName == config
        }

        if (!configProp) {
            handleProcedureError("Configuration $config does not exist!")
        }

        result = doHttpGet("/rest/v1.0/propertySheets/$configProp.propertySheetId")

        def values = result.data.propertySheet.property.collectEntries{
            [(it.propertyName): it.value]
        }

        logger(INFO, "Plugin configuration values: " + values)

        def cred = getCredentials(config)
        values << [credential: [userName: cred.userName, password: cred.password]]

        //Set the log level using the plugin configuration setting
        logLevel = (values.logLevel?: INFO).toInteger()

        values
    }

    def getServiceCluster(String serviceName,
                          String projectName,
                          String applicationName,
                          String applicationRevisionId,
                          String environmentName,
                          String envProjectName) {

        def result = doHttpGet("/rest/v1.0/projects/${projectName}/applications/${applicationName}/tierMaps")

        logger DEBUG, "Tier Maps: " + prettyPrint(result)
        // Filter tierMap based on environment.
        def tierMap = result.data.tierMap.find {
            it.environmentName == environmentName && it.environmentProjectName == envProjectName
        }

        logger DEBUG, "Environment tier map for environment '$environmentName' and environment project '$envProjectName': \n" + prettyPrint(tierMap)
        // Filter applicationServiceMapping based on service name.
        def svcMapping = tierMap?.appServiceMappings?.applicationServiceMapping?.find {
            it.serviceName == serviceName
        }
        // If svcMapping not found, try with serviceClusterMappings for post 8.0 tierMap structure
        if (!svcMapping) {
            svcMapping = tierMap?.serviceClusterMappings?.serviceClusterMapping?.find {
                it.serviceName == serviceName
            }
        }

        // Fail if service mapping still not found fail.
        if (!svcMapping) {
            handleError("Could not find the service mapping for service '$serviceName', " +
                    "therefore, the cluster cannot be determined. Try specifying the cluster name " +
                    "explicitly when invoking 'Undeploy Service' procedure.")
        }
        logger DEBUG, "Service map for service '$serviceName': \n" + prettyPrint(svcMapping)
        svcMapping.clusterName

    }

    def getProvisionClusterParameters(String clusterName,
                                      String clusterOrEnvProjectName,
                                      String environmentName) {

        def partialUri = environmentName ?
                "projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName" :
                "projects/$clusterOrEnvProjectName/clusters/$clusterName"

        def result = doHttpGet("/rest/v1.0/$partialUri")

        def params = result.data.cluster?.provisionParameters?.parameterDetail

        if(!params) {
            handleError("No provision parameters found for cluster $clusterName!")
        }

        def provisionParams = params.collectEntries {
            [(it.parameterName): it.parameterValue]
        }

        logger DEBUG, "Cluster parameters from ElectricFlow cluster definition: $provisionParams"

        return provisionParams
    }

    def getServiceDeploymentDetails(String serviceName,
                                    String serviceProjectName,
                                    String applicationName,
                                    String applicationRevisionId,
                                    String clusterName,
                                    String clusterProjectName,
                                    String environmentName,
                                    String serviceEntityRevisionId = null) {

        def partialUri = applicationName ?
                "projects/$serviceProjectName/applications/$applicationName/services/$serviceName" :
                "projects/$serviceProjectName/services/$serviceName"
        def jobStepId = System.getenv('COMMANDER_JOBSTEPID')
        // def jobStepId = '$[/myJobStep/jobStepId]'
        def queryArgs = [
                request: 'getServiceDeploymentDetails',
                clusterName: clusterName,
                clusterProjectName: clusterProjectName,
                environmentName: environmentName,
                applicationEntityRevisionId: applicationRevisionId,
                jobStepId: jobStepId
        ]

        if (serviceEntityRevisionId) {
            queryArgs << [serviceEntityRevisionId: serviceEntityRevisionId]
        }

        def result = doHttpGet("/rest/v1.0/$partialUri", /*failOnErrorCode*/ true, queryArgs)

        def svcDetails = result.data.service
        logger DEBUG, "Service Details: " + prettyPrint(svcDetails)

        svcDetails
    }

    def expandString(String str) {
        def jobStepId = '$[/myJobStep/jobStepId]'
        def payload = [
                value: str,
                jobStepId: jobStepId
        ]

        def result = doHttpPost("/rest/v1.0/expandString", /* request body */ payload,
                /*failOnErrorCode*/ false, [request: 'expandString'])

        if (result.status >= 400){
            handleProcedureError("Failed to expand '$str'. $result.statusLine")
        }

        result.data?.value
    }

    def getActualParameters() {
        def jobId = '$[/myJob/jobId]'
        def result = doHttpGet("/rest/v1.0/jobs/$jobId")
        (result.data.job.actualParameter?:[:]).collectEntries {
            [(it.actualParameterName): it.value]
        }
    }

    def getCredentials(def credentialName) {
        def jobStepId = '$[/myJobStep/jobStepId]'
        // Use the new REST mapping for getFullCredential with 'credentialPaths'
        // which works around the restMapping matching issue with the credentialName being a path.
        def result = doHttpGet("/rest/v1.0/jobSteps/$jobStepId/credentialPaths/$credentialName")
        result.data.credential
    }

    def handleConfigurationError(String msg) {
        createProperty('/myJob/configError', msg)
        handleProcedureError(msg)
    }

    def handleProcedureError (String msg) {
        createProperty('summary', "ERROR: $msg")
        handleError(msg)
    }

    boolean runningInPipeline() {
        def result = getEFProperty('/myPipelineStageRuntime/id', /*ignoreError*/ true)
        return result.data ? true : false
    }

    def createProperty(String propertyName, String value, Map additionalArgs = [:]) {
        // Creating the property in the context of a job-step by default
        def jobStepId = '$[/myJobStep/jobStepId]'
        def payload = [:]
        payload << additionalArgs
        payload << [
                propertyName: propertyName,
                value: value,
                jobStepId: jobStepId
        ]

        doHttpPost("/rest/v1.0/properties", /* request body */ payload)
    }

    def createProperty2(String propertyName, String value, Map additionalArgs = [:]) {
        // Creating the property in the context of a job-step by default
        def jobStepId = '$[/myJobStep/jobStepId]'
        def payload = [:]
        payload << additionalArgs
        payload << [
                propertyName: propertyName,
                value: value,
                jobStepId: jobStepId
        ]
        // to prevent getting the value getting converted to json
        payload = JsonOutput.toJson(payload)
        doHttpPost("/rest/v1.0/properties", /* request body */ payload)
    }

    def createPropertyInPipelineContext(String applicationName,
                                        String serviceName, String targetPort,
                                        String propertyName, String value) {
        if (runningInPipeline()) {

            String relativeProp = applicationName ?
                    "${applicationName}/${serviceName}/${targetPort}" :
                    "${serviceName}/${targetPort}"
            String fullProperty = "/myStageRuntime/${relativeProp}/${propertyName}"
            logger INFO, "Registering pipeline runtime property '$fullProperty' with value $value"
            setEFProperty(fullProperty, value)
        }
    }

    def setEFProperty(String propertyName, String value, Map additionalArgs = [:]) {
        // Creating the property in the context of a job-step by default
        def jobStepId = '$[/myJobStep/jobStepId]'
        def payload = [:]
        payload << additionalArgs
        payload << [
                value: value,
                jobStepId: jobStepId
        ]
        // to prevent getting the value getting converted to json
        payload = JsonOutput.toJson(payload)
        doHttpPut("/rest/v1.0/properties/${propertyName}", /* request body */ payload)
    }
	
    def getAppEnvMaps(projectName, applicationName, tierMapName) {
        def result = doHttpGet("/rest/${REST_VERSION}/projects/${projectName}/applications/${applicationName}/tierMaps/${tierMapName}/serviceClusterMappings")
        result?.data?.serviceClusterMapping
    }

    def createPort(projName, serviceName, payload, containerName = null, boolean failOnError = false, appName = null) {
        if (appName) {
            payload.applicationName = appName
        }
        if (containerName) {
            payload.containerName = containerName
        }
        payload.serviceName = serviceName
        def json = JsonOutput.toJson(payload)
        def result = doHttpPost("/rest/${REST_VERSION}/projects/${projName}/services/${serviceName}/ports", json, failOnError)
        result?.data
    }

    def createEnvironmentVariable(projName, serviceName, containerName, payload, applicationName = null) {
        if (applicationName) {
            payload.applicationName = applicationName
        }
        payload.containerName = containerName
        payload.serviceName = serviceName
        def json = JsonOutput.toJson(payload)
        def result = doHttpPost("/rest/${REST_VERSION}/projects/${projName}/containers/${containerName}/environmentVariables", json)
        result?.data
    }

    def createContainer(String projectName, String serviceName, payload, applicationName = null) {
        if (payload.volumeMount) {
            def volumeMount = [
                    "mountPath": payload?.volumeMount
            ]
            payload.volumeMount = new JsonBuilder(volumeMount).toString()
        }
        payload.serviceName = serviceName
        if (applicationName) {
            payload.applicationName = applicationName
        }
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projectName}/containers", payload, true)
        result?.data
    }
	
	def createEnvironmentMap(projName, serviceName, payload) {
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/services/${serviceName}/environmentMaps", payload)
        result?.data
    }

    def createAppProcess(projName, applicationName, payload) {
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/applications/${applicationName}/processes", payload)
        result?.data
    }

	def createProcess(projName, serviceName, payload) {
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/services/${serviceName}/processes", payload)
        result?.data
    }


    def createAppProcessStep(projName, applicationName, processName, payload) {
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/applications/${applicationName}/processes/${processName}/processSteps", payload, false)
        result?.data
    }

	def createProcessStep(projName, serviceName, processName, payload) {
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/services/${serviceName}/processes/${processName}/processSteps", payload, false)
        result?.data
    }
	
	def createServiceMapDetails(projName, serviceName, envMapName, serviceClusterMapName, payload, applicationName = null) {
        if (applicationName) {
            payload.applicationName = applicationName
        }
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/services/${serviceName}/environmentMaps/${envMapName}/serviceClusterMappings/${serviceClusterMapName}/serviceMapDetails", payload, false)
        result?.data
    }

    def createAppServiceClusterMapping(projName, applicationName, tierMapName, payload) {
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/applications/${applicationName}/tierMaps/${tierMapName}/serviceClusterMappings", payload)
        result?.data
    }

	def createServiceClusterMapping(projName, serviceName, envMapName, payload) {
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/services/${serviceName}/environmentMaps/${envMapName}/serviceClusterMappings", payload)
        result?.data
    }
	
	def getContainers(projectName, serviceName, applicationName = null) {
        def query = [
            serviceName: serviceName
        ]
        if (applicationName) {
            query.applicationName = applicationName
        }
        def result = doHttpGet("/rest/${REST_VERSION}/projects/${projectName}/containers", false, query)
        result?.data?.container
    }
	
	def updateJobSummary(String message) {
        def jobStepId = System.getenv('COMMANDER_JOBSTEPID')
        def summary = getEFProperty('myJob/summary', true)?.value
        def lines = []
        if (summary) {
            lines = summary.split(/\n/)
        }
        lines.add(message)
        setEFProperty('myJob/summary', lines.join("\n"))
    }

    def createTierMap(projName, applicationName, payload){
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/applications/${applicationName}/tierMaps", payload)
        result?.data
    }

    def evalDsl(String dslStr) {
        // Run the dsl in the context of a job-step by default
        def jobStepId = '$[/myJobStep/jobStepId]'
        def payload = [:]
        payload << [
                dsl: dslStr,
                jobStepId: jobStepId
        ]

        doHttpPost("/rest/v1.0/server/dsl", /* request body */ payload)
    }

    def getEFProperty(String propertyName, boolean ignoreError = false) {
        // Get the property in the context of a job-step by default
        def jobStepId = '$[/myJobStep/jobStepId]'

        doHttpGet("/rest/v1.0/properties/${propertyName}",
                /* failOnErrorCode */ !ignoreError, [jobStepId: jobStepId])
    }

    // Discovery methods, EF model generation
    def createService(projName, payload, appName = null) {
        if (appName) {
            payload.applicationName = appName
        }
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/services", /* request body */ payload)
        result?.data
    }

    def updateService(projName, serviceName, payload, applicationName = null) {
        if (applicationName) {
            payload.applicationName = applicationName
        }
        def result = doRestPut("/rest/${REST_VERSION}/projects/${projName}/services/${serviceName}", /* request body */ payload)
        result?.data
    }

    def getServices(projName, appName = null) {
        def query = [:]
        if (appName) {
            query.applicationName = appName
        }
        def result = doHttpGet("/rest/${REST_VERSION}/projects/${projName}/services", true, query)
        result?.data?.service
    }

    def getPorts(projectName, serviceName, appName = null, containerName = null) {
        def query = [:]
        if (containerName) {
            query.containerName = containerName
        }
        if (appName) {
            query.applicationName = appName
        }
        def result = doHttpGet("/rest/${REST_VERSION}/projects/${projectName}/services/${serviceName}/ports", true, query)
        result?.data?.port
    }

    def createContainerPort(projName, serviceName, containerName, payload, String appName = null) {
        if (appName) {
            payload.applicationName = appName
        }
        def json = JsonOutput.toJson(payload)
        def result = doHttpPost("/rest/${REST_VERSION}/projects/${projName}/services/${serviceName}/containers/${containerName}/ports", json)
        result?.data
    }

    def createServicePort(projName, serviceName, payload, String appName = null) {
        if (appName) {
            payload.applicationName = appName
        }
        def json = JsonOutput.toJson(payload)
        def result = doHttpPost("/rest/${REST_VERSION}/projects/${projName}/services/${serviceName}/ports", json)
        result?.data
    }

    def updateContainer(String projectName, String serviceName, String containerName, payload, appName = null) {
        payload.serviceName = serviceName
        if (appName) {
            payload.applicationName = appName
        }
        def result = doRestPut("/rest/${REST_VERSION}/projects/${projectName}/containers/${containerName}", payload, true)
        result?.data
    }

    def createCredential(projName, credName, userName, password) {
        def payload = [
                credentialName: credName,
                userName: userName,
                password: password
        ]
        def result = doRestPost("/rest/${REST_VERSION}/projects/${projName}/credentials", payload)
        result?.data?.credential
    }

    /* Function to convert B, KB, MB, GB to MB.
 * Defaults to 1 MB if less that 1 MB
 */

    def convertToMBs(String memory){

        def memoryInMBs = null
        if(memory != null){
            def suffix = memory[-1]
            def floatMemory
            if(!suffix.isNumber()){
                floatMemory = Float.parseFloat(memory.substring(0, memory.length()-1))

                switch (suffix){

                    case ['B', 'b']:
                        // Round off to 1 MB. Minimum size supported by Flow UI is 1 MB
                        memoryInMBs = "1"
                        break
                    case ['K', 'k']:
                        // Round off to 1 MB. Minimum size supported by Flow UI is 1 MB
                        memoryInMBs = "1"
                        break
                    case ['M', 'm']:
                        memoryInMBs = Integer.toString(floatMemory as int)
                        break
                    case ['G', 'g']:
                        memoryInMBs = Integer.toString(floatMemory * 1000 as int)
                        break
                }
            }else{
                // If no memory unit defined in compose file then Default is B(byte)
                floatMemory = Float.parseFloat(memory)

                if((floatMemory * 0.000001) < 1){
                    // less than 1 MB.. defaulting to 1 MB
                    memoryInMBs = "1"
                }else{
                    memoryInMBs = Integer.toString(floatMemory * 0.000001 as int)
                }
            }
        }
        memoryInMBs
    }

    def prettyPrint(object) {
        JsonOutput.prettyPrint(JsonOutput.toJson(object))
    }

}


