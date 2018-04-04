import java.io.File

procedure 'EditConfiguration',
        description: 'Edits a previously created configuration for a stand-alone Docker host or Docker Swarm manager', {

    step 'setup',
      subproject: '',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'call',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

          actualParameter 'additionalArtifactVersion', ''
    }

    step 'testConnection',
            command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/testConnection.groovy').text,
            errorHandling: 'abortProcedure',
            condition: '$[testConnection]',
            exclusiveMode: 'none',
            releaseMode: 'none',
            resourceName: '$[grabbedResource]',
            shell: 'ec-groovy',
            timeLimitUnits: 'minutes'


}
