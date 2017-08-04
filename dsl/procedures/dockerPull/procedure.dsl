import java.io.File

procedure 'runDockerPull',
	description: 'Performs a docker pull on the requested image', {
    jobNameTemplate = 'docker-pull-$[jobId]'

	step 'dockerPull',
    	  command: new File(pluginDir, 'dsl/procedures/dockerPull/steps/dockerPull.pl').text,
    	  errorHandling: 'abortProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-perl',
    	  timeLimitUnits: 'minutes'

}