package dev.hachikuu.autogfncloudgg;

import dev.hachikuu.autogfncloudgg.utils.*;

import java.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppThread extends Thread {
    public String status = "IDLE";
    private static final String banner = """
        __________________________________________________
             __  __           __    _ __              
            / / / /___ ______/ /_  (_) /____  ____  __
           / /_/ / __ `/ ___/ __ \\/ / //_/ / / / / / /
          / __  / /_/ / /__/ / / / / ,< / /_/ / /_/ /
         /_/ /_/\\__,_/\\___/_/ /_/_/_/|_||__,_/\\__,_/
                                  ____/ /__ _   __
                                 / __  / _ \\ | / /
          [ version 1.0 ]      _/ /_/ /  __/ |/ /
                              (_)__,_/\\___/|___/
        __________________________________________________
        """;

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
        System.out.println(banner);
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
        System.out.println(banner);
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
        if (card[2].length() == 4) card[2] = card[2].substring(2, 4);
        card[3] = jsonObject.getString("cvv");
        card[4] = jsonObject.getString("owner");
        logger.info("[%s] Configuration loaded.".formatted(instanceName));
    }

    public final void init() {
        logger.info("[%s] Session started.".formatted(instanceName));
        if (controller != null) controller.driver.quit();;
        controller = new DriverController();
        controller.driver.get("https://cloud.gg");
        controller.waitClickElement("body > div.main-container > div.main-content > section.top-banner > div > div > div > div > div > a.btn.btn-secondary");
        controller.scrollTo("body > div.main-container > div.main-content > section.top-banner > div > div > div > div > h1");
        controller.driver.executeScript("arguments[0].removeAttribute('target');", controller.findElement("body > div.main-container > div.main-content > section.top-banner > div > div > div > div > div > a.btn.btn-warning"));
        logger.info("[%s] Please wait for CloudFlare check on tab 2, then close tab 2, return to tab 1 and click on \"Sign Up Now\" button!".formatted(instanceName));
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
        logger.info("[%s] Please solve the captcha. Then click \"Continue\" button. (Timeout: 90s)".formatted(instanceName));
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
            Thread.sleep(3000);
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

    private final void activatePlan() throws Exception {
        controller.driver.get(mail.activateLink);
        controller.waitClickElement("body > div.main-container > div.main-content.auth-content > div.container > div > div > a");
        Thread.sleep(3000);
        controller.driver.executeScript("arguments[0].click();", controller.findElement("button[name=\"selectPlan[]\"][data-id=\"%d\"]".formatted(plan)));
        controller.waitClickElement("button#planCheckout");
        controller.waitSendKeys("input#paymentDetails_name", card[4]);
        controller.waitSendKeys("input#paymentDetails_street", randomName.generate(8));
        controller.waitSendKeys("input#paymentDetails_suburb", randomName.generate(6));
        controller.waitSelect("select#paymentDetails_country", "AU");
        controller.waitSelect("select#paymentDetails_state", "TAS");
        controller.waitSendKeys("input#paymentDetails_postcode", "3550");
        controller.waitClickElement("button#paymentDetails_submit");
        Thread.sleep(3000);
        WebElement stripeFrame = controller.findElement("div.__PrivateStripeElement > iframe");
        controller.driver.switchTo().frame(stripeFrame);
        controller.waitSendKeys("input[name=\"cardnumber\"]", card[0]);
        controller.waitSendKeys("input[name=\"exp-date\"]", "%s / %s".formatted(card[1], card[2]));
        controller.waitSendKeys("input[name=\"cvc\"]", card[3]);
        controller.driver.switchTo().defaultContent();
        controller.waitClickElement("button#addCard");
        logger.info("Please complete the 3D-Secure check and Captcha, if appears. (Timeout: 150s)");
        // Vẫn bị detect selenium =)))
        controller.waitClickElement("button#planSubmit", 150);
        Thread.sleep(60000);
    }
    
    private final void logOut() {
        if (controller.driver.getCurrentUrl().contains("my.cloud.gg")) controller.driver.get("https://my.cloud.gg/");;
        controller.driver.manage().deleteCookieNamed("laravel_session");
        logger.debug("[%s] Removed current account cookies.".formatted(instanceName));
    }

    @Override
    public final void run() {
        try {
            logger.info("[%s] Instance started.".formatted(instanceName));
            init();
            for (int i = 0; i < quantity; i ++) {
                logger.info("[%s] Started creating account (%d/%d).".formatted(instanceName, i + 1, quantity));
                createAccountRequest();
                activateAccount();
                activatePlan();
                // Thread.sleep(1500000);
                logOut();
                saveData(true);
            }

        } catch (Exception e) {
            logger.error("[%s] An error occurred while instance running. Stopping instance.".formatted(instanceName), e);
            saveData(false);
        } finally {
            quit();
        }
    }

    private final void saveData(boolean success) {
        logger.debug("[%s] Saving data to output file".formatted(instanceName));
        File dataFile = new File("%s.output.txt".formatted(instanceName));
        try {
            boolean newFile = !dataFile.exists();
            if (newFile) dataFile.createNewFile();
            if (!dataFile.canRead() || !dataFile.canWrite()) throw new IOException("Cannot access output file. Make sure that the current user have permission to read/write the file.");
            String currentDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            String line = "[%s] %s | %s | %s\n".formatted(currentDateTime, mail.currentMail(), password, success ? "SUCCESS" : "ERROR");
            FileWriter writer = new FileWriter(dataFile, true);
            if (newFile) writer.append(banner).append("# [DATETIME]  EMAIL  |  PASSWORD  |  STATUS\n\n");
            writer.append(line);
            writer.close();
        } catch (IOException e) {
            logger.error("An error occured when saving data to output file", e);
        }
    }

    public void quit() {
        controller.driver.quit();
        logger.info("[%s] Instance stopped.".formatted(instanceName));
        Thread.currentThread().interrupt();
    }
}
