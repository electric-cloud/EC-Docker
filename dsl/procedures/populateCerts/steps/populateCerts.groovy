$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

def tempDir = System.getProperty("java.io.tmpdir")
// Create certs dir in temp directory to hold certs
def certDir = new File("${tempDir}/certs")
if(!certDir.exists())
{
	new File("${tempDir}/certs").mkdir() 
}
 
File cacertFile = new File("${tempDir}/certs/ca.pem")
cacertFile.text = pluginConfig.credential_cacert.password

File clientcertFile = new File("${tempDir}/certs/cert.pem")
clientcertFile.text = pluginConfig.credential_cert.password

File clientkeyFile = new File("${tempDir}/certs/key.pem")
clientkeyFile.text = pluginConfig.credential_key.password