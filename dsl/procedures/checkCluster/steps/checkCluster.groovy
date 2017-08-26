$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

def result = DockerClient.checkConnection(pluginConfig)
if (!result.success) {
    def endpoint = pluginConfig.get('endpoint')
    efClient.handleProcedureError("Connection check for Docker endpoint '${endpoint}' failed: ${result.text}")
}
