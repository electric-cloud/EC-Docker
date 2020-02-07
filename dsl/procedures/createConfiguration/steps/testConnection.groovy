$[/myProject/scripts/preamble]

@Grab('log4j:log4j:1.2.17')

import org.apache.log4j.Logger

// Plugin logger
PluginLogger.init(configurationModel.getLogLevel()?.name())
Logger logger = PluginLogger.getLogger()

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
        procedureOutputHandler.addErrorSummary(errMsg)
        procedureOutputHandler.addErrorOutcome()

        CommonUtils.logErrorDiag("Create Configuration failed.\n\n" + errMsg);

        def suggestions = '''Reasons could be due to one or more of the following. Please ensure they are correct and try again:
1. Is your 'Docker Endpoint' correct?
2. Are your 'CA Certificate' and 'Client Certificate' correct?
3. Are your credentials correct?
   Are you able to use these credentials to work with BigIp using 'curl', 'wget', etc.?
'''
        CommonUtils.logInfoDiag(suggestions);

        procedureOutputHandler.bailOut()
    }
}
