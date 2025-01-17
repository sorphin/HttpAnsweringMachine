package org.kendar.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.managers.ChromeDriverManager;
import io.github.bonigarcia.wdm.versions.VersionDetector;
import org.kendar.globaltest.Sleeper;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.stream.Collectors;

import static org.kendar.cucumber.Utils.takeMessageSnapshot;

public class SeleniumTasks {
    private ChromeDriver driver;
    private JavascriptExecutor js;

    private static void setupSize(ChromeDriver driver) {
        driver.manage().window().setSize(new Dimension(1366, 900));
    }

    private static String retrieveBrowserVersion(){
        ChromeDriverManager.getInstance().setup();
        var versionDetector = new VersionDetector(ChromeDriverManager.getInstance().config(),null);
        var version = Integer.parseInt(versionDetector.getBrowserVersionFromTheShell("chrome").get());
        var available = ChromeDriverManager.getInstance().getDriverVersions().stream().
                map(v->Integer.parseInt(v.split("\\.")[0])).sorted().distinct().collect(Collectors.toList());
        var matching =available.get(available.size()-1);
        if(available.stream().anyMatch(v->v==(version))){
            matching=version;
        }
        return matching.toString();
    }
    @Given("^Selenium initialized$")
    public void seleniumInitialized() throws Exception {

        //ChromeDriverManager.getInstance().setup();
        //var chromeExecutable = SeleniumBase.findchrome();

        Proxy proxy = new Proxy();
        proxy.setSocksProxy("127.0.0.1:1080");
        proxy.setSocksVersion(5);
        proxy.setProxyType(Proxy.ProxyType.MANUAL);
        DesiredCapabilities desired = new DesiredCapabilities();
        var options = new ChromeOptions();
        options.setBrowserVersion(retrieveBrowserVersion());
        options.setProxy(proxy);
        options.setAcceptInsecureCerts(true);
        options.addArguments("--remote-allow-origins=*");
        //options.addArguments("--disable-dev-shm-usage");
        //options.addArguments("disable-infobars"); // disabling infobars
        //options.addArguments("--disable-extensions"); // disabling extensions
        options.addArguments("--disable-gpu"); // applicable to windows os only
        options.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
        options.addArguments("--no-sandbox"); // Bypass OS security model

        //options.addArguments("--user-data-dir=/tmp/sticazzi2");
        driver = (ChromeDriver) WebDriverManager
                .chromedriver()
                .capabilities(options)
                .clearDriverCache()
                .clearResolutionCache()
                .create();
        driver.manage().deleteAllCookies();


        js = (JavascriptExecutor) driver;
        Utils.setCache("driver", driver);
        Utils.setCache("js", js);
        Sleeper.sleep(1000);
        setupSize(driver);
        Sleeper.sleep(1000);
    }

    @When("^Resetting driver$")
    public void resettingDriver() throws Exception {
        driver.quit();
        Utils.setCache("driver", null);
        Utils.setCache("js", null);
        Sleeper.sleep(1000);
        seleniumInitialized();
    }

    @When("^Quit selenium$")
    public void quitSelenium() throws Exception {
        driver.quit();
        Utils.setCache("driver", null);
        Utils.setCache("js", null);
        Sleeper.sleep(1000);
        takeMessageSnapshot("End of test");
    }
}
