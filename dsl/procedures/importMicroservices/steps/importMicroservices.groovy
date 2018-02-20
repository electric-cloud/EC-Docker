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
def clusterParameters = efClient.getProvisionClusterParameters(clusterName, environmentProjectName, environmentName)

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', clusterParameters.config, pluginProjectName)
DockerClient dockerClient = new DockerClient(pluginConfig)

String dir = System.getenv('COMMANDER_WORKSPACE')
File composeFile = new File(dir, 'docker-compose.yml')
composeFile << dockerComposeContent

// read the compose file
def composeConfig = DockerClient.readCompose(composeFile.absolutePath)




/*
//custom project name by using the -p command line option or the COMPOSE_PROJECT_NAME

EFClient efClient = new EFClient()

//1. write out the docker compose to the workspace directory
String dir = System.getenv('COMMANDER_WORKSPACE')
File composeFile = new File(dir, 'docker-compose.yml')
composeFile << dockerComposeContent

 //// -- Driver script logic to provision cluster -- //
def clusterParameters = efClient.getProvisionClusterParameters(clusterName, environmentProjectName, environmentName)
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', clusterParameters.config, pluginProjectName)

// 2. read the compose file
def composeConfig = DockerClient.readCompose(composeFile.absolutePath)

//3. build out the dsl for each service and container within
def dslStr = efClient.buildApplicationDsl(projectName, applicationName, composeConfig)
println "Application DSL for the Docker Compose content:\n $dslStr"

//4. run eval dsl for application
efClient.evalDsl(dslStr)
*/