package perf.parse.factory;

/**
 *
 */


import org.junit.BeforeClass;
import org.junit.Test;
import perf.parse.JsonConsumer;
import perf.parse.Parser;
import perf.yaup.json.Json;

import static org.junit.Assert.assertEquals;

public class ServerLogFactoryTest {


    private static ServerLogFactory f;

    @BeforeClass
    public static void staticInit(){
        f = new ServerLogFactory();
    }

    @Test
    public void exception_multiline_message(){
        String test[] = new String[]{
          "2019-03-27 01:35:09,403 ERROR [org.jboss.as.ejb3.invocation] (default task-8) WFLYEJB0034: EJB Invocation failed on component PolicyHolderRegistration for method public void org.spec.jent.insurance.service.PolicyHolderRegistration.register(org.spec.jent.common.insurance.entity.PolicyHolder) throws java.lang.Exception: javax.ejb.EJBException: javax.validation.ConstraintViolationException: 4 constraint violation(s) occurred during method validation.\n",
             "Constructor or Method: public void org.spec.jent.insurance.service.PolicyHolderRegistration.register(org.spec.jent.common.insurance.entity.PolicyHolder) throws java.lang.Exception\n",
             "Argument values: [PolicyHolder{id=null, firstName='', lastName='Skrocki', middleName='Ingrid', email='Joyce.Skrocki_600820@apple.com', password='SI983fGzZdC', gender=MALE, birthDate=Tue Apr 27 00:00:00 UTC 1937, movingViolations=0, claims=0, accidents=0, address=425 N nervous Street\n",
             "297\n",
             ",NJ 20247\n",
             "Joyce.Skrocki_600820@apple.com, vehicles=[]}]\n",
             "Constraint violations: \n",
             " (1) Kind: PROPERTY\n",
             " message: size must be between 1 and 30\n",
             " root bean: Registrations [@Default Event<PolicyHolder>]\n",
             " property path: register.arg0.address.city\n",
             " constraint: @javax.validation.constraints.Size(groups=[], min=1, message={javax.validation.constraints.Size.message}, payload=[], max=30)\n",
             " (2) Kind: PROPERTY\n",
             " message: numeric value out of bounds (<12 digits>.<0 digits> expected)\n",
             " root bean: Registrations [@Default Event<PolicyHolder>]\n",
             " property path: register.arg0.address.phone\n",
             " constraint: @javax.validation.constraints.Digits(message={javax.validation.constraints.Digits.message}, payload=[], groups=[], fraction=0, integer=12)\n",
             " (3) Kind: PROPERTY\n",
             " message: size must be between 8 and 12\n",
             " root bean: Registrations [@Default Event<PolicyHolder>]\n",
             " property path: register.arg0.address.phone\n",
             " constraint: @javax.validation.constraints.Size(groups=[], min=8, message={javax.validation.constraints.Size.message}, payload=[], max=12)\n",
             " (4) Kind: PROPERTY\n",
             " message: size must be between 1 and 25\n",
             " root bean: Registrations [@Default Event<PolicyHolder>]\n",
             " property path: register.arg0.firstName\n",
             " constraint: @javax.validation.constraints.Size(groups=[], min=1, message={javax.validation.constraints.Size.message}, payload=[], max=25)\n",
             "\tat org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:246)\n",
             "\tat org.jboss.as.ejb3.tx.CMTTxInterceptor.required(CMTTxInterceptor.java:362)\n",
             "\tat org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:144)\n",
             "\tat org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:422)\n"
        };


        Parser p = f.newParser();
        JsonConsumer.List consumer = new JsonConsumer.List();
        p.add(consumer);
        for(String line : test){
            p.onLine(line);
        }
        p.addUnparsedConsumer(((remainder, original, lineNumber) -> {
            System.out.println("WTF::"+lineNumber+"::"+remainder);
        }));
        //p.close();
        Json root = p.getBuilder().getRoot();

        System.out.println(root.toString(2));
        System.out.println(consumer.getJson().size());
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
        System.out.println(root.toString(2));
        assertEquals("Top Stack should have 2 frames",2,root.getJson("stack").size());



    }
}
