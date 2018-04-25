import java.io.File

procedure 'Discover',
	description: '[Deprecated] This procedure is deprecated. Use the "Import Microservices" procedure to create microservice models based on the given Docker Compose file contents.', {

    //hide the deprecated procedure from the step-picker
    property 'standardStepPicker', value: false
    
	step 'discover',
    	  command: new File(pluginDir, 'dsl/procedures/discover/steps/discover.groovy').text,
    	  errorHandling: 'abortProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'

}