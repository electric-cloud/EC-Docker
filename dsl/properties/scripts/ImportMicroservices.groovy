/**	Docker Compose File structure:
 *
 *	version:
 *	'2'
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

        //Networks
        yamlConfig.each { networksConfig ->
            networksConfig.networks.each { name, networkConfig ->
                def efNetwork = buildServiceMapping(name, networkConfig)
                efServices.push(efNetwork)
            }
        }

        // Services
        composeConfig.services.each { name, serviceConfig ->
            checkForUnsupportedParameters(name, serviceConfig)
            logger DEBUG, "VOLUME CLASS: ${serviceConfig.volumes?.source.getClass()}"
            def efService = buildServiceDefinition(name, serviceConfig)
            efServices.push(efService)
        }

        efServices
    }

    def checkForUnsupportedParameters(def name, def serviceConfig) {
        if(serviceConfig.capAdd != null) {
            logger WARNING, "Service ${name} has unsupported option cap_add: ${serviceConfig.capAdd}"
        }
        if(serviceConfig.envFile != null) {
            logger WARNING, "Service ${name} has unsupported option env_file: ${serviceConfig.envFile}"
        }
        if(serviceConfig.extraHosts != null) {
            logger WARNING, "Service ${name} has unsupported option extra_hosts: ${serviceConfig.extraHosts}"
        }
        if(serviceConfig.healthcheck != null) {
            logger WARNING, "Service ${name} has unsupported option healthcheck: ${serviceConfig.healthcheck}"
        }
        if(serviceConfig.hostname != null) {
            logger WARNING, "Service ${name} has unsupported option hostname: ${serviceConfig.hostname}"
        }
        if(serviceConfig.labels != null) {
            logger WARNING, "Service ${name} has unsupported option labels: ${serviceConfig.labels}"
        }
        if(serviceConfig.logging != null) {
            logger WARNING, "Service ${name} has unsupported option logging: ${serviceConfig.logging}"
        }
        if(serviceConfig.pid != null) {
            logger WARNING, "Service ${name} has unsupported option pid: ${serviceConfig.pid}"
        }
        if(serviceConfig.secrets != null) {
            logger WARNING, "Service ${name} has unsupported option secrets: ${serviceConfig.secrets}"
        }
        if(serviceConfig.stdinOpen != null) {
            logger WARNING, "Service ${name} has unsupported option stdin_open: ${serviceConfig.stdinOpen}"
        }
        if(serviceConfig.stopGracePeriod != null) {
            logger WARNING, "Service ${name} has unsupported option stop_grace_period: ${serviceConfig.stopGracePeriod}"
        }
        if(serviceConfig.stopSignal != null) {
            logger WARNING, "Service ${name} has unsupported option stop_signal: ${serviceConfig.stopSignal}"
        }
        if(serviceConfig.tty != null) {
            logger WARNING, "Service ${name} has unsupported option tty: ${serviceConfig.tty}"
        }
        if(serviceConfig.ulimits != null) {
            logger WARNING, "Service ${name} has unsupported option ulimits: ${serviceConfig.ulimits}"
        }
        if(serviceConfig.user != null) {
            logger WARNING, "Service ${name} has unsupported option user: ${serviceConfig.user}"
        }
        if(serviceConfig.workingDir != null) {
            logger WARNING, "Service ${name} has unsupported option working_dir: ${serviceConfig.workingDir}"
        }
    }

    def buildServiceDefinition(def name, def serviceConfig) {
        def efServiceName = name
        def efService = [
            service: [
                serviceName: efServiceName
            ]
        ]

        // Service Fields
        efService.service.defaultCapacity = serviceConfig.deploy?.replicas
        if (serviceConfig.deploy?.replicas && serviceConfig.deploy?.updateConfig?.parallelism) {
            efService.service.minCapacity = serviceConfig.deploy?.updateConfig?.parallelism - serviceConfig.deploy?.replicas
        } else {
            efService.service.minCapacity = serviceConfig.deploy?.updateConfig?.parallelism
        }

        // image
        def image = ''
        def version = ''
        def url = ''
        def repositoryName = ''
        String imageInfo = serviceConfig?.image
        if (imageInfo != null) {
            if (imageInfo.contains('/')) {
                String[] parts = imageInfo.split('/')
                if (parts.length > 2) {
                    url = parts[0]
                    repositoryName = parts[1]
                    if(parts[2].contains(':')) {
                        String[] imageParts = parts[2].split(':')
                        image = imageParts[0]
                        version = imageParts[1]
                    } else {
                        image = parts[2]
                    }

                }
                else if (parts.length > 1) {
                    repositoryName = parts[0]
                    if (parts[1].contains(':')) {
                        String[] imageParts = parts[1].split(':')
                        image = imageParts[0]
                        version = imageParts[1]
                    } else {
                        image = parts[1]
                    }

                }
            } else {
                String[] parts = imageInfo.split(':')
                image = parts[0]
                if (parts.length > 1) {
                    version = parts[1]
                }
            }
        }
        def imageName = repositoryName ? "${repositoryName}/${image}" : image

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
        String serviceVolumeValue = null
        def servicesVolumes = serviceConfig.volumes?.source
        if(servicesVolumes != null) {
            serviceVolumeValue = servicesVolumes.first()
        }
        efService.service.volume = serviceVolumeValue

        String containerVolumeValue = null
        def containerVolumes = serviceConfig.volumes?.target
        if(containerVolumes != null) {
            containerVolumeValue = containerVolumes.first()
        }

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
                volumeMount: containerVolumeValue,
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
                createApplication(projectName, applicationName)
                createAppDeployProcess(projectName, applicationName)
                if (envProjectName && envName) {
                    createTierMap(projectName, envProjectName, envName, applicationName)
                }
                logger INFO, "Application ${applicationName} has been created"
            }
        }
        def efServices = applicationName ? [] : getServices(projectName)
        services.each { service ->
            if (service?.network == null) {
                createService(projectName, envProjectName, envName, clusterName, efServices, service, applicationName)
            }
        }

        def lines = ["Imported services: ${importedSummary.size()}"]
        importedSummary.each { serviceName, containers ->
            def containerNames = containers.collect { k -> k }
            if (applicationName) {
                lines.add("${applicationName}: ${serviceName}: ${containerNames.join(', ')}")
            } else {
                lines.add("${serviceName}: ${containerNames.join(', ')}")
            }
        }

        updateJobSummary(lines.join("\n"))
    }

     def createService(def projectName, def envProjectName, def envName, def clusterName, def efServices, def service, def applicationName) {
        def existingService = efServices.find { s ->
            logger INFO, "createService: efServices.find = ${s.serviceName}"
            equalNames(s.serviceName, service.service.serviceName)
        }
        def serviceName

        logger INFO, "Service payload:"
        logger INFO, prettyPrint(service)

        if (existingService) {
            logger WARNING, "Service ${existingService.serviceName} already exists, skipping"
            // return since we do not want to update an existing service.
            return
        }
        else {
            serviceName = service.service.serviceName
            createEFService(projectName, service, applicationName)
            logger INFO, "Service ${serviceName} has been created"
            importedSummary[serviceName] = [:]
        }
        assert serviceName

        // Container - Docker service has only one container
         createEFContainer(projectName, serviceName, service.container, applicationName)
         createEFPorts(projectName, serviceName, service.container, service.service, applicationName)

        if (envProjectName && envName && clusterName) {
            if (!applicationName) { // for application-scoped services the tier map is created at the time of app creation.
                createEnvironmentMap(projectName, envProjectName, envName, serviceName)
            }
            createServiceClusterMapping(projectName, envProjectName, envName, clusterName, serviceName, service, applicationName)
        }
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
        def serviceName = payload.serviceName
        payload.description = "Created by EF Import Microservices"
        ElectricFlow ef  = new ElectricFlow();
        if(applicationName.equals('')) applicationName = null
        ef.createService(projectName: projectName , serviceName: payload?.serviceName, addDeployProcess: payload?.addDeployProcess, applicationName:  applicationName,
                defaultCapacity: payload?.defaultCapacity, description: payload?.description, maxCapacity: payload?.maxCapacity, minCapacity: payload?.minCapacity,
                volume: payload?.volume)
        //_createService (args.projectName, args.serviceName, args.addDeployProcess, args.applicationName, args.defaultCapacity, args.description,
        // args.maxCapacity, args.minCapacity, args.volume, onSuccess, onFailure)
        //def result = createService(projectName, payload)
        //result
        if (!applicationName) {
            // Add deploy process for top-level service
            createDeployProcess(projectName, serviceName)
        } else {
            createAppDeployProcessStep(projectName, applicationName, serviceName)
        }
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

    def prettyPrint(object) {
        println new JsonBuilder(object).toPrettyString()
    }

}