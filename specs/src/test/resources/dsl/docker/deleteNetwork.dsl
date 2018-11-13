package dsl.docker

def names = args.params,
    config = names.config,
    network = names.network,
    resource = names.resource


// Create plugin configuration
def pluginProjectName = getPlugin(pluginName: 'EC-Docker').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Delete Network',
        resourceName: resource,
        actualParameter: [
                pluginConfig: config,
                networkName: network
        ]
)
