
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

    String addDependentArtifactVersion();

    String addExcludePattern();

    String addFilter();

    String addIncludePattern();

    String artifactLabel();

    String dependentArtifactVersionsLabel();

    String enableCompressionLabel();

    String exactLabel();

    String excludePatternsLabel();

    String filtersLabel();

    String followSymlinksLabel();

    String fromDirectoryLabel();

    String includePatternsLabel();

    String inclusiveLabel();

    String latestLabel();

    String maximumLabel();

    String minimumLabel();

    String pubArtifactDesc();

    String pubCompressDesc();

    String pubDependentArtifactsDesc();

    String pubExcludePatternsDesc();

    String pubFollowSymlinksDesc();

    String pubFromDirectoryDesc();

    String pubIncludePatternsDesc();

    String pubRepositoryDesc();

    String pubVersionDesc();

    String rangeLabel();

    String repositoryLabel();

    String retArtifactDesc();

    String retFiltersDesc();

    String retLocationPropDesc();

    String retrievedArtifactLocationPropertyLabel();

    String retrieveToDirectory();

    String retrieveToDirectoryDesc();

    String retVersionDesc();

    String versionLabel();

    String targetRepositoryLabel();

    String targetRepositoryDesc();

    String sourceRepositoryLabel();

    String sourceRepositoryDesc();

    String addTargetRepoPanel();

    String overwriteLabel();

    String overwriteDesc();

    String parallelUploadLabel();

    String parallelUploadDesc();

}
