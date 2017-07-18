import java.io.File

procedure 'Docker Run',
	description: 'Performs a docker run', {

	step 'dockerRun',
    	  command: new File(pluginDir, 'dsl/procedures/dockerRun/steps/dockerRun.pl').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-perl',
    	  timeLimitUnits: 'minutes'

}