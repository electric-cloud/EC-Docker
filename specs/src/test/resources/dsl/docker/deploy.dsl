package dsl.docker

def names = args.names,
        project = names.project,
        environment = names.environment,
        service = names.service,
        envProject = names.envProject

runServiceProcess(
        projectName: project,
        serviceName: service,
        environmentName: environment,
        environmentProjectName: envProject,
        processName: 'Deploy',
)