package org.kendar.ham;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProxiesTest {
    @BeforeAll
    public static void beforeAll() throws HamTestException {
        HamStarter.runHamJar(ProxiesTest.class);
    }
    private HamBasicBuilder hamBuilder = GlobalSettings.builder();
    @Test
    public void testAddingProxy() throws HamException, InterruptedException {
        var proxyId = hamBuilder
                .proxies()
                .addProxy("http://www.microsoft.com","http://www.local.test/api/health","www.local.test:80");

        assertNotNull(proxyId);
        hamBuilder.proxies().removeProxy(proxyId);
    }
}