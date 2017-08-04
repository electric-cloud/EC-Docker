EC-Docker
============

The ElectricFlow Docker integration

## Build ##

Run dev/build.sh to compile the plugin

`./dev/build.sh`

## Tests 
[TBD]

## Build And Upload ##
0. Install git
   sudo apt-get install git
1. Get this plugin
   https://github.com/electric-cloud/EC-Docker.git
2. Build the plugin
   `./dev/build.sh` (in EC-Docker directory)
3. Upload the plugin to EC server
4. Create a configuration for the EC-Docker plugin.

## Prerequisites:
   + Docker engine must be accessible on TCP port.
   + For deploying services, a swarm cluster must already be setup. Follow bellow steps to setup one.

        1. Install Docker on all machines that needs to be part of cluster.
           Minimum version of Docker required for swarm is v1.12.
        2. Run bellow command on any of the machines. The machine on which this command is run becomes the manager node.

           ```
           docker swarm init --advertise-addr 192.168.99.100
            Swarm initialized: current node (dxn1zf6l61qsb1josjja83ngz) is now a manager.
             
            To add a worker to this swarm, run the following command:
             
                docker swarm join \
                --token SWMTKN-1-49nj1cmql0jkz5s954yi3oex3nedyz0fb0xx14ie39trti4wxv-8vxv8rssmk743ojnwacrr2e7c \
                192.168.99.100:2377
             
            To add a manager to this swarm, run 'docker swarm join-token manager' and follow the instructions.

           ```
        3. Join worker nodes. Run bellow command on rest of nodes.
           The command to run itself is reported as output of 'docker swarm init' command.
            ```
            docker swarm join \
              --token SWMTKN-1-49nj1cmql0jkz5s954yi3oex3nedyz0fb0xx14ie39trti4wxv-8vxv8rssmk743ojnwacrr2e7c \
              192.168.99.100:2377
           ```
           
          4. Swarm cluster can also be setup on AWS from [AWS Marketplace](https://aws.amazon.com/marketplace/pp/B06XCFDF9K)


## Required files: 
   If docker engine is running behind TLS enabled endpoint, then CA root cert, client private key and client certificate are required.

## Detailed Info: 
    
  Wiki: http://wiki/display/ec/EC-Docker+integration+with+Docker+Swarm