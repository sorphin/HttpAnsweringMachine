package org.kendar;// Generated by Selenium IDE

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kendar.SeleniumBase.doClick;
import static org.kendar.SeleniumBase.setupSize;

public class GoogleHackSetupTest {

    public static void verify(FirefoxDriver driver) throws InterruptedException {


        driver.get("https://www.google.com");
        Thread.sleep(1000);
        setupSize(driver);
        Thread.sleep(1000);
        WebElement el = driver.findElement(By.xpath("//*[text()='Accetta tutto']"));
        if (el == null) {
            el = driver.findElement(By.xpath("//*[text()='Accept all']"));
        }
        el.click();
        Thread.sleep(1000);
        var text = driver.executeScript("return document.documentElement.outerHTML;").toString();
        assertTrue(text.contains("Bing_logo"));
    }


    public static void setup(FirefoxDriver driver) throws InterruptedException {
        driver.get("https://www.google.com");

        Thread.sleep(1000);
        WebElement el = driver.findElement(By.xpath("//*[text()='Accetta tutto']"));
        if (el == null) {
            el = driver.findElement(By.xpath("//*[text()='Accept all']"));
        }
        el.click();
        Thread.sleep(3000);
        var text = driver.executeScript("return document.documentElement.outerHTML;").toString();
        assertFalse(text.contains("Bing_logo"));
        driver.get("http://www.local.test/index.html");
        Thread.sleep(1000);
        setupSize(driver);
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("grid-rowc-2-0")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("ssl-sites-add")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("address")));
        driver.findElement(By.id("address")).sendKeys("www.google.com");
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("address")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("mod-save")));
//    Thread.sleep(1000);
//    try {
//doClick(()->driver.findElement(By.id("mod-save")));
//    }catch (Exception e){}
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.linkText("Main")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("grid-rowc-0-0")));
        Thread.sleep(1000);

        doClick(() -> driver.findElement(By.linkText("MAPPINGS")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("dns-mappings-add")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("dns")));
        Thread.sleep(1000);
        driver.findElement(By.id("dns")).sendKeys("www.google.com");
        Thread.sleep(1000);

        doClick(() -> driver.findElement(By.id("mod-save")));
//    try {
//doClick(()->driver.findElement(By.id("mod-save")));
//    }catch (Exception ex){}
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.linkText("Main")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.linkText("JsFilter web")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("js-grid-addnew")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("createScriptName")));
        Thread.sleep(1000);
        driver.findElement(By.id("createScriptName")).sendKeys("GoogleHack");
        Thread.sleep(1000);

        doClick(() -> driver.findElement(By.id("createScriptBt")));
//    try{
//
//doClick(()->driver.findElement(By.id("createScriptBt")));
//    }catch (Exception ex){}
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("grid-rowe-0-0")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.linkText("CURRENT")));
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("phase")));
        Thread.sleep(1000);
        {
            WebElement dropdown = driver.findElement(By.id("phase"));
            dropdown.findElement(By.xpath("//option[. = 'POST_CALL']")).click();
        }
        Thread.sleep(1000);
        driver.findElement(By.cssSelector("#phase > option:nth-child(6)")).click();
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("hostAddress")));
        Thread.sleep(1000);
        driver.findElement(By.id("hostAddress")).clear();
        driver.findElement(By.id("hostAddress")).sendKeys("www.google.com");
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("pathAddress")));
        Thread.sleep(1000);
        driver.findElement(By.id("pathAddress")).clear();
        driver.findElement(By.id("pathAddress")).sendKeys("/");
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("source")));
        Thread.sleep(1000);
        driver.findElement(By.id("source")).clear();
        //driver.findElement(By.id("source")).sendKeys("var regex=/\\/images\\/branding\\/[_a-zA-Z0-9]+\\/[_a-zA-Z0-9]+\\/[_a-zA-Z0-9]+\\.png/gm;\\nvar responseText = response.getResponseText()+\"\";\\nvar changedText = responseText.replace(regex,\'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/Bing_logo_%282016%29.svg/320px-Bing_logo_%282016%29.svg.png\');\\nresponse.setResponseText(changedText);\\nreturn false;");
        driver.findElement(By.id("source")).sendKeys("var regex=/\\/images\\/branding\\/[_a-zA-Z0-9]+\\/[_a-zA-Z0-9]+\\/[_a-zA-Z0-9]+\\.png/gm;\n" +
                "var responseText = response.getResponseText()+\"\";\n" +
                "var changedText = responseText.replace(regex,'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/Bing_logo_%282016%29.svg/320px-Bing_logo_%282016%29.svg.png');\n" +
                "response.setResponseText(changedText);\n" +
                "return false;");
        Thread.sleep(1000);
        doClick(() -> driver.findElement(By.id("editfilter-save")));
        Thread.sleep(1000);
    }
}
