package org.kendar.cucumber;

import io.cucumber.java.en.And;
import org.kendar.globaltest.Sleeper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.kendar.cucumber.Utils.*;

public class FakeDbTasks {
    @And("^Prepare db null test '(.+)'$")
    public void prepareNullGatewayTest(String gatewayTestName) throws Exception {
        var gatewayTestId = (String) Utils.getCache("recording_" + gatewayTestName);
        var driver = (WebDriver) Utils.getCache("driver");
        driver.get("http://www.local.test/plugins/recording/script.html?id=" + gatewayTestId);
        Sleeper.sleep(1000);
        takeSnapShot();
        scrollFind(() -> driver.findElement(By.id("scriptstab_0"))).click();
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("grid-visibility"))).click();
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("show-key-requestHost")));
        checkCheckBox(() -> driver.findElement(By.id("show-key-requestHost")));
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("show-key-stimulatorTest")));
        checkCheckBox(() -> driver.findElement(By.id("show-key-stimulatorTest")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("mod-save")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("grid-s-c-4")));
        Sleeper.sleep(1000);
        sendKeys(By.id("grid-s-c-4"),"www");
        doClick(() -> driver.findElement(By.id("recording-list-checkall")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-delsel")));
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("grid-s-c-1")), 100).click();
        sendKeys(By.id("grid-s-c-5"),"/int/gat");
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-checkall")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-delsel")));
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("grid-s-c-5")), 100).click();
        Sleeper.sleep(1000);
        sendKeys(By.id("grid-s-c-5"),"/int/be");
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-checkall")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-seltostim")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-saverglobscriptdata")));
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("grid-s-c-5")), 100).click();
        Sleeper.sleep(1000);
    }

    @And("^Prepare db only test '(.+)'$")
    public void prepareDbOnlyTest(String gatewayTestName) throws Exception {
        var gatewayTestId = (String) Utils.getCache("recording_" + gatewayTestName);
        var driver = (WebDriver) Utils.getCache("driver");
        driver.get("http://www.local.test/plugins/recording/script.html?id=" + gatewayTestId);
        Sleeper.sleep(1000);
        takeSnapShot();
        scrollFind(() -> driver.findElement(By.id("scriptstab_0"))).click();
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("grid-visibility"))).click();
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("show-key-requestHost")));
        checkCheckBox(() -> driver.findElement(By.id("show-key-requestHost")));
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("show-key-stimulatorTest")));
        checkCheckBox(() -> driver.findElement(By.id("show-key-stimulatorTest")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("mod-save")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("grid-s-c-4")));
        Sleeper.sleep(1000);
        sendKeys(By.id("grid-s-c-4"),"www");
        doClick(() -> driver.findElement(By.id("recording-list-checkall")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-delsel")));
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("grid-s-c-1")), 100).click();
        sendKeys(By.id("grid-s-c-5"),"/int/gat");
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-checkall")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-delsel")));
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("grid-s-c-5")), 100).click();
        Sleeper.sleep(1000);
        sendKeys(By.id("grid-s-c-5"),"/int/be");
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-checkall")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-list-delsel")));
        Sleeper.sleep(1000);
        doClick(() -> driver.findElement(By.id("recording-saverglobscriptdata")));
        Sleeper.sleep(1000);
        scrollFind(() -> driver.findElement(By.id("grid-s-c-5")), 100).click();
        Sleeper.sleep(1000);
    }
}
