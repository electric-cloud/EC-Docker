package dsl.flow

def names = args.params,
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