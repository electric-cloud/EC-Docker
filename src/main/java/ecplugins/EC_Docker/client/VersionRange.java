
// VersionRange.java --
//
// VersionRange.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.EC_Docker.client;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import com.google.common.base.Objects;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.electriccloud.commander.client.util.StringUtil;

@SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
public class VersionRange
        extends Composite
        implements HasValue<String>
{

    //~ Enums ------------------------------------------------------------------

    private enum VersionMode
    {

        //~ Enum constants -----------------------------------------------------

        LATEST,
        EXACT,
        RANGE
    }

    //~ Instance fields --------------------------------------------------------

    @NotNull private final VerticalPanel m_panel;
    @NotNull private final RadioButton   m_latestVersionSelector;
    @NotNull private final RadioButton   m_exactVersionSelector;
    @NotNull private final TextBox       m_exactVersion;
    @NotNull private final RadioButton   m_versionRangeSelector;
    @NotNull private final TextBox       m_minimumVersion;
    @NotNull private final CheckBox      m_minimumInclusive;
    @NotNull private final TextBox       m_maximumVersion;
    @NotNull private final CheckBox      m_maximumInclusive;
    @NotNull private VersionMode         m_versionModeSelected =
            VersionMode.LATEST;

    //~ Constructors -----------------------------------------------------------

    @SuppressWarnings("OverlyLongMethod")
    public VersionRange(
            @NotNull DockerResources resources,
            @NotNull DockerMessages  messages)
    {
        DockerStyles css = resources.css();

        m_panel = new VerticalPanel();

        m_panel.addStyleName(css.versionRange());
        initWidget(m_panel);

        String radioGroupName = "versionRange";

        // Latest version selector
        m_latestVersionSelector = new RadioButton(radioGroupName,
                messages.latestLabel());
        m_latestVersionSelector.addValueChangeHandler(
                new ValueChangeHandler<Boolean>() {
                    @Override public void onValueChange(
                            ValueChangeEvent<Boolean> event)
                    {

                        if (event.getValue()) {
                            m_versionModeSelected = VersionMode.LATEST;
                        }
                    }
                });
        m_panel.add(m_latestVersionSelector);

        // Exact version
        HorizontalPanel exactVersionPanel = new HorizontalPanel();

        m_exactVersionSelector = new RadioButton(radioGroupName,
                messages.exactLabel());

        m_exactVersionSelector.addValueChangeHandler(
                new ValueChangeHandler<Boolean>() {
                    @Override public void onValueChange(
                            ValueChangeEvent<Boolean> event)
                    {

                        if (event.getValue()) {
                            m_versionModeSelected = VersionMode.EXACT;
                        }
                    }
                });
        exactVersionPanel.add(m_exactVersionSelector);
        m_exactVersion = new TextBox();
        m_exactVersion.addStyleName(css.mediumInput());
        exactVersionPanel.add(m_exactVersion);
        m_panel.add(exactVersionPanel);

        // Version range
        m_versionRangeSelector = new RadioButton(radioGroupName,
                messages.rangeLabel());
        m_versionRangeSelector.addValueChangeHandler(
                new ValueChangeHandler<Boolean>() {
                    @Override public void onValueChange(
                            ValueChangeEvent<Boolean> event)
                    {

                        if (event.getValue()) {
                            m_versionModeSelected = VersionMode.RANGE;
                        }
                    }
                });
        m_panel.add(m_versionRangeSelector);

        // Minimum
        HTMLTable versionRangeGrid    = new Grid(2, 3);
        Widget    minimumVersionLabel = new Label(messages.minimumLabel());

        minimumVersionLabel.addStyleName(css.formLabel());
        versionRangeGrid.setWidget(0, 0, minimumVersionLabel);
        m_minimumVersion = new TextBox();
        m_minimumVersion.addStyleName(css.shortInput());
        versionRangeGrid.setWidget(0, 1, m_minimumVersion);
        m_minimumInclusive = new CheckBox(messages.inclusiveLabel());
        versionRangeGrid.setWidget(0, 2, m_minimumInclusive);
        versionRangeGrid.getCellFormatter()
                .addStyleName(0, 2, css.inclusive());

        // Maximum
        Widget maximumVersionLabel = new Label(messages.maximumLabel());

        maximumVersionLabel.addStyleName(css.formLabel());
        versionRangeGrid.setWidget(1, 0, maximumVersionLabel);
        m_maximumVersion = new TextBox();
        m_maximumVersion.addStyleName(css.shortInput());
        versionRangeGrid.setWidget(1, 1, m_maximumVersion);
        m_maximumInclusive = new CheckBox(messages.inclusiveLabel());
        versionRangeGrid.setWidget(1, 2, m_maximumInclusive);
        versionRangeGrid.getCellFormatter()
                .addStyleName(1, 2, css.inclusive());
        m_panel.add(versionRangeGrid);
    }

    //~ Methods ----------------------------------------------------------------

    @Override public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<String> handler)
    {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    private void clearExactVersion()
    {
        m_exactVersionSelector.setValue(false);
        m_exactVersion.setValue("");
    }

    private void clearVersionRange()
    {
        m_versionRangeSelector.setValue(false);
        m_minimumVersion.setValue("");
        m_minimumInclusive.setValue(false);
        m_maximumVersion.setValue("");
        m_maximumInclusive.setValue(false);
    }

    @NotNull TextBox getExactVersion()
    {
        return m_exactVersion;
    }

    @NotNull RadioButton getExactVersionSelector()
    {
        return m_exactVersionSelector;
    }

    @NotNull CheckBox getMaximumInclusive()
    {
        return m_maximumInclusive;
    }

    @NotNull TextBox getMaximumVersion()
    {
        return m_maximumVersion;
    }

    @NotNull CheckBox getMinimumInclusive()
    {
        return m_minimumInclusive;
    }

    @NotNull TextBox getMinimumVersion()
    {
        return m_minimumVersion;
    }

    @NotNull VerticalPanel getPanel()
    {
        return m_panel;
    }

    @Override public String getValue()
    {
        @NonNls String value = "";

        if (m_versionModeSelected == VersionMode.EXACT) {
            value = m_exactVersion.getValue();
        }
        else if (m_versionModeSelected == VersionMode.RANGE) {
            value =  m_minimumInclusive.getValue()
                    ? "["
                    : "(";
            value += m_minimumVersion.getValue() + ","
                    + m_maximumVersion.getValue();
            value += m_maximumInclusive.getValue()
                    ? "]"
                    : ")";
        }

        return value;
    }

    @NotNull RadioButton getVersionRangeSelector()
    {
        return m_versionRangeSelector;
    }

    @Override public void setValue(@NotNull String value)
    {
        setValue(value, false);
    }

    @Override public void setValue(
            String  value,
            boolean fireEvents)
    {
        if (!StringUtil.isEmpty(value) && Objects.equal(value, getValue())) {
            return;
        }

        // Clear exact & range
        clearExactVersion();
        clearVersionRange();
        value = value != null
                ? value.trim()
                : "";

        if (StringUtil.isEmpty(value)) {
            m_versionModeSelected = VersionMode.LATEST;
            m_latestVersionSelector.setValue(true, fireEvents);

            if (fireEvents) {
                ValueChangeEvent.fire(this, value);
            }

            return;
        }

        if (value.contains(",")) {

            // Set version range
            m_versionModeSelected = VersionMode.RANGE;
            m_versionRangeSelector.setValue(true, fireEvents);

            String[]       versionTokens = value.split(",");
            @NonNls String beginRange    = versionTokens[0].trim();

            if (!StringUtil.isEmpty(beginRange)) {
                boolean minInclusive = "[".equals(beginRange.substring(0, 1));

                m_minimumInclusive.setValue(minInclusive);
                m_minimumVersion.setValue(beginRange.substring(1)
                        .trim());
            }

            if (versionTokens.length > 1) {
                @NonNls String endRange          = versionTokens[1].trim();
                int            endRangeLastIndex = endRange.length() - 1;
                boolean        maxInclusive      = "]".equals(
                        endRange.substring(endRangeLastIndex));

                m_maximumInclusive.setValue(maxInclusive);
                m_maximumVersion.setValue(endRange.substring(0,
                        endRangeLastIndex)
                        .trim());
            }
        }
        else {

            // Set specific version
            m_versionModeSelected = VersionMode.EXACT;
            m_exactVersionSelector.setValue(true, fireEvents);
            m_exactVersion.setValue(value);
        }

        if (fireEvents) {
            ValueChangeEvent.fire(this, value);
        }
    }
}
