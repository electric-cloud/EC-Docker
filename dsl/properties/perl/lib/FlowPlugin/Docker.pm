package FlowPlugin::Docker;
use strict;
use warnings;
use base qw/FlowPDF/;
use FlowPDF::Log;
use FlowPDF::Helpers qw/bailOut/;
use Text::ParseWords qw(shellwords);
use IPC::Open3;
use IO::Select;
use Symbol qw(gensym);

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
    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    # Use $configValues to check connection, e.g. perform some ping request
    # my $client = Client->new($configValues); $client->ping();
    my ($exit_code, $error_message) = $self->login($configValues);
    if($exit_code) {
        $sr->setOutcomeProperty("/myJob/configError", $error_message);
        $sr->apply();
        die $error_message;
    }
    logInfo("Connection is successful.");
    1;
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

    if(!defined $p->{build_path}) {
        bailOut("ERROR: build path parameter is required but not set.");
    }

    logDebug("Try to login...\n");
    my ($exit_code, $error_message) = $self->login($configValues);
    if($exit_code) {
        bailOut($error_message);
    }

    my @command;
    if($p->{use_sudo}) {
        @command = ('sudo', 'docker', 'build');
    } else {
        @command = ('docker', 'build');
    }
    push @command, _split_tokens($p->{build_path});

    logInfo("Command to execute: " . _quote_for_log(@command));
    logInfo("Building docker image:");

    my $docker_output = _runDockerCommand(@command);
    if($? != 0) {
        bailOut("Exit code: $?.\n$docker_output");
    }
    print $docker_output . "\n";

    $sr->setJobStepOutcome('success');
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
        bailOut("ERROR: image name parameter is required but not set.");
    }

    logDebug("Try to login...\n");
    my ($exit_code, $error_message) = $self->login($configValues);
    if($exit_code) {
        bailOut($error_message);
    }

    my @command;
    if($p->{use_sudo}) {
        @command = ('sudo', 'docker', 'pull');
    } else {
        @command = ('docker', 'pull');
    }

    my $image_name = _trim($p->{image_name});
    if($p->{image_tag}) {
        $image_name .= ':' . _trim($p->{image_tag});
    }
    # The literal '--' marks the end of options, so Docker always treats
    # image_name as data even if it starts with '-' - without it, Docker's
    # own CLI parsing (not a shell) would otherwise read a value like
    # "-foo" as an unrecognized flag instead of an image name.
    push @command, '--', $image_name;

    logInfo("Command to execute: " . _quote_for_log(@command));
    logInfo('Pulling docker image:');

    my $docker_output = _runDockerCommand(@command);
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
# Parameter: additional_options

# $sr - StepResult object
sub runDockerRun {
    my ($self, $p, $sr) = @_;

    my $context = $self->getContext();
    logInfo("Current context is: ", $context->getRunContext());
    logInfo("Step parameters are: ", $p);

    my $configValues = $context->getConfigValues();
    logInfo("Config values are: ", $configValues);

    if(!defined $p->{image_name}) {
        bailOut("ERROR: image name parameter is required, but not set.");
    }

    logDebug("Try to login...\n");
    my ($exit_code, $error_message) = $self->login($configValues);
    if($exit_code) {
        bailOut($error_message);
    }

    my @command;
    if($p->{use_sudo}) {
        @command = ('sudo', 'docker', 'run');
    } else {
        @command = ('docker', 'run');
    }

    if($p->{container_name}) {
        push @command, '--name=' . _trim($p->{container_name});
    }

    if($p->{detached_mode}) {
        push @command, '-d';
    }

    if($p->{entrypoint}) {
        push @command, '--entrypoint=' . _trim($p->{entrypoint});
    }

    if($p->{working_dir}) {
        push @command, '--workdir=' . _trim($p->{working_dir});
    }

    if($p->{published_ports}) {
        # split on runs of whitespace (not a single literal space) and
        # drop empty tokens, so a leading/trailing/doubled space does not
        # produce an empty -p value that Docker then rejects
        my @port_mappings = grep { $_ ne '' } split /\s+/, $p->{published_ports};
        foreach my $mapping (@port_mappings) {
            push @command, '-p', $mapping;
        }
    }

    if($p->{publish_all_ports}) {
        push @command, '-P';
    }

    if($p->{container_links}) {
        push @command, '--link', _trim($p->{container_links});
    }

    if($p->{privileged}) {
        push @command, '--privileged=true';
    }

    #Additional options can be added to the Docker run command
    if($p->{additional_options}  && $p->{additional_options} ne "") {
        push @command, _split_tokens($p->{additional_options});
    }

    # Same '--' end-of-options marker as runDockerPull. Caveat: if
    # additional_options ends in a flag that itself takes a value (for
    # example a dangling "-e" with nothing after it), Docker consumes this
    # '--' as that flag's value instead of as the end-of-options marker,
    # which reopens the leading-dash risk for image_name. This does not
    # grant any capability additional_options doesn't already have -
    # whoever can set it can already pass arbitrary Docker options such as
    # --privileged - so it is a documented caveat, not a privilege gap.
    push @command, '--', _trim($p->{image_name});

    if($p->{command_with_args}) {
        push @command, _split_tokens($p->{command_with_args});
    }

    print "\nCommand to execute: " . _quote_for_log(@command) . "\n";
    print "Launching docker container:\n\n";

    my $docker_output = _runDockerCommand(@command);
    if($? != 0) {
        bailOut("Exit code: $?.\n$docker_output");
    }
    print $docker_output . "\n";


    $sr->setJobStepOutcome('success');
}
## === step ends ===
# Please do not remove the marker above, it is used to place new procedures into this file.


# Splits a step parameter that is documented as holding multiple
# whitespace-separated tokens (additional_options, command_with_args,
# build_path) into the individual argv elements list-form system() needs,
# using shell quoting RULES but never a shell: shellwords understands
# quotes exactly as a shell would, without ever executing anything, so a
# quoted value like `-e "MSG=hello world"` still becomes two tokens
# (-e, "MSG=hello world") instead of being torn apart on every space, and
# an injection payload like `; touch MARKER #` still comes back as inert
# argv elements (;, touch, MARKER, #) rather than shell syntax.
#
# Leading/trailing whitespace is trimmed first: the old shell-based code
# effectively dropped it too (unquoted leading/trailing spaces are word
# separators to a shell, not part of the word), so this keeps e.g. a
# trailing space typed into "Image name" from becoming part of the value
# and getting rejected by Docker as an invalid reference.
#
# On Windows, shellwords' backslash-escaping rules would otherwise mangle
# a plain path like C:\tools\app.exe into C:toolsapp.exe, so backslashes
# are doubled first to make shellwords treat them literally. This is an
# incomplete workaround, not a full Windows cmd.exe quoting emulation:
# a Windows value that mixes backslashes with escaped quotes (for example
# `-e "MSG=say \"hi\""`) is not round-tripped correctly, and a lone
# apostrophe that was legal in a Windows value under the old cmd.exe-based
# code (for example `C:\o'brien`) now fails the unbalanced-quote check
# below, because shellwords treats "'" as a quote character on every
# platform. Neither of these is a security regression - they only affect
# whether a legitimate Windows value parses - but they are not fixed here;
# flagging for whoever can test this against a real Windows agent.
#
# shellwords also does not follow POSIX backslash rules inside double
# quotes (POSIX keeps a backslash before most characters; shellwords
# drops it before any character), so a value like a regex or printf
# format string containing backslashes - `grep -E "\d+\.\d+" file` - comes
# out with the backslashes silently removed, not an error. Also not a
# security issue, since the removed character was never going to reach a
# shell either way, but worth knowing if a command_with_args value stops
# working after this change.
#
# An unbalanced quote makes shellwords return an empty list even though
# the input was not empty; that is treated as a fatal input error rather
# than silently dropping the value.
sub _split_tokens {
    my ($value) = @_;
    return () unless defined $value;

    (my $trimmed = $value) =~ s/^\s+|\s+$//g;
    return () if $trimmed eq '';

    my $to_split = $trimmed;
    if ($^O eq 'MSWin32') {
        $to_split =~ s/\\/\\\\/g;
    }

    my @tokens = shellwords($to_split);
    if (!@tokens) {
        bailOut("ERROR: Unable to parse parameter value (unbalanced quote?): $value");
    }
    return @tokens;
}

# Trims leading/trailing whitespace from a step parameter that is a single
# value rather than multiple tokens (image_name, image_tag, container_name,
# container_links). Several of these were interpolated unquoted into the
# old shell string, where unquoted leading/trailing whitespace is just a
# word separator and never became part of the value; trimming here keeps
# that same behavior now that there is no shell to do it implicitly.
sub _trim {
    my ($value) = @_;
    return $value unless defined $value;
    $value =~ s/^\s+|\s+$//g;
    return $value;
}

# Renders an argv list for the job log so a reader can see where one
# argument ends and the next begins. This is for readability only - it is
# never passed back to system() - but without it, a neutralized payload
# like (';', 'touch', 'MARKER') logs as ") touch MARKER", which reads
# exactly like live shell syntax even though it never ran as one.
sub _quote_for_log {
    my (@args) = @_;
    return join(' ', map {
        my $arg = $_;
        $arg =~ s/'/'\\''/g;
        "'$arg'";
    } @args);
}

# Runs an external command with no shell involved (list-form system()), so
# that no step parameter can be interpreted as shell syntax regardless of
# metacharacters it contains. Temporarily duplicates STDERR onto STDOUT for
# the call, reproducing the old "2>&1" shell redirection without a shell.
#
# Uses the "system PROGRAM LIST" indirect-object form rather than plain
# "system LIST": per perlfunc, on Windows only the PROGRAM-LIST form
# reliably avoids the shell - plain LIST form, even with multiple
# elements, silently falls back to cmd.exe if the first spawn attempt
# fails, which would reopen the shell-injection hole this fix closes.
#
# Known limitation, not verified here: Perl's own Win32 argument-to-
# command-line conversion is reported not to escape an embedded double
# quote correctly in some builds. This has not been verified against a
# real Windows CDRO agent (none was available while writing this fix) and
# should be checked there before being relied on for Windows agents.
sub _runDockerCommand {
    my (@command) = @_;
    my $program = $command[0];

    open(my $saved_stderr, '>&', \*STDERR)
        or bailOut("ERROR: Unable to save STDERR before running docker command: $!");
    open(STDERR, '>&', \*STDOUT)
        or bailOut("ERROR: Unable to redirect STDERR to STDOUT before running docker command: $!");

    my $rc = system $program @command;
    my $exec_errno = $!;

    open(STDERR, '>&', $saved_stderr)
        or bailOut("ERROR: Unable to restore STDERR after running docker command: $!");
    close($saved_stderr);

    if ($rc == -1) {
        bailOut("ERROR: Failed to execute '$command[0]': $exec_errno");
    }

    return $rc;
}

sub login {
    my ($self, $configValues, $use_sudo) = @_;

    logDebug('Try to login...');
    my $registry = $configValues->getParameter('registry');
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

    # Shells out via FlowPDF::Component::CLI, same as every other call in
    # this file used to: it still routes through single-string system()
    # internally, and its argument quoting does not escape an embedded
    # single quote, so a username, password or registry containing one
    # breaks out of the quoted shell string. Build the argv list directly
    # and run it with no shell instead, exactly like the other procedures.
    # The password goes in via --password-stdin, never as a CLI argument,
    # so it never appears in argv, in a process listing, or in this log.
    #
    # Known limitation, not verified here: IPC::Open3 falls back to
    # plain "system LIST" internally on Windows (not the PROGRAM-LIST form
    # _runDockerCommand uses), so the same Windows cmd.exe-fallback-on-
    # failed-spawn risk described there also applies to this login, and
    # is not mitigated by that other fix. Not verified against a real
    # Windows CDRO agent.
    my @command;
    if ($use_sudo) {
        @command = ('sudo', 'docker', 'login', '-u', $username, '--password-stdin');
    } else {
        @command = ('docker', 'login', '-u', $username, '--password-stdin');
    }
    push @command, $registry if $registry;

    logInfo('Command to execute: ' . _quote_for_log(@command));

    my ($child_in, $child_out, $child_err);
    $child_err = gensym();
    my $pid = eval { open3($child_in, $child_out, $child_err, @command) };
    if (!$pid) {
        my $error = "Unable to execute docker login: $@$!";
        logDebug('LOGIN EXIT CODE: -1');
        logInfo('LOGIN STDERR:', $error);
        return (-1, $error);
    }

    # Writing the password can hit SIGPIPE if docker has already exited
    # (for example because it rejected the username/registry before
    # reading stdin at all) - caught here rather than left to kill the
    # whole plugin step, since the real failure reason is still available
    # from the stderr this reads below.
    my $write_failed = 0;
    {
        local $SIG{PIPE} = sub { $write_failed = 1 };
        print($child_in $password) or $write_failed = 1;
        close($child_in) or $write_failed = 1 unless $write_failed;
    }

    # Read stdout and stderr concurrently with IO::Select rather than one
    # after the other: docker login's output is normally tiny, but a
    # sequential <$child_out> then <$child_err> deadlocks forever (no
    # timeout) if the child ever writes enough to the *second* stream to
    # fill its pipe buffer before this code finishes reading the first.
    my ($stdout, $stderr) = ('', '');
    my $selector = IO::Select->new($child_out, $child_err);
    while ($selector->count) {
        for my $fh ($selector->can_read) {
            my $buf;
            my $n = sysread($fh, $buf, 65536);
            if (!$n) {
                $selector->remove($fh);
                next;
            }
            if ($fh == $child_out) { $stdout .= $buf; } else { $stderr .= $buf; }
        }
    }

    waitpid($pid, 0);
    my $rc = $? == -1 ? -1 : ($? >> 8);
    if ($write_failed && $rc == 0) {
        # system/pipe close failed but the child still reported success
        # (e.g. a race rather than a real rejection) - do not mask that
        # with an error the exit code disagrees with.
        logDebug('Note: writing the password hit an error, but docker login still exited 0');
    }

    logDebug('LOGIN EXIT CODE: ' . $rc);
    logInfo('LOGIN STDOUT:', $stdout);
    logInfo('LOGIN STDERR:', $stderr);
    return ($rc, $stderr);
}

1;