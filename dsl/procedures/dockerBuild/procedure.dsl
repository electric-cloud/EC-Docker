import java.io.File

procedure 'runDockerBuild',
	description: 'Performs a docker build', {
    jobNameTemplate = 'docker-build-$[jobId]'

	step 'dockerPull',
    	  command: new File(pluginDir, 'dsl/procedures/dockerBuild/steps/dockerBuild.pl').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-perl',
    	  timeLimitUnits: 'minutes'

}