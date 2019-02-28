package com.electriccloud.specs

import spock.lang.*
import com.electriccloud.client.api.DockerApi

class CreateIngress  extends DockerHelper {

    @Shared
    def procedureName = "Create Ingress",
        projectName = "EC Specs $procedureName",
        resourceName = "local",
        dockerApi = new DockerApi(getDockerSwarmEndpoint(), certsPath, false)

    @Shared
    def TC = [
            C1: [ ids: 'C1', description: 'only required fields'],
            C2: [ ids: 'C2', description: 'all fields'],
            C3: [ ids: 'C3', description: 'run with empty config name'],
            C4: [ ids: 'C4', description: 'run with empty network name'],
            C5: [ ids: 'C5', description: 'run with invalid config name'],
            C6: [ ids: 'C6', description: 'run with invalid Subnets'],
            C7: [ ids: 'C7', description: 'run with empty Subnets'],
            C8: [ ids: 'C8', description: 'run with invalid Gateways'],
            C9: [ ids: 'C9', description: 'run with invalid Labels']
    ]


    def doSetupSpec() {
        createConfig(configSwarm,getDockerSwarmEndpoint())
        dslFile "dsl/RunProcedure.dsl", [ projectName  : projectName, resourceName : resourceName, procedureName: procedureName, params: createIngressParams ]
        dslFile "dsl/RunProcedure.dsl", [ projectName  : projectName, resourceName : resourceName, procedureName: 'Delete Network', params: deleteNetworkParams ]
    }

    def doCleanupSpec() {
        deleteConfig(configSwarm)
        conditionallyDeleteProject(projectName)
    }

    @Unroll
    def 'Positive test. #caseId'() {
        setup: 'Define the parameters for Procedure running'

        def runParams = [
                // Required
                pluginConfig : configSwarm,
                networkName  : networkName,
                // Optional
                subnetList   : subnetList,
                gatewayList  : gatewayList,
                enableIpv6   : enableIpv6,
                mtu          : mtu,
                labels       : labels
        ]

        when: 'Procedure runs: '
        def result = runTestedProcedure(projectName, procedureName, runParams)

        then: 'Wait until job run is completed: '
        def debugLog =  readJobLogs(result.jobId)

        expect: 'Outcome and Upper Summary verification'
        assert result.outcome == 'success'
        assert debugLog  =~ /docker network create/

        def network = dockerApi.client.networks().content.find { it.Name == networkName }
        assert network['Name'] == networkName

        cleanup:
        deleteNetwork(networkName)

        where: 'The following params will be: '
        caseId | networkName    | subnetList     | gatewayList | enableIpv6 | mtu                                  | labels
        TC.C1  | 'test-network' | ''             | ''          | true       | ''                                   | ''
        TC.C2  | 'test-network' | '10.11.0.0/16' | '10.11.0.2' | true       | 'com.docker.network.driver.mtu=1200' | ''
    }

    @Unroll
    def 'Negative test. #caseId'() {
        setup: 'Define the parameters for Procedure running'

        def runParams = [
                // Required
                pluginConfig : pluginConfig,
                networkName  : networkName,
                // Optional
                subnetList   : subnetList,
                gatewayList  : gatewayList,
                enableIpv6   : enableIpv6,
                mtu          : mtu,
                labels       : labels
        ]

        when: 'Procedure runs: '
        def result = runTestedProcedure(projectName, procedureName, runParams)

        then: 'Wait until job run is completed: '
        def debugLog = result.logs

        expect: 'Outcome and Upper Summary verification'
        assert result.outcome == outcome
        if (status){
            assert debugLog  =~ /$status/
        }

        where: 'The following params will be: '
        caseId | pluginConfig    | networkName    | subnetList     | gatewayList | enableIpv6 | mtu | labels    | outcome   | status
        TC.C3  | ''              | 'test-network' | ''             | ''          | false      | ''  | ''        | 'error'   | "ERROR: Configuration  does not exist!"
        TC.C4  | configSwarm     | ''             | ''             | ''          | true       | ''  | ''        | 'error'   | 'docker network create failed'
        TC.C5  | 'invalidConfig' | ''             | ''             | ''          | false      | ''  | ''        | 'error'   | "ERROR: Configuration $pluginConfig does not exist!"
        TC.C6  | configSwarm     | 'test-network' | 'invalidSub'   | ''          | false      | ''  | ''        | 'error'   | "Invalid subnet $subnetList : invalid CIDR address: $subnetList"
        TC.C7  | configSwarm     | 'test-network' | ''             | 'invalidGat'| false      | ''  | ''        | 'error'   | "Invalid subnet  : invalid CIDR address: "
        TC.C8  | configSwarm     | 'test-network' | '10.11.0.0/16' | 'invalidGat'| false      | ''  | ''        | 'error'   | "rpc error: code = 3 desc = ipam configuration: invalid gateway $gatewayList"
        TC.C9  | configSwarm     | 'test-network' | '10.11.0.0/16' | '10.11.0.2' | true       | ''  | 'invalid' | 'error'   | ''
    }


    def deleteNetwork(String networkName){
        def runParams = [
                pluginConfig : configSwarm,
                networkName  : networkName
        ]
        def result = runTestedProcedure(projectName, 'Delete Network', runParams)
        assert result.outcome == 'success'
    }
}

