
// RetrieveArtifactVersionParameterPanel.java --
//
// RetrieveArtifactVersionParameterPanel.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.EC_Docker.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NonNls;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.client.domain.Artifact;
import com.electriccloud.commander.client.requests.CommanderRequest;
import com.electriccloud.commander.client.requests.GetArtifactsRequest;
import com.electriccloud.commander.client.responses.ArtifactListCallback;
import com.electriccloud.commander.client.responses.CommanderError;
import com.electriccloud.commander.client.util.StringUtil;
import com.electriccloud.commander.gwt.client.ui.FormTable;

import ecinternal.client.ui.FormElementGroup;

import static com.electriccloud.commander.gwt.client.ui.FormBuilder.MISSING_REQUIRED_ERROR_MESSAGE;

import static ecinternal.client.ui.NamedObjectSuggestPicker.loadOptions;

public class Artifact2ImageParameterPanel
        extends DockerParameterPanel
        implements ChainedCallback
{

    //~ Static fields/initializers ---------------------------------------------

    /** Form row IDs. */
    static final String ARTIFACT_NAME_ID                      = "artifactName";
    static final String VERSION_RANGE_ID                      = "versionRange";
    static final String ARTIFACT_VERSION_LOCATION_PROPERTY_ID =
            "artifactVersionLocationProperty";
    static final String FILTER_LIST_ID                        = "filterList";
    static final String RETRIEVE_DIRECTORY_ID                 =
            "retrieveToDirectory";
    static final String OVERWRITE_ID                          = "overwrite";
    static final String ADD_FILTER_ID                         = "addFilter";

    //~ Instance fields --------------------------------------------------------

    /** Form elements. */
    private SuggestBox          m_artifactName;
    private VersionRange        m_versionRange;
    private TextBox             m_artifactVersionLocationProperty;
    private FormElementGroup<?> m_filters;
    private CheckBox            m_retrieveToDirectory;
    private TextBox             m_downloadDirectory;
    private ListBox             m_overwrite;

    /** Info from Commander. */
    private List<Artifact> m_artifacts;

    /** Internals. */
    private boolean m_awaitingResponse;
    private boolean m_renderOnResponse;

    private final List<String> overwriteValues = Arrays.asList( "true", "update", "false" );
    //~ Methods ----------------------------------------------------------------

    @Override public Widget doInit()
    {
        final Widget widget = super.doInit();

        m_awaitingResponse = true;

        getRequestManager().doRequest(this, getRequests());

        return widget;
    }

    @Override public void onComplete()
    {
        m_awaitingResponse = false;

        if (m_renderOnResponse) {
            render();
            m_renderOnResponse = false;
        }
    }

    @Override public void render()
    {

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

            // Retrieve to Directory
            String retrieveToDirectory = actualParams.get(
                    RETRIEVE_DIRECTORY_ID);

            if (retrieveToDirectory != null && !retrieveToDirectory.isEmpty()) {
                m_retrieveToDirectory.setValue(true, true);
                m_downloadDirectory.setValue(retrieveToDirectory);
            }

            // ArtifactVersion Location Property
            String artifactVersionLocationProperty = actualParams.get(
                    ARTIFACT_VERSION_LOCATION_PROPERTY_ID);

            if (artifactVersionLocationProperty != null) {
                m_artifactVersionLocationProperty.setValue(
                        artifactVersionLocationProperty);
            }

            // Filters
            String filters = actualParams.get(FILTER_LIST_ID);

            if (filters != null) {
                m_filters.setValue(Arrays.asList(filters.split("\n")));
            }

            // Overwrite
            String actualOverwriteValue = actualParams.get(OVERWRITE_ID);
            if(actualOverwriteValue != null) {
                m_overwrite.setSelectedIndex(overwriteValues.indexOf(actualOverwriteValue));
            }
        }
    }

    @Override public boolean validate()
    {
        FormTable form = getForm();

        form.clearAllErrors();

        // Make sure required fields have been specified
        String artifactName = m_artifactName.getValue();

        if (StringUtil.isEmpty(artifactName)) {
            form.setErrorMessage(ARTIFACT_NAME_ID,
                    MISSING_REQUIRED_ERROR_MESSAGE);

            return false;
        }

        return true;
    }

    @Override protected void initForm()
    {
        DockerStyles css = getResources().css();

        // Artifact picker
        m_artifactName = new SuggestBox();
        m_artifactName.addStyleName(css.mediumInput());
        getForm().addFormRow(ARTIFACT_NAME_ID, getMessages().artifactLabel(),
                m_artifactName, true, getMessages().retArtifactDesc());

        // Version Range entry
        m_versionRange = new VersionRange(getResources(), getMessages());
        getForm().addFormRow(VERSION_RANGE_ID, getMessages().versionLabel(),
                m_versionRange, false, getMessages().retVersionDesc());

        // Retrieve to Directory entry
        m_retrieveToDirectory = new CheckBox();
        m_retrieveToDirectory.addValueChangeHandler(
                new ValueChangeHandler<Boolean>() {
                    @Override public void onValueChange(
                            ValueChangeEvent<Boolean> event)
                    {

                        if (event.getValue()) {
                            m_downloadDirectory.setEnabled(true);
                        }
                        else {
                            m_downloadDirectory.setValue("");
                            m_downloadDirectory.setEnabled(false);
                        }
                    }
                });
        m_downloadDirectory = new TextBox();
        m_downloadDirectory.setEnabled(false);
        m_overwrite = new ListBox();
        for(String value: overwriteValues ) {
            m_overwrite.addItem(value, value);
        }
        m_overwrite.setSelectedIndex(1);

        FlowPanel flowPanel = new FlowPanel();

        flowPanel.add(m_retrieveToDirectory);
        flowPanel.add(m_downloadDirectory);

        Label label = new Label("Overwrite:");

        label.setStyleName(getResources().css()
                .overwriteLabel(), true);
        flowPanel.add(label);
        flowPanel.add(m_overwrite);
        getForm().addFormRow(RETRIEVE_DIRECTORY_ID,
                getMessages().retrieveToDirectory(), flowPanel, false,
                getMessages().retrieveToDirectoryDesc());

        // ArtifactVersion Location Property entry
        m_artifactVersionLocationProperty = new TextBox();
        m_artifactVersionLocationProperty.addStyleName(css.longInput());
        m_artifactVersionLocationProperty.setValue(
                "/myJob/retrievedArtifactVersions/$[assignedResourceName]");
        getForm().addFormRow(ARTIFACT_VERSION_LOCATION_PROPERTY_ID,
                getMessages().retrievedArtifactLocationPropertyLabel(),
                m_artifactVersionLocationProperty, false,
                getMessages().retLocationPropDesc());

        // Filter list
        m_filters = getUIFactory().createSearchFilterGroup();
        m_filters.addValueChangeHandler(new ValueChangeHandler<List<String>>() {
            @Override public void onValueChange(
                    ValueChangeEvent<List<String>> event)
            {

                // If the list is empty, hide the row
                setFormRowVisibility(FILTER_LIST_ID, !m_filters.isEmpty());
            }
        });
        getForm().addFormRow(FILTER_LIST_ID, getMessages().filtersLabel(),
                m_filters, false, getMessages().retFiltersDesc());
        setFormRowVisibility(FILTER_LIST_ID, false);

        // Add Filter link
        SimplePanel addFilterPanel = new SimplePanel();

        addFilterPanel.addStyleName(css.addListElement());
        addFilterPanel.setWidget(getUIFactory().createLinkTable(
                getUIFactory().getInternalResources()
                        .addIconSmall(), getMessages().addFilter(),
                new ClickHandler() {
                    @Override public void onClick(ClickEvent event)
                    {
                        m_filters.addElement();
                    }
                }));
        getForm().addRow(ADD_FILTER_ID, new Label(), addFilterPanel);
    }

    SuggestBox getArtifactName()
    {
        return m_artifactName;
    }

    TextBox getArtifactVersionLocationProperty()
    {
        return m_artifactVersionLocationProperty;
    }

    TextBox getDownloadDirectory()
    {
        return m_downloadDirectory;
    }

    FormElementGroup<?> getFilters()
    {
        return m_filters;
    }

    ListBox getOverwrite()
    {
        return m_overwrite;
    }

    List<CommanderRequest<?>> getRequests()
    {
        List<CommanderRequest<?>> requests =
                new ArrayList<CommanderRequest<?>>();

        // Load Artifacts
        GetArtifactsRequest getArtifactsRequest = getRequestFactory()
                .createGetArtifactsRequest();

        getArtifactsRequest.setCallback(new ArtifactListCallback() {
            @Override public void handleError(CommanderError error)
            {
                getCommanderErrorHandler().handleError(error);
            }

            @Override public void handleResponse(List<Artifact> response)
            {
                m_artifacts = response;
            }
        });
        requests.add(getArtifactsRequest);

        return requests;
    }

    CheckBox getRetrieveToDirectory()
    {
        return m_retrieveToDirectory;
    }

    @Override public Map<String, String> getValues()
    {
        Map<String, String> values = new HashMap<String, String>();

        values.put(ARTIFACT_NAME_ID, m_artifactName.getValue());
        values.put(VERSION_RANGE_ID, m_versionRange.getValue());
        values.put(ARTIFACT_VERSION_LOCATION_PROPERTY_ID,
                m_artifactVersionLocationProperty.getValue());

        if (m_retrieveToDirectory.getValue()) {
            values.put(RETRIEVE_DIRECTORY_ID, m_downloadDirectory.getValue());
        } else {
            values.put(RETRIEVE_DIRECTORY_ID, "");
        }

        values.put(OVERWRITE_ID,
                m_overwrite.getValue(m_overwrite.getSelectedIndex()));
        values.put(FILTER_LIST_ID, StringUtil.join(m_filters.getValue(), "\n"));

        return values;
    }

    VersionRange getVersionRange()
    {
        return m_versionRange;
    }
}
