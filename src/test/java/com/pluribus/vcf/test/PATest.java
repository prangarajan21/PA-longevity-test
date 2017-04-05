package com.pluribus.vcf.test;
import com.pluribus.vcf.helper.TestSetup;
import com.pluribus.vcf.pagefactory.VCFHomePage;
import com.pluribus.vcf.pagefactory.VCFLoginPage;
import com.pluribus.vcf.pagefactory.VCFPaIndexPage;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class PATest extends TestSetup{
	private VCFHomePage home1;
	private VCFPaIndexPage paIndex;
	private VCFLoginPage login;
	
	@BeforeClass(alwaysRun = true)
	public void init() {
		home1 = new VCFHomePage(getDriver());
		login = new VCFLoginPage(getDriver());
		paIndex = new VCFPaIndexPage(getDriver());
	}
	
	@Parameters({"password"}) 
	@Test(alwaysRun = true)
	public void logintoPA(@Optional("test123") String password) {
		login.login("admin", password);
		home1.gotoPA();
	}
	
	@Parameters({"pcapName","vcfIp"}) 
	@Test(groups={"smoke","regression"}, dependsOnMethods={"logintoPA"}, description="Add local Pcap")
	public void addPcapTest(@Optional("pcap1") String pcapName, String vcfIp) throws Exception{
		paIndex.addLocalPcap(pcapName,vcfIp);
		if(paIndex.verifyPcap(pcapName)) {
			com.jcabi.log.Logger.info("addPcap","Pcap "+pcapName+" configured and verified succcessfully");
		} else {
			com.jcabi.log.Logger.error("addPcap","Pcap configuration not successful");
			throw new Exception("Pcap configuration failed");
		}
	}
	
	@Parameters({"switchName","pcapName","inPort","outPort","flowDuration","flowName"})
	@Test(groups={"smoke","regression"},dependsOnMethods={"addPcapTest"},description="Add vflow")
	public void addEnableFlowTest(String switchName, @Optional("pcap1") String pcapName, String inPort, String outPort, String flowDuration, @Optional("flow1") String flowName) throws Exception{
		paIndex.addVFlow(flowName,switchName,inPort,outPort,flowDuration,pcapName);
		if(paIndex.togglevFlowState(flowName)) {
			com.jcabi.log.Logger.info("addVFlowTest","Turned on vflow successfully");
		} else {
			com.jcabi.log.Logger.error("addVFlowTest","Could not turn on vflow");
			throw new Exception("Vflow turn on failed");
		}
	}
	
	@Parameters({"pcapToggleCount","flowName","flowDuration"})
	@Test(groups={"smoke","regression"},dependsOnMethods={"addEnableFlowTest"},description="Turn on pcap after it gets turned off each time for n iterations")
	public void reEnablePcap (String pcapToggleCount, @Optional("flow1") String flowName, String flowDuration) throws Exception{
		int loopCount = Integer.parseInt(pcapToggleCount);
		int sleepTime = Integer.parseInt(flowDuration)*60*1000;
		
		for(int i =0; i < loopCount; i++) {
			com.jcabi.log.Logger.info("reEnablePcap","Iteration:"+i);
			com.jcabi.log.Logger.info("reEnablePcap","Sleeping for duration of configured pcap:"+flowDuration+" mins ");
			keepSessionActivesleep(sleepTime);	
			//Check if "completed"
			if(!paIndex.togglevFlowState(flowName)) {
					com.jcabi.log.Logger.error("reEnablePcap","Could not turn off capture");
					throw new Exception("Reenabling pcap test failed");
			}
			com.jcabi.log.Logger.info("reEnablePcap","Now turning on capture");
			keepSessionActivesleep(1000);
			if(!paIndex.togglevFlowState(flowName)) {
					com.jcabi.log.Logger.error("reEnablePcap","Could not turn on capture");
					throw new Exception("Reenabling pcap test failed");
			}
		}
	}
	
	
	public void keepSessionActivesleep(int totalSleep) {
		int slTime = totalSleep;
		while (slTime > 100000) {
			try {
				Thread.sleep(100000); //sleepfor100seconds
			} catch(Exception e) {
				com.jcabi.log.Logger.error("keepSessionActivesleep",e.toString());
			}
			paIndex.getUrl();
			slTime = slTime - 100000;
		} 
		try {
				Thread.sleep(slTime);
			} catch(Exception e) {
				com.jcabi.log.Logger.error("keepSessionActivesleep",e.toString());
		}
	}
	
}