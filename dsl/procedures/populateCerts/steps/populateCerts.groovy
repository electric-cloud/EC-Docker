$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

def workspaceDir = System.getenv("COMMANDER_WORKSPACE")
def pathSeparator = File.separator

File cacertFile = new File("${workspaceDir}${pathSeparator}ca.pem")
cacertFile.text = pluginConfig.credential_cacert.password

File clientcertFile = new File("${workspaceDir}${pathSeparator}cert.pem")
clientcertFile.text = pluginConfig.credential_cert.password

File clientkeyFile = new File("${workspaceDir}${pathSeparator}key.pem")
clientkeyFile.text = pluginConfig.credential_key.password