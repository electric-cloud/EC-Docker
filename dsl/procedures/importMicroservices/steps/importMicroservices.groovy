$[/myProject/scripts/preamble]

// Input parameters
def dockerComposeContent = '''$[ec_docker_compose_file_content]'''
def projectName = '$[ec_docker_project]'
def applicationScoped = '$[ec_docker_application_scoped]'
def applicationName = '$[ec_docker_application]'
def environmentProjectName = '$[ec_docker_environment_project]'
def environmentName = '$[ec_docker_environment]'
def clusterName = '$[ec_docker_cluster]'

EFClient efClient = new EFClient()

// write out the docker compose to the workspace directory
String dir = System.getenv('COMMANDER_WORKSPACE')
File composeFile = new File(dir, 'docker-compose.yml')
composeFile << dockerComposeContent

def composeConfig = DockerClient.readCompose(composeFile)

if(efClient.toBoolean(applicationScoped)) {
	if (!applicationName) {
		println "Application name is required for creating application-scoped microservices"
		System.exit(-1)
	}
} else {
	applicationName = null
}

if (environmentName && clusterName && environmentProjectName) {
	def clusters = efClient.getClusters(environmentProjectName, environmentName)
	def cluster = clusters.find {
		it.clusterName == clusterName
	}
	if (!cluster) {
		println "Cluster '${clusterName}' does not exist in '${envName}' environment."
		System.exit(-1)
	}
	if (cluster.pluginKey != 'EC-Kubernetes') {
		println "Wrong cluster type: ${cluster.pluginKey}"
		println "ElectricFlow cluster '${clusterName}' in '${envName}' environment is not backed by a Kubernetes-based cluster."
		System.exit(-1)
	}
} else if (environmentName || clusterName || environmentProjectName) {
	// If any of the environment parameters are specified then *all* of them must be specified.
	println "Either specify all the parameters required to identify the Kubernetes-backed ElectricFlow cluster (environment project name, environment name, and cluster name) where the newly created microservice(s) will be deployed. Or do not specify any of the cluster related parameters in which case the service mapping to a cluster will not be created for the microservice(s)."
	System.exit(-1)
}

// read the yaml file to collect networks params
Yaml parser = new Yaml()
def DELIMITER = "#" 
def parsedYamlConfigList = []
def configList = dockerComposeContent.split(DELIMITER)  
configList.each { config ->   
	def parsedConfig = parser.load(config)   
	parsedYamlConfigList.push(parsedConfig)  
}

def importServices = new ImportMicroservices(composeConfig, parsedYamlConfigList)
def services = importServices.buildServicesDefinitions(projectName, applicationName)

importServices.saveToEF(services, projectName, environmentProjectName, environmentName, clusterName, applicationName)