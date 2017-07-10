#
#  Copyright 2016 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

##########################
# createAndAttachCredential.pl
##########################
use ElectricCommander;

use constant {
	SUCCESS => 0,
	ERROR   => 1,
};

## get an EC object
my $ec = new ElectricCommander();
$ec->abortOnError(0);

my $configName = "$[/myJob/config]";

@credentials = ("cacert", "cert", "key");

foreach $credential (@credentials)
{
    my $xpath = $ec->getFullCredential($credential);
    my $errors = $ec->checkAllErrors($xpath);
    my $clientID = $xpath->findvalue("//userName");
    my $clientSecret = $xpath->findvalue("//password");
    # Format for name of credential object is "pluginConfigName_credential"
    # For e.g. If plugin configuration name is "dockerConfig"
    # then "dockerConfig_cacert", "dockerConfig_cert", "dockerConfig_key"
    # credential objects will be created.
    my $credName = $configName . "_" . $credential;
    my $projName = "$[/myProject/projectName]";

    # Create credential
    $ec->deleteCredential($projName, $credName);
    $xpath = $ec->createCredential($projName, $credName, $clientID, $clientSecret);
    $errors .= $ec->checkAllErrors($xpath);

    #Give config the credential's real name
    my $configPath = "/projects/$projName/ec_plugin_cfgs/$configName";
    $xpath = $ec->setProperty($configPath . "/credential_$credential", $credName);
    $errors .= $ec->checkAllErrors($xpath);

    # Give job launcher full permissions on the credential
    my $user = "$[/myJob/launchedByUser]";
    $xpath = $ec->createAclEntry("user", $user,
        {projectName => $projName,
         credentialName => $credName,
         readPrivilege => allow,
         modifyPrivilege => allow,
         executePrivilege => allow,
         changePermissionsPrivilege => allow});
    $errors .= $ec->checkAllErrors($xpath);

    # Attach credential to steps that will need it
    $xpath = $ec->attachCredential($projName, $credName,
        {procedureName => "Check Cluster",
         stepName => "checkCluster"});
    $errors .= $ec->checkAllErrors($xpath);

    $xpath = $ec->attachCredential($projName, $credName,
        {procedureName => "Deploy Service",
         stepName => "createOrUpdateDeployment"});
    $errors .= $ec->checkAllErrors($xpath);

    $xpath = $ec->attachCredential($projName, $credName,
        {procedureName => "Populate Certs",
         stepName => "populateDockerClientCerts"});
    $errors .= $ec->checkAllErrors($xpath);

    $xpath = $ec->attachCredential($projName, $credName,
        {procedureName => "Delete Service",
         stepName => "cleanup"});
    $errors .= $ec->checkAllErrors($xpath);


     if ("$errors" ne "") {
        my $errMsg = "Error creating configuration credential: " . $errors;
        $ec->setProperty("/myJob/configError", $errMsg);
        print $errMsg;      
        last;
     }
}

if ("$errors" ne "") {
    foreach $credential (@credentials){
        # Cleanup the partially created configuration we just created
        my $configPath = "/projects/$projName/ec_plugin_cfgs/$configName";
        $ec->deleteProperty($configPath);

        my $credName = $configName . "_" . $credential;
        $ec->deleteCredential($projName, $credName);       
    }
    exit 1;
}