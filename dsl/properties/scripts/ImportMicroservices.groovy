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
            logger INFO, "VOLUME CLASS: ${serviceConfig.volumes?.source.getClass()}"
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
		efService.service.minCapacity = serviceConfig.deploy?.updateConfig?.parallelism
		
		// image
		def image = ''
		def version = ''
		def repositoryName = '' 
		String imageInfo = serviceConfig?.image
		if (imageInfo != null) {
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
				image = parts[0]
				if (parts.length > 1) {
					version = parts[1]
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
        if(serviceConfig.ports){
            containerPort = serviceConfig.ports.portConfigs?.target
            servicePort = serviceConfig.ports.portConfigs?.published
        }
		
		efService.service.port = servicePort
		
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
				volumeMount: containerVolumeValue,
				port: containerPort
            ]
        ]
		
		efService.container = container
		efService.container.env = envVars
		
		// dependencies
        def processDependency = serviceConfig.dependsOn?.collect {
			[processDependencyName: it.dependency, targetProcessStepName: name, branchType: 'ALWAYS']
        }
		efService.service.processDependency = processDependency
		
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
	
	def saveToEF(services, projectName, envProjectName, envName, clusterName, applicationName) {
        def efServices = getServices(projectName)
        services.each { service ->
			if (service?.network == null) {
                createOrUpdateService(projectName, envProjectName, envName, clusterName, efServices, service, applicationName)
            }
        }

        def lines = ["Imported services: ${importedSummary.size()}"]
        importedSummary.each { serviceName, containers ->
            def containerNames = containers.collect { k -> k }
            lines.add("${serviceName}: ${containerNames.join(', ')}")
        }

        updateJobSummary(lines.join("\n"))
    }
	
	 def createOrUpdateService(def projectName, def envProjectName, def envName, def clusterName, def efServices, def service, def applicationName) {
        def existingService = efServices.find { s ->
            equalNames(s.serviceName, service.service.serviceName)
            logger INFO, "createOrUpdateService: efServices.find = ${s.serviceName}"
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
            result = createEFService(projectName, service, applicationName)
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
	
	def createEFService(projectName, service, applicationName) {
        def payload = service.service
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