package org.kendar;// Generated by Selenium IDE

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kendar.SeleniumBase.doClick;

public class GoogleHackSetupTest {

  public static void verify(FirefoxDriver driver) throws InterruptedException {
    driver.get("https://www.google.com");
    Thread.sleep(1000);
    WebElement el = driver.findElement(By.xpath("//*[text()='Accetta tutto']"));
    if(el==null){
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
    if(el==null){
      el = driver.findElement(By.xpath("//*[text()='Accept all']"));
    }
    el.click();
    Thread.sleep(3000);
    var text = driver.executeScript("return document.documentElement.outerHTML;").toString();
    assertFalse(text.contains("Bing_logo"));
    driver.get("http://www.local.test/index.html");
    Thread.sleep(1000);
    driver.manage().window().setSize(new Dimension(1024, 768));
    Thread.sleep(1000);
    driver.findElement(By.id("grid-rowc-2-0")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("ssl-sites-add")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("address")).click();
    driver.findElement(By.id("address")).sendKeys("www.google.com");
    Thread.sleep(1000);
    driver.findElement(By.id("address")).click();
    Thread.sleep(1000);
    doClick(()->driver.findElement(By.id("mod-save")));
//    Thread.sleep(1000);
//    try {
//      driver.findElement(By.id("mod-save")).click();
//    }catch (Exception e){}
    Thread.sleep(1000);
    driver.findElement(By.linkText("Main")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("grid-rowc-0-0")).click();
    Thread.sleep(1000);

    driver.findElement(By.linkText("MAPPINGS")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("dns-mappings-add")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("dns")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("dns")).sendKeys("www.google.com");
    Thread.sleep(1000);

    doClick(()->driver.findElement(By.id("mod-save")));
//    try {
//      driver.findElement(By.id("mod-save")).click();
//    }catch (Exception ex){}
    Thread.sleep(1000);
    doClick(()->driver.findElement(By.linkText("Main")));
    Thread.sleep(1000);
    doClick(()->driver.findElement(By.linkText("JsFilter web")));
    Thread.sleep(1000);
    driver.findElement(By.id("js-grid-addnew")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("createScriptName")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("createScriptName")).sendKeys("GoogleHack");
    Thread.sleep(1000);

    doClick(()->driver.findElement(By.id("createScriptBt")));
//    try{
//
//      driver.findElement(By.id("createScriptBt")).click();
//    }catch (Exception ex){}
    Thread.sleep(1000);
    driver.findElement(By.id("grid-rowe-0-0")).click();
    Thread.sleep(1000);
    driver.findElement(By.linkText("CURRENT")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("phase")).click();
    Thread.sleep(1000);
    {
      WebElement dropdown = driver.findElement(By.id("phase"));
      dropdown.findElement(By.xpath("//option[. = 'POST_CALL']")).click();
    }
    Thread.sleep(1000);
    driver.findElement(By.cssSelector("#phase > option:nth-child(6)")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("hostAddress")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("hostAddress")).clear();
    driver.findElement(By.id("hostAddress")).sendKeys("www.google.com");
    Thread.sleep(1000);
    driver.findElement(By.id("pathAddress")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("pathAddress")).clear();
    driver.findElement(By.id("pathAddress")).sendKeys("/");
    Thread.sleep(1000);
    driver.findElement(By.id("source")).click();
    Thread.sleep(1000);
    driver.findElement(By.id("source")).clear();
    //driver.findElement(By.id("source")).sendKeys("var regex=/\\/images\\/branding\\/[_a-zA-Z0-9]+\\/[_a-zA-Z0-9]+\\/[_a-zA-Z0-9]+\\.png/gm;\\nvar responseText = response.getResponseText()+\"\";\\nvar changedText = responseText.replace(regex,\'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/Bing_logo_%282016%29.svg/320px-Bing_logo_%282016%29.svg.png\');\\nresponse.setResponseText(changedText);\\nreturn false;");
    driver.findElement(By.id("source")).sendKeys("var regex=/\\/images\\/branding\\/[_a-zA-Z0-9]+\\/[_a-zA-Z0-9]+\\/[_a-zA-Z0-9]+\\.png/gm;\n" +
            "var responseText = response.getResponseText()+\"\";\n" +
            "var changedText = responseText.replace(regex,'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/Bing_logo_%282016%29.svg/320px-Bing_logo_%282016%29.svg.png');\n" +
            "response.setResponseText(changedText);\n" +
            "return false;");
    Thread.sleep(1000);
    doClick(()->driver.findElement(By.id("editfilter-save")));
    Thread.sleep(1000);
  }
}