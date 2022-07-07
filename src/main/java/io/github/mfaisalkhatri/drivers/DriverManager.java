package io.github.mfaisalkhatri.drivers;

import static java.text.MessageFormat.format;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;

import lombok.SneakyThrows;
import org.openqa.selenium.remote.DesiredCapabilities;

public class DriverManager {

    private static final ThreadLocal<AppiumDriver<MobileElement>> DRIVER = new ThreadLocal<> ();

    private static final String LT_USERNAME     = System.getenv ("username");
    private static final String LT_ACCESS_TOKEN = System.getenv ("token");
    private static final String GRID_URL        = "@mobile-hub.lambdatest.com/wd/hub";

    @SneakyThrows
    public DriverManager createRemoteDriver () {
        System.out.println (capabilities ());
        DRIVER.set (new AppiumDriver<> (new URL (format ("https://{0}:{1}{2}", LT_USERNAME, LT_ACCESS_TOKEN, GRID_URL)),
            capabilities ()));

        setupDriverTimeouts ();
        return this;
    }

    @SuppressWarnings ("unchecked")
    public <D extends AppiumDriver<MobileElement>> D getDriver () {
        if (null == DRIVER.get ()) {
            createRemoteDriver ();
        }
        return (D) DRIVER.get ();
    }

    public void quitDriver () {
        if (null != DRIVER.get ()) {
            getDriver ().quit ();
            DRIVER.remove ();
        }
    }

    private void setupDriverTimeouts () {
        getDriver ().manage ()
            .timeouts ()
            .implicitlyWait (30, TimeUnit.SECONDS);
    }

    private DesiredCapabilities capabilities () {
        DesiredCapabilities capabilities = new DesiredCapabilities ();
        try (
            FileInputStream in = new FileInputStream (getClass ().getClassLoader ()
                .getResource ("parallel.config.json")
                .getPath ())) {

            ObjectMapper objectMapper = new ObjectMapper ();
            JsonNode jsonNode = objectMapper.readValue (in, JsonNode.class);

            //            JsonNode deviceInfoNode = jsonNode.findParent ("deviceNumber");
            //
            //
            //            deviceInfoNode.fields ()
            //                .forEachRemaining (entry -> {
            //                    String fieldName = entry.getKey ();
            //                    String fieldValue = entry.getValue ()
            //                        .asText ();
            //                    capabilities.setCapability (fieldName, fieldValue);
            //                });

            Stream<JsonNode> deviceInfoNode = Stream.of (jsonNode.get ("deviceAndBuildCaps"));

            List<JsonNode> sortedDataNodes = deviceInfoNode.sorted (Comparator.comparing (o -> o.get ("deviceNumber")
                    .asText ()))
                .collect (Collectors.toList ());
            ArrayNode arrayNode = objectMapper.createArrayNode ()
                .addAll (sortedDataNodes);
            ObjectNode node = objectMapper.createObjectNode ()
                .set ("deviceAndBuildCaps", arrayNode);
            node.remove ("deviceNumber");

            node.fields ()
                .forEachRemaining (entry -> {
                    String fieldName = entry.getKey ();
                    String fieldValue = entry.getValue ()
                        .asText ();
                    System.out.println (fieldName + " " + fieldValue);
                    capabilities.setCapability (fieldName, fieldValue);
                });

            JsonNode capabilitiesNode = jsonNode.get ("commoncapabilities");
            capabilitiesNode.fields ()
                .forEachRemaining (capsEntry -> {
                    String commonCapsfieldName = capsEntry.getKey ();
                    String commonCapsfieldValue = capsEntry.getValue ()
                        .asText ();
                    System.out.println (commonCapsfieldName + " " + commonCapsfieldValue);
                    capabilities.setCapability (commonCapsfieldName, commonCapsfieldValue);
                });
        } catch (FileNotFoundException e) {
            new Error ("File Not Found", e);
        } catch (IOException e) {
            new Error ("I/O Error", e);
        }
        return capabilities;
    }

}
