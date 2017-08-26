$[/myProject/scripts/preamble]

EFClient efClient = new EFClient()
def actualParams = efClient.getActualParameters()

if (efClient.toBoolean(actualParams.get('testConnection'))) {

	def cred = efClient.getCredentials('credential')
	// add the credential values to the actual params for 'CreateConfiguration'
	// before calling the DockerClient constructor
	actualParams["credential"] = [userName: cred.userName, password: cred.password]

	def result = DockerClient.checkConnection(actualParams)
	if (!result.success) {
		def endpoint = actualParams.get('endpoint')
		efClient.handleConfigurationError("Connection check for Docker endpoint '${endpoint}' failed: ${result.text}")
	}
}