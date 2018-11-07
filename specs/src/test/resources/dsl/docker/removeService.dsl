package dsl.docker

def names = args.params,
    config = names.config,
    service = names.service
    resource = names.resource


// Create plugin configuration
def pluginProjectName = getPlugin(pluginName: 'EC-Docker').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Remove Docker Service',
        resourceName: resource,
        actualParameter: [
                config: config,
                serviceName: service
        ]
)
