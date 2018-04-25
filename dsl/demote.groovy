import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.util.BasePlugin

//noinspection GroovyUnusedAssignment
@BaseScript BasePlugin baseScript

def pluginName = args.pluginName
def pluginKey = getProject("/plugins/$pluginName/project").pluginKey

cleanup(pluginKey, pluginName)

return "Plugin $pluginKey demoted"
