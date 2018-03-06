$[/myProject/scripts/preamble]

String configName = '$[config]'
String artifactName = '$[ec_docker_artifactName]'
String imageName = '$[ec_docker_imageName]'
String registryUrl = '$[ec_docker_registryUrl]'

//Credential

String baseName = '$[ec_docker_baseImage]'
String ports = '''
$[ec_docker_ports]
'''
String command = '''
$[ec_docker_command]
'''

String env = '''
$[ec_docker_env]
'''

//// -- Driver script logic to provision cluster -- //
EFClient efClient = new EFClient()
String projectName = '$[/myProject/projectName]'
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, projectName)
DockerClient dockerClient = new DockerClient(pluginConfig)

def liftAndShift = new LiftAndShift(
	dockerClient: dockerClient,
)