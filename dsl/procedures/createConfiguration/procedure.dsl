import java.io.File

procedure 'CreateConfiguration',
        description: 'Creates a configuration for a stand-alone Docker host or Docker Swarm manager', {

    step 'setup',
        subproject: '',
        subprocedure: 'flowpdk-setup',
        command: null,
        errorHandling: 'failProcedure',
        exclusiveMode: 'none',
        postProcessor: '$[/myProject/postpLoader]',
        releaseMode: 'none',
        timeLimitUnits: 'minutes'

    step 'testConnection',
        command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/testConnection.groovy').text,
        errorHandling: 'abortProcedure',
        condition: '$[testConnection]',
        exclusiveMode: 'none',
        releaseMode: 'none',
        shell: 'ec-groovy',
        resourceName: '$[grabbedResource]',
        timeLimitUnits: 'minutes',
        postProcessor: '$[/myProject/postpLoader]'

    step 'createConfiguration',
        command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createConfiguration.pl').text,
        errorHandling: 'failProcedure',
        exclusiveMode: 'none',
        postProcessor: '$[/myProject/postpLoader]',
        releaseMode: 'none',
        shell: 'ec-perl',
        timeLimitUnits: 'minutes'

    step 'createAndAttachCredential',
	    command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createAndAttachCredential.pl').text,
	    errorHandling: 'failProcedure',
	    exclusiveMode: 'none',
	    releaseMode: 'none',
	    shell: 'ec-perl',
	    timeLimitUnits: 'minutes'

}
