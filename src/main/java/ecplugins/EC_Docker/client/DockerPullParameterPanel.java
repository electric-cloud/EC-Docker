/**
 *  Copyright 2015 Electric Cloud, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
	private static final String USE_SUDO   = "use_sudo";
    private static final String IMAGE_NAME = "image_name";
    private static final String TAG        = "tag";

    //~ Instance fields --------------------------------------------------------

    private FormTable m_form;
	private CheckBox  m_UseSudo;
    private TextBox   m_ImageName;
    private TextBox   m_Tag;

    //~ Methods ----------------------------------------------------------------

    @Override public Widget doInit()
    {
        m_form      = getUIFactory().createFormTable();
		m_UseSudo   = new CheckBox();
        m_Tag       = new TextBox();
        m_ImageName = new TextBox();

		m_form.addFormRow(USE_SUDO, "Use sudo:", m_UseSudo, true,
            "Use sudo for running docker pull");	
        m_form.addFormRow(IMAGE_NAME, "Image name:", m_ImageName, true,
            "Image to pull from Docker Hub");
        m_form.addFormRow(TAG, "Image tag:", m_Tag, true,
            "Tag of image");

        return m_form.asWidget();
    }

    @Override public boolean validate()
    {
        m_form.clearAllErrors();

        if (StringUtil.isEmpty(m_ImageName.getValue())) {
            m_form.setErrorMessage(IMAGE_NAME, MISSING_REQUIRED_ERROR_MESSAGE);

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
		
		values.put(TAG, m_Tag.getValue());
		values.put(IMAGE_NAME, m_ImageName.getValue());

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
            } else if (TAG.equals(name)) {
                m_Tag.setValue(value);
            } else if (IMAGE_NAME.equals(name)) {
                m_ImageName.setValue(value);
            }
        }
    }

    @Override public void setFormalParameters(
            Collection<FormalParameter> formalParameters) { }
}
