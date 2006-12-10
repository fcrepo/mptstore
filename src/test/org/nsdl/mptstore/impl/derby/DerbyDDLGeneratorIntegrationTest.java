package org.nsdl.mptstore.impl.derby;

import junit.framework.TestCase;
import junit.swingui.TestRunner;

import org.nsdl.mptstore.TestConfig;
import org.nsdl.mptstore.core.DDLGeneratorIntegrationTest;

public class DerbyDDLGeneratorIntegrationTest 
        extends DDLGeneratorIntegrationTest {

    static {
        TestConfig.init();
    }

    public DerbyDDLGeneratorIntegrationTest(String name) { 
        super(name, DerbyDDLGenerator.class.getName());
    }

    public static void main(String[] args) {
        TestRunner.run(DerbyDDLGeneratorIntegrationTest.class);
    }   

}
