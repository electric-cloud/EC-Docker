package dsl.docker

def names = args.params,
    service = names.service
    serviceRevId = names.serviceRevId
    serviceProject = names.serviceProject
    application = names.application
    applicationRevId = names.applicationRevId
    cluster = names.cluster
    envProject = names.envProject
    envName = names.envName
        resource = names.resource


// Create plugin configuration
def pluginProjectName = getPlugin(pluginName: 'EC-Docker').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Deploy Service',
        resourceName: resource,
        actualParameter: [
                applicationName: application,
                applicationRevisionId: applicationRevId,
                clusterName: cluster,
                clusterOrEnvProjectName: envProject,
                environmentName: envName,
                resultsPropertySheet: '',
                serviceEntityRevisionId: serviceRevId,
                serviceName: service,
                serviceProjectName: serviceProject
        ]
)
