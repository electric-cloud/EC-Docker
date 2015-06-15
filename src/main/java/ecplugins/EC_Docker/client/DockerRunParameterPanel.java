
// DockerRunParameterPanel.java --
//
// DockerRunParameterPanel.java is part of ElectricCommander.
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

public class DockerRunParameterPanel
    extends InternalComponentBase
    implements ParameterPanel,
        ParameterPanelProvider
{

    //~ Static fields/initializers ---------------------------------------------

	private static final String USE_SUDO           = "use_sudo";
    private static final String IMAGE_NAME         = "image_name";
    private static final String CONTAINER_NAME     = "container_name";
	private static final String DETACHED_MODE      = "detached_mode";
	private static final String ENTRYPOINT         = "entrypoint";
	private static final String WORKING_DIR        = "working_dir";
	private static final String PUBLISHED_PORTS    = "published_ports";
	private static final String PUBLISH_ALL_PORTS  = "publish_all_ports";
	private static final String PRIVILEGED         = "privileged";
	private static final String LINK               = "container_links";
	private static final String COMMAND_WITH_ARGS  = "command_with_args";
	
	
    //~ Instance fields --------------------------------------------------------

    private FormTable    m_form;
	private CheckBox     m_UseSudo;
	private TextBox      m_ImageName;
    private TextBox      m_ContainerName;
    private CheckBox     m_DetachedMode;
	private TextBox      m_EntryPoint;
	private TextBox      m_WorkingDir;
	private TextBox      m_PublishedPorts;
	private CheckBox     m_PublishAllPorts;
	private CheckBox     m_Privileged;
	private TextBox      m_Links;
	private TextBox      m_CommandWithArgs;
	
    //~ Methods ----------------------------------------------------------------

    @Override public Widget doInit()
    {
        m_form            = getUIFactory().createFormTable();
		m_UseSudo         = new CheckBox();
        m_DetachedMode    = new CheckBox();
		m_PublishAllPorts = new CheckBox();
		m_Privileged      = new CheckBox();
		m_ImageName       = new TextBox();
        m_ContainerName   = new TextBox();
		m_EntryPoint      = new TextBox();
		m_WorkingDir      = new TextBox();
		m_PublishedPorts  = new TextBox();
		m_Links           = new TextBox();
		m_CommandWithArgs = new TextBox();
		
        m_form.addFormRow(USE_SUDO, "Use sudo:", m_UseSudo, true,
            "Use sudo for running docker run");		
        m_form.addFormRow(IMAGE_NAME, "Image name:", m_ImageName, true,
            "Image to run a container from");
        m_form.addFormRow(CONTAINER_NAME, "Container name:", m_ContainerName, true,
            "Assign a name to the container");
		m_form.addFormRow(ENTRYPOINT, "Entrypoint:", m_EntryPoint, true,
            "Overwrite the default ENTRYPOINT of the image");
		m_form.addFormRow(WORKING_DIR, "Container working directory:", m_WorkingDir, true,
            "Working directory inside the container");
		m_form.addFormRow(PUBLISHED_PORTS, "Publish ports:", m_PublishedPorts, true,
            "Publish a container's port to the host (format: ip:hostPort:containerPort | ip::containerPort | hostPort:containerPort | containerPort)");
		m_form.addFormRow(PUBLISH_ALL_PORTS, "Publish all ports:", m_PublishAllPorts, true,
            "Publish all exposed ports to the host interfaces");
		m_form.addFormRow(PRIVILEGED, "Privileged:", m_Privileged, true,
            "Give extended privileges to this container");
		m_form.addFormRow(LINK, "Link:", m_Links, true,
            "Add link to another container in the form of name:alias");
		m_form.addFormRow(DETACHED_MODE, "Detached mode (-d):", m_DetachedMode, true,
            "Detached mode: run the container in the background and print the new container ID");
		m_form.addFormRow(COMMAND_WITH_ARGS, "Command with args:", m_CommandWithArgs, true,
            "Command to run within container");
			
        return m_form.asWidget();
    }

    @Override public boolean validate()
    {
        m_form.clearAllErrors();

        if (StringUtil.isEmpty(m_ImageName.getValue()) ) {
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

        values.put(IMAGE_NAME, m_ImageName.getValue());
		values.put(CONTAINER_NAME, m_ContainerName.getValue());
		values.put(ENTRYPOINT, m_EntryPoint.getValue());
		values.put(WORKING_DIR, m_WorkingDir.getValue());
		values.put(PUBLISHED_PORTS, m_PublishedPorts.getValue());
		values.put(LINK, m_Links.getValue());
		values.put(COMMAND_WITH_ARGS, m_CommandWithArgs.getValue());
		
		boolean detached = m_DetachedMode.getValue();
        if(detached) {
			values.put(DETACHED_MODE, "1");
		} else {
			values.put(DETACHED_MODE, "0");
		}
		
		boolean publish_all_ports = m_PublishAllPorts.getValue();
		if(publish_all_ports) {
			values.put(PUBLISH_ALL_PORTS, "1");
		} else {
			values.put(PUBLISH_ALL_PORTS, "0");
		}
		
		boolean privileged = m_Privileged.getValue();
		if(privileged) {
			values.put(PRIVILEGED, "1");
		} else {
			values.put(PRIVILEGED, "0");
		}
		
		boolean useSudo = m_UseSudo.getValue();
		if(useSudo) {
			values.put(USE_SUDO, "1");
		} else {
			values.put(USE_SUDO, "0");
		}
		
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
            } else if (DETACHED_MODE.equals(name)) {
                m_DetachedMode.setValue(value.equals("1"));
            } else if (IMAGE_NAME.equals(name)) {
                m_ImageName.setValue(value);
            } else if (CONTAINER_NAME.equals(name)) {
				m_ContainerName.setValue(value);
			} else if (ENTRYPOINT.equals(name)) {
                m_EntryPoint.setValue(value);
            } else if (WORKING_DIR.equals(name)) {
				m_WorkingDir.setValue(value);
			} else if (PUBLISHED_PORTS.equals(name)) {
                m_PublishedPorts.setValue(value);
            } else if (PUBLISH_ALL_PORTS.equals(name)) {
				m_PublishAllPorts.setValue(value.equals("1"));
			} else if (PRIVILEGED.equals(name)) {
                m_Privileged.setValue(value.equals("1"));
            } else if (LINK.equals(name)) {
				m_Links.setValue(value);
			} else if (COMMAND_WITH_ARGS.equals(name)) {
				m_CommandWithArgs.setValue(value);
			}
        }
    }

    @Override public void setFormalParameters(
            Collection<FormalParameter> formalParameters) { }
}
