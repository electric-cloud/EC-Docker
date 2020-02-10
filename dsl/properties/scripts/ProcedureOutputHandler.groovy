abstract class ProcedureOutputHandler {
    List summaries = []
    ProcedureOutcome procedureOutcome = ProcedureOutcome.success

    abstract void setSummary(String summary)
    abstract void setOutcome(ProcedureOutcome procedureOutcome)
    abstract void setConfigError(String error)
    abstract void bailOut()
    abstract void setOutputProperty(String name, String value)

    void addSummary(String summary) {
        summaries.add(summary)
        setSummary(summaries.join("\n"))
    }

    void addWarningSummary(String summary) {
        addSummary("Warning: $summary")
    }

    void addErrorSummary(String summary) {
        addSummary("Error: $summary")
    }

    void setWarningOutcome() {
        setOutcome(ProcedureOutcome.warning)
    }

    void setErrorOutcome() {
        setOutcome(ProcedureOutcome.error)
    }

    void addWarningOutcome() {
        if (procedureOutcome != ProcedureOutcome.error) {
            setWarningOutcome()
        }
    }

    void addErrorOutcome() {
        setErrorOutcome()
    }
}
