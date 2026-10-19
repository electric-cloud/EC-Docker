package FlowPlugin::Docker;
use strict;
use warnings;
use base qw/FlowPDF/;
use FlowPDF::Log;
use FlowPDF::Helpers qw/bailOut trim isWin/;
use Text::ParseWords qw(shellwords);
use IPC::Open3;

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
    my ($exit_code, $error_message) = $self->login($configValues, $p->{use_sudo});
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

    # docker's own stdout/stderr already stream live to the job log via
    # the inherited file descriptors _runDockerCommand() sets up - its
    # return value is the raw system() wait-status integer, not captured
    # output text, so it is never printed or embedded in a message here
    # (doing so would show a stray number like "0" or "256", not output).
    _runDockerCommand(@command);
    if($? != 0) {
        bailOut("Exit code: $?.");
    }

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
    my ($exit_code, $error_message) = $self->login($configValues, $p->{use_sudo});
    if($exit_code) {
        bailOut($error_message);
    }

    my @command;
    if($p->{use_sudo}) {
        @command = ('sudo', 'docker', 'pull');
    } else {
        @command = ('docker', 'pull');
    }

    my $image_name = trim($p->{image_name});
    # Checking for a non-empty string *after* trimming, not the raw
    # parameter's truthiness: a raw value of the single character "0" is
    # a legitimate Docker tag but is false in a Perl boolean context, and
    # a whitespace-only value is true before trimming but empty after,
    # which would otherwise silently produce a malformed "image:" with no
    # tag. Guard trim() with "defined" first - trim(undef) warns and the
    # common case is this field being unset entirely.
    my $tag = defined $p->{tag} ? trim($p->{tag}) : undef;
    if (defined $tag && $tag ne '') {
        $image_name .= ':' . $tag;
    }
    # The literal '--' marks the end of options, so Docker always treats
    # image_name as data even if it starts with '-' - without it, Docker's
    # own CLI parsing (not a shell) would otherwise read a value like
    # "-foo" as an unrecognized flag instead of an image name.
    push @command, '--', $image_name;

    logInfo("Command to execute: " . _quote_for_log(@command));
    logInfo('Pulling docker image:');

    # See runDockerBuild's identical comment: _runDockerCommand()'s return
    # value is a wait-status integer, not captured output text.
    _runDockerCommand(@command);
    if($? != 0) {
        bailOut("Exit code: $?.");
    }

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
    my ($exit_code, $error_message) = $self->login($configValues, $p->{use_sudo});
    if($exit_code) {
        bailOut($error_message);
    }

    my @command;
    if($p->{use_sudo}) {
        @command = ('sudo', 'docker', 'run');
    } else {
        @command = ('docker', 'run');
    }

    # Each of these trims first and then checks for a non-empty string,
    # rather than checking the raw parameter's truthiness first: checking
    # truthiness before trimming would treat a whitespace-only value as
    # set (producing a malformed, empty flag value after trimming), and
    # would treat a legitimate single-character "0" value as unset
    # (since the string "0" is false in a Perl boolean context).
    my $container_name = defined $p->{container_name} ? trim($p->{container_name}) : undef;
    if (defined $container_name && $container_name ne '') {
        push @command, '--name=' . $container_name;
    }

    if($p->{detached_mode}) {
        push @command, '-d';
    }

    my $entrypoint = defined $p->{entrypoint} ? trim($p->{entrypoint}) : undef;
    if (defined $entrypoint && $entrypoint ne '') {
        push @command, '--entrypoint=' . $entrypoint;
    }

    my $working_dir = defined $p->{working_dir} ? trim($p->{working_dir}) : undef;
    if (defined $working_dir && $working_dir ne '') {
        push @command, '--workdir=' . $working_dir;
    }

    # "defined", not truthiness: a raw value of "0" is a legitimate (if
    # unusual) port mapping but is false in a Perl boolean context, which
    # would otherwise silently drop it, the same bug class already fixed
    # above for container_name/entrypoint/working_dir/container_links.
    if (defined $p->{published_ports}) {
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

    my $container_links = defined $p->{container_links} ? trim($p->{container_links}) : undef;
    if (defined $container_links && $container_links ne '') {
        push @command, '--link', $container_links;
    }

    if($p->{privileged}) {
        push @command, '--privileged=true';
    }

    #Additional options can be added to the Docker run command
    # _split_tokens() already treats undef/whitespace-only as "nothing to
    # add" and returns an empty list - no truthiness guard needed here,
    # and one would silently drop a literal "0" value (same bug class
    # fixed above for container_name/entrypoint/working_dir).
    push @command, _split_tokens($p->{additional_options});

    # Same '--' end-of-options marker as runDockerPull. Caveat: if
    # additional_options ends in a flag that itself takes a value (for
    # example a dangling "-e" with nothing after it), Docker consumes this
    # '--' as that flag's value instead of as the end-of-options marker,
    # which reopens the leading-dash risk for image_name. This does not
    # grant any capability additional_options doesn't already have -
    # whoever can set it can already pass arbitrary Docker options such as
    # --privileged - so it is a documented caveat, not a privilege gap.
    push @command, '--', trim($p->{image_name});

    # Same reasoning as additional_options above: _split_tokens() already
    # handles undef/empty correctly, and a truthiness guard here would
    # silently drop a literal "0" command_with_args value.
    push @command, _split_tokens($p->{command_with_args});

    print "\nCommand to execute: " . _quote_for_log(@command) . "\n";
    print "Launching docker container:\n\n";

    # See runDockerBuild's identical comment: _runDockerCommand()'s return
    # value is a wait-status integer, not captured output text.
    _runDockerCommand(@command);
    if($? != 0) {
        bailOut("Exit code: $?.");
    }

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
    if (isWin()) {
        $to_split =~ s/\\/\\\\/g;
    }

    my @tokens = shellwords($to_split);
    if (!@tokens) {
        bailOut("ERROR: Unable to parse parameter value (unbalanced quote?): $value");
    }
    return @tokens;
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
        # An embedded newline would otherwise split the log into text
        # that reads as independent, legitimate-looking log lines (for
        # example a container_name of "foo\nException: Login successful,
        # continuing") - shown visibly instead, so the log always reflects
        # one argument per quoted segment.
        $arg =~ s/\r?\n/\\n/g;
        $arg =~ s/'/'\\''/g;
        "'$arg'";
    } @args);
}

# Quotes a single argument the way CommandLineToArgvW expects, so a value
# survives Windows' own command-line round trip unchanged. Verified on a
# real Windows agent (cb-perl 5.32.1, MSWin32-x64): without this, a value
# containing an embedded double quote (for example a container_name of
# foo"bar) corrupts argv entirely - two adjacent arguments silently merge
# into one, moving every later argument (including the image name) out of
# place - and a backslash immediately before a quote is silently dropped.
# Both are real, reproduced bugs in Perl's default Win32 argument-to-
# command-line conversion, not a security issue by themselves (there is
# still no shell involved), but a correctness one worth closing since the
# fix was on hand to verify.
sub _win32_quote_arg {
    my ($arg) = @_;
    return '""' if $arg eq '';
    return $arg unless $arg =~ /[\s"]/;
    my $result = '"';
    my $i = 0;
    my $len = length($arg);
    while ($i < $len) {
        my $num_backslashes = 0;
        while ($i < $len && substr($arg, $i, 1) eq '\\') {
            $num_backslashes++;
            $i++;
        }
        if ($i == $len) {
            $result .= ('\\' x ($num_backslashes * 2));
        } elsif (substr($arg, $i, 1) eq '"') {
            $result .= ('\\' x ($num_backslashes * 2 + 1)) . '"';
            $i++;
        } else {
            $result .= ('\\' x $num_backslashes) . substr($arg, $i, 1);
            $i++;
        }
    }
    $result .= '"';
    return $result;
}

# Looks up a program name against PATH (and PATHEXT on Windows) the same
# way the OS loader would, returning the resolved path or undef. Used
# only to fail cleanly before IPC::Open3 ever attempts to spawn a missing
# program: open3 has no PROGRAM-LIST form (unlike system/exec), so on
# Windows it falls back to cmd.exe whenever the spawn it tries fails -
# verified on a real Windows agent, where a missing program plus an
# "&"-joined command actually ran the injected command via cmd.exe and
# reported success. Checking first means that failure never happens, so
# the fallback is never reached.
sub _resolve_executable {
    my ($name) = @_;
    # "-x" alone is true for a directory (its search/traversal bit), so a
    # directory literally named e.g. "docker" earlier in PATH than the
    # real binary would be wrongly reported as found - checked and
    # rejected with "-d" before every "-x" test below.
    return $name if $name =~ m{[\\/]} && -x $name && !-d $name;
    my $sep = isWin() ? ';' : ':';
    my @exts = isWin() ? (split /;/, $ENV{PATHEXT} // '.EXE;.BAT;.CMD') : ('');
    for my $dir (split /\Q$sep\E/, $ENV{PATH} // '') {
        next unless length $dir;
        for my $ext (@exts) {
            my $candidate = "$dir/$name$ext";
            return $candidate if -x $candidate && !-d $candidate;
        }
    }
    return undef;
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
# Verified on a real Windows agent: a nonexistent program name with a
# "&"-joined command in plain LIST form ran the injected command via
# cmd.exe and reported success; the identical PROGRAM-LIST call correctly
# failed to find the program and ran nothing.
sub _runDockerCommand {
    my (@command) = @_;
    my $program = $command[0];

    if (isWin()) {
        @command = map { _win32_quote_arg($_) } @command;
    }

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

    # FlowPDF::Component::CLI::runCommand chdir()s to the configured
    # working directory before running its command; this rewrite replaces
    # that component entirely (see below) but keeps the same chdir so
    # anything cwd-sensitive about the login (a relative credential
    # helper path, for example) still sees the same directory as before.
    if ($ENV{COMMANDER_WORKSPACE}) {
        chdir($ENV{COMMANDER_WORKSPACE}) or bailOut("ERROR: Unable to chdir to $ENV{COMMANDER_WORKSPACE}: $!");
    }

    # Shells out via FlowPDF::Component::CLI, same as every other call in
    # this file used to: it still routes through single-string system()
    # internally, and its argument quoting does not escape an embedded
    # single quote, so a username, password or registry containing one
    # breaks out of the quoted shell string. Build the argv list directly
    # and run it with no shell instead, exactly like the other procedures.
    # The password goes in via --password-stdin, never as a CLI argument,
    # so it never appears in argv, in a process listing, or in this log.
    my @command;
    if ($use_sudo) {
        @command = ('sudo', 'docker', 'login', '-u', $username, '--password-stdin');
    } else {
        @command = ('docker', 'login', '-u', $username, '--password-stdin');
    }
    # Same '--' end-of-options marker used for image_name elsewhere in
    # this file: without it, a configured registry value starting with
    # '-' would be read by Docker's own CLI parser as a flag rather than
    # the positional SERVER argument.
    push @command, '--', $registry if $registry;

    logInfo('Command to execute: ' . _quote_for_log(@command));

    # IPC::Open3 has no PROGRAM-LIST form, so unlike _runDockerCommand this
    # can't avoid the Windows cmd.exe-on-failed-spawn fallback by syntax
    # alone - instead, resolve the program against PATH first so that
    # failure never happens. Verified on a real Windows agent: without
    # this check, a missing program plus an injected argument ran via
    # cmd.exe and reported success instead of failing cleanly.
    if (!defined _resolve_executable($command[0])) {
        my $error = "Unable to execute docker login: '$command[0]' not found";
        logDebug('LOGIN EXIT CODE: -1');
        logInfo('LOGIN STDERR:', $error);
        return (-1, $error);
    }

    my @exec_command = isWin() ? (map { _win32_quote_arg($_) } @command) : @command;

    # Passing undef for the third handle merges the child's stderr into
    # the same pipe as stdout, so there is only one output stream to
    # read. This replaces an earlier version that read two separate
    # streams concurrently with IO::Select - which is documented
    # (perlport) to work only on sockets on Windows, not on pipes, so it
    # busy-looped at 100% CPU forever on Windows instead of ever reading
    # anything. Merging the streams removes the need for select() at
    # all, on every platform, and sidesteps the deadlock a strict
    # stdout-then-stderr read order could hit if the child wrote enough
    # to the second stream before the first was fully drained.
    my ($child_in, $child_out);
    my $pid = eval { open3($child_in, $child_out, undef, @exec_command) };
    if (!$pid) {
        my $error = "Unable to execute docker login: $@$!";
        logDebug('LOGIN EXIT CODE: -1');
        logInfo('LOGIN STDERR:', $error);
        return (-1, $error);
    }

    # Write the password and close stdin before reading any output.
    # docker login reads the whole of stdin before producing output of
    # its own, so this ordering does not deadlock in practice - verified
    # locally with a password up to 100KB (well beyond a realistic
    # registry token) with no hang. Writing can still hit SIGPIPE if
    # docker already exited (for example it rejected the username before
    # ever reading stdin) - caught here rather than left to kill the
    # whole plugin step, since the real failure reason is still available
    # from the output read below.
    # close() always runs, even if the print already failed: an earlier
    # version used "close(...) or ... unless $write_failed", but Perl's
    # "unless" statement modifier guards the *entire* preceding
    # statement, so when $write_failed was already true it skipped
    # close() entirely instead of still closing the handle - leaking one
    # file descriptor per failed login.
    my $write_failed = 0;
    {
        local $SIG{PIPE} = sub { $write_failed = 1 };
        print($child_in $password) or $write_failed = 1;
        close($child_in) or $write_failed = 1;
    }

    my $output = '';
    while (1) {
        my $buf;
        my $n = sysread($child_out, $buf, 65536);
        if (!defined $n) {
            # sysread() returns undef, not 0, when an unrelated signal
            # interrupts the blocking read (EINTR) - verified directly: a
            # SIGALRM arriving mid-read reproduces this even with a
            # handler that just returns. Treating undef the same as 0
            # (real EOF) would stop reading and silently truncate the
            # captured login output. Retry on EINTR; give up on any other
            # read error, since there is nothing more this loop can do.
            next if $!{EINTR};
            last;
        }
        last if $n == 0;
        $output .= $buf;
    }

    # waitpid() itself can fail (return -1, e.g. ECHLD if something else
    # in this process reaps SIGCHLD globally), in which case $? is stale
    # or meaningless - checked separately from the exit-status decode
    # below so a wait failure can never be misread as a real exit code.
    my $wait_rc = waitpid($pid, 0);
    if ($wait_rc == -1) {
        bailOut("ERROR: Unable to determine docker login's exit status (waitpid failed: $!)");
    }
    # $? >> 8 alone would report a signal-killed docker login (for
    # example OOM-killed) as exit code 0 - success - because the exit
    # status byte is 0 when a process dies by signal; check for that
    # first so a killed login is never treated as a successful one.
    my $rc = ($? & 127) ? 1 : ($? >> 8);
    if ($write_failed && $rc == 0) {
        # system/pipe close failed but the child still reported success
        # (e.g. a race rather than a real rejection) - do not mask that
        # with an error the exit code disagrees with.
        logDebug('Note: writing the password hit an error, but docker login still exited 0');
    }

    logDebug('LOGIN EXIT CODE: ' . $rc);
    logInfo('LOGIN OUTPUT:', $output);
    return ($rc, $output);
}

1;