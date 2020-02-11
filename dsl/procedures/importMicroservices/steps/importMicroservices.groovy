$[/myProject/scripts/preamble]

// Input parameters
def dockerComposeContent = '''$[docker_compose_file_content]'''
def projectName = '$[docker_project]'
def applicationScoped = '$[docker_application_scoped]'
def applicationName = '$[docker_application]'
def environmentProjectName = '$[docker_environment_project]'
def environmentName = '$[docker_environment]'
def clusterName = '$[docker_cluster]'

EFClient efClient = new EFClient()
if(efClient.toBoolean(applicationScoped)) {
    if (!applicationName) {
        println "Application name is required for creating application-scoped microservices"
        System.exit(-1)
    }
} else {
	//reset application name since its not relevant if application_scoped is not set
    applicationName = null
}

if (environmentProjectName && environmentName && clusterName) {
    def clusters = efClient.getClusters(environmentProjectName, environmentName)
    def cluster = clusters.find {
        it.clusterName == clusterName
    }
    if (!cluster) {
        println "Cluster '${clusterName}' does not exist in '${environmentName}' environment."
        System.exit(-1)
    }
    if (cluster.pluginKey != 'EC-Docker') {
        println "Wrong cluster type: ${cluster.pluginKey}"
        println "CloudBees Flow cluster '${clusterName}' in '${environmentName}' environment is not backed by a Docker-based cluster."
        System.exit(-1)
    }
} else if (environmentProjectName || environmentName || clusterName) {
    // If any of the environment parameters are specified then *all* of them must be specified.
    println "Either specify all the parameters required to identify the Docker-backed CloudBees Flow cluster (environment project name, environment name, and cluster name) where the newly created microservice(s) will be deployed. Or do not specify any of the cluster related parameters in which case the service mapping to a cluster will not be created for the microservice(s)."
    System.exit(-1)
}



// write out the docker compose to the workspace directory
String dir = System.getenv('COMMANDER_WORKSPACE')
File composeFile = new File(dir, 'docker-compose.yml')
String composeContent = dockerComposeContent
composeFile << composeContent


def composeConfig
try {
    composeConfig = DockerClient.readCompose(composeFile)
    if(Double.parseDouble(composeConfig?.version) < 3) {
        println("ERROR: Unsupported version of Docker Compose file: version " + composeConfig?.version + ". Please use version 3 or above.")
        System.exit(-1)
    }
} catch (Exception ex) {
    println("ERROR: Failed to read the Docker Compose file contents")
    ex.printStackTrace()
    System.exit(-1)
}

// read the yaml file to collect networks params
Yaml parser = new Yaml()
def DELIMITER = "#"
def parsedYamlConfigList = []
def configList = dockerComposeContent.replace("\t", "").split(DELIMITER)
configList.each { config ->
    def parsedConfig = parser.load(config)
    parsedYamlConfigList.push(parsedConfig)
}

def importServices = new ImportMicroservices(composeConfig, parsedYamlConfigList)
def services = importServices.buildServicesDefinitions(projectName, applicationName)

importServices.saveToEF(services, projectName, environmentProjectName, environmentName, clusterName, applicationName)