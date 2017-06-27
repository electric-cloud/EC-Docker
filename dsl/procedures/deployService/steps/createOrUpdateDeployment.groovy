$[/myProject/scripts/preamble]

//// Input parameters
String serviceName = '$[serviceName]'
String serviceProjectName = '$[serviceProjectName]'
String applicationName = '$[applicationName]'
String clusterName = '$[clusterName]'
String clusterOrEnvProjectName = '$[clusterOrEnvProjectName]'
// default cluster project name if not explicitly set
if (!clusterOrEnvProjectName) {
    clusterOrEnvProjectName = serviceProjectName
}
String environmentName = '$[environmentName]'
String applicationRevisionId = '$[applicationRevisionId]'

String resultsPropertySheet = '$[resultsPropertySheet]'
if (!resultsPropertySheet) {
    resultsPropertySheet = '/myParent/parent'
}
//// -- Driver script logic to provision cluster -- //
EFClient efClient = new EFClient()
def clusterParameters = efClient.getProvisionClusterParameters(clusterName, clusterOrEnvProjectName, environmentName)

DockerClient dockerClient = new DockerClient()

def pluginConfig = dockerClient.getPluginConfig(efClient, clusterName, clusterOrEnvProjectName, environmentName)
def clusterEndpoint = pluginConfig.endpoint

println "serviceName: ${serviceName} serviceProjectName:${serviceProjectName} applicationName:${applicationName} clusterName:${clusterName} clusterOrEnvProjectName:${clusterOrEnvProjectName} environmentName:${environmentName} clusterEndpoint:${clusterEndpoint} applicationRevisionId:${applicationRevisionId}"

dockerClient.deployService(
		        efClient,    
		        clusterEndpoint,  
		        serviceName,
		        serviceProjectName,
		        applicationName,
		        applicationRevisionId,
		        clusterName,
		        clusterOrEnvProjectName,
		        environmentName,
		        resultsPropertySheet)