
// ArtifactMessages.java --
//
// ArtifactMessages.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.EC_Docker.client;

import com.google.gwt.i18n.client.Messages;

public interface DockerMessages
        extends Messages
{

    //~ Methods ----------------------------------------------------------------

    String artifactLabel();

    String exactLabel();

    String inclusiveLabel();

    String latestLabel();

    String maximumLabel();

    String minimumLabel();

    String rangeLabel();

    String retArtifactDesc();

    String retVersionDesc();

    String versionLabel();

    String imageName();

    String registryURL();

    String imageNameDescription();

    String envLabel();

    String envDoc();

    String configLabel();

    String configDoc();

    String registryUrlDoc();

    String credLabel();

    String credDoc();

    String baseImageLabel();

    String baseImageDoc();

    String portsLabel();

    String portsDoc();

    String commandLabel();

    String commandDoc();

}
