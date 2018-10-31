package dsl.docker

def names = args.names,
    templateYaml = names.templateYaml
    projectName = names.projectName
    applicationScoped = names.applicationScoped
    applicationName = names.applicationName
    envProjectName = names.envProjectName
    environmentName = names.environmentName
    clusterName = names.clusterName


// Create plugin configuration
def pluginProjectName = getPlugin(pluginName: 'EC-Docker').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Import Microservices',
        actualParameter: [
                docker_compose_file_content: templateYaml,
                docker_project: projectName,
                docker_application_scoped: applicationScoped,
                docker_application: applicationName,
                docker_environment_project: envProjectName,
                docker_environment: environmentName,
                docker_cluster: clusterName
        ]
)