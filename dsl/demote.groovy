import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.util.BasePlugin

//noinspection GroovyUnusedAssignment
@BaseScript BasePlugin baseScript

def pluginName = args.pluginName
def otherPluginName = args.otherPluginName
def pluginKey = getProject("/plugins/$pluginName/project").pluginKey

cleanup(pluginKey, otherPluginName)

return "Plugin $otherPluginName demoted"
