/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.management;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

public class ManagedCamelContextAutoStartupTest extends ManagementTestSupport {

    @Test
    public void testManagedCamelContext() throws Exception {
        // JMX tests dont work well on AIX CI servers (hangs them)
        if (isPlatform("aix")) {
            return;
        }

        MBeanServer mbeanServer = getMBeanServer();

        ObjectName on = ObjectName.getInstance("org.apache.camel:context=camel-1,type=context,name=\"camel-1\"");
        ObjectName onRoute = ObjectName.getInstance("org.apache.camel:context=camel-1,type=routes,name=\"foo\"");

        assertTrue("Should be registered", mbeanServer.isRegistered(on));
        String name = (String) mbeanServer.getAttribute(on, "CamelId");
        assertEquals("camel-1", name);

        assertTrue("Should be registered", mbeanServer.isRegistered(onRoute));
        String state = (String) mbeanServer.getAttribute(onRoute, "State");
        assertEquals("Stopped", state);

        // start the route
        mbeanServer.invoke(onRoute, "start", null, null);

        state = (String) mbeanServer.getAttribute(onRoute, "State");
        assertEquals("Started", state);

        Object reply = mbeanServer.invoke(on, "requestBody", new Object[]{"direct:foo", "Hello World"}, new String[]{"java.lang.String", "java.lang.Object"});
        assertEquals("Bye World", reply);

        // stop Camel
        mbeanServer.invoke(on, "stop", null, null);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                context.setAutoStartup(false);

                from("direct:foo").routeId("foo").transform(constant("Bye World"));
            }
        };
    }

}
