/**
 * ElectricFlow API client
 */
public class EFClient extends BaseClient {

    def getServerUrl() {
        def commanderServer = System.getenv('COMMANDER_SERVER')
        def secure = Integer.getInteger("COMMANDER_SECURE", 0).intValue()
        def protocol = secure ? "https" : "http"
        def commanderPort = secure ? System.getenv("COMMANDER_HTTPS_PORT") : System.getenv("COMMANDER_PORT")
        def url = "$protocol://$commanderServer:$commanderPort"
        logger DEBUG, "Using ElectricFlow server url: $url"
        url
    }

    Object doHttpDelete(String requestUri, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(DELETE, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"],
                failOnErrorCode, /*requestBody*/ null, query)
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

    def getApplication(def projectName, def applicationName) {

        def result = doHttpGet("/rest/v1.0/projects/$projectName/applications/$applicationName", /*failOnErrorCode*/ false)
        result.data
    }

    def deleteApplication(def projectName, def applicationName) {

        doHttpDelete("/rest/v1.0/projects/$projectName/applications/$applicationName")
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

        logger(DEBUG, "Config values: " + values)
        def cred = getCredentials(config)
        values["credential"] = [userName: cred.userName, password: cred.password]

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

        logger INFO, "Tier Maps: " + JsonOutput.toJson(result)
        // Filter tierMap based on environment.
        def tierMap = result.data.tierMap.find {
            it.environmentName == environmentName && it.environmentProjectName == envProjectName
        }

        logger INFO, "Environment tier map for environment '$environmentName' and environment project '$envProjectName': \n" + JsonOutput.toJson(tierMap)
        // Filter applicationServiceMapping based on service name.
        def appSvcMapping = tierMap?.appServiceMappings?.applicationServiceMapping?.find {
            it.serviceName == serviceName
        }
        logger INFO, "Service map for service '$serviceName': \n" + JsonOutput.toJson(appSvcMapping)
        appSvcMapping?.clusterName
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

        logger DEBUG, "Cluster params from Deploy: $provisionParams"

        return provisionParams
    }

    def getServiceDeploymentDetails(String serviceName,
                                    String serviceProjectName,
                                    String applicationName,
                                    String applicationRevisionId,
                                    String clusterName,
                                    String clusterProjectName,
                                    String environmentName) {

        def partialUri = applicationName ?
                "projects/$serviceProjectName/applications/$applicationName/services/$serviceName" :
                "projects/$serviceProjectName/services/$serviceName"
        def queryArgs = [
                request: 'getServiceDeploymentDetails',
                clusterName: clusterName,
                clusterProjectName: clusterProjectName,
                environmentName: environmentName,
                applicationEntityRevisionId: applicationRevisionId,
                jobStepId: System.getenv('COMMANDER_JOBSTEPID')
        ]
        def result = doHttpGet("/rest/v1.0/$partialUri", /*failOnErrorCode*/ true, queryArgs)

        def svcDetails = result.data.service
        logger DEBUG, "Service Details: " + JsonOutput.toJson(svcDetails)

        svcDetails
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

    def buildApplicationDsl(def projectName, def applicationName, def composeConfig) {

        def servicesDsl = ''
        composeConfig.services.each { name, serviceConfig ->
            def serviceDsl = buildServiceDsl(name, projectName, applicationName, serviceConfig)
            servicesDsl += "\n$serviceDsl"
        }

        """
        application '$applicationName', projectName: '$projectName', {
            $servicesDsl
        }
        """.toString()
    }

    def buildServiceDsl(def name, def projectName, def applicationName, def serviceConfig) {

        String[] imageInfo = serviceConfig.image?.split(':')
        def image = ''
        def version = ''
        if (imageInfo && imageInfo.length > 0) {
            image = imageInfo[0]
            if (imageInfo.length > 1) {
                version = imageInfo[1]
            }
        }

        def command = serviceConfig.command?.parts?.join(',') ?: ''
        def entrypoint = serviceConfig.entrypoint?:''
        def defaultCapacity = serviceConfig.deploy?.replicas
       
        // Volumes
        // Initial empty json volume spec
        def serviceVolumes = null
        def containerVolumes = null
        if(serviceConfig.volumes){
            def serviceVolumesList = []
            def containerVolumesList = []
            def counter = 0
            for(volume in serviceConfig.volumes){

                def volumeName, hostPath
                if(volume.type == "volume"){
                    volumeName = volume.source
                    hostPath = ""
                }else{
                    // bind volume type
                    volumeName = "${name}_volume_${counter}"
                    hostPath = volume.source
                }

                serviceVolumesList << """
                {
                    \"name\": \"${volumeName}\",
                    \"hostPath\": \"${hostPath}\" 
                }""".toString()   

                containerVolumesList << """
                {
                    \"name\": \"${volumeName}\",
                    \"mountPath\": \"${volume.target}\" 
                }""".toString()   
                counter++
            }
            serviceVolumes = "'''[" + serviceVolumesList.join(",") + "\n]'''"
            containerVolumes = "'''[" + containerVolumesList.join(",") + "\n]'''"
        }

        // port config
        def containerPort = ""
        def servicePort = ""
        if(serviceConfig.ports){
            // Append port config
            int counter = 0
            for (portConfig in serviceConfig.ports.portConfigs){
                def targetPort = portConfig?.target
                def publishedPort = portConfig?.published 

                containerPort +=  """
                    port '${name}_containerPort_${counter}', {
                        applicationName = '$applicationName'
                        containerName = '$name'
                        containerPort = '$targetPort'
                        projectName = '$projectName'
                        serviceName = '$name'
                    }
                """.toString()

                servicePort +=  """
                port '${name}_servicePort_${counter}', {
                      applicationName = '$applicationName'
                      listenerPort = '$publishedPort'
                      projectName = '$projectName'
                      serviceName = '$name'
                      subcontainer = '$name'
                      subport = '${name}_containerPort_${counter}'
                }
                """.toString()
                counter++
            }
        }

        // ENV variables
        def envVars = ""
        //if(serviceConfig.environment.entries.size()>0){
            serviceConfig.environment.entries.each{key, value ->
                envVars += """
                environmentVariable '$key', {
                    type = 'string'
                    value = '$value'
                }""".toString()
            }
            
        //}

        // update config
        def minCapacity = serviceConfig.deploy?.updateConfig?.parallelism

        // Limits and reservations
        def memoryLimit = convertToMBs(serviceConfig.deploy?.resources?.limits?.memory)
        def memorySize = convertToMBs(serviceConfig.deploy?.resources?.reservations?.memory)
        def cpuLimit = serviceConfig.deploy?.resources?.limits?.nanoCpus
        def cpuCount = serviceConfig.deploy?.resources?.reservations?.nanoCpus

        def dsl = """
        service '$name', {
            defaultCapacity = '$defaultCapacity'
            volume = $serviceVolumes
            minCapacity = '$minCapacity'
            container '$name', {
              command = '$command'
              entryPoint = '$entrypoint'
              imageName = '$image'
              imageVersion = '$version'
              memoryLimit = '$memoryLimit'
              memorySize = '$memorySize'
              cpuLimit = '$cpuLimit'
              cpuCount = '$cpuCount'
              volumeMount = $containerVolumes
              $containerPort
              $envVars
            }
            $servicePort
        }
        """.toString()
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
}


