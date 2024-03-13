package dev.hachikuu.autogfncloudgg;

import dev.hachikuu.autogfncloudgg.utils.*;

import com.frogking.chromedriver.ChromeDriverBuilder;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import com.alibaba.fastjson.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppThread {
    private static final ChromeOptions chromeOptions = new ChromeOptions()
        .addArguments("--incognito")
        .addArguments("--window-size=800,600");
    
    private static final RandomString randomName = new RandomString().allowLowerCase(true);

    private final Logger logger = LoggerFactory.getLogger(AppThread.class);
    private ChromeDriver driver;
    private MailProvider mail = new MailProvider();

    private String threadName;
    private String usernamePrefix;
    private int quantity;

    public AppThread() {
        logger.warn("No spectific configuration passed. Using default configuration (in config.jsonc) for this thread.");
        File config = new File("config.jsonc");
        if (!config.canRead()) {
            logger.error("Cannot read default configuration. Make sure that the file is in the same location with the excutable, and current user have permission to read the file.");
            quit();
        }
    }

    public AppThread(String json) {
        loadConfig(json);
    }

    public void loadConfig(String txt) {
        
    }

    public void quit() {
        driver.quit();
    }
}
