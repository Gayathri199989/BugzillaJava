/*
 * Copyright 2011 Thomas Golden
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.j2bugzilla.base;


import com.j2bugzilla.rpc.LogIn;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransport;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;


/**
 * The {@code BugzillaConnector} class handles all access to a given Bugzilla installation.
 * The Bugzilla API uses XML-RPC, implemented via the Apache XML-RPC library in this instance.
 * @author Tom
 * 
 * @see <a href="http://www.bugzilla.org/docs/tip/en/html/api/Bugzilla/WebService.html">WebService</a>
 * @see <a href="http://www.bugzilla.org/docs/tip/en/html/api/Bugzilla/WebService/Server/XMLRPC.html">XML-RPC</a>
 *
 */
public class BugzillaConnector {
	
	/**
	 * The {@link XmlRpcClient} handles all requests to Bugzilla by transforming method names and
	 * parameters into properly formatted XML documents, which it then transmits to the host.
	 */
	private XmlRpcClient client;	

	/**
	 * The token represents a login and is used in place of login cookies.
	 * See {@link com.j2bugzilla.rpc.LogIn#getToken()}
	 */
	private String token;
	
	/**
	 * Use this method to designate a host to connect to. You must call this method 
	 * before executing any other methods of this object.
	 * 
	 * @param host A string pointing to the domain of the Bugzilla installation
	 * @throws ConnectionException if a connection cannot be established
	 */
    public void connectTo(String host) throws ConnectionException {
        connectTo(host,null,null);
    }

    /**
     * Use this method to designate a host to connect to. You must call this method 
     * before executing any other methods of this object.
     * 
     * If httpUser is not null, than the httpUser and the httpPasswd will be 
     * used to connect to the bugzilla server. This currently only supports basic
     * http authentication ( @see <a href="http://en.wikipedia.org/wiki/Basic_access_authentication">Basic access authentication</a>).
     * 
     * This is not used to login into bugzilla. To authenticate with your specific Bugzilla installation,
     * please see {@link com.j2bugzilla.rpc.LogIn LogIn}.
     * 
     * @param host A string pointing to the domain of the Bugzilla installation
     * @param httpUser username for an optional Basic access authentication
     * @param httpPasswd password for an optional Basic access authentication
     * @throws ConnectionException if a connection cannot be established
     */
    public void connectTo(final String host, final String httpUser, final String httpPasswd) throws ConnectionException {

        String newHost = host;
        if(!newHost.endsWith("xmlrpc.cgi")) {
            if(newHost.endsWith("/")) {
                newHost += "xmlrpc.cgi";
            } else {
                newHost += "/xmlrpc.cgi";
            }
        }

        URL hostURL;
        try {
            hostURL = new URL(newHost);
        } catch (MalformedURLException e) {
            throw new ConnectionException("Host URL is malformed; URL supplied was " + newHost, e);
        }
        connectTo(hostURL,httpUser,httpPasswd);
    }

    /**
     * Use this method to designate a host to connect to. You must call this method 
     * before executing any other methods of this object.
     * 
     * If httpUser is not null, than the httpUser and the httpPasswd will be 
     * used to connect to the bugzilla server. This currently only supports basic
     * http authentication ( @see <a href="http://en.wikipedia.org/wiki/Basic_access_authentication">Basic access authentication</a>).
     * 
     * This is not used to login into bugzilla. To authenticate with your specific Bugzilla installation,
     * please see {@link com.j2bugzilla.rpc.LogIn LogIn}.
     * 
     * @param host A URL of form http:// + somedomain + /xmlrpc.cgi
     * @param httpUser username for an optional Basic access authentication
     * @param httpPasswd password for an optional Basic access authentication
     * 
     */
    public void connectTo(URL host, String httpUser, String httpPasswd) {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        if (httpUser != null) {
            config.setBasicUserName(httpUser);
            config.setBasicPassword(httpPasswd);
        }
        config.setServerURL(host);

        client = new XmlRpcClient();
        client.setConfig(config);

        /**
         * Here, we override the default behavior of the transport factory to properly
         * handle cookies for authentication
         */
        XmlRpcTransportFactory factory = new XmlRpcSunHttpTransportFactory(client) {
        	
        	private final XmlRpcTransport transport = new TransportWithCookies(client);
        	
			public XmlRpcTransport getTransport() {
				return transport;
			}
		};
		client.setTransportFactory(factory);
	}
	
	/**
	 * Allows the API to execute any properly encoded XML-RPC method.
	 * If the method completes properly, the {@link BugzillaMethod#setResultMap(Map)}
	 * method will be called, and the implementation class will provide
	 * methods to access any data returned. 
	 * 
	 * @param method A {@link BugzillaMethod} to call on the connected installation
	 * @throws BugzillaException If the XML-RPC library returns a fault, a {@link BugzillaException}
	 * with a descriptive error message for that fault will be thrown.
	 */
	@SuppressWarnings("unchecked")//Must cast Object from client.execute()
	public void executeMethod(BugzillaMethod method) throws BugzillaException {
		if(client == null) { 
			throw new IllegalStateException("Cannot execute a method without connecting!");
		}//We are not currently connected to an installation
		Map<Object, Object> params = new HashMap<Object, Object>();
		if (token != null) {
			params.put("Bugzilla_token", token);
		}
		
		params.putAll(method.getParameterMap());
		Object[] obj = {params};
		try {
			Object results = client.execute(method.getMethodName(), obj);
			if(!(results instanceof Map<?, ?>)) { results = Collections.emptyMap(); }
			Map<Object, Object> readOnlyResults = Collections.unmodifiableMap((Map<Object, Object>)results);
			method.setResultMap(readOnlyResults);
			if (method instanceof LogIn) {
				LogIn login = (LogIn)method;
				setToken(login.getToken());
			}
		} catch (XmlRpcException e) {
			BugzillaException wrapperException = XmlExceptionHandler.handleFault(e);
			throw wrapperException;
		}
	}

	public void setToken(String t) {
		token = t;
	}
	
	/**
	 * We need a transport class which will correctly handle cookies set by Bugzilla. This private
	 * subclass will appropriately set the Cookie HTTP headers.
	 *
	 * Cookies are not support by Bugzilla 4.4.3+.
	 *
	 * @author Tom
	 *
	 */
	private static final class TransportWithCookies extends XmlRpcSunHttpTransport {

		/**
		 * A {@code List} of cookies received from the installation, used for authentication
		 */
		private List<String> cookies = new ArrayList<String>();
		
		/**
		 * Creates a new {@link TransportWithCookies} object.
		 * @param pClient The {@link XmlRpcClient} that does the heavy lifting.
		 */
		public TransportWithCookies(XmlRpcClient pClient) {
			super(pClient);
		}
		
//		private URLConnection conn;
//		
//		protected URLConnection newURLConnection(URL pURL) throws IOException {
//            conn = super.newURLConnection(pURL);
//            return conn;
//		}
		
		/**
		 * This is the meat of these two overrides -- the HTTP header data now includes the 
		 * cookies received from the Bugzilla installation on login and will pass them every
		 * time a connection is made to transmit or receive data.
		 */
		protected void initHttpHeaders(XmlRpcRequest request) throws XmlRpcClientException {
	        super.initHttpHeaders(request);
	        if(cookies.size()>0) {
	        	StringBuilder commaSep = new StringBuilder();
	        	
	        	for(String str : cookies) {
	        		commaSep.append(str);
	        		commaSep.append(",");
	        	}
	        	setRequestHeader("Cookie", commaSep.toString());
	        	
	        }
	        
	    }
		
//		protected void close() throws XmlRpcClientException {
//            getCookies(conn);
//		}
		
		/**
		 * Retrieves cookie values from the HTTP header of Bugzilla responses
		 * @param conn
		 */
		private void getCookies(URLConnection conn) {
	    	  if(cookies.size()==0) {
	    		  Map<String, List<String>> headers = conn.getHeaderFields();
	    		  if(headers.containsKey("Set-Cookie")) {//avoid NPE
	    			  List<String> vals = headers.get("Set-Cookie");
			    	  for(String str : vals) {
			    		  cookies.add(str);
			    	  }
	    		  }					    	  
	    	  }
	    	  
	    }
		
	}
}
