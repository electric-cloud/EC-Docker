if ($promoteAction eq "promote") {
	$batch->deleteProperty("/server/ec_customEditors/pickerStep/Pull Docker image");
    $batch->deleteProperty("/server/ec_customEditors/pickerStep/Build Docker image");
	$batch->deleteProperty("/server/ec_customEditors/pickerStep/Run Docker container");
}

# Data that drives the create step picker registration for this plugin.
my %pull = (
    label       => "Pull Docker image",
    procedure   => "runDockerPull",
    description => "Performs a docker pull on the requested image",
    category    => "Resource Management"
);

my %build = (
    label       => "Build Docker image",
    procedure   => "runDockerBuild",
    description => "Performs a docker build",
    category    => "Resource Management"
);

my %run = (
    label       => "Run Docker container",
    procedure   => "runDockerRun",
    description => "Performs a container to run",
    category    => "Resource Management"
);

@::createStepPickerSteps = (\%pull, \%build, \%run);
