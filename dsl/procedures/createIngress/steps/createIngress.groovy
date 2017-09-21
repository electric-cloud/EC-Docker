$[/myProject/scripts/preamble]

// Input parameters
String pluginConfig = '$[pluginConfig]'
def pluginProjectName = '$[/myProject/projectName]'
def networkName = '$[networkName]'
def subnetList = '$[subnetList]'
def gatewayList = '$[gatewayList]'
def enableIpv6 = '$[enableIpv6]'
def mtu = '$[mtu]'
def labels = '$[labels]'

EFClient efClient = new EFClient()
def pluginConfigValues = efClient.getConfigValues('ec_plugin_cfgs', pluginConfig, pluginProjectName)
DockerClient dockerClient = new DockerClient(pluginConfigValues)

dockerClient.createIngress(networkName,
						  subnetList.split(","),
						  gatewayList.split(","),
						  enableIpv6,
						  mtu,
						  labels)
