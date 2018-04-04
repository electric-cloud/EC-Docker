import java.io.File

procedure 'Import Microservices',
	description: 'Import microcervices using Docker Compose file and creates corresponding application models for them in ElectricFlow', {

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

	step 'importMicroservices',
    	  command: new File(pluginDir, 'dsl/procedures/importMicroservices/steps/importMicroservices.groovy').text,
    	  errorHandling: 'abortProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  resourceName: '$[grabbedResource]',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'

}