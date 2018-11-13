package dsl.flow

def names = args.params,
    replicas = names.replicas,
    sourceVolume = names.sourceVolume,
    targetVolume = names.targetVolume


project 'dockerProj', {

    application 'nginx-application', {
        description = ''

        service 'nginx-service', {
            applicationName = 'nginx-application'
            defaultCapacity = replicas.toString()
            maxCapacity = (replicas + 1).toString()
            minCapacity = '1'
            volume = sourceVolume

            container 'nginx-container', {
                description = ''
                applicationName = 'nginx-application'
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
                    applicationName = 'nginx-application'
                    containerName = 'nginx-container'
                    containerPort = '80'
                    projectName = 'dockerProj'
                    serviceName = 'nginx-service'
                }
            }

            port '_servicehttpnginx-container01542017110575', {
                applicationName = 'nginx-application'
                listenerPort = '81'
                projectName = 'dockerProj'
                serviceName = 'nginx-service'
                subcontainer = 'nginx-container'
                subport = 'http'
            }

            process 'Deploy', {
                processType = 'DEPLOY'
                serviceName = 'nginx-service'

                processStep 'deployService', {
                    alwaysRun = '0'
                    errorHandling = 'failProcedure'
                    processStepType = 'service'
                    useUtilityResource = '0'
                }
            }

            process 'Undeploy', {
                processType = 'UNDEPLOY'
                serviceName = 'nginx-service'

                processStep 'Undeploy', {
                    alwaysRun = '0'
                    dependencyJoinType = 'and'
                    errorHandling = 'abortJob'
                    processStepType = 'service'
                    subservice = 'nginx-service'
                    useUtilityResource = '0'
                }
            }
        }

        process 'Deploy', {
            applicationName = 'nginx-application'
            processType = 'OTHER'

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            formalParameter 'ec_nginx-service-run', defaultValue: '1', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            processStep 'Deploy', {
                alwaysRun = '0'
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                processStepType = 'process'
                subcomponentApplicationName = 'nginx-application'
                subservice = 'nginx-service'
                subserviceProcess = 'Deploy'
                useUtilityResource = '0'

                // Custom properties

                property 'ec_deploy', {

                    // Custom properties
                    ec_notifierStatus = '0'
                }
            }

            // Custom properties

            property 'ec_deploy', {

                // Custom properties
                ec_notifierStatus = '0'
            }
        }

        process 'Undeploy', {
            applicationName = 'nginx-application'
            processType = 'OTHER'

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            formalParameter 'ec_nginx-service-run', defaultValue: '1', {
                expansionDeferred = '1'
                required = '0'
                type = 'checkbox'
            }

            processStep 'Undeploy', {
                alwaysRun = '0'
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                processStepType = 'process'
                subcomponentApplicationName = 'nginx-application'
                subservice = 'nginx-service'
                subserviceProcess = 'Undeploy'
                useUtilityResource = '0'

                // Custom properties

                property 'ec_deploy', {

                    // Custom properties
                    ec_notifierStatus = '0'
                }
            }

            // Custom properties

            property 'ec_deploy', {

                // Custom properties
                ec_notifierStatus = '0'
            }
        }

        tierMap 'ab0a2553-e3fd-11e8-a1e3-00505696d38e', {
            applicationName = 'nginx-application'
            environmentName = 'docker-environment'
            environmentProjectName = 'dockerProj'
            projectName = 'dockerProj'

            serviceClusterMapping 'ab91a538-e3fd-11e8-8359-00505696d38e', {
                actualParameter = [
                        'networkList': 'test-network',
                ]
                clusterName = 'docker-cluster'
                serviceName = 'nginx-service'
                tierMapName = 'ab0a2553-e3fd-11e8-a1e3-00505696d38e'

                serviceMapDetail 'nginx-container', {
                    serviceMapDetailName = 'cb7910a3-e3fd-11e8-8359-00505696d38e'
                    serviceClusterMappingName = 'ab91a538-e3fd-11e8-8359-00505696d38e'
                }
            }
        }

        property 'ec_deploy', {

            // Custom properties
            ec_notifierStatus = '0'
        }
    }
}