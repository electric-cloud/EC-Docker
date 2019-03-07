package com.electriccloud.specs

import spock.lang.*

class DeleteConfiguration extends DockerHelper{

    @Shared
    def procedureName = "DeleteConfiguration",
        projectName = "EC Specs $procedureName",
        resourceName = "local"

    @Shared
    TC = [
            C1: 'Delete Configuration',
            //Negative
            C2: 'Run procedure with empty value of config name',
            C3: 'Run procedure for non-exists config'
    ]


    def doSetupSpec() {
        dslFile "dsl/RunProcedure.dsl", [ projectName  : projectName, resourceName : resourceName, procedureName: procedureName, params: deleteConfigParams ]
        dslFile "dsl/RunProcedure.dsl", [ projectName  : projectName, resourceName : resourceName, procedureName: 'CreateConfiguration', params: createConfigParams ]
    }

    def doCleanupSpec() {
        conditionallyDeleteProject(projectName)
    }

    @Unroll
    def '#caseId'() {
        setup: 'Define the parameters for Procedure running'

        if (outcome == 'success'){
            createConfig(configurationName)
        }

        def runParams = [
                config : pluginConfig
        ]

        when: 'Procedure runs: '
        def result = runTestedProcedure(projectName, procedureName, runParams)

        then: 'Wait until job run is completed: '
        def debugLog =  readJobLogs(result.jobId)

        expect: 'Outcome and Upper Summary verification'
        assert result.outcome == outcome
        if(status){
            assert debugLog  =~ /$status/
        }

        where: 'The following params will be: '
        caseId | pluginConfig      | outcome   | status
        TC.C1  | configurationName | 'success' | ''
        TC.C2  | ''                | 'error'   | ''
        TC.C3  | 'non-exists'      | 'error'   | ''
    }
}
