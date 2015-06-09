
// DockerPullParameterPanel.java --
//
// DockerPullParameterPanel.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.EC_Docker.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.electriccloud.commander.client.domain.ActualParameter;
import com.electriccloud.commander.client.domain.FormalParameter;
import com.electriccloud.commander.client.util.StringUtil;
import com.electriccloud.commander.gwt.client.ui.FormTable;
import com.electriccloud.commander.gwt.client.ui.ParameterPanel;
import com.electriccloud.commander.gwt.client.ui.ParameterPanelProvider;

import ecinternal.client.InternalComponentBase;

import static com.electriccloud.commander.gwt.client.ui.FormBuilder.MISSING_REQUIRED_ERROR_MESSAGE;

public class DockerPullParameterPanel
    extends InternalComponentBase
    implements ParameterPanel,
        ParameterPanelProvider
{

    //~ Static fields/initializers ---------------------------------------------

    private static final String SOURCE    = "source";
    private static final String DIRECTORY = "directory";
    private static final String VERSION   = "version";
    private static final String ARTIFACT  = "artifact";

    //~ Instance fields --------------------------------------------------------

    private FormTable    m_form;
    private TextBox      m_Artifact;
    private TextBox      m_Source;

    private TextBox      m_Directory;

    //~ Methods ----------------------------------------------------------------

    @Override public Widget doInit()
    {
        m_form         = getUIFactory().createFormTable();
        m_Source       = new TextBox();
        m_Artifact     = new TextBox();

        m_Directory    = new TextBox();

        m_form.addFormRow(SOURCE, "Source:", m_Source, true,
            "Source directory of artifact to be retrieved from");
        m_form.addFormRow(ARTIFACT, "Artifact:", m_Artifact, true,
            "Name of artifact to be retrieved");
        m_form.addFormRow(DIRECTORY, "Retrieve to Directory:", m_Directory,
            false,
            "Directory to retrieve artifact to. Defaults to workspace directory");

        return m_form.asWidget();
    }

    @Override public boolean validate()
    {
        m_form.clearAllErrors();

        if (StringUtil.isEmpty(m_Source.getValue())) {
            m_form.setErrorMessage(SOURCE, MISSING_REQUIRED_ERROR_MESSAGE);

            return false;
        }
        else if (StringUtil.isEmpty(m_Artifact.getValue())) {
            m_form.setErrorMessage(ARTIFACT, MISSING_REQUIRED_ERROR_MESSAGE);

            return false;
        }

        return true;
    }

    @Override public ParameterPanel getParameterPanel()
    {
        return this;
    }

    @Override public Map<String, String> getValues()
    {
        Map<String, String> values = new HashMap<String, String>();

        values.put(SOURCE, m_Source.getValue());
        values.put(ARTIFACT, m_Artifact.getValue());
        values.put(DIRECTORY, m_Directory.getValue());

        return values;
    }

    @Override public void setActualParameters(
            Collection<ActualParameter> actualParameters)
    {
        for (ActualParameter actualParameter : actualParameters) {
            String name  = actualParameter.getName();
            String value = actualParameter.getValue();

            if (SOURCE.equals(name)) {
                m_Source.setValue(value);
            }
            else if (ARTIFACT.equals(name)) {
                m_Artifact.setValue(value);
            }
            else if (DIRECTORY.equals(name)) {
                m_Directory.setValue(value);
            }
        }
    }

    @Override public void setFormalParameters(
            Collection<FormalParameter> formalParameters) { }
}
