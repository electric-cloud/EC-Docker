Integrated Version
------------------

This plugin was tested against Docker Engine 17.06.

Prerequisite for deploying CloudBees CD service on stand-alone Docker engine
----------------------------------------------------------------------------

Docker engine daemon must listen on TCP socket. By default, daemon listens on Unix socket on linux/unix based OSs and on named pipe (npipe://) on windows.

To make above changes,

1.  Add `{`hosts`: [`tcp://IP:PORT`]}` entry in `/etc/docker/daemon.json` (on linux) and in `C:\ProgramData\Docker\config\daemon.json` (on Windows) file .
2.  Restart Docker daemon

Prerequisite for deploying CloudBees CD service on Docker Swarm cluster
-----------------------------------------------------------------------

Docker Swarm cluster must be already setup and Docker Swarm manager must be accessible over network.

Prerequisite for supporting TLS based authentication
----------------------------------------------------

In case of Docker endpoint supporting TLS based authentication, CA certificate (CA's public key), client's public and private keys are required.
All three keys can be stored in plugin configuration. More information about how to setup TLS certificates on Docker endpoint can be found
[here](https://github.com/docker/docker.github.io/blob/master/swarm/configure-tls.md).

If the Docker Swarm cluster is on Docker EE then client certificates can be obtained from UCP (Universal Control Plane) from `User Profile` page by clicking on `Create a Client Bundle`. Bundle contains all the required certificates.

In case of stand-alone Docker engine and for Docker Swarm cluster on Docker CE, perform below steps to generate client side certificates using openssl utility:

**Note:**If you already have access to a CA and certificates, you should skip this step 1.

Step 1: Setup CA server

1.  openssl genrsa -out ca-priv-key.pem 2048
2.  openssl req -config /usr/lib/ssl/openssl.cnf -new -key ca-priv-key.pem -x509 -days 1825 -out ca.pem

Step 2: Generete client side certificates

1.  openssl genrsa -out key.pem 2048
2.  openssl req -subj `/CN=client` -new -key key.pem -out client.csr
3.  openssl x509 -req -days 1825 -in client.csr -CA ca.pem -CAkey ca-priv-key.pem -CAcreateserial -out cert.pem -extensions v3_req -extfile /usr/lib/ssl/openssl.cnf

If TLS support is not enabled on Docker endpoint then leave `CA Certificate`, `Client Certificate` and `Client Private Key` input parameters on plugin configuration page empty. Plugin will establish simple HTTP connection with such Docker endpoint.