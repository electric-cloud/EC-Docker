package Setup;
use strict;
use warnings;
use JSON;
use Data::Dumper;
use Digest::MD5;
use MIME::Base64 qw(decode_base64);
use Archive::Zip;
use ElectricCommander::Util;
use ElectricCommander;
use File::Find;
use File::Spec;
use File::Path qw(mkpath);
use File::Temp q(tempfile);
use File::Copy::Recursive qw(rcopy);

use constant {
    META_PROPERTY => '/myProject/ec_binary_dependencies',
    DEPS_CACHE => "depsCache",
    SHARED_DEPENDENCIES_PATH => 'sharedDependenciesPath',
};

sub logInfo {
    my @messages = @_;

    for my $m (@messages) {
        if (ref $m) {
            print Dumper $m;
        }
        else {
            print "$m\n";
        }
    }
}

sub new {
    my ($class) = @_;
    return bless {}, $class;
}


sub fetchFromServer {
    my ($self, $dest) = @_;

    # REST Client
    my $ec = ElectricCommander->new;
    my $session = $ENV{COMMANDER_SESSIONID};
    my $ua = LWP::UserAgent->new(
        conn_cache => new LWP::ConnCache( total_capacity => 100 ),
        cookie_jar => {},
        timeout => $ec->{timeout},
        ssl_opts => { verify_hostname => 0 }
    );

    my $httpProxy = $ENV{COMMANDER_HTTP_PROXY};
    if ($httpProxy) {
        $ua->proxy(https => $httpProxy);
        $ua->proxy(http => $httpProxy);
    }

    my $protocol = $ENV{COMMANDER_SECURE} ? 'https' : 'http';
    my $httpsPort = $ENV{COMMANDER_HTTPS_PORT} || 8443;
    my $httpPort = $ENV{COMMANDER_PORT} || 8000;
    my $port = $ENV{COMMANDER_SECURE} ? $httpsPort : $httpPort;
    my $server = $ENV{COMMANDER_SERVER} || 'localhost';
    my $pluginName = '@PLUGIN_NAME@';
    my $url = "$protocol://$server:$port/rest/v1.0/plugins/$pluginName/agent-dependencies";

    my $dependencies = File::Spec->catfile($dest, ".cbDependenciesTarget.zip");
    my $response = $ua->get($url, cookie => "sessionId=$session", ':content_file' => $dependencies);
    logInfo "Saved response to $dependencies";
    return $dependencies;
}


sub fetchFromDsl {
    my ($self, $dest) = @_;

    my $ec = ElectricCommander->new;
    my $dsl = $ec->getPropertyValue('/myProcedure/ec_compressAndDeliver');

    my $hasMore = 1;
    my $offset = 0;

    my $dependencies = File::Spec->catfile($dest, ".cbDependenciesTarget.zip");
    open my $fh, ">$dependencies" or die "Cannot open $dependencies: $!";
    binmode $fh;
    my $source = $ec->getPropertyValue('/server/settings/pluginsDirectory') .'/@PLUGIN_NAME@/agent';
    while($hasMore) {
        my $args = {
            offset => $offset,
            source => $source,
            chunkSize => 1024 * 1024 * 4
        };
        print "Calling evalDSl with arguments " . Dumper ($args) . "\n";
        my $xpath = $self->ec->evalDsl({
            dsl => $dsl,
            parameters => encode_json($args),
        });
        my $result = $xpath->findvalue('//value')->string_value;
        my $chunks = decode_json($result);
        my $chunk = $chunks->{chunk};
        my $bytes = decode_base64($chunk);
        my $remaining = $chunks->{remaining};
        print "Got bytes: " . length($bytes) . "\n";
        print "Bytes remaining: " . $remaining . "\n";
        my $readBytes = $chunks->{readBytes};
        $offset += $readBytes;
        print "Read bytes: $readBytes\n";
        if ($readBytes > 0) {
            print $fh $bytes;
        }
        if ($remaining == 0 || $readBytes <= 0) {
            $hasMore = 0;
        }
    }
    close $fh;
    return $dependencies;
}

# Auto-generated method for the procedure DeliverDependencies/DeliverDependencies
# Add your code into this method and it will be called when step runs
sub deliverDependencies {
    my ($self) = @_;


    my $resName = '$[resourceName]';
    $self->ec->setProperty('/myJob/grabbedResource', $resName);
    logInfo "Grabbed resource $resName";

    if ($self->checkCache()) {
        print "Local file cache is ok\n";
        $self->copyGrapes();
        $self->copySharedDeps();
        exit 0;
    }

    logInfo "Local cache failed, reloading files";

    my $source = $self->ec->getPropertyValue('/server/settings/pluginsDirectory') .'/@PLUGIN_NAME@/agent';
    my $dest = File::Spec->catfile($ENV{COMMANDER_PLUGINS}, '@PLUGIN_NAME@/agent');
    mkpath($dest);
    my $dependencies;
    # TODO add check version when there is one
    if (1) {
        $dependencies = $self->fetchFromServer($dest);
    }
    else {
        $dependencies = $self->fetchFromDsl($dest);
    }

    my $zip = Archive::Zip->new();
    unless($zip->read($dependencies) == Archive::Zip::AZ_OK()) {
      die "Cannot read .zip dependencies: $!";
    }
    $zip->extractTree("", $dest . '/');

    unlink $dependencies;
    print "Extracted dependencies archive\n";
    $self->writeMeta();


    $self->copyGrapes();
    $self->copySharedDeps();
}

sub copyGrapes {
    my ($self) = @_;

    my $grapes = File::Spec->catfile(
        $ENV{COMMANDER_PLUGINS},
        '@PLUGIN_NAME@/agent/grape'
    );

    unless(-d $grapes) {
        return;
    }

    logInfo "Grapes folder found";
    my $grapesDir = $ENV{COMMANDER_DATA} . '/grape';
    mkpath($grapesDir);
    rcopy($grapes, $grapesDir);
    logInfo "Copied grapes dependencies into $grapesDir";
}

sub copySharedDeps {
    my ($self) = @_;

    my $sharedDeps = File::Spec->catfile(
        $ENV{COMMANDER_PLUGINS},
        '@PLUGIN_NAME@/agent/shared'
    );

    unless(-d $sharedDeps) {
        return;
    }

    my $destFolder = eval {
        $self->ec->getPropertyValue(META_PROPERTY . '/sharedDependenciesPath');
    };
    $destFolder ||= 'shared-deps';

    my $destination = File::Spec->catfile($ENV{COMMANDER_DATA}, $destFolder);
    rcopy($sharedDeps, $destination);
    logInfo "Shared dependencies copied into $destination";
}

sub checkCache {
    my ($self) = @_;

    my $prop = META_PROPERTY . '/' . DEPS_CACHE;
    my $meta;
    eval {
        my $metaJson = $self->ec->getPropertyValue($prop);
        $meta = JSON->new->decode($metaJson);
        1;
    } or do {
        logInfo "Cannot read dependencies map $prop";
        return 0;
    };
    my $folder = File::Spec->catfile($ENV{COMMANDER_PLUGINS}, '@PLUGIN_NAME@/agent');
    for my $file (keys %$meta) {
        my $fullname = File::Spec->catfile($folder, $file);
        if (!-f $fullname) {
            logInfo "Cannot find file $fullname declared in the cache map";
            return 0;
        }
    }
    return 1;
}

sub writeMeta {
    my ($self) = @_;
    my $folder = File::Spec->catfile($ENV{COMMANDER_PLUGINS}, '@PLUGIN_NAME@/agent');
    my %files = ();
    find(sub {
        return if $_ =~ /^\./;
        return if -d $File::Find::name;
        my $rel = File::Spec->abs2rel($File::Find::name, $folder);
        $files{$rel} = -1;
    }, $folder);

    my $meta = JSON->new->encode(\%files);
    my $property = META_PROPERTY . '/' . DEPS_CACHE;
    $self->ec->setProperty($property, $meta);
    logInfo "Saved meta into $property", \%files;
}

sub ec {
    my ($self) = @_;

    $self->{ec} ||= ElectricCommander->new;
    return $self->{ec};
}


sub getSharedDepsFolder {
    my ($self) = @_;

    my $destFolder = eval {
        $self->ec->getPropertyValue(META_PROPERTY . '/' . SHARED_DEPENDENCIES_PATH);
    };
    $destFolder ||= 'shared-deps';

    my $destination = File::Spec->catfile($ENV{COMMANDER_DATA}, $destFolder);
    return $destination;
}


1;


my $o = Setup->new;
$o->deliverDependencies();

