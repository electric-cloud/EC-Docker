package ecplugins.EC_Docker.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;

import com.electriccloud.commander.client.domain.ActualParameter;
import com.electriccloud.commander.client.domain.FormalParameter;
import com.electriccloud.commander.gwt.client.ui.FormTable;
import com.electriccloud.commander.gwt.client.ui.ParameterPanel;
import com.electriccloud.commander.gwt.client.ui.ParameterPanelProvider;
import com.electriccloud.commander.gwt.client.ui.RenderableParameterPanel;

import ecinternal.client.InternalComponentBase;

public abstract class DockerParameterPanel
        extends InternalComponentBase
        implements RenderableParameterPanel,
        ParameterPanelProvider
{

    //~ Instance fields --------------------------------------------------------

    private DockerResources   m_resources;
    private DockerMessages    m_messages;
    private FormTable           m_form;
    private Map<String, String> m_actualParams;

    //~ Methods ----------------------------------------------------------------

    @Override public Widget doInit()
    {
        m_form = getUIFactory().createFormTable();
        m_form.asWidget()
                .addStyleName(getResources().css()
                        .artifactVersionForm());

        // Let subclass initialize the form
        initForm();

        return m_form.asWidget();
    }

    @Override public void onBind() { }

    @Override public void onHide() { }

    @Override public void onReset() { }

    @Override public void onReveal() { }

    @Override public void onUnbind() { }

    protected abstract void initForm();

    Map<String, String> getActualParams()
    {
        return m_actualParams;
    }

    FormTable getForm()
    {
        return m_form;
    }

    DockerMessages getMessages()
    {

        if (m_messages == null) {
            m_messages = GWT.create(DockerMessages.class);
        }

        return m_messages;
    }

    @Override public ParameterPanel getParameterPanel()
    {
        return this;
    }

    DockerResources getResources()
    {

        if (m_resources == null) {
            m_resources = GWT.create(DockerResources.class);
            m_resources.css()
                    .ensureInjected();
        }

        return m_resources;
    }

    @Override public void setActualParameters(
            Collection<ActualParameter> actualParameters)
    {

        // Store actual params into a hash for easy retrieval later
        m_actualParams = new HashMap<String, String>();

        for (ActualParameter actualParameter : actualParameters) {
            m_actualParams.put(actualParameter.getName(),
                    actualParameter.getValue());
        }
    }

    @Override public void setFormalParameters(
            Collection<FormalParameter> formalParameters)
    {
        // We don't care about the formals
    }

    protected void setFormRowVisibility(
            String  rowId,
            boolean visible)
    {

        if (visible) {
            m_form.removeRowStyleName(rowId, 0, "noDisplay");
        }
        else {
            m_form.addRowStyleName(rowId, 0, "noDisplay");
        }
    }
}
