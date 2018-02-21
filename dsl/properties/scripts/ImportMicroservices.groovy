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
	def dockerClient
    def pluginConfig
    def clusterEndpoint
	def composeConfig
    def importedSummary = [:]
	
	static final String CREATED_DESCRIPTION = "Created by Container ImportMicroservices"
	
	def ImportMicroservices(def dockerClient, def pluginConfig, def composeConfig) {
        dockerClient = dockerClient
        pluginConfig = params.pluginConfig
        accessToken = kubeClient.retrieveAccessToken(pluginConfig)
		this.composeConfig = composeConfig
        clusterEndpoint = pluginConfig.clusterEndpoint
    }
	
	def buildServicesDefinitions(def projectName, def applicationName) {
		def efServices = []
		composeConfig.services.each { name, serviceConfig ->
			def efService = buildServiceDefinition(name, projectName, applicationName, serviceConfig)
			efServices.push(efService)
        }
		efServices
	}
	
	def buildServiceDefinition(def name, def projectName, def applicationName, def serviceConfig) {
        def efServiceName = name
        def efService = [
            service: [
                name: efServiceName
            ],
            serviceMapping: [:]
        ]

        // Service Fields
		String[] imageInfo = serviceConfig.image?.split(':')
        def image = ''
        def version = ''
        if (imageInfo && imageInfo.length > 0) {
            image = imageInfo[0]
            if (imageInfo.length > 1) {
                version = imageInfo[1]
            }
        }
		efService.service.image = image
		efService.service.version = version
		
		efService.service.command = serviceConfig.command?.parts?.join(',') ?:null
        efService.service.entrypoint = serviceConfig.entrypoint?:null
        efService.service.defaultCapacity = serviceConfig.deploy?.replicas
		efService.service.minCapacity = serviceConfig.updateConfig?.parallelism
		
		// Limits and reservations
        efService.service.memoryLimit = convertToMBs(serviceConfig.deploy?.resources?.limits?.memory)
        efService.service.memorySize = convertToMBs(serviceConfig.deploy?.resources?.reservations?.memory)
        efService.service.cpuLimit = serviceConfig.deploy?.resources?.limits?.nanoCpus
        efService.service.cpuCount = serviceConfig.deploy?.resources?.reservations?.nanoCpus
		
		// ENV variables
        def envVars = ""
        serviceConfig.environment.entries.each{key, value ->
            envVars += """
            environmentVariable '$key', {
                type = 'string'
                value = '$value'
            }""".toString()
        }
		efService.service.environmentVariable = envVars
		//serviceMapping
		
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
		efService.container.port = containerPort
		efService.service.port = servicePort
		
		// Volumes
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
		efService.container.volumeMount = containerVolumes
		efService.service.volume = serviceVolumes
		
		// dependencies
		def processDependency = ''
        serviceConfig.dependsOn.each{ dependency ->
            processDependency += """
            processDependency '$dependency', targetProcessStepName: '$name', {
               branchType = 'ALWAYS'
            }""".toString()
        }
		efService.service.processDependency = processDependency
		
		// Networks
		
		
        efService
    }
	
	def saveToEF(services, projectName, envProjectName, envName, clusterName) {
        /**
		 *
		 */
    }