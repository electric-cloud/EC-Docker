/**	Docker Compose File structure:
 *
 *	version:
 *	'3'
 *	services:
 *   web:
 *	    image: nginx:13.1
 *	    ports:
 *	     - "5000:5000"
 *	    volumes:
 *	- .:/code
 *	redis:
 *	image: redis
 */
import com.electriccloud.client.groovy.ElectricFlow
import groovy.json.JsonBuilder

public class ImportMicroservices extends EFClient {

    static def REPORT_URL_PROPERTY = '/myJob/report-urls/'
    static def REPORT_TEMPLATE_UNSUPPORTED_PARAMS = '''
    $[/myProject/resources/report]
    '''

    def composeConfig
    // For networks params
    def yamlConfig
    def importedSummary = [:]

    static final String CREATED_DESCRIPTION = "Created by Container ImportMicroservices"

    def ImportMicroservices(def composeConfig, def yamlConfig) {
        this.composeConfig = composeConfig
        this.yamlConfig = yamlConfig
    }

    def buildServicesDefinitions(def projectName, def applicationName) {
        def efServices = []
        def globalServiceNetworksParams = []
        def globalServicesVolumesParams = []

        //Networks
        yamlConfig.each { networksConfig ->
            networksConfig.networks.each { name, networkConfig ->
                def efNetwork = buildServiceMapping(name, networkConfig)
                logger INFO, "Reading networks config: " + prettyPrint(efNetwork)
                globalServiceNetworksParams.push(efNetwork)
            }
        }

        def networks = []
        if (globalServiceNetworksParams) {
            networks = [
                    networkName: globalServiceNetworksParams?.network.networkName ? globalServiceNetworksParams?.network.networkName.get(0) : null,
                    driver     : globalServiceNetworksParams?.serviceMapping.driver ? globalServiceNetworksParams?.serviceMapping.driver.get(0) : null,
                    subnet     : globalServiceNetworksParams?.serviceMapping.subnet ? globalServiceNetworksParams?.serviceMapping.subnet.get(0) : null,
                    gateway    : globalServiceNetworksParams?.serviceMapping.gateway ? globalServiceNetworksParams?.serviceMapping.gateway.get(0) : null
            ]
        }

        // Global volumes
        composeConfig.volumes.each { name, volumeConfig ->
            def efVolume = buildVolumeDefinition(name, volumeConfig)
            logger INFO, "Reading global volumes parameters: " + prettyPrint(efVolume)
            globalServicesVolumesParams.push(efVolume)
        }

        def volumes
        if(globalServicesVolumesParams) {
            volumes = globalServicesVolumesParams
        }

        def unsupportedParams = []

        // Services
        composeConfig.services.each { name, serviceConfig ->
            checkForUnsupportedParameters(name, serviceConfig, unsupportedParams)
            def efService = buildServiceDefinition(name, serviceConfig)
            efService.network = networks
            efServices.push(efService)
        }

        if(unsupportedParams) {
            def unsupportedParamsTable = buildSummaryReport(unsupportedParams)
            publishSummaryReportWithUnsupportedComposeParameters(unsupportedParamsTable)
        }

        efServices
    }

    def checkForUnsupportedParameters(def name, def serviceConfig, def unsupportedParams) {
        if(serviceConfig.capAdd != null) {
            unsupportedParams.push("Service ${name} has unsupported option cap_add: ${serviceConfig.capAdd}")
        }
        if(serviceConfig.envFile != null) {
            unsupportedParams.push("Service ${name} has unsupported option env_file: ${serviceConfig.envFile}")
        }
        if(serviceConfig.extraHosts != null) {
            unsupportedParams.push("Service ${name} has unsupported option extra_hosts: ${serviceConfig.extraHosts}")
        }
        if(serviceConfig.healthcheck != null) {
            unsupportedParams.push("Service ${name} has unsupported option healthcheck: ${serviceConfig.healthcheck}")
        }
        if(serviceConfig.hostname != null) {
            unsupportedParams.push("Service ${name} has unsupported option hostname: ${serviceConfig.hostname}")
        }
        if(serviceConfig.labels != null) {
            unsupportedParams.push("Service ${name} has unsupported option labels: ${serviceConfig.labels}")
        }
        if(serviceConfig.logging != null) {
            unsupportedParams.push("Service ${name} has unsupported option logging: ${serviceConfig.logging}")
        }
        if(serviceConfig.pid != null) {
            unsupportedParams.push("Service ${name} has unsupported option pid: ${serviceConfig.pid}")
        }
        if(serviceConfig.secrets != null) {
            unsupportedParams.push("Service ${name} has unsupported option secrets: ${serviceConfig.secrets}")
        }
        if(serviceConfig.stdinOpen != null) {
            unsupportedParams.push("Service ${name} has unsupported option stdin_open: ${serviceConfig.stdinOpen}")
        }
        if(serviceConfig.stopGracePeriod != null) {
            unsupportedParams.push("Service ${name} has unsupported option stop_grace_period: ${serviceConfig.stopGracePeriod}")
        }
        if(serviceConfig.stopSignal != null) {
            unsupportedParams.push("Service ${name} has unsupported option stop_signal: ${serviceConfig.stopSignal}")
        }
        if(serviceConfig.tty != null) {
            unsupportedParams.push("Service ${name} has unsupported option tty: ${serviceConfig.tty}")
        }
        if(serviceConfig.ulimits != null) {
            unsupportedParams.push("Service ${name} has unsupported option ulimits: ${serviceConfig.ulimits}")
        }
        if(serviceConfig.user != null) {
            unsupportedParams.push("Service ${name} has unsupported option user: ${serviceConfig.user}")
        }
        if(serviceConfig.workingDir != null) {
            unsupportedParams.push("Service ${name} has unsupported option working_dir: ${serviceConfig.workingDir}")
        }
    }

    def buildVolumeDefinition(def name, def volumeConfig) {
        def efVolumeName = name
        def efVolume = [
            volumeName: efVolumeName,
            configName: volumeConfig?.name,
            driverName: volumeConfig?.driver
        ]
    }

    def buildServiceDefinition(def name, def serviceConfig) {
        def efServiceName = name
        def efService = [
                service: [
                        serviceName: efServiceName
                ]
        ]

        // Service Fields
        def defaultCapacity = serviceConfig.deploy?.replicas ?: 1
        def minCapacity = defaultCapacity - (serviceConfig.deploy?.updateConfig?.parallelism ?: 1)

        efService.service.defaultCapacity = defaultCapacity
        efService.service.minCapacity = minCapacity
        // 'minCapacity' must be between 1 and 2147483647
        if(efService.service.minCapacity < 1) {
            efService.service.minCapacity = 1
        }

        // image
        def imageName = null
        def version = null
        def url = null

        if(serviceConfig.image) {
            imageName = getImageName(serviceConfig.image)
            version = getImageVersion(serviceConfig.image)
            url = getRegistryUri(serviceConfig.image)
        }

        // ENV variables
        def envVars = serviceConfig.environment.entries?.collect{
            [environmentVariableName: it.key, type: 'string', value: it.value]
        }

        // port config
        logger INFO, "Reading port configs: " + prettyPrint(serviceConfig.ports)
        def containerPorts = []
        def servicePorts = []
        serviceConfig.ports?.portConfigs?.each { port ->
            containerPorts << port.target
            servicePorts << port.published
        }

        efService.service.ports = servicePorts

        // Volumes
        def containerVolumes = []
        def serviceVolumes = []

        serviceConfig.volumes.each { volume ->
            def containerVolume = [:]
            def serviceVolume = [:]

            def containerVolumeName
            def containerVolumeMountPath
            def serviceVolumeName
            def serviceVolumeHostPath
            if(volume.type && volume.type.equals("volume")) {
                serviceVolumeName = volume?.source
                containerVolumeName = volume?.source
                containerVolumeMountPath = volume?.target
            }
            else if (volume.type && volume.type.equals("bind")) {
                serviceVolumeName = volume.source ? name + "_serviceVolume_" + volume.source : name + "_serviceVolume"
                serviceVolumeHostPath = volume?.source
                containerVolumeName = volume.source ? name + "_containerVolume_" + volume.source : name + "__containerVolume"
                containerVolumeMountPath = volume?.target
            }

            containerVolume.name = containerVolumeName
            containerVolume.mountPath = containerVolumeMountPath

        String containerVolumeValue = null
        def containerVolumes = serviceConfig.volumes?.target
        if(containerVolumes != null) {
            containerVolumeValue = containerVolumes.first()
        }*/

        // Volumes
        def containerVolumes = []
        def serviceVolumes = []

        serviceConfig.volumes.each { volume ->
            def containerVolume = [:]
            def serviceVolume = [:]

            def containerVolumeName
            def containerVolumeMountPath
            def serviceVolumeName
            def serviceVolumeHostPath
            logger DEBUG, "SERVICE ${name} !!VOLUME type: ${volume?.type}, source: ${volume?.source}, target: ${volume.target} \n"
            if(volume.type && volume.type.equals("volume")) {
                serviceVolumeName = volume?.source
                containerVolumeName = volume?.source
                containerVolumeMountPath = volume?.target
            }
            else if (volume.type && volume.type.equals("bind")) {
                serviceVolumeName = volume.source ? name + "_serviceVolume_" + volume.source : name + "_serviceVolume"
                serviceVolumeHostPath = volume?.source
                containerVolumeName = volume.source ? name + "_containerVolume_" + volume.source : name + "__containerVolume"
                containerVolumeMountPath = volume?.target
            }

            containerVolume.name = containerVolumeName
            containerVolume.mountPath = containerVolumeMountPath
          
            serviceVolume.name = serviceVolumeName
            serviceVolume.hostPath = serviceVolumeHostPath

            serviceVolumes.push(serviceVolume)
            containerVolumes.push(containerVolume)
        }

        efService.volumes = serviceVolumes

        def container = [
                containerName: efServiceName,
                command: serviceConfig.command?.parts?.join(',') ?: null,
                entryPoint: serviceConfig.entrypoint ?: null,
                registryUri: url,
                imageName: imageName,
                imageVersion: version,
                memoryLimit: convertToMBs(serviceConfig.deploy?.resources?.limits?.memory),
                memorySize: convertToMBs(serviceConfig.deploy?.resources?.reservations?.memory),
                cpuLimit: serviceConfig.deploy?.resources?.limits?.nanoCpus,
                cpuCount: serviceConfig.deploy?.resources?.reservations?.nanoCpus,
                volumes: containerVolumes,
                //volumeMount: containerVolumeValue,
            ports: containerPorts

        ]

        efService.container = container
        efService.container.env = envVars

        // dependencies
        def processDependency = serviceConfig.dependsOn?.collect {
            [processDependencyName: it.dependency, targetProcessStepName: name, branchType: 'ALWAYS']
        }
        efService.service.processDependency = processDependency

        logger INFO, "Service definition read from the Docker Compose file:"
        logger INFO, prettyPrint(efService)
        efService
    }

    private def parseImage(image) {
        // Image can consist of
        // repository url
        // repo name
        // image name
        def parts = image.split('/')
        // The name always exists
        def imageName = parts.last()
        def registry
        def repoName
        if (parts.size() >= 2) {
            repoName = parts[parts.size() - 2]
            // It may be an image without repo, like nginx
            if (repoName =~ /\./) {
                registry = repoName
                repoName = null
            }
        }
        if (!registry && parts.size() > 2) {
            registry = parts.take(parts.size() - 2).join('/')
        }
        if (repoName) {
            imageName = repoName + '/' + imageName
        }
        def versioned = imageName.split(':')
        def version
        if (versioned.size() > 1) {
            version = versioned.last()
        }
        else {
            version = 'latest'
        }
        imageName = versioned.first()
        return [imageName: imageName, version: version, repoName: repoName, registry: registry]
    }

    def getImageName(image) {
        parseImage(image).imageName
    }

    def getImageRepo(image) {
        parseImage(image).repoName
    }

    def getImageVersion(image) {
        parseImage(image).version
    }

    def getRegistryUri(image) {
        parseImage(image).registry
    }

    def buildServiceMapping(def name, def networkConfig) {

        def efNetworkName = name
        def efNetwork = [
            network: [
                networkName: efNetworkName
            ],
            serviceMapping: [:]
        ]

        def driver = networkConfig?.driver
        def subnet = null
        def gateway = null

        if(networkConfig.ipam?.config) {
            networkConfig.ipam.config.subnet?.each{ param ->
                if(param != null) {
                    subnet = param
                }
            }

            networkConfig.ipam.config.gateway?.each { param ->
                if(param != null) {
                    gateway = param
                }
            }
        }

        efNetwork.serviceMapping.driver = driver
        efNetwork.serviceMapping.subnet = subnet
        efNetwork.serviceMapping.gateway = gateway

        efNetwork
    }

    def saveToEF(services, projectName, envProjectName, envName, clusterName, applicationName = null) {
        if (applicationName){
            if (getExistingApp(applicationName, projectName)) {
                handleError("Application '${applicationName}' already exists in the project '${projectName}'")
            }
            else {
                def app = createApplication(projectName, applicationName)
                createAppDeployProcess(projectName, applicationName)
                if (envProjectName && envName) {
                    createTierMap(projectName, envProjectName, envName, applicationName)
                }
                logger INFO, "Application ${applicationName} has been created"
                // create link for the application
                def applicationId = app.applicationId
                setEFProperty("/myJob/report-urls/Application: $applicationName", "/flow/#applications/$applicationId")
            }
        }
        def efServices = applicationName ? [] : getServices(projectName)
        services.each { service ->
            def svc = createService(projectName, envProjectName, envName, clusterName, efServices, service, applicationName)
            // create links for the service if creating top-level services
            if (svc && !applicationName) {
                def serviceId = svc.serviceId
                setEFProperty("/myJob/report-urls/Microservice: ${svc.serviceName}", "/flow/#services/$serviceId")
            }
        }

        def lines = ["Imported services: ${importedSummary.size()}"]
        importedSummary.each { serviceName, containers ->
            def containerNames = containers.collect { k -> k.key }
            if (applicationName) {
                lines.add("${applicationName}: ${serviceName}: ${containerNames.join(', ')}")
            } else {
                lines.add("${serviceName}: ${containerNames.join(', ')}")
            }
        }

        updateJobSummary(lines.join("\n"), /*jobStepSummary*/ true)
    }

     def createService(def projectName, def envProjectName, def envName, def clusterName, def efServices, def service, def applicationName) {
        def existingService = efServices.find { s ->
            logger INFO, "createService: efServices.find = ${s.serviceName}"
            equalNames(s.serviceName, service.service.serviceName)
        }
        def serviceName

        logger INFO, "Service payload:"
        logger INFO, prettyPrint(service)

        def result
        if (existingService) {
            logger WARNING, "Service ${existingService.serviceName} already exists, skipping"
            // return since we do not want to update an existing service.
            return null
        }
        else {
            assert service?.service?.serviceName
            serviceName = service.service.serviceName
            result = createEFService(projectName, service, applicationName)
            logger INFO, "Service ${serviceName} has been created"
            importedSummary[serviceName] = [:]
        }
        assert serviceName

        // Container - Docker service has only one container
         createEFContainer(projectName, serviceName, service.container, applicationName)
         //container name is the same as the service name in Docker service
         importedSummary[serviceName][serviceName] = [:]
         createEFPorts(projectName, serviceName, service.container, service.service, applicationName)

        if (envProjectName && envName && clusterName) {
            if (!applicationName) { // for application-scoped services the tier map is created at the time of app creation.
                createEnvironmentMap(projectName, envProjectName, envName, serviceName)
            }
            createServiceClusterMapping(projectName, envProjectName, envName, clusterName, serviceName, service, applicationName)
        }
        result
    }

    def createEFPorts(projectName, serviceName, container, service, applicationName) {
        def containerName = container.containerName
        // the ports in the built up container and service definitions are stored
        // at the same indices so we match by index
        container.ports?.eachWithIndex { containerPort, index ->
            def servicePort = service.ports[index]
            // A docker service contains only one container so the port name uniqueness can be ensured
            // using just the port number. No other prefixes are needed.
            def containerPortName = "port${containerPort}"
            // create container port
            def generatedPort = [
                    portName: containerPortName,
                    containerPort: containerPort
            ]
            createContainerPort(projectName, serviceName, containerName, generatedPort, applicationName)
            logger INFO, "Port ${generatedPort.portName} has been created for container ${containerName}, container port: ${generatedPort.containerPort}"

            // create corresponding service port
            // A docker service contains only one container so the port name uniqueness can be ensured
            // within a service using the container name and the port number. No other prefixes are needed.
            def servicePortName = "port${containerName}${containerPort}"
            generatedPort = [
                    portName: servicePortName,
                    listenerPort: servicePort?:containerPort,
                    subcontainer: containerName,
                    subport: containerPortName
            ]
            createServicePort(projectName, serviceName, generatedPort, applicationName)
            logger INFO, "Port ${servicePortName} has been created for service ${serviceName}, listener port: ${generatedPort.listenerPort}, container port: ${generatedPort.subport}"

        }
    }

    def getExistingApp(applicationName, projectName){
        def applications = getApplications(projectName)
        def existingApplication = applications?.find {
            it.applicationName == applicationName && it.projectName == projectName
        }
        existingApplication
    }

    def createEnvironmentMap(projName, envProjName, envName, serviceName) {
        def payload = [
                environmentMapName: "${serviceName}-${envName}",
                environmentName: envName,
                environmentProjectName: envProjName,
                description: CREATED_DESCRIPTION,
        ]
        createEnvironmentMap(projName, serviceName, payload)
    }

    def createTierMap(projName, envProjName, envName, applicationName) {
        def payload = [
                tierMapName: "${applicationName}-${envName}",
                environmentName: envName,
                environmentProjectName: envProjName,
                description: CREATED_DESCRIPTION,
        ]
        createTierMap(projName, applicationName, payload)
    }

    def createServiceClusterMapping(projName, envProjName, envName, clusterName, serviceName, service, applicationName) {
        def mapping = service.serviceMapping

        def payload = [
            clusterName: clusterName,
            environmentName: envName,
            environmentProjectName: envProjName
        ]

        if (mapping) {
            def actualParameters = []
            mapping.each {k, v ->
                if (v) {
                    actualParameters.add([actualParameterName: k, value: v])
                }
            }
            payload.actualParameter = actualParameters
        }

        if(applicationName) {
            def tierMapName = "${applicationName}-${envName}"
            payload.serviceName = "${serviceName}"
            createAppServiceClusterMapping(projName, applicationName, tierMapName, payload)
            logger INFO, "Created Service Cluster Mapping for ${serviceName} and ${clusterName} in tier map ${tierMapName}"
        } else {
            def envMapName = "${serviceName}-${envName}"
            createServiceClusterMapping(projName, serviceName, envMapName, payload)
            logger INFO, "Created Service Cluster Mapping for ${serviceName} and ${clusterName}"
        }
    }

    def createEFContainer(projectName, serviceName, container, application) {
        logger DEBUG, "Container payload:"
        logger DEBUG, prettyPrint(container)

        def containerName = container.containerName
        assert containerName
        if (application) {
            logger INFO, "Going to create container ${serviceName}/${containerName} in application $application"
        } else {
            logger INFO, "Going to create container ${serviceName}/${containerName}"
        }
        logger INFO, prettyPrint(container)
        createContainer(projectName, serviceName, container, application)
        logger INFO, "Container ${serviceName}/${containerName} has been created"

        //creating container ports at the same time as service ports in createEFPorts
        /*container.ports?.each { port ->
            createPort(projectName, serviceName, port, containerName)
            logger INFO, "Port ${port.portName} has been created for container ${containerName}, container port: ${port.containerPort}"
        }*/

        container.env?.each { env ->
            createEnvironmentVariable(projectName, serviceName, containerName, env, application)
            logger INFO, "Environment variable ${env.environmentVariableName} has been created"
        }
    }

    def createAppDeployProcess(projectName, applicationName) {
        def processName = 'Deploy'
        createAppProcess(projectName, applicationName, [processName: processName, processType: 'DEPLOY'])
        logger INFO, "Process ${processName} has been created for applicationName: '${applicationName}'"
    }

    def createAppDeployProcessStep(projectName, applicationName, serviceName) {
        def processName = 'Deploy'
        def processStepName = "deployService-${serviceName}"
        createAppProcessStep(projectName, applicationName, processName, [
                processStepName: processStepName,
                processStepType: 'service', subservice: serviceName]
        )
        logger INFO, "Process step ${processStepName} has been created for process ${processName} in service ${serviceName}"
    }

    def createDeployProcess(projectName, serviceName) {
        def processName = 'Deploy'
        createProcess(projectName, serviceName, [processName: processName, processType: 'DEPLOY'])
        logger INFO, "Process ${processName} has been created for ${serviceName}"
        def processStepName = 'deployService'
        createProcessStep(projectName, serviceName, processName, [
            processStepName: processStepName,
            processStepType: 'service', subservice: serviceName]
        )
        logger INFO, "Process step ${processStepName} has been created for process ${processName} in service ${serviceName}"
    }

    def createEFService(projectName, service, applicationName) {
        def payload = service.service
        def container = service.container
        def serviceName = payload.serviceName
        payload.description = "Created by EF Import Microservices"
        ElectricFlow ef  = new ElectricFlow()
        if(applicationName.equals('')) applicationName = null
        Map argsForService = [
                projectName: projectName,
                serviceName: payload.serviceName,
                addDeployProcess: true,
                applicationName:  applicationName,
                defaultCapacity: payload.defaultCapacity?.toString(),
                description: payload.description,
                minCapacity: payload.minCapacity?.toString(),
                volume: payload.volumes ? new JsonBuilder(payload.volumes).toString() : null
        ]
        def svc = ef.createService(argsForService)?.service
        if (applicationName) {
            createAppDeployProcessStep(projectName, applicationName, serviceName)
        }
        svc
    }

    def equalNames(String oneName, String anotherName) {
        assert oneName
        assert anotherName
        def normalizer = { name ->
            name = name.toLowerCase()
            name = name.replaceAll('-', '.')
        }
        return normalizer(oneName) == normalizer(anotherName)
    }

    def publishSummaryReportWithUnsupportedComposeParameters(def unsupportedParams) {
        def text = renderReportForUnsupportedParam(unsupportedParams, REPORT_TEMPLATE_UNSUPPORTED_PARAMS)

        def dir = new File('artifacts').mkdir()
        def random = new Random()
        def randomSuffix = random.nextInt(10 ** 5)

        def reportFilename = "dockerImportMicroservices_${randomSuffix}.html"
        def report = new File("artifacts/${reportFilename}")
        report.write(text)
        String jobStepId = System.getenv('COMMANDER_JOBSTEPID')

        def reportName = "Docker Import Microservices Report (${randomSuffix})"
        publishLink(reportName, "/commander/jobSteps/${jobStepId}/${reportFilename}")
    }

    def publishLink(String name, String link) {
        setEFProperty("${REPORT_URL_PROPERTY}${name}", link)
        try {
            setEFProperty("/myJob/report-urls/${name}",
                    "<html><a href=\"${link}\" target=\"_blank\">${name}</a></html>")
        }
        catch (Throwable e) {
            logger ERROR, "ImportMicroservices - publishLink error: ${e}"
        }
    }

    def buildSummaryReport(def reportList){
        def writer = new StringWriter()
        def markup = new groovy.xml.MarkupBuilder(writer)
        markup.html{
            reportList.each{ item ->
                tr {
                    td(class:"text-center", item)
                }
            }
        }
        writer.toString()
    }

    def renderReportForUnsupportedParam(def unsupportedParams, String template) {
        def engine = new groovy.text.SimpleTemplateEngine()
        def templateParams = [:]
        templateParams.unsupParams = unsupportedParams

        def text = engine.createTemplate(template).make(templateParams).toString()
        return text
    }

    def prettyPrint(object) {
        println new JsonBuilder(object).toPrettyString()
    }

}