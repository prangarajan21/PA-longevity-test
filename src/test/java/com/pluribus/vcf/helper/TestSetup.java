package com.pluribus.vcf.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Optional;
import static org.testng.Assert.assertTrue;
import com.browserstack.local.Local;
import com.jcabi.ssh.Shell;
import com.jcabi.ssh.SSHByPassword;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
/**
 *
 * @author Haritha
 */
public class TestSetup {
   private RemoteWebDriver driver;
   private ResourceBundle bundle;
   Local bsLocal = new Local();
   
   @Parameters({"vcfIp","clean"})
   @BeforeSuite(alwaysRun = true)
   public void cleanLogs(String vcfIp,@Optional("1")String clean) throws IOException,InterruptedException {
	   if(Integer.parseInt(clean) == 1) {
		Shell sh1 = new Shell.Verbose(
	            new SSHByPassword(
	                vcfIp,
			22,
	                "vcf",
	                "changeme"
	            )
	        );
		String out1;
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/flowfilters.json");	
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/pcap-agent.properties");	
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/pcap-engine.json");
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/vcf-center.properties");
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/pcap_agents.properties");
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/pcap-file.json");
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/switch-details.json");
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/vcf-license.json");
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/vcf-user.properties");
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/vcf-maestro.properties");	
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/es_nodes.properties");	
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/es_node.json");	
		out1 = new Shell.Plain(sh1).exec("rm  /home/vcf/srv/vcf/config/vcf-collector.json");
		out1 = new Shell.Plain(sh1).exec("/home/vcf/srv/vcf/bin/stop-vcfc.sh");
		Thread.sleep(10000);
		out1 = new Shell.Plain(sh1).exec("/home/vcf/srv/vcf/bin/start-vcfc.sh");
		Thread.sleep(30000);
	}
   }
   @Parameters({"vcfIp","browser","bsUserId","bsKey"}) 
   @BeforeClass(alwaysRun = true)
	public void startDriver(String vcfIp,String browser,@Optional("pratikdam1")String bsUserId, @Optional("uZCXEzKXwgzgzMr3G7R6") String bsKey) throws Exception {
		HashMap<String,String> bsLocalArgs = new HashMap<String,String>();
		String sessionId = null;
		String command = null;
		
		bsLocalArgs.put("key",bsKey); //BrowserStack Key
		bsLocalArgs.put("force", "true"); //Kill previously open BrowserStack local sessions
		bsLocal.start(bsLocalArgs);
	    DesiredCapabilities caps = new DesiredCapabilities();
		caps.setCapability("browser",browser);
		caps.setCapability("build", "VCFC SmokeTest Cases");
		caps.setCapability("acceptSslCerts","true");
		caps.setCapability("browserstack.local", "true");
		caps.setCapability("browserstack.debug","true");
		caps.setCapability("browserstack.idleTimeout","150");
		caps.setCapability("platform","ANY");
		driver = new RemoteWebDriver(
			      new URL("https://"+bsUserId+":"+bsKey+"@hub-cloud.browserstack.com/wd/hub"),
			      caps
			    );
		driver.manage().timeouts().implicitlyWait(100, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		driver.manage().deleteAllCookies();
        // Get a handle to the driver. This will throw an exception if a matching driver cannot be located
	    driver.get("https://"+ vcfIp);
	    com.jcabi.log.Logger.info("Logfile",getBSLogs(bsUserId,bsKey));
   }
   
   public String getBSLogs(String bsUserId,String bsKey) {
	    String sessId = driver.getSessionId().toString();
	    System.out.println("sessionId:"+sessId);
	    //URL url = new URL ("https://browserstack.com/automate/sessions/"+sessId+".json");
	    String url = "https://browserstack.com/automate/sessions/"+sessId+".json";
	    System.out.println("url:"+url.toString());
	    String authUser = bsUserId+":"+bsKey;
	    String encoding = Base64.encodeBase64String(authUser.getBytes());
	    Client restClient = Client.create();
        WebResource webResource = restClient.resource(url);
        ClientResponse resp = webResource.accept("application/json")
                                         .header("Authorization", "Basic " + encoding)
                                         .get(ClientResponse.class);
        if(resp.getStatus() != 200){
            System.err.println("Unable to connect to the server");
        }
        String output = resp.getEntity(String.class);
        JSONObject obj = new JSONObject(output);
        JSONObject bsLogs = (JSONObject) obj.get("automation_session");
        String publicUrl = bsLogs.get("public_url").toString();
        return publicUrl;
   }
   
   @AfterClass(alwaysRun = true)
    public void setupAfterSuite() throws Exception {
        //driver.close();
	    driver.quit();
    	bsLocal.stop();
    }
	
    public boolean isContainsText(String text) {
        return driver.getPageSource().contains(text);
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public String getPageSource() {
        return driver.getPageSource();
    }

    public void pageRefresh() {
        driver.navigate().refresh();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String isRequired(String field) {
        String isrequired = getBundle().getString("errors.required").replace("{0}", field);
        return isrequired;
    }

    public String matchPattern(String field) {
        String isrequired = getBundle().getString("errors.required").replace("{0}", field);
        return isrequired;
    }
}

