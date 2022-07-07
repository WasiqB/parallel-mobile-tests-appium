package io.github.mfaisalkhatri.mobileautomation.tests;

import io.github.mfaisalkhatri.drivers.DriverManager;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;


public class BaseTest {

    protected DriverManager driverManager;

    @BeforeClass (alwaysRun = true)
    public void setupTest () {
        driverManager = new DriverManager ();
        driverManager.createRemoteDriver ();
    }

    @AfterClass (alwaysRun = true)
    public void tearDown () {
        driverManager.quitDriver ();
    }
}
