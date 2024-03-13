package dev.hachikuu.autogfncloudgg.utils;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailProvider {
    public static final String provider = "https://mailforspam.com";
    public static String mailPrefix = "";

    private final RandomString randomSurfix = new RandomString();
    private static final Logger log = LoggerFactory.getLogger(MailProvider.class);
    private static final CloseableHttpClient httpClient = HttpClientBuilder.create()
        .disableAuthCaching()
        .disableAutomaticRetries()
        .disableCookieManagement()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
        .disableRedirectHandling()
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(3000)
            .setConnectionRequestTimeout(3000)
            .setSocketTimeout(3000)
            .build()    
        ).build();


    public String currentMailUsername = "";

    public final String currentMail() {
        return currentMailUsername.concat("@mailforspam.com");
    }


    public MailProvider() {
        randomSurfix.allowLowerCase(true);
        randomSurfix.allowNumber(true);
    }


    public final String generateEmail() {
        currentMailUsername = mailPrefix.concat(randomSurfix.generate(8));
        log.debug("Email generated: ".concat(currentMail()));
        return currentMail();
    }


    private final String request(String url) throws IOException {
        log.debug("Sending GET request to '%s'".formatted(url));
        HttpResponse resp = httpClient.execute(new HttpGet(url));
        int code = resp.getStatusLine().getStatusCode();
        log.debug("Response code: %d".formatted(code));
        if (code != 200) {
            throw new IOException("Request to '%s' failed with code %d.".formatted(url, resp.getStatusLine().getStatusCode()));
        }
        HttpEntity entity = resp.getEntity();
        return EntityUtils.toString(entity);
    }


    private final String parseMailList(String respData) {
        Document doc = Jsoup.parse(respData);
        Elements links = doc.select("a");
        for (Element link : links) {
            if (link.text().contains("Confirm your CloudGG email here")) {
                String href = link.attr("href");
                log.debug("Parse Mail List: Found activation email: %s".formatted(provider.concat(href)));
                return provider.concat(href);
            }
        }
        log.warn("Parse Mail List: Cannot find the activation email.");
        return null;
    }

    private final String parseActivationMail(String respData) {
        Document doc = Jsoup.parse(respData);
        Elements links = doc.select("a");
        for (Element link : links) {
            String href = link.attr("href");
            if (href.contains("http://email.mail1.cloud.gg/c/")) {
                log.info("Parse Activation Mail: Successfully found the activation link over email.");
                return href;
            }
        }
        log.error("Parse Activation Mail: Cannot find activation link.");
        return null;
    }


    public final String getActivationLink() {
        log.info("Connecting to mailforspam.com to get mail content for ".concat(currentMail()));
        try {
            String activationMailLink = null;
            int retries = 0;
            while (retries < 3) {
                if (retries != 0) {
                    log.info("Attempting to request again after %d seconds. [Press Ctrl+C to skip cooldown]".formatted(5 * retries));
                    try {
                        Thread.sleep(5000 * retries);
                    } catch (InterruptedException e) {
                        log.info("Cooldown skipped.");
                    }
                }
                String respData = request(provider.concat("/mail/").concat(currentMailUsername));
                activationMailLink = parseMailList(respData);
                if (activationMailLink != null) break;
                retries ++;
            }

            if (activationMailLink == null) return null;
            String respData = request(activationMailLink);
            return parseActivationMail(respData);

        } catch (IOException err) {
            log.error("An IOExpection occured when sending request.", err);
            return null;
        }
    }
}
