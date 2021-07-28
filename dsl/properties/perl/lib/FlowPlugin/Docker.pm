package FlowPlugin::Docker;
use strict;
use warnings;
use base qw/FlowPDF/;
use FlowPDF::Log;
use FlowPDF::Helpers qw/bailOut/;

# Feel free to use new libraries here, e.g. use File::Temp;

# Service function that is being used to set some metadata for a plugin.
sub pluginInfo {
    return {
        pluginName          => '@PLUGIN_KEY@',
        pluginVersion       => '@PLUGIN_VERSION@',
        configFields        => ['config'],
        configLocations     => ['ec_plugin_cfgs'],
        defaultConfigValues => {}
    };
}

# Auto-generated method for the connection check.
# Add your code into this method and it will be called when configuration is getting created.
# $self - reference to the plugin object
# $p - step parameters
# $sr - StepResult object
# Parameter: config
# Parameter: desc
# Parameter: endpoint
# Parameter: cacert
# Parameter: cert
# Parameter: credential
# Parameter: logLevel

sub checkConnection {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    my $configValues = $context->getConfigValues()->asHashref();
    logInfo("Config values are: ", $configValues);

    eval {
        # Use $configValues to check connection, e.g. perform some ping request
        # my $client = Client->new($configValues); $client->ping();
        my $password = $configValues->{password};
        if ($password ne 'secret') {
            # die "Failed to test connection - dummy check connection error\n";
        }
        1;
    } or do {
        my $err = $@;
        # Use this property to surface the connection error details in the CD server UI
        $sr->setOutcomeProperty("/myJob/configError", $err);
        $sr->apply();
        die $err;
    };
}
## === check connection ends ===

# Auto-generated method for the procedure runDockerBuild/runDockerBuild
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: use_sudo
# Parameter: build_path

# $sr - StepResult object
sub runDockerBuild {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
# Auto-generated method for the procedure runDockerPull/runDockerPull
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: use_sudo
# Parameter: image_name
# Parameter: tag

# $sr - StepResult object
sub runDockerPull {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    if(!defined $p->{image_name}) {
        die "ERROR: image name parameter is required but not set.";
    }

    logDebug("Try to login...\n");
    my ($exit_code, $error_message) = $self->login($configValues);
    if($exit_code) {
        bailOut($error_message);
    }

    my $command;
    if($p->{use_sudo}) {
        $command = "sudo docker pull";
    } else {
        $command = "docker pull";
    }

    my $image_name = $p->{image_name};
    if($p->{image_tag}) {
        $image_name .= ":$p->{image_tag}";
    }

    $command .= " $image_name 2>&1";

    logInfo("Command to execute: $command");
    logInfo('Pulling docker image:');

    my $docker_output = system($command);
    if($? != 0) {
        bailOut("Exit code: $?.\n$docker_output");
    }
    print $docker_output . "\n";

    $sr->setJobStepOutcome('success');
}
# Auto-generated method for the procedure runDockerRun/runDockerRun
# Add your code into this method and it will be called when step runs
# $self - reference to the plugin object
# $p - step parameters
# Parameter: use_sudo
# Parameter: image_name
# Parameter: container_name
# Parameter: detached_mode
# Parameter: entrypoint
# Parameter: working_dir
# Parameter: published_ports
# Parameter: publish_all_ports
# Parameter: privileged
# Parameter: container_links
# Parameter: command_with_args

# $sr - StepResult object
sub runDockerRun {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    $sr->setJobStepOutcome('warning');
    $sr->setJobSummary("This is a job summary.");
}
## === step ends ===
# Please do not remove the marker above, it is used to place new procedures into this file.


sub login {
    my ($self, $configValues, $use_sudo) = @_;

    logDebug('Try to login...');
    # Get credentials for Docker Registry
    my $cred = $configValues->getParameter('credential');
    my ($username, $password);
    if ($cred) {
        $username = $cred->getUserName();
        $password = $cred->getSecretValue();
    }

    if (!defined $username || $username eq '') {
        logDebug('Credentials empty, login skipped...');
        return (0, '');
    }

    my $cli = FlowPDF::ComponentManager->loadComponent('FlowPDF::Component::CLI', {
        workingDirectory => $ENV{COMMANDER_WORKSPACE}
    });

    my $command = $use_sudo ? $cli->newCommand('sudo', ['docker']) : $cli->newCommand('docker');
    $command->addArguments('login', '-u', $username, '-p', $password);
    my $res = $cli->runCommand($command);
    logDebug('LOGIN EXIT CODE: ' . $res->getCode());
    logInfo('LOGIN STDOUT:', $res->getStdout());
    logInfo('LOGIN STDERR:', $res->getStderr());
    return ($res->getCode(), $res->getStderr());
}

1;