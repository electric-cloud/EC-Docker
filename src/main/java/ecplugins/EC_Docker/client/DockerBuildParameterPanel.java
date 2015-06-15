
// DockerBuildParameterPanel.java --
//
// DockerBuildParameterPanel.java is part of ElectricCommander.
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

public class DockerBuildParameterPanel
    extends InternalComponentBase
    implements ParameterPanel,
        ParameterPanelProvider
{

    //~ Static fields/initializers ---------------------------------------------
	private static final String USE_SUDO      = "use_sudo";
    private static final String BUILD_PATH    = "build_path";

    //~ Instance fields --------------------------------------------------------

    private FormTable    m_form;
	private CheckBox     m_UseSudo;
    private TextBox      m_BuildPath;

    //~ Methods ----------------------------------------------------------------

    @Override public Widget doInit()
    {
        m_form         = getUIFactory().createFormTable();
		m_UseSudo      = new CheckBox();
        m_BuildPath    = new TextBox();

		m_form.addFormRow(USE_SUDO, "Use sudo:", m_UseSudo, true,
            "Use sudo for running docker build");
        m_form.addFormRow(BUILD_PATH, "Build path:", m_BuildPath, true,
            "Path to source code or URL");

        return m_form.asWidget();
    }

    @Override public boolean validate()
    {
        m_form.clearAllErrors();

        if (StringUtil.isEmpty(m_BuildPath.getValue())) {
            m_form.setErrorMessage(BUILD_PATH, MISSING_REQUIRED_ERROR_MESSAGE);

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

		boolean useSudo = m_UseSudo.getValue();
		if(useSudo) {
			values.put(USE_SUDO, "1");
		} else {
			values.put(USE_SUDO, "0");
		}
		
        values.put(BUILD_PATH, m_BuildPath.getValue());

        return values;
    }

    @Override public void setActualParameters(
            Collection<ActualParameter> actualParameters)
    {
        for (ActualParameter actualParameter : actualParameters) {
            String name  = actualParameter.getName();
            String value = actualParameter.getValue();

            if (USE_SUDO.equals(name)) {
                m_UseSudo.setValue(value.equals("1"));
            } else if (BUILD_PATH.equals(name)) {
                m_BuildPath.setValue(value);
            }
        }
    }

    @Override public void setFormalParameters(
            Collection<FormalParameter> formalParameters) { }
}