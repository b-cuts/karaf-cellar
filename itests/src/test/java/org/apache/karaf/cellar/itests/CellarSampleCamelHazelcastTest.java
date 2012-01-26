/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.itests;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CellarSampleCamelHazelcastTest extends CellarTestSupport {

    @Test
    public void testCamelSampleApp() throws InterruptedException {
        installCellar();
        createCellarChild("child1");
        createCellarChild("child2");
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        System.err.println(executeCommand("features:addurl mvn:org.apache.karaf.cellar.samples/camel-hazelcast-app/2.2.3-SNAPSHOT/xml/features"));

        System.err.println(executeCommand("admin:list"));

        System.err.println(executeCommand("cluster:nodes-list"));
        Node localNode = clusterManager.getNode();
        Set<Node> nodes = clusterManager.listNodes();
        assertTrue("There should be at least 3 cellar nodes running", nodes.size() >= 3);

        Thread.sleep(DEFAULT_TIMEOUT);

        String node1 = getNodeIdOfChild("child1");
        String node2 = getNodeIdOfChild("child2");

        System.err.println("Child1:" + node1);
        System.err.println("Child2:" + node2);

        System.err.println(executeCommand("cluster:group-set producer-grp " + localNode.getId()));
        System.err.println(executeCommand("cluster:group-set consumer-grp " + node1));
        System.err.println(executeCommand("cluster:group-set consumer-grp " + node2));
        System.err.println(executeCommand("cluster:group-list"));

        System.err.println(executeCommand("cluster:features-install consumer-grp cellar-sample-camel-consumer"));
        System.err.println(executeCommand("cluster:features-install producer-grp cellar-sample-camel-producer"));
        Thread.sleep(10000);
        System.err.println(executeCommand("features:list"));
        System.err.println(executeCommand("osgi:list"));

        System.err.println(executeCommand("cluster:group-list"));
        System.err.println(executeCommand("admin:connect child2 osgi:list -t 0"));

        Thread.sleep(10000);
        String output1 = executeCommand("admin:connect child1 log:display | grep \"Hallo Cellar\"");
        System.err.println(output1);
        String output2 = executeCommand("admin:connect child2 log:display | grep \"Hallo Cellar\"");
        System.err.println(output2);
        assertTrue("Expected at least lines", countOutputEntires(output1) >= 2);
        assertTrue("Expected at least lines", countOutputEntires(output2) >= 2);
    }

    public int countOutputEntires(String output) {
        String[] lines = output.split("\n");
        return lines.length;
    }

    @After
    public void tearDown() {
        try {
            destroyCellarChild("child1");
            destroyCellarChild("child2");
            unInstallCellar();
        } catch (Exception e) {
            // ignore
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                cellarDistributionConfiguration(), keepRuntimeFolder(), logLevel(LogLevelOption.LogLevel.ERROR)
        };
    }

}