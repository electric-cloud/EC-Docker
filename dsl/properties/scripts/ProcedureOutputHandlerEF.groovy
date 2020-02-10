class ProcedureOutputHandlerEF extends ProcedureOutputHandler {
    @Override
    void setSummary(String summary) {
        EFPluginHelper.getInstance().getEf().setProperty(propertyName: '/myJobStep/summary', value: summary)
    }

    @Override
    void setOutcome(ProcedureOutcome procedureOutcome) {
        EFPluginHelper.getInstance().getEf().setProperty(propertyName: '/myJobStep/outcome', value: procedureOutcome.name())
    }

    @Override
    void setConfigError(String error) {
        EFPluginHelper.getInstance().getEf().setProperty(propertyName: '/myJob/configError', value: error)
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
