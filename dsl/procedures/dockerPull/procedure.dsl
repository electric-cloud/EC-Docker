import java.io.File

procedure 'Docker Pull',
	description: 'Performs a docker pull on the requested image', {

	step 'dockerPull',
    	  command: new File(pluginDir, 'dsl/procedures/dockerPull/steps/dockerPull.pl').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-perl',
    	  timeLimitUnits: 'minutes'

}