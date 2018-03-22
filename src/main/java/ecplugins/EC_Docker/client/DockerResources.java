
// RetrieveArtifactResources.java --
//
// RetrieveArtifactResources.java is part of ElectricCommander.
//
// Copyright (c) 2005-2016 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.EC_Docker.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;

/**
 * This interface houses a class that extends CssResource.
 *
 * <p>More information here:
 * http://code.google.com/webtoolkit/doc/latest/DevGuideClientBundle.html</p>
 */
public interface DockerResources
    extends ClientBundle
{

    //~ Instance fields --------------------------------------------------------

    // The instance of the ClientBundle that must be injected during doInit()
    DockerResources RESOURCES = GWT.create(DockerResources.class);

    //~ Methods ----------------------------------------------------------------

    // Specify explicit stylesheet. Every class in the stylesheet should have a
    // function defined in DockerStyles
    @Source("Docker.css")
    DockerStyles css();
}
