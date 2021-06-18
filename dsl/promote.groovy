import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.util.BasePlugin

//noinspection GroovyUnusedAssignment
@BaseScript BasePlugin baseScript

// Variables available for use in DSL code
def pluginName = args.pluginName
def upgradeAction = args.upgradeAction
def otherPluginName = args.otherPluginName

def pluginKey = getProject("/plugins/$pluginName/project").pluginKey
def pluginDir = getProperty("/projects/$pluginName/pluginDir").value
def pluginCategory = 'Container Management'

//List of procedure steps to which the plugin configuration credentials need to be attached
def stepsWithAttachedCredentials = [
		[
				procedureName: 'Check Cluster',
				stepName: 'checkCluster'
		], [
				procedureName: 'Remove Docker Service',
				stepName: 'cleanup'
		],[
				procedureName: 'CreateConfiguration',
				stepName: 'testConnection'
		], [
				procedureName: 'Populate Certs',
				stepName: 'populateDockerClientCerts'
		], [
				procedureName: 'Create Ingress',
				stepName: 'createIngress'
		], [
				procedureName: 'Delete Network',
				stepName: 'deleteNetwork'
		], [
		    	procedureName: 'Artifact2Image',
				stepName: 'artifact2image'
		]

]
project pluginName, {

	ec_visibility = 'pickListOnly'

	loadPluginProperties(pluginDir, pluginName)
	loadProcedures(pluginDir, pluginKey, pluginName, stepsWithAttachedCredentials)


	//plugin configuration metadata
    property 'ec_formXmlCompliant', value: 'true'
	property 'ec_config', {
		configLocation = 'ec_plugin_cfgs'
		form = '$[' + "/projects/${pluginName}/procedures/CreateConfiguration/ec_parameterForm]"
		property 'fields', {
			property 'desc', {
				property 'label', value: 'Description'
				property 'order', value: '1'
			}
		}
	}

	procedure 'Define Container', {
		containerMappingsForm = new File(pluginDir, 'dsl/procedures/defineContainer/containerMappingsForm.xml').text
	}
	procedure 'Define Service', {
		serviceMappingsForm = new File(pluginDir, 'dsl/procedures/defineService/serviceMappingsForm.xml').text
	}
	// End-of container service plugin metadata
}

//Grant permissions to the plugin project
def objTypes = ['resources', 'workspaces', 'projects'];

objTypes.each { type ->
		aclEntry principalType: 'user',
			 principalName: "project: $pluginName",
			 systemObjectName: type,
             objectType: 'systemObject',
			 readPrivilege: 'allow',
			 modifyPrivilege: 'allow',
			 executePrivilege: 'allow',
			 changePermissionsPrivilege: 'allow'
}

// Copy existing plugin configurations from the previous
// version to this version. At the same time, also attach
// the credentials to the required plugin procedure steps.
upgrade(upgradeAction, pluginName, otherPluginName, stepsWithAttachedCredentials)

//delete the step picker for the Deprecated 'Discover' procedure
deleteStepPicker(pluginKey, 'Discover')