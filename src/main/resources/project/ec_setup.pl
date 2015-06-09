if ($promoteAction eq "promote") {
}

# Data that drives the create step picker registration for this plugin.
my %pull = (
    label       => "Pull Docker image",
    procedure   => "runDockerPull",
    description => "Performs a docker pull on the requested image",
    category    => "Resource Management"
);
@::createStepPickerSteps = (\%pull);
