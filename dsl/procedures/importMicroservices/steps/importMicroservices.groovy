$[/myProject/scripts/preamble]

// Input parameters
def dockerComposeContent = '''$[docker_compose_file_content]'''
def projectName = '$[project]'
def applicationScoped = '$[application_scoped]'
def applicationName = '$[application]'
def environmentProjectName = '$[environment_project]'
def environmentName = '$[environment]'
def clusterName = '$[cluster]'

EFClient efClient = new EFClient()

// write out the docker compose to the workspace directory
String dir = System.getenv('COMMANDER_WORKSPACE')
File composeFile = new File(dir, 'docker-compose.yml')
composeFile << dockerComposeContent

def composeConfig = DockerClient.readCompose(composeFile)

// read the yaml file to collect networks params
Yaml parser = new Yaml()
def DELIMITER = "#" 
def parsedYamlConfigList = []
def configList = dockerComposeContent.split(DELIMITER)  
configList.each { config ->   
	def parsedConfig = parser.load(config)   
	parsedYamlConfigList.push(parsedConfig)  
}

ImportMicroservices importServices = new ImportMicroservices(composeConfig, parsedYamlConfigList)
def services = importServices.buildServicesDefinitions(projectName, applicationName)

importServices.saveToEF(services, projectName, environmentProjectName, environmentName, clusterName)