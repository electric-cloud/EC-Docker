import java.io.File

procedure 'Cleanup Certs',
	description: 'Cleanup TLS certificates (ca-cert, client cert and client key) on agent machine in temp dir', {

    // don't add a step picker for this procedure since it is internally invoked
    property 'standardStepPicker', value: false

	step 'cleanupCerts',
    	  command: new File(pluginDir, 'dsl/procedures/cleanupCerts/steps/cleanupCerts.groovy').text,
    	  errorHandling: 'failProcedure',
    	  exclusiveMode: 'none',
    	  postProcessor: 'postp',
    	  releaseMode: 'none',
    	  shell: 'ec-groovy',
    	  timeLimitUnits: 'minutes'

}