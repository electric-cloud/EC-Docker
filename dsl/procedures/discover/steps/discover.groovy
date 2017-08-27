$[/myProject/scripts/preamble]

// Input parameters
def dockerComposeContent = '''$[docker_compose_file_content]'''
def projectName = '$[project]'
def applicationName = '$[application]'
def recreateApplication = '$[recreate_application]'

EFClient efClient = new EFClient()
def app = efClient.getApplication(projectName, applicationName)
if (app) {
    if (recreateApplication) {
        efClient.deleteApplication(projectName, applicationName)
    } else {
        println "Application $applicationName already exists"
        return
    }
}


//1. write out the docker compose to the workspace directory
String dir = System.getenv('COMMANDER_WORKSPACE')
File composeFile = new File(dir, 'docker-compose.yml')
composeFile << dockerComposeContent

// 2. read the compose file
def composeConfig = DockerClient.readCompose(composeFile.absolutePath)

//3. build out the dsl for each service and container within
def dslStr = efClient.buildApplicationDsl(projectName, applicationName, composeConfig)
println "Application DSL for the Docker Compose content:\n $dslStr"

//4. run eval dsl for application
efClient.evalDsl(dslStr)

