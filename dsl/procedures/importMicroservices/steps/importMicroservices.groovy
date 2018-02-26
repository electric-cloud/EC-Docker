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

String dir = System.getenv('COMMANDER_WORKSPACE')
File composeFile = new File(dir, 'docker-compose.yml')
composeFile << dockerComposeContent

// read the compose file
def composeConfig = DockerClient.readCompose(composeFile.absolutePath)

ImportMicroservices importServices = new ImportMicroservices(composeConfig)
def services = importServices.buildServicesDefinitions(projectName, applicationName)

importServices.saveToEF(services, projectName, envProjectName, environmentName, clusterName)