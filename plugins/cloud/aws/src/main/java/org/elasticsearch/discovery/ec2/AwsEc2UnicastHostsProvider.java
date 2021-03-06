/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.discovery.ec2;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Sets;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.PortsRange;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastHostsProvider;

import java.util.List;
import java.util.Set;

/**
 * @author kimchy (shay.banon)
 */
public class AwsEc2UnicastHostsProvider extends AbstractComponent implements UnicastHostsProvider {

    private static enum HostType {
        PRIVATE_IP,
        PUBLIC_IP,
        PRIVATE_DNS,
        PUBLIC_DNS
    }

    private final AmazonEC2 client;

    private final String ports;

    private final ImmutableSet<String> groups;

    private final ImmutableSet<String> availabilityZones;

    private final HostType hostType;

    @Inject public AwsEc2UnicastHostsProvider(Settings settings, AmazonEC2 client) {
        super(settings);
        this.client = client;

        this.hostType = HostType.valueOf(componentSettings.get("host_type", "private_ip").toUpperCase());
        this.ports = componentSettings.get("ports", "9300-9302");

        Set<String> groups = Sets.newHashSet(componentSettings.getAsArray("groups"));
        if (componentSettings.get("groups") != null) {
            groups.addAll(Strings.commaDelimitedListToSet(componentSettings.get("groups")));
        }
        this.groups = ImmutableSet.copyOf(groups);

        Set<String> availabilityZones = Sets.newHashSet(componentSettings.getAsArray("availability_zones"));
        if (componentSettings.get("availability_zones") != null) {
            availabilityZones.addAll(Strings.commaDelimitedListToSet(componentSettings.get("availability_zones")));
        }
        this.availabilityZones = ImmutableSet.copyOf(availabilityZones);
    }

    @Override public List<DiscoveryNode> buildDynamicNodes() {
        List<DiscoveryNode> discoNodes = Lists.newArrayList();

        DescribeInstancesResult descInstances = client.describeInstances(new DescribeInstancesRequest());

        logger.trace("building dynamic unicast discovery nodes...");
        for (Reservation reservation : descInstances.getReservations()) {
            if (!groups.isEmpty()) {
                // lets see if we can filter based on groups
                boolean filter = false;
                for (String group : reservation.getGroupNames()) {
                    if (!groups.contains(group)) {
                        logger.trace("filtering out reservation {} based on group {}, not part of {}", reservation.getReservationId(), group, groups);
                        filter = true;
                        break;
                    }
                }
                if (filter) {
                    // if we are filtering, continue to the next reservation
                    continue;
                }
            }

            for (Instance instance : reservation.getInstances()) {
                if (!availabilityZones.isEmpty()) {
                    if (!availabilityZones.contains(instance.getPlacement().getAvailabilityZone())) {
                        logger.trace("filtering out instance {} based on availability_zone {}, not part of {}", instance.getInstanceId(), instance.getPlacement().getAvailabilityZone(), availabilityZones);
                        continue;
                    }
                }
                InstanceState state = instance.getState();
                if (state.getName().equalsIgnoreCase("pending") || state.getName().equalsIgnoreCase("running")) {
                    String address = null;
                    switch (hostType) {
                        case PRIVATE_DNS:
                            address = instance.getPrivateDnsName();
                            break;
                        case PRIVATE_IP:
                            address = instance.getPrivateIpAddress();
                            break;
                        case PUBLIC_DNS:
                            address = instance.getPublicDnsName();
                            break;
                        case PUBLIC_IP:
                            address = instance.getPublicDnsName();
                            break;
                    }
                    for (int port : new PortsRange(ports).ports()) {
                        logger.trace("adding {}, address {}", instance.getInstanceId(), address);
                        discoNodes.add(new DiscoveryNode("#cloud-" + instance.getInstanceId() + "-" + port, new InetSocketTransportAddress(address, port)));
                    }
                }
            }
        }

        logger.debug("using dynamic discovery nodes {}", discoNodes);

        return discoNodes;
    }
}
