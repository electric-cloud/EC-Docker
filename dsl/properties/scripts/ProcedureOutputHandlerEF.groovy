@Grab('log4j:log4j:1.2.17')
import org.apache.log4j.Logger

class ProcedureOutputHandlerEF extends ProcedureOutputHandler {
    private static final Logger logger = PluginLogger.getLogger()

    @Override
    void setSummary(String summary) {
        EFPluginHelper.getInstance().getEf().setProperty(propertyName: '/myJobStep/summary', value: summary)
    }

    @Override
    void setOutcome(ProcedureOutcome procedureOutcome) {
        EFPluginHelper.getInstance().getEf().setProperty(propertyName: '/myJobStep/outcome', value: procedureOutcome.name())
    }

    @Override
    void bailOut() {
        System.exit(-1)
    }

    @Override
    void setOutputProperty(String name, String value) {
        EFPluginHelper.getInstance().getEf().setProperty(propertyName: name, value: value)
    }
}
