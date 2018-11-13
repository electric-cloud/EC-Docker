package dsl.docker

def names = args.params,
        config = names.config,
        network = names.network,
        subnetList = names.subnetList,
        enableIpV6 = names.enableIpV6,
        gateways = names.gateways,
        labels = names.labels,
        mtu = names.mtu,
        resource = names.resource


// Create plugin configuration
def pluginProjectName = getPlugin(pluginName: 'EC-Docker').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Create Ingress',
        resourceName: resource,
        actualParameter: [
                enableIpv6: enableIpV6,
                gatewayList: gateways,
                labels: labels,
                mtu: mtu,
                pluginConfig: config,
                networkName: network,
                subnetList: subnetList
        ]
)
