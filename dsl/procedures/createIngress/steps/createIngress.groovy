$[/myProject/scripts/preamble]

// Input parameters
String serviceProjectName = '$[serviceProjectName]'
String clusterName = '$[clusterName]'
String envProjectName = '$[envProjectName]'
// default cluster project name if not explicitly set
if (!envProjectName) {
    envProjectName = serviceProjectName
}
String environmentName = '$[environmentName]'
def pluginProjectName = '$[/myProject/projectName]'

def subnetList = '$[subnetList]'
def gatewayList = '$[gatewayList]'
def enableIpv6 = '$[enableIpv6]'
def attachable = '$[attachable]'
def mtu = '$[mtu]'
def labels = '$[labels]'

EFClient efClient = new EFClient()

def clusterParameters = efClient.getProvisionClusterParameters(clusterName, envProjectName, environmentName)
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', clusterParameters.config, pluginProjectName)
DockerClient dockerClient = new DockerClient(pluginConfig)

dockerClient.createIngress(subnetList.split(","),
							  gatewayList.split(","),
							  enableIpv6,
							  attachable,
							  mtu,
							  labels)