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
                //TODO: Handle registry urls of the form:
                // ecdocker/motorbike:v1 - this is a public DockerHub registry example so no need to specify the registry url
                // gcr.io/google_samples/gb-redisslave:v1 - this is a Google Container registry url url/user/image:version
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
            registryUri: repositoryName,
            imageName: image,
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
	
	def saveToEF(services, projectName, envProjectName, envName, clusterName, applicationName) {
        def efServices = getServices(projectName)
        services.each { service ->
            createService(projectName, envProjectName, envName, clusterName, efServices, service, applicationName)
        }

        def lines = ["Imported services: ${importedSummary.size()}"]
        importedSummary.each { serviceName, containers ->
            def containerNames = containers.collect { k -> k }
            lines.add("${serviceName}: ${containerNames.join(', ')}")
        }

        updateJobSummary(lines.join("\n"))
    }
	
	 def createService(def projectName, def envProjectName, def envName, def clusterName, def efServices, def service, def applicationName) {
        def existingService = efServices.find { s ->
            logger INFO, "createService: efServices.find = ${s.serviceName}"
            equalNames(s.serviceName, service.service.serviceName)
        }
        def result
        def serviceName

        logger INFO, "Service payload:"
        logger INFO, prettyPrint(service)

        if (existingService) {
            serviceName = existingService.serviceName
            logger WARNING, "Service ${existingService.serviceName} already exists, skipping"
            // return since we do not want to update an existing service.
            return
        }
        else {
            serviceName = service.service.serviceName
            result = createEFService(projectName, service, applicationName)
            logger INFO, "Service ${serviceName} has been created"
            importedSummary[serviceName] = [:]
        }
        assert serviceName

        // Container - Docker service has only one container
         createEFContainer(projectName, serviceName, service.container)
         createEFPorts(projectName, serviceName, service.container, service.service)

        if (service.serviceMapping && envProjectName && envName && clusterName) {
            createOrUpdateMapping(projectName, envProjectName, envName, clusterName, serviceName, service)
        }

        // Add deploy process
        createDeployProcess(projectName, serviceName)			
    }
	
	def createEFPorts(projectName, serviceName, container, service) {
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
            createContainerPort(projectName, serviceName, containerName, generatedPort)
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
            createServicePort(projectName, serviceName, generatedPort)
            logger INFO, "Port ${servicePortName} has been created for service ${serviceName}, listener port: ${generatedPort.listenerPort}, container port: ${generatedPort.subport}"

        }
    }
	
	def createOrUpdateMapping(projName, envProjName, envName, clusterName, serviceName, service) {
        assert envProjectName
        assert envName
        assert clusterName
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
                [containerName: container.containerName]
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
	
	def createEFContainer(projectName, serviceName, container) {
        logger DEBUG, "Container payload:"
        logger DEBUG, prettyPrint(container)

        def containerName = container.containerName
        assert containerName
        logger INFO, "Going to create container ${serviceName}/${containerName}"
        logger INFO, prettyPrint(container)
        createContainer(projectName, serviceName, container)
        logger INFO, "Container ${serviceName}/${containerName} has been created"

        //creating container ports at the same time as service ports in createEFPorts
        /*container.ports?.each { port ->
            createPort(projectName, serviceName, port, containerName)
            logger INFO, "Port ${port.portName} has been created for container ${containerName}, container port: ${port.containerPort}"
        }*/

        container.env?.each { env ->
            createEnvironmentVariable(projectName, serviceName, containerName, env)
            logger INFO, "Environment variable ${env.environmentVariableName} has been created"
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
	
}