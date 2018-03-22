
// RetrieveArtifactVersionParameterPanel.java --
//
// RetrieveArtifactVersionParameterPanel.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.EC_Docker.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gwt.user.client.ui.*;
import ecinternal.client.ui.CredentialParameter;
import org.jetbrains.annotations.NonNls;


import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.client.domain.Artifact;
import com.electriccloud.commander.client.requests.CommanderRequest;
import com.electriccloud.commander.client.requests.GetArtifactsRequest;
import com.electriccloud.commander.client.responses.ArtifactListCallback;
import com.electriccloud.commander.client.responses.CommanderError;
import com.electriccloud.commander.client.util.StringUtil;
import com.electriccloud.commander.gwt.client.ui.FormTable;


import static com.electriccloud.commander.gwt.client.ui.FormBuilder.MISSING_REQUIRED_ERROR_MESSAGE;

import static ecinternal.client.ui.NamedObjectSuggestPicker.loadOptions;

public class Artifact2ImageParameterPanel
        extends DockerParameterPanel
        implements ChainedCallback {

    //~ Static fields/initializers ---------------------------------------------

    /**
     * Form row IDs.
     */
    static final String ARTIFACT_NAME_ID = "ecp_docker_artifactName";
    static final String VERSION_RANGE_ID = "ecp_docker_versionRange";
    static final String CONFIG_ID = "config";
    static final String CREDENTIAL_ID = "ecp_docker_credential";
    static final String IMAGE_NAME_ID = "ecp_docker_imageName";
    static final String BASE_IMAGE_ID = "ecp_docker_baseImage";
    static final String PORTS_ID = "ecp_docker_ports";
    static final String REGISTRY_URL_ID = "ecp_docker_registryUrl";
    static final String COMMAND_ID = "ecp_docker_command";
    static final String ENV_ID = "ecp_docker_env";

    static final int TEXTAREA_HEIGHT = 10;
    static final int TEXTAREA_WIDTH = 150;


    Logger log = Logger.getLogger("info");

    //~ Instance fields --------------------------------------------------------

    /**
     * Form elements.
     */
    private SuggestBox m_artifactName;
    private TextBox m_config;
    private TextBox m_imageName;
    private TextBox m_registryUrl;
    private CredentialParameter m_credential;
    private TextArea m_ports;
    private VersionRange m_versionRange;
    private TextArea m_command;
    private TextArea m_env;
    private TextBox m_baseImage;

    /**
     * Info from Commander.
     */
    private List<Artifact> m_artifacts;

    /**
     * Internals.
     */
    private boolean m_awaitingResponse;
    private boolean m_renderOnResponse;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Widget doInit() {
        final Widget widget = super.doInit();

        m_awaitingResponse = true;

        getRequestManager().doRequest(this, getRequests());

        return widget;
    }

    @Override
    public void onComplete() {
        m_awaitingResponse = false;

        if (m_renderOnResponse) {
            render();
            m_renderOnResponse = false;
        }
    }

    public static native void console(String text)
/*-{
    console.log(text);
}-*/;

    @Override
    public void render() {

        if (m_awaitingResponse) {

            // We don't have the data for loadOptions() yet. Set a flag that
            // onComplete() will notice.
            m_renderOnResponse = true;

            return;
        }

        // Load Artifact options
        if (m_artifacts != null) {
            loadOptions(m_artifactName, m_artifacts, false);
        }

        // We want latest version selected by default
        m_versionRange.setValue("");

        // Load actual param values
        @NonNls Map<String, String> actualParams = getActualParams();

        if (actualParams != null) {

            String config = actualParams.get(CONFIG_ID);
            if (config != null) {
                m_config.setValue(config);
            }

            // Artifact
            String artifactName = actualParams.get(ARTIFACT_NAME_ID);

            if (artifactName != null) {
                m_artifactName.setValue(artifactName);
            }

            // Version Range
            String artifactVersion = actualParams.get(VERSION_RANGE_ID);

            if (artifactVersion != null) {
                m_versionRange.setValue(actualParams.get(VERSION_RANGE_ID));
            }

//            Credential
            String credential = actualParams.get(CREDENTIAL_ID);
            if (credential != null) {
                m_credential.setValue(credential);
            }

            String registryUrl = actualParams.get(REGISTRY_URL_ID);
            if (registryUrl != null) {
                m_registryUrl.setValue(registryUrl);
            }

            String imageName = actualParams.get(IMAGE_NAME_ID);
            if (imageName != null) {
                m_imageName.setValue(imageName);
            }

            String baseImage = actualParams.get(BASE_IMAGE_ID);
            if (baseImage != null) {
                m_baseImage.setValue(baseImage);
            }

            String ports = actualParams.get(PORTS_ID);
            if (ports != null) {
                m_ports.setValue(ports);
            }

            String command = actualParams.get(COMMAND_ID);
            if (command != null) {
                m_command.setValue(command);
            }

            String env = actualParams.get(ENV_ID);
            if (env != null) {
                m_env.setValue(env);
            }

        }
    }

    @Override
    public boolean validate() {
        FormTable form = getForm();

        form.clearAllErrors();

        // Make sure required fields have been specified
        String artifactName = m_artifactName.getValue();

        if (StringUtil.isEmpty(artifactName)) {
            form.setErrorMessage(ARTIFACT_NAME_ID,
                    MISSING_REQUIRED_ERROR_MESSAGE);

            return false;
        }

        String imageName = m_imageName.getValue();
        if (StringUtil.isEmpty(imageName)) {
            form.setErrorMessage(IMAGE_NAME_ID, MISSING_REQUIRED_ERROR_MESSAGE);
            return false;
        }

        String config = m_config.getValue();
        if (StringUtil.isEmpty(config)) {
            form.setErrorMessage(CONFIG_ID, MISSING_REQUIRED_ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    @Override
    protected void initForm() {
        DockerStyles css = getResources().css();

//        Config
        m_config = new TextBox();
        getForm().addFormRow(CONFIG_ID, getMessages().configLabel(), m_config, true, getMessages().configDoc());

        // Artifact picker
        m_artifactName = new SuggestBox();
        m_artifactName.addStyleName(css.mediumInput());
        getForm().addFormRow(ARTIFACT_NAME_ID, getMessages().artifactLabel(),
                m_artifactName, true, getMessages().retArtifactDesc());

        // Version Range entry
        m_versionRange = new VersionRange(getResources(), getMessages());
        getForm().addFormRow(VERSION_RANGE_ID, getMessages().versionLabel(),
                m_versionRange, false, getMessages().retVersionDesc());

//        Image name
        m_imageName = new TextBox();
        getForm().addFormRow(IMAGE_NAME_ID, getMessages().imageName(), m_imageName, true, getMessages().imageNameDescription());

//        Docker details
        m_registryUrl = new TextBox();
        getForm().addFormRow(REGISTRY_URL_ID, getMessages().registryURL(), m_registryUrl, false, getMessages().registryUrlDoc());

        m_credential = (CredentialParameter) getUIFactory().createCredentialParameter("password");
        getForm().addFormRow(CREDENTIAL_ID, getMessages().credLabel(), m_credential, false, getMessages().credDoc());


//        Image details
        m_baseImage = new TextBox();
        getForm().addFormRow(BASE_IMAGE_ID, getMessages().baseImageLabel(), m_baseImage, false, getMessages().baseImageDoc());

        m_ports = new TextArea();
        m_ports.setCharacterWidth(TEXTAREA_WIDTH);
        m_ports.setVisibleLines(TEXTAREA_HEIGHT);
        getForm().addFormRow(PORTS_ID, getMessages().portsLabel(), m_ports, false, getMessages().portsDoc());

        m_command = new TextArea();
        m_command.setCharacterWidth(TEXTAREA_WIDTH);
        m_command.setVisibleLines(TEXTAREA_HEIGHT);
        getForm().addFormRow(COMMAND_ID, getMessages().commandLabel(), m_command, false, getMessages().commandDoc());

        m_env = new TextArea();
        m_env.setCharacterWidth(TEXTAREA_WIDTH);
        m_env.setVisibleLines(TEXTAREA_HEIGHT);
        getForm().addFormRow(ENV_ID, getMessages().envLabel(), m_env, false, getMessages().envDoc());
    }

    SuggestBox getArtifactName() {
        return m_artifactName;
    }


    List<CommanderRequest<?>> getRequests() {
        List<CommanderRequest<?>> requests =
                new ArrayList<CommanderRequest<?>>();

        // Load Artifacts
        GetArtifactsRequest getArtifactsRequest = getRequestFactory()
                .createGetArtifactsRequest();

        getArtifactsRequest.setCallback(new ArtifactListCallback() {
            @Override
            public void handleError(CommanderError error) {
                getCommanderErrorHandler().handleError(error);
            }

            @Override
            public void handleResponse(List<Artifact> response) {
                m_artifacts = response;
            }
        });
        requests.add(getArtifactsRequest);

        return requests;
    }


    @Override
    public Map<String, String> getValues() {
        Map<String, String> values = new HashMap<String, String>();

        values.put(ARTIFACT_NAME_ID, m_artifactName.getValue());
        values.put(VERSION_RANGE_ID, m_versionRange.getValue());
        values.put(CONFIG_ID, m_config.getValue());
        values.put(CREDENTIAL_ID, m_credential.getValue());
        values.put(IMAGE_NAME_ID, m_imageName.getValue());
        values.put(REGISTRY_URL_ID, m_registryUrl.getValue());
        values.put(PORTS_ID, m_ports.getValue());
        values.put(COMMAND_ID, m_command.getValue());
        values.put(ENV_ID, m_env.getValue());

        return values;
    }

    VersionRange getVersionRange() {
        return m_versionRange;
    }
}
