package dsl.docker

def names = args.names,
        projectName = names.projectName,
        serviceName = names.serviceName

getEnvironmentMaps(
        projectName: projectName,
        serviceName: serviceName
)