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

public class ImportMicroservices extends EFClient {

	def composeConfig
    def importedSummary = [:]
	
	static final String CREATED_DESCRIPTION = "Created by Container ImportMicroservices"
	
	def ImportMicroservices(def composeConfig) {
		this.composeConfig = composeConfig
    }
	
	def buildServicesDefinitions(def projectName, def applicationName) {
		def efServices = []
		println "buildServicesDefinitions: "
		composeConfig.services.each { name, serviceConfig ->
			println "composeConfig.services.each:  " + name
			def efService = buildServiceDefinition(name, projectName, applicationName, serviceConfig)
			efServices.push(efService)
        }
		efServices
	}
	
	def buildServiceDefinition(def name, def projectName, def applicationName, def serviceConfig) {
        def efServiceName = name
		print "buildServiceDefinition: " + name
        def efService = [
            service: [
                serviceName: efServiceName
            ],
            serviceMapping: [:]
        ]

        // Service Fields
		efService.service.defaultCapacity = serviceConfig.deploy?.replicas
		efService.service.minCapacity = serviceConfig.deploy?.updateConfig?.parallelism
		
		// image
		def image = ''
		def version = ''
		def repositoryName = '' 
		String imageInfo = serviceConfig?.image
		print "IMAGE !: " + imageInfo
		if ( imageInfo != null ) {
			if (imageInfo.contains("/")) {
				String[] parts = imageInfo.split('/')
				repositoryName = parts[0]
				if (parts.length > 1 && parts[1].contains(":")) {
					String[] imageParts = parts[1].split(':')
					image = imageParts[0]
					version = imageParts[1]
				} else {
					image = parts[1]
				}
			} else {
				String[] parts = imageInfo.split(':')	
				image = imageInfo[0]
				if (parts.length > 1) {
					version = imageInfo[1]
				}
			}
		} 
		
		// ENV variables
        def envVars = serviceConfig.environment.entries?.collect{
			[environmentVariableName: it.key, type: 'string', value: it.value]
        }
		
		// port config
		def containerPort = ""
        def servicePort = ""
		def ports = serviceConfig.ports.portConfigs?.collect {
			[containerPort: it.target, servicePort: it.published]
		}
		
		/*
        def containerPort = ""
        def servicePort = ""
		String ports = serviceConfig?.ports
		print "PORTS!!: " + ports
		String[] port = ports.split(":");
		if(port[1].contains("tcp")) {
			containerPort = port[0].replaceAll("[^0-9]+", "") 
			servicePort = "tcp"
		} else {
			containerPort = port[0].replaceAll("[^0-9]+", "") 
			servicePort = port[1].replaceAll("[^0-9]+", "")
		}
		efService.service.port = servicePort */
		
		// Volumes
        def serviceVolumes = null
        def containerVolumes = null
        if(serviceConfig.volumes){
            def serviceVolumesList = []
            def containerVolumesList = []
            for(volume in serviceConfig.volumes){

                def volumeName, hostPath
                if(volume.type == "volume"){
                    volumeName = volume.source
                    hostPath = ""
                }else{
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
            }
            serviceVolumes = "'''[" + serviceVolumesList.join(",") + "\n]'''"
            containerVolumes = "'''[" + containerVolumesList.join(",") + "\n]'''"
        }
		efService.service.volume = serviceVolumes
		
		def container = [
            container: [
                containerName: efServiceName,
				command: serviceConfig.command?.parts?.join(',') ?: null,
				entryPoint: serviceConfig.entrypoint ?: null,
				repositoryName: repositoryName,
                image: image,
                version: version,
                memoryLimit: convertToMBs(serviceConfig.deploy?.resources?.limits?.memory),
				memorySize: convertToMBs(serviceConfig.deploy?.resources?.reservations?.memory),
				cpuLimit: serviceConfig.deploy?.resources?.limits?.nanoCpus,
				cpuCount: serviceConfig.deploy?.resources?.reservations?.nanoCpus,
				volumeMount: containerVolumes,
				port: ports
            ]
        ]
		
		efService.container = container
		efService.container.env = envVars
		
		// dependencies
        def processDependency = serviceConfig.dependsOn?.collect {
			[processDependencyName: it.dependency, targetProcessStepName: name, branchType: 'ALWAYS']
        }
		efService.service.processDependency = processDependency
		
		// Networks
		def networkListParams = null
        def subnetListParams = null
		def gatewayListParams = null
		if(serviceConfig.networks) {
		
			def networkList = []
			def subnetList = []
			def gatewayList = []
		
			for(network in serviceConfig.networks){
                def networkParam, subnetParam, gatewayParam
				networParam = network.network
                subnetParam = network.subnet
				gatewayParam = network.gateway

                networkList << """
                {
                    \"name\": \"${name}\",
                    \"network\": \"${networParam}\"
                }""".toString()

                subnetList << """
                {
                    \"name\": \"${volumeName}\",
                    \"subnet\": \"${subnetParam}\"
                }""".toString()
				
				gatewayList << """
                {
                    \"name\": \"${volumeName}\",
                    \"gateway\": \"${gatewayParam}\"
                }""".toString()
            }
			
            networkListParams = "'''[" + networkList.join(",") + "\n]'''"
            subnetListParams = "'''[" + subnetList.join(",") + "\n]'''"
			gatewayListParams = "'''[" + gatewayList.join(",") + "\n]'''"
		}
		
		efService.serviceMapping.networkList = networkListParams
		efService.serviceMapping.subnetList = subnetListParams
		efService.serviceMapping.gatewayList = gatewayListParams
		
        efService
    }
	
	def saveToEF(services, projectName, envProjectName, envName, clusterName) {
        def efServices = getServices(projectName)
        services.each { service ->
            createOrUpdateService(projectName, envProjectName, envName, clusterName, efServices, service)
        }

        def lines = ["Imported services: ${importedSummary.size()}"]
        importedSummary.each { serviceName, containers ->
            def containerNames = containers.collect { k -> k }
            lines.add("${serviceName}: ${containerNames.join(', ')}")
        }
        updateJobSummary(lines.join("\n"))
    }
	
	 def createOrUpdateService(projectName, envProjectName, envName, clusterName, efServices, service) {
        def existingService = efServices.find { s ->
            equalNames(s.serviceName, service.service.serviceName)
        }
        def result
        def serviceName

        logger DEBUG, "Service payload:"
        logger DEBUG, new JsonBuilder(service).toPrettyString()

        if (existingService) {
            serviceName = existingService.serviceName
            logger WARNING, "Service ${existingService.serviceName} already exists, skipping"
        }
        else {
            serviceName = service.service.serviceName
            result = createEFService(projectName, service)
            logger INFO, "Service ${serviceName} has been created"
            importedSummary[serviceName] = [:]
        }
        assert serviceName

        // Containers
        def efContainers = getContainers(projectName, serviceName)

        service.containers.each { container ->
            createOrUpdateContainer(projectName, serviceName, container, efContainers)
            mapContainerPorts(projectName, serviceName, container, service)
        }

        if (service.serviceMapping) {
            createOrUpdateMapping(projectName, envProjectName, envName, clusterName, serviceName, service)
        }

        // Add deploy process
        createDeployProcess(projectName, serviceName)
    }
	
	def mapContainerPorts(projectName, serviceName, container, service) {
        container.ports?.each { containerPort ->
            service.ports?.each { servicePort ->
                prettyPrint(servicePort)
                prettyPrint(containerPort)
                if (containerPort.portName == servicePort.portName || servicePort.targetPort == containerPort.name) {
                    def generatedPortName = "servicehttp${serviceName}${container.container.containerName}${containerPort.containerPort}"
                    def generatedPort = [
                        portName: generatedPortName,
                        listenerPort: servicePort.listenerPort,
                        subcontainer: container.container.containerName,
                        subport: containerPort.portName
                    ]
                    createPort(projectName, serviceName, generatedPort)
                    logger INFO, "Port ${generatedPortName} has been created for service ${serviceName}, listener port: ${generatedPort.listenerPort}, container port: ${generatedPort.subport}"
                }
            }
        }
    }
	
	def createOrUpdateMapping(projName, envProjName, envName, clusterName, serviceName, service) {
        def mapping = service.serviceMapping

        def envMaps = getEnvMaps(projName, serviceName)
        def existingMap = getExistingMapping(projName, serviceName, envProjName, envName)

        def envMapName
        if (existingMap) {
            logger INFO, "Environment map already exists for service ${serviceName} and cluster ${clusterName}"
            envMapName = existingMap.environmentMapName
        }
        else {
            def payload = [
                environmentProjectName: envProjName,
                environmentName: envName,
                description: CREATED_DESCRIPTION,
            ]

            def result = createEnvMap(projName, serviceName, payload)
            envMapName = result.environmentMap?.environmentMapName
        }

        assert envMapName

        def existingClusterMapping = existingMap?.serviceClusterMappings?.serviceClusterMapping?.find {
            it.clusterName == clusterName
        }

        def serviceClusterMappingName
        if (existingClusterMapping) {
            logger INFO, "Cluster mapping already exists"
            serviceClusterMappingName = existingClusterMapping.serviceClusterMappingName
        }
        else {
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
            def result = createServiceClusterMapping(projName, serviceName, envMapName, payload)
            logger INFO, "Created Service Cluster Mapping for ${serviceName} and ${clusterName}"
            serviceClusterMappingName = result.serviceClusterMapping.serviceClusterMappingName
        }

        assert serviceClusterMappingName

        service.containers?.each { container ->
            createServiceMapDetails(
                projName,
                serviceName,
                envMapName,
                serviceClusterMappingName,
                [containerName: container.container.containerName]
            )
        }
    }
	
	def getExistingMapping(projectName, serviceName, envProjectName, envName) {
        def envMaps = getEnvMaps(projectName, serviceName)
        def existingMap = envMaps.environmentMap?.find {
            it.environmentProjectName == envProjectName && it.projectName == projectName && it.serviceName == serviceName && it.environmentName == envName
        }
        existingMap
    }
	
	def createOrUpdateContainer(projectName, serviceName, container, efContainers) {
        def existingContainer = efContainers.find {
            equalNames(it.containerName, container.container.containerName)
        }
        def containerName
        def result
        logger DEBUG, "Container payload:"
        logger DEBUG, new JsonBuilder(container).toPrettyString()
        if (existingContainer) {
            containerName = existingContainer.containerName
            logger WARNING, "Container ${containerName} already exists, skipping"
        }
        else {
            containerName = container.container.containerName
            logger INFO, "Going to create container ${serviceName}/${containerName}"
            logger INFO, pretty(container.container)
            result = createContainer(projectName, serviceName, container.container)
            logger INFO, "Container ${serviceName}/${containerName} has been created"
            discoveredSummary[serviceName][containerName] = [:]
        }

        assert containerName
        def efPorts = getPorts(projectName, serviceName, /* appName */ null, containerName)
        container.ports.each { port ->
            createPort(projectName, serviceName, port, containerName)
            logger INFO, "Port ${port.portName} has been created for container ${containerName}, container port: ${port.containerPort}"
        }

        if (container.env) {
            container.env.each { env ->
                createEnvironmentVariable(projectName, serviceName, containerName, env)
                logger INFO, "Environment variable ${env.environmentVariableName} has been created"
            }
        }
    }
	
	def createDeployProcess(projectName, serviceName) {
        def processName = 'Deploy'
        def process = createProcess(projectName, serviceName, [processName: processName, processType: 'DEPLOY'])
        logger INFO, "Process ${processName} has been created for ${serviceName}"
        def processStepName = 'deployService'
        def processStep = createProcessStep(projectName, serviceName, processName, [
            processStepName: processStepName,
            processStepType: 'service', subservice: serviceName
        ])
        logger INFO, "Process step ${processStepName} has been created for process ${processName} in service ${serviceName}"
    }
	
	def createEFService(projectName, service) {
        def payload = service.service
        payload.description = "Created by EF Import Microservices"
        def result = createService(projectName, payload)
        result
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