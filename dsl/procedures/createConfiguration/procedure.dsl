import java.io.File

procedure 'CreateConfiguration',
        description: 'Creates a configuration for a stand-alone Docker host or Docker Swarm manager', {

    step 'setup',
      subproject: '',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

          actualParameter 'additionalArtifactVersion', ''
    }
    
    step 'testConnection',
            command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/testConnction.groovy').text,
            errorHandling: 'abortProcedure',
            exclusiveMode: 'none',
            releaseMode: 'none',
            shell: 'ec-groovy',
            timeLimitUnits: 'minutes'

    step 'createConfiguration',
            command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createConfiguration.pl').text,
            errorHandling: 'abortProcedure',
            exclusiveMode: 'none',
            postProcessor: 'postp',
            releaseMode: 'none',
            shell: 'ec-perl',
            timeLimitUnits: 'minutes'

    step 'createAndAttachCredential',
	        command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createAndAttachCredential.pl').text,
	        errorHandling: 'abortProcedure',
	        exclusiveMode: 'none',
	        releaseMode: 'none',
	        shell: 'ec-perl',
	        timeLimitUnits: 'minutes'
   
}
