import java.io.File

procedure 'Import Microservices',
	description: 'Import microcervices using Docker Compose file and creates corresponding application models for them in ElectricFlow', {

	step 'importMicroservices',
    	  command: new File(pluginDir, 'dsl/procedures/importMicroservices/steps/importMicroservices.groovy').text,
    	  errorHandling: 'abortProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'

}