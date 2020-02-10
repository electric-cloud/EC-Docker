$[/myProject/scripts/preamble]

println 'Using plugin @PLUGIN_NAME@'

// Procedure output handler
ProcedureOutputHandler procedureOutputHandler = new ProcedureOutputHandlerEF()


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
        // efClient.handleConfigurationError("Connection check for Docker endpoint '${endpoint}' failed: ${result.text}")

        def errMsg = "Connection check for Docker endpoint '${endpoint}' failed: ${result.text}"
        def suggestions = '''Reasons could be due to one or more of the following. Please ensure they are correct and try again:
1. Is your 'Docker Endpoint' correct?
2. Are your 'CA Certificate' and 'Client Certificate' correct?
3. Are your credentials correct?
   Are you able to use these credentials to work with docker using 'curl', 'wget', etc.?
'''
        procedureOutputHandler.addErrorOutcome()
        procedureOutputHandler.addErrorSummary(suggestions + "\n\n" + errMsg)
        procedureOutputHandler.setConfigError(suggestions + "\n\n" + errMsg)

        CommonUtils.logErrorDiag("Create Configuration failed.\n\n" + errMsg);
        CommonUtils.logInfoDiag(suggestions);

        procedureOutputHandler.bailOut()
    }
}
