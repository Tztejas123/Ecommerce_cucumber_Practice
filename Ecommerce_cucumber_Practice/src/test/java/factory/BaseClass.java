package factory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class BaseClass {

    protected static WebDriver driver;
    protected static Properties p;
    protected static Logger logger;
    
    // Initialize browser with enhanced capabilities
    public static WebDriver initilizeBrowser() throws Exception {
        p = getProperties();
        String executionEnv = p.getProperty("execution_env");
        String browser = p.getProperty("browser").toLowerCase();
        String os = p.getProperty("os").toLowerCase();
        String headless = p.getProperty("headless", "false");
        
        logger = getLogger();
        
        try {
            if (executionEnv.equalsIgnoreCase("remote")) {
                driver = initRemoteDriver(browser, os);
            } else if (executionEnv.equalsIgnoreCase("local")) {
                driver = initLocalDriver(browser, headless);
            }
            
            configureDriverSettings();
            checkForCaptcha();
            
        } catch (Exception e) {
            logger.error("Browser initialization failed: " + e.getMessage());
            throw e;
        }
        
        return driver;
    }
    
    // Remote driver initialization
    private static WebDriver initRemoteDriver(String browser, String os) throws Exception {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        
        // Set OS capabilities
        switch (os) {
            case "windows":
                capabilities.setPlatform(Platform.WINDOWS);
                break;
            case "mac":
                capabilities.setPlatform(Platform.MAC);
                break;
            case "linux":
                capabilities.setPlatform(Platform.LINUX);
                break;
            default:
                throw new IllegalArgumentException("Unsupported OS: " + os);
        }
        
        // Set browser capabilities
        switch (browser) {
            case "chrome":
                capabilities.setBrowserName("chrome");
                capabilities.setCapability(ChromeOptions.CAPABILITY, getChromeOptions());
                break;
            case "edge":
                capabilities.setBrowserName("MicrosoftEdge");
                capabilities.setCapability(EdgeOptions.CAPABILITY, getEdgeOptions());
                break;
            case "firefox":
                capabilities.setBrowserName("firefox");
                capabilities.setCapability(FirefoxOptions.FIREFOX_OPTIONS, getFirefoxOptions());
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browser);
        }
        
        return new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capabilities);
    }
    
    // Local driver initialization
    private static WebDriver initLocalDriver(String browser, String headless) {
        switch (browser) {
            case "chrome":
                return new ChromeDriver(getChromeOptions(headless.equalsIgnoreCase("true")));
            case "edge":
                return new EdgeDriver(getEdgeOptions(headless.equalsIgnoreCase("true")));
            case "firefox":
                return new FirefoxDriver(getFirefoxOptions(headless.equalsIgnoreCase("true")));
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browser);
        }
    }
    
    // Browser options configurations
    private static ChromeOptions getChromeOptions() {
        return getChromeOptions(false);
    }
    
    private static ChromeOptions getChromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        
        return options;
    }
    
    private static EdgeOptions getEdgeOptions() {
        return getEdgeOptions(false);
    }
    
    private static EdgeOptions getEdgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }
        
        return options;
    }
    
    private static FirefoxOptions getFirefoxOptions() {
        return getFirefoxOptions(false);
    }
    
    private static FirefoxOptions getFirefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        
        if (headless) {
            options.addArguments("--headless");
            options.addArguments("--window-size=1920,1080");
        }
        
        return options;
    }
    
    // Configure common driver settings
    private static void configureDriverSettings() {
        driver.manage().deleteAllCookies();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(15));
        driver.manage().window().maximize();
    }
    
    // CAPTCHA handling methods
    public static void checkForCaptcha() {
        try {
            if (driver.getTitle().contains("Verifying") || 
                driver.getPageSource().contains("challenge-platform")) {
                handleCaptchaManually();
            }
        } catch (Exception e) {
            logger.warn("Error checking for CAPTCHA: " + e.getMessage());
        }
    }
    
    public static void handleCaptchaManually() {
        logger.info("CAPTCHA detected - waiting for manual resolution");
        takeScreenshot("before-captcha");
        
        System.out.println("\n=== MANUAL CAPTCHA REQUIRED ===");
        System.out.println("Browser paused at: " + driver.getCurrentUrl());
        System.out.println("1. Solve the CAPTCHA in the browser window");
        System.out.println("2. Press ENTER in console when complete");
        
        try {
            new Scanner(System.in).nextLine();
            takeScreenshot("after-captcha");
            logger.info("CAPTCHA resolved manually, resuming automation");
        } catch (Exception e) {
            logger.error("Error waiting for manual CAPTCHA resolution", e);
        }
    }
    
    // Utility methods
    public static void takeScreenshot(String name) {
        try {
            File src = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(src, new File("screenshots/" + name + ".png"));
            logger.info("Screenshot saved: " + name);
        } catch (Exception e) {
            logger.error("Failed to take screenshot: " + e.getMessage());
        }
    }
    
    public static void humanClick(WebElement element) {
        try {
            Thread.sleep(300 + (long)(Math.random() * 500));
            element.click();
            Thread.sleep(200 + (long)(Math.random() * 300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void humanType(WebElement element, String text) {
        try {
            element.click();
            for (char c : text.toCharArray()) {
                Thread.sleep(100 + (long)(Math.random() * 200));
                element.sendKeys(String.valueOf(c));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Existing methods with improvements
    public static WebDriver getDriver() {
        return driver;
    }

    public static Properties getProperties() throws IOException {
        FileReader file = new FileReader(System.getProperty("user.dir") + 
            "/src/test/resources/config.properties");
        p = new Properties();
        p.load(file);
        return p;
    }
    
    public static Logger getLogger() {
        return LogManager.getLogger();
    }
    
    public static String randomeString() {
        return RandomStringUtils.randomAlphabetic(5);
    }
    
    public static String randomeNumber() {
        return RandomStringUtils.randomNumeric(10);
    }
    
    public static String randomAlphaNumeric() {
        return RandomStringUtils.randomAlphabetic(5) + 
               RandomStringUtils.randomNumeric(10);
    }
    
    // Cleanup method
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
            logger.info("Browser session ended");
        }
    }
}