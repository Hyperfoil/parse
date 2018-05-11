package perf.parse.factory;

/**
 *
 */


import org.junit.BeforeClass;
import org.junit.Test;
import perf.parse.Exp;
import perf.parse.Parser;
import perf.parse.internal.CheatChars;
import perf.yaup.json.Json;

import static org.junit.Assert.assertEquals;

public class ServerLogFactoryTest {


    private static ServerLogFactory f;

    @BeforeClass
    public static void staticInit(){
        f = new ServerLogFactory();
    }

    @Test
    public void exception_causedBy(){
        //TODO how should the parser handle the [jar:version] that can apppear for rt.jar?
        String test[] = new String[]{
                "2013-10-24 09:21:34,973 WARN  [org.jboss.jca.core.connectionmanager.pool.strategy.OnePool] (JCA PoolFiller) IJ000610: Unable to fill pool: javax.resource.ResourceException: Could not create connection\n",
                "        at org.jboss.jca.adapters.jdbc.xa.XAManagedConnectionFactory.getXAManagedConnection(XAManagedConnectionFactory.java:461)\n",
                "        at org.jboss.jca.adapters.jdbc.xa.XAManagedConnectionFactory.createManagedConnection(XAManagedConnectionFactory.java:398)\n",
                "Caused by: com.mysql.jdbc.exception_causedBy.jdbc4.CommunicationsException: Communications link failure\n",
                "The last packet sent successfully to the server was 0 milliseconds ago. The driver has not received any packets from the server.\n",
                "        at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method) [rt.jar:1.7.0_45]\n",
                "        at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:339) [rt.jar:1.7.0_45]\n",
                "        ... 5 more\n",
                "Caused by: com.mysql.jdbc.exception_causedBy.jdbc4.CommunicationsException: Communications link failure\n",
                "        at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method) [rt.jar:1.7.0_45]\n",
                "        at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:339) [rt.jar:1.7.0_45]\n"
        };
        Parser p = f.newParser();

        for(String line : test){
            p.onLine(line);
        }
        //p.close();
        Json root = p.getBuilder().getRoot();
        assertEquals("Top Stack should have 2 frames",2,root.getJson("stack").size());



    }
}
