<p>
User can provide a comma-separated list of networks on which to deploy the container or the swarm service in the service mapping page when mapping the service to a cluster in ElectricFlow. If the network does not already exist then the procedure will create one with the provided subnet and gateway. If no subnet and gateway is specified, Docker uses default values. Each of the user-defined networks can have multiple subnets and gateways. In that case, multiple subnets/gateways must be separated by '|'(pipe). If deploying to a stand-alone Docker engine then the user-defined <b>"bridge"</b> network is created.  If deploying to a Docker swarm cluster then the user-defined <b>"overlay"</b> network is created.
</p>
<p>
For example,
<table class="grid">
<thead>
    <tr>
        <td>Networks:</td>
        <td>bridge, net1, net2</td>
    </tr>
</thead>
<tbody>
    <tr>
        <td>Subnets:</td>
        <td>,10.200.1.10/24|10.200.2.10/24,198.168.10.10/24</td>
    </tr>
    <tr>
        <td>Gateways:</td>
        <td>,10.200.1.1|10.200.2.1,198.168.10.1</td>
    </tr>
</tbody>
</table>
In this example, container gets attached to bridge, net1 and net2 networks. "bridge" network is already created by Docker and no need to specify subnet/gateway for it. "net1" and "net2" are user defined networks. "net1" have two subnet IP ranges i.e. 10.200.1.10/24(Gateway:10.200.1.1) and 10.200.2.10/24(Gateway:10.200.2.1) while "net2" have single subnet IP range i.e. 198.168.10.10/24(Gateway:198.168.10.1).
</p>
<p>
For more information on docker networking, see <a href="https://docs.docker.com/engine/userguide/networking/">here</a>
</p>
