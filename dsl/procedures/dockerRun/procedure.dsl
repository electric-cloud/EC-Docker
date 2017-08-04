import java.io.File

procedure 'runDockerRun',
	description: 'Performs a docker run', {
	jobNameTemplate = 'docker-run-$[jobId]'

	step 'dockerRun',
    	  command: new File(pluginDir, 'dsl/procedures/dockerRun/steps/dockerRun.pl').text,
    	  errorHandling: 'abortProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-perl',
    	  timeLimitUnits: 'minutes'

}