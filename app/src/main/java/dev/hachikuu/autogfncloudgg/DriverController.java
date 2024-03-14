package dev.hachikuu.autogfncloudgg;

import com.frogking.chromedriver.ChromeDriverBuilder;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.*;

public class DriverController {
    public static final class ControlException extends RuntimeException {
        public ControlException(String msg) {
            super(msg);
        }

        public ControlException(String msg, Throwable t) {
            super(msg, t);
        }
    };


    private static final ChromeOptions chromeOptions = new ChromeOptions()
        .addArguments("--incognito")
        .addArguments("--window-size=800,600")
        .addArguments("--disable-popup-blocking");
    
    private final Logger logger = LoggerFactory.getLogger(DriverController.class);
    private WebDriverWait driverWait; 

    public ChromeDriver driver;

    public DriverController() {
        driver = new ChromeDriverBuilder().build(chromeOptions, "");
        driverWait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public final void waitForCloudFlare() throws ControlException {
        logger.info("Driver is waiting for CloudFlare check.");
        int countdown = 120;
        while (!driver.getTitle().contains("CloudGG")) {
            if (countdown < 0) {
                logger.warn("Waiting timed out.");
                break;
            } else countdown --;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.info("Wait skipped.");
                break;
            }
        }
        if (!driver.getTitle().contains("CloudGG")) {
            throw new ControlException("Cannot automatic bypass Cloudflare check.");
        }
        logger.info("CloudFlare check passed.");
    }

    public final void waitForRedirect(String targetUrl) {
        logger.info("Driver is waiting for redirect.");
        int countdown = 120;
        while (!driver.getCurrentUrl().contains(targetUrl)) {
            if (countdown < 0) {
                logger.warn("Waiting timed out.");
                break;
            } else countdown --;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.info("Wait skipped.");
                break;
            }
        }
        if (!driver.getCurrentUrl().contains(targetUrl)) {
            throw new ControlException("Cannot reach %s.".formatted(targetUrl));
        }
    }

    public final WebElement findElement(String cssSelector) {
        return driver.findElement(By.cssSelector(cssSelector));
    }

    public final WebElement waitClickElement(String cssSelector) {
        WebElement element = driverWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
        driver.executeScript("arguments[0].click();", element);
        return element;
    }

    public final WebElement waitSendKeys(String cssSelector, String keys) {
        WebElement element = driverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(cssSelector)));
        element.sendKeys(keys);
        return element;
    }

    public final WebElement scrollTo(String cssSelector) {
        WebElement element = driverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(cssSelector)));
        driver.executeScript("arguments[0].scrollIntoView(true);", element);
        return element;
    }
}
