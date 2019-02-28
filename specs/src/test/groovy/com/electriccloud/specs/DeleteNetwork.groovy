package com.electriccloud.specs

import spock.lang.*

class DeleteNetwork extends DockerHelper {
    @Shared
    def procedureName = "Delete Network",
        projectName = "EC Specs $procedureName",
        resourceName = "local"

    @Shared
    def TC = [
            C1: [ ids: 'C1', description: 'with required fields'],
            C2: [ ids: 'C2', description: 'run without configuration name'],
            C3: [ ids: 'C3', description: 'run without network name'],
            C4: [ ids: 'C4', description: 'run with invalid config name'],
            C5: [ ids: 'C5', description: 'run with non-exist network name']
    ]


    def doSetupSpec() {
        createConfig(configSwarm,getDockerCommunityEndpoint())
        dslFile "dsl/RunProcedure.dsl", [ projectName  : projectName, resourceName : resourceName, procedureName: 'Create Ingress', params: createIngressParams ]
        dslFile "dsl/RunProcedure.dsl", [ projectName  : projectName, resourceName : resourceName, procedureName: procedureName, params: deleteNetworkParams ]
    }

    def doCleanupSpec() {
        deleteConfig(configSwarm)
        conditionallyDeleteProject(projectName)
    }

    @Unroll
    def 'Positive test. #caseId'() {
        setup: 'Define the parameters for Procedure running'
        createNetwork( pluginConfig, networkName )

        def runParams = [
                // Required
                pluginConfig : pluginConfig,
                networkName  : networkName
        ]

        when: 'Procedure runs: '
        def result = runTestedProcedure(projectName, procedureName, runParams)

        then: 'Wait until job run is completed: '
        def debugLog =  readJobLogs(result.jobId)

        expect: 'Outcome and Upper Summary verification'
        assert result.outcome == 'success'
        assert debugLog  =~ /docker network rm/

        cleanup:

        where: 'The following params will be: '
        caseId | pluginConfig | networkName
        TC.C1  | configSwarm  | 'test-network'

    }

    @Unroll
    def 'Negative test. #caseId'() {
        setup: 'Define the parameters for Procedure running'

        def runParams = [
                // Required
                pluginConfig : pluginConfig,
                networkName  : networkName
        ]

        when: 'Procedure runs: '
        def result = runTestedProcedure(projectName, procedureName, runParams)

        then: 'Wait until job run is completed: '
        def debugLog = result.logs

        expect: 'Outcome and Upper Summary verification'
        assert result.outcome == 'error'
        assert debugLog  =~ /$status/

        where: 'The following params will be: '
        caseId | pluginConfig    | networkName     | status
        TC.C2  | ''              | 'testNetwork'   | "ERROR: Configuration  does not exist!"
        TC.C3  | configSwarm     | ''              | "docker network rm failed"
        TC.C4  | 'invalidConfig' | ''              | "ERROR: Configuration $pluginConfig does not exist!"
        TC.C5  | configSwarm     | 'invalidNetwork'| "network $networkName not found"
    }

    def createNetwork(String configName, networkName){
        def runParams = [
                pluginConfig : configName,
                networkName  : networkName,
                subnetList   : '',
                gatewayList  : '',
                enableIpv6   : '',
                mtu          : '',
                labels       : ''
        ]
        def result = runTestedProcedure(projectName, 'Create Ingress', runParams)
        assert result.outcome == 'success'
    }
}
