import java.io.File

procedure 'Populate Certs',
	description: 'Dump TLS certificates (ca-cert, client cert and client key) on agent machine in job workspace', {

    // don't add a step picker for this procedure since it is internally invoked
    property 'standardStepPicker', value: false

	step 'populateDockerClientCerts',
    	  command: new File(pluginDir, 'dsl/procedures/populateCerts/steps/populateCerts.groovy').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'

}