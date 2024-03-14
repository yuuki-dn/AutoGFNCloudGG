package dev.hachikuu.autogfncloudgg;

import dev.hachikuu.autogfncloudgg.utils.*;

import java.io.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppThread extends Thread {
    private static final RandomString randomName = new RandomString().allowLowerCase(false).allowUpperCase(true);    

    private final Logger logger = LoggerFactory.getLogger(AppThread.class);
    private final MailProvider mail = new MailProvider();
    private DriverController controller;

    private String instanceName;
    private String usernamePrefix;
    private String[] card = new String[5];
    private String password;
    private int quantity;
    private int plan;
    

    public AppThread() {
        logger.warn("No spectific configuration passed. Using default configuration (in config.json) for this thread.");
        InputStream configInputStream = getClass().getResourceAsStream("/config.json");
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(configInputStream));
            StringBuilder configContentBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) configContentBuilder.append(line).append("\n");
            loadConfig(configContentBuilder.toString());
            configInputStream.close();

        } catch (IOException e) {
            logger.error("Cannot read default configuration. Make sure that the file is in the same location with the excutable, and current user have permission to read the file.", e);
            quit();
        }        
    }

    public AppThread(String json) {
        loadConfig(json);
    }

    public void loadConfig(String txt) {
        JSONObject jsonObject = JSON.parseObject(txt);
        instanceName = jsonObject.getString("instance_name");
        usernamePrefix = jsonObject.getJSONObject("prefix").getString("username");
        mail.mailPrefix = jsonObject.getJSONObject("prefix").getString("mail");
        password = jsonObject.getString("default_password");
        quantity = jsonObject.getIntValue("quantity");
        plan = jsonObject.getIntValue("plan");
        jsonObject = jsonObject.getJSONObject("card");
        card[0] = jsonObject.getString("number");
        card[1] = jsonObject.getString("month");
        card[2] = jsonObject.getString("year");
        card[3] = jsonObject.getString("cvv");
        card[4] = jsonObject.getString("card_holder");
        logger.info("[%s] Configuration loaded.".formatted(instanceName));
    }

    public final void init() {
        logger.info("[%s] Session started.".formatted(instanceName));
        if (controller != null) controller.driver.quit();;
        controller = new DriverController();
        controller.driver.get("https://cloud.gg");
        controller.waitClickElement("body > div.main-container > div.main-content > section.top-banner > div > div > div > div > div > a.btn.btn-secondary");
        controller.scrollTo("body > div.main-container > div.main-content > section.top-banner > div > div > div > div > div > a.btn.btn-warning");
        controller.driver.executeScript("arguments[0].removeAttribute('target');", controller.findElement("body > div.main-container > div.main-content > section.top-banner > div > div > div > div > div > a.btn.btn-warning"));
        logger.info("[%s] Please click on \"Sign Up Now\" button in the tab 1 of opened browser!".formatted(instanceName));
        controller.waitForRedirect("https://my.cloud.gg");
        controller.waitForCloudFlare();
        logger.info("[%s] Session initalize successfully.".formatted(instanceName));
    }

    private final void createAccountRequest() {
        mail.generateEmail();
        logger.info("[%s] Creating account with email: %s.".formatted(instanceName, mail.currentMail()));
        controller.driver.get("https://my.cloud.gg/register");
        controller.waitForCloudFlare();
        controller.waitSendKeys("input#email", mail.currentMail());
        controller.waitSendKeys("input#dob", "10/10/2000");
        controller.waitSendKeys("input#postcode", "3550");
        controller.waitClickElement("button#next");
        controller.waitSendKeys("input#username", usernamePrefix.concat(randomName.generate(6)));
        controller.waitSendKeys("input#password", password);
        controller.waitSendKeys("input#password-confirm", password);
        controller.waitClickElement("input#agreeGFN");
        controller.waitClickElement("input#agreeTOS");
        logger.info("[%s] Please solve the captcha. Then click \"Continue\" button.".formatted(instanceName));
        controller.scrollTo("button#next");
        int countdown = 90;
        while (!controller.findElement("div#verifyEmailModal").isDisplayed()) {
            if (countdown < 0) {
                logger.warn("[%s] Waiting for captcha solve timed out.".formatted(instanceName));
                break;
            } else countdown --;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.info("[%s] Wait for captcha solve skipped.".formatted(instanceName));
                break;
            }
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {};
    }

    private void activateAccount() {
        logger.info("[%s] Getting activation link from mail provider.".formatted(instanceName));
        int retries = 0;
        while (mail.getActivationLink() == null) {
            if (retries == 1) {
                logger.error("[%s] Cannot get activation link from mail provider. Stopping instance.".formatted(instanceName));
                quit();
                return;
            }
            logger.warn("[%s] Requested to resend activation mail.".formatted(instanceName));
            controller.waitClickElement("button#verifyEmailModal_resend");
            retries ++;
        }
        logger.info("[%s] Successfully get activation link from mail provider.".formatted(instanceName));
    }

    private final void activatePlan() {
        controller.driver.get(mail.activateLink);
        controller.waitClickElement("body > div.main-container > div.main-content.auth-content > div.container > div > div > a");
        
    }
    
    private final void logOut() {
        controller.driver.get("https://my.cloud.gg/logout");
        controller.waitForCloudFlare();
    }

    public final void run() {
        try {
            for (int i = 0; i < quantity; i ++) {
                logger.info("[%s] Started creating account (%d/%d).".formatted(instanceName, i + 1, quantity));
                createAccountRequest();
                activateAccount();
                activatePlan();
                Thread.sleep(1500000);
                logOut();
            }

        } catch (Exception e) {
            logger.error("[%s] An error occurred while instance running. Stopping instance.".formatted(instanceName), e);
        } finally {
            quit();
        }
    }

    public void quit() {
        controller.driver.quit();
        System.exit(0);
        logger.info("[%s] Thread stopped.".formatted(instanceName));
    }
}
