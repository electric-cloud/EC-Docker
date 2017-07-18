import java.io.File

procedure 'Docker Build',
	description: 'Performs a docker build', {

	step 'dockerPull',
    	  command: new File(pluginDir, 'dsl/procedures/dockerBuild/steps/dockerBuild.pl').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-perl',
    	  timeLimitUnits: 'minutes'

}