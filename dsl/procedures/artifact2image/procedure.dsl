import java.io.File

procedure 'Artifact2Image',
	description: 'Creates and pushes a new docker image from the existing artifact', {

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

	step 'download artifact',
		command: new File(pluginDir, 'dsl/procedures/artifact2image/steps/downloadArtifact.pl').text,
		shell: 'ec-perl',
		errorHandling: 'abortProcedureNow'


	step 'artifact2image',
		command: new File(pluginDir, 'dsl/procedures/artifact2image/steps/artifact2image.groovy').text,
		errorHandling: 'failProcedure',
		exclusiveMode: 'none',
		postProcessor: 'postp',
		releaseMode: 'none',
		resourceName: '$[grabbedResource]',
		shell: 'ec-groovy',
		timeLimitUnits: 'minutes'

}
