package com.j2bugzilla.base;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.j2bugzilla.rpc.LogIn;
import com.j2bugzilla.rpc.ReportBug;

//@RunWith(Parameterized.class)
public class BugzillaConnectorIT {

//	@Parameters
//	public static List<Object[]> getUrls() {
//		List<Object[]> urls = new ArrayList<Object[]>();
//		
//		urls.add(new Object[]{"https://landfill.bugzilla.org/bugzilla-3.6-branch/"});
//		urls.add(new Object[]{"https://landfill.bugzilla.org/bugzilla-4.0-branch/"});
//		urls.add(new Object[]{"https://landfill.bugzilla.org/bugzilla-4.2-branch/"});
//		urls.add(new Object[]{"https://landfill.bugzilla.org/bugzilla-4.4-branch/"});
//		
//		return urls;
//	}
	
	String url = "http://dev.permisso.in/bugzilla/?api_key=BQL3JdHxaQcSQixoL92e3HJpR2xDPUDs0sXstpsa";
	
//	public BugzillaConnectorIT(String url) {
//		this.url = url;
//	}
	
	@Test
	public void testConnect() throws ConnectionException, BugzillaException {
		BugzillaConnector conn = new BugzillaConnector();
		
		conn.connectTo(url);
		  LogIn logIn = new LogIn("niranjani.j", "welcome");
		  conn.executeMethod(logIn);
		  Bug bug = new BugFactory().newBug().setProduct("xserp").setComponent("Accounts").setVersion("2.17.2")
		    		.setPlatform("PC").setOperatingSystem("Windows XP").setDescription("Settle bill with advance test case failed ").setSummary("Settle bill with advance test case failed ").createBug();
		    ReportBug report = new ReportBug(bug);
		    conn.executeMethod(report);
	}

}
