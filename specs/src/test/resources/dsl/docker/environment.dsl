package dsl.docker

def names = args.names,
    configName = names.configName

project 'dockerProj', {
    resourceName = null
    workspaceName = null

    environment 'docker-environment', {
        environmentEnabled = '1'
        projectName = 'dockerProj'
        reservationRequired = '0'
        rollingDeployEnabled = null
        rollingDeployType = null

        cluster 'docker-cluster', {
            environmentName = 'docker-environment'
            pluginKey = 'EC-Docker'
            pluginProjectName = null
            providerClusterName = null
            providerProjectName = null
            provisionParameter = [
                    'config': configName,
            ]
            provisionProcedure = 'Check Cluster'

            // Custom properties

            property 'ec_provision_parameter', {

                // Custom properties
                config = configName
            }
        }
    }
}