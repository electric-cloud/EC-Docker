import java.io.File

procedure 'Discover',
	description: 'Discovers services in Docker Compose file and creates corresponding application models for them in ElectricFlow', {

	step 'discover',
    	  command: new File(pluginDir, 'dsl/procedures/discover/steps/discover.pl').text,
    	  errorHandling: 'abortProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-perl',
    	  timeLimitUnits: 'minutes'

}