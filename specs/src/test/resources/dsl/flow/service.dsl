package dsl.flow


def names = args.params,
        replicas = names.replicas,
        sourceVolume = names.sourceVolume,
        targetVolume = names.targetVolume

project 'dockerProj', {

    service 'nginx-service', {
        defaultCapacity = replicas.toString()
        maxCapacity = (replicas + 1).toString()
        minCapacity = '1'
        volume = sourceVolume

        container 'nginx-container', {
            description = ''
            cpuCount = '0.1'
            cpuLimit = '2'
            imageName = 'nginx'
            imageVersion = 'latest'
            memoryLimit = '255'
            memorySize = '128'
            serviceName = 'nginx-service'
            volumeMount = targetVolume
            environmentVariable 'NGINX_PORT', {
                type = 'string'
                value = '80'
            }

            port 'http', {
                containerName = 'nginx-container'
                containerPort = '80'
                projectName = 'dockerProj'
                serviceName = 'nginx-service'
            }
        }

        environmentMap '37e487e1-584d-11e8-8102-00155d01ef00', {
            environmentName = 'docker-environment'
            environmentProjectName = 'dockerProj'
            projectName = 'dockerProj'
            serviceName = 'nginx-service'

            serviceClusterMapping '603c4a58-584d-11e8-bc66-00155d01ef00', {
                actualParameter = [
                        'networkList': 'test-network'
                ]
                clusterName = 'docker-cluster'
                environmentMapName = '37e487e1-584d-11e8-8102-00155d01ef00'
                serviceName = 'nginx-service'

                serviceMapDetail 'nginx-container', {
                    serviceMapDetailName = '7990836a-584d-11e8-b4aa-00155d01ef00'
                    serviceClusterMappingName = '603c4a58-584d-11e8-bc66-00155d01ef00'
                }
            }
        }

        port '_servicehttpnginx-container01526394857739', {
            applicationName = null
            listenerPort = '81'
            projectName = 'dockerProj'
            serviceName = 'nginx-service'
            subcontainer = 'nginx-container'
            subport = 'http'
        }

        process 'Deploy', {
            applicationName = null
            processType = 'DEPLOY'
            serviceName = 'nginx-service'
            smartUndeployEnabled = null
            timeLimitUnits = null
            workingDirectory = null
            workspaceName = null

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                label = null
                orderIndex = null
                required = '0'
                type = 'checkbox'
            }

            processStep 'deploy', {
                afterLastRetry = null
                alwaysRun = '0'
                applicationTierName = null
                componentRollback = null
                dependencyJoinType = null
                errorHandling = 'failProcedure'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                processStepType = 'service'
                retryCount = null
                retryInterval = null
                retryType = null
                rollbackSnapshot = null
                rollbackType = null
                rollbackUndeployProcess = null
                skipRollbackIfUndeployFails = null
                smartRollback = null
                subcomponent = null
                subcomponentApplicationName = null
                subcomponentProcess = null
                subprocedure = null
                subproject = null
                subservice = null
                subserviceProcess = null
                timeLimitUnits = null
                useUtilityResource = '0'
                utilityResourceName = null
                workingDirectory = null
                workspaceName = null
            }

            property 'ec_deploy', {

                // Custom properties
                ec_notifierStatus = '0'
            }
        }

        process 'Undeploy', {
            applicationName = null
            processType = 'UNDEPLOY'
            serviceName = 'nginx-service'
            smartUndeployEnabled = null
            timeLimitUnits = null
            workingDirectory = null
            workspaceName = null

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                label = null
                orderIndex = null
                required = '0'
                type = 'checkbox'
            }

            processStep 'Undeploy', {
                afterLastRetry = null
                alwaysRun = '0'
                applicationTierName = null
                componentRollback = null
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                processStepType = 'service'
                retryCount = null
                retryInterval = null
                retryType = null
                rollbackSnapshot = null
                rollbackType = null
                rollbackUndeployProcess = null
                skipRollbackIfUndeployFails = null
                smartRollback = null
                subcomponent = null
                subcomponentApplicationName = null
                subcomponentProcess = null
                subprocedure = null
                subproject = null
                subservice = 'nginx-service'
                subserviceProcess = null
                timeLimitUnits = null
                useUtilityResource = '0'
                utilityResourceName = null
                workingDirectory = null
                workspaceName = null

                property 'ec_deploy', {
                    ec_notifierStatus = '0'
                }
            }

            property 'ec_deploy', {

                // Custom properties
                ec_notifierStatus = '0'
            }
        }

        property 'ec_deploy', {

            // Custom properties
            ec_notifierStatus = '0'
        }
        jobCounter = '2'
    }
}
