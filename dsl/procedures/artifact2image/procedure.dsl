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


//	step 'Retrieve', {
//		description = ''
//		alwaysRun = '0'
//		broadcast = '0'
//		command = null
//		condition = ''
//		errorHandling = 'failProcedure'
//		exclusiveMode = 'none'
//		logFileName = null
//		parallel = '0'
//		postProcessor = null
//		precondition = ''
//		projectName = 'Artifactory'
//		releaseMode = 'none'
//		resourceName = ''
//		shell = null
//		subprocedure = 'Retrieve Artifact from Artifactory'
//		subproject = '/plugins/EC-Artifactory/project'
//		timeLimit = ''
//		timeLimitUnits = 'minutes'
//		workingDirectory = null
//		workspaceName = ''
//		actualParameter 'artifact', 'ECSCM'
//		actualParameter 'classifier', ''
//		actualParameter 'config', 'local'
//		actualParameter 'destination', ''
//		actualParameter 'extension', 'jar'
//		actualParameter 'extract', '0'
//		actualParameter 'fileItegRev', ''
//		actualParameter 'folderItegRev', ''
//		actualParameter 'latestVersion', '1'
//		actualParameter 'org', ''
//		actualParameter 'orgPath', 'com/electriccloud'
//		actualParameter 'overwrite', '1'
//		actualParameter 'repository', 'ec'
//		actualParameter 'repositoryPath', ''
//		actualParameter 'repoType', 'Maven'
//		actualParameter 'resultPropertySheet', '/myJob/retrievedArtifactVersions/$[assignedResourceName]'
//		actualParameter 'type', ''
//		actualParameter 'useRepositoryLayout', '1'
//		actualParameter 'version', ''
//


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
