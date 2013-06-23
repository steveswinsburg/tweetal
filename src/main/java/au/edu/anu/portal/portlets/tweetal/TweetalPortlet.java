/**
 * Copyright 2009-2011 The Australian National University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package au.edu.anu.portal.portlets.tweetal;

/*
 * Copyright 2010 - Twitter Portlet for uPortal (tweetal).
 */

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.ReadOnlyException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ValidatorException;

import net.sf.ehcache.CacheManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;
import au.edu.anu.portal.portlets.tweetal.logic.TwitterLogic;
import au.edu.anu.portal.portlets.tweetal.util.Messages;

public class TweetalPortlet extends GenericPortlet {

	private final Log log = LogFactory.getLog(getClass().getName());
	
	//pages
	private String viewUrl;
	private String helpUrl;
	private String editUrl;
	private String errorUrl;
	private String configUrl;
	
	private RequestToken requestToken = null;
	private TwitterLogic twitterLogic;

	
	/**
	 * Initialize variables
	 */
	public void init(PortletConfig config) throws PortletException 
	{	   
		super.init(config);
		log.info("TweetalPortlet init()");
		
		//get pages
		viewUrl = config.getInitParameter("viewUrl");
		editUrl = config.getInitParameter("editUrl");
		helpUrl = config.getInitParameter("helpUrl");
		errorUrl = config.getInitParameter("errorUrl");
		configUrl = config.getInitParameter("configUrl");
		
		twitterLogic = new TwitterLogic();
	}
	
	
    /**
     * Delegate to appropriate PortletMode.
     */
    protected void doDispatch(RenderRequest request, RenderResponse response) 
            throws PortletException, IOException 
    {
        if (StringUtils.equalsIgnoreCase(request.getPortletMode().toString(), "config")) {
            doConfig(request, response);
        } else {
            super.doDispatch(request, response);
        }
    }	
    
	
    protected void doConfig (RenderRequest request, RenderResponse response) throws PortletException, IOException 
	{
        // load stored key and secret
		String consumerKey = (String) request.getPreferences().getValue("consumerKey", null);
		request.setAttribute("consumerKey", consumerKey);
		
		String consumerSecret = (String) request.getPreferences().getValue("consumerSecret", null);
		request.setAttribute("consumerSecret", consumerSecret);
		
		PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher(configUrl);
		dispatcher.include(request, response);
	}
      
    
	/**
	 * Render the main view
	 */  
	protected void doView(RenderRequest request, RenderResponse response) 
	        throws PortletException, IOException 
	{
		response.setContentType("text/html");

		PortletSession session = request.getPortletSession();
	
		String consumerKey = (String) request.getPreferences().getValue("consumerKey", null);
		String consumerSecret = (String) request.getPreferences().getValue("consumerSecret", null);
  
        if (StringUtils.isBlank(consumerKey) || StringUtils.isBlank(consumerSecret)) {
            // TODO: should have a different error: e.g. contact admin
            // doError and tell them to configure it
            doConfigError(request, response);
            return;
        }

        // pass consumer key and consumer secret as portlet session to servlet
        session.setAttribute("consumerKey", consumerKey, PortletSession.APPLICATION_SCOPE);
        session.setAttribute("consumerSecret", consumerSecret, PortletSession.APPLICATION_SCOPE);
        
		//user token and secret
		String userToken = (String)request.getPreferences().getValue("userToken", null);
		String userSecret = (String)request.getPreferences().getValue("userSecret", null);

		if(StringUtils.isBlank(userToken) || StringUtils.isBlank(userSecret)) {
			//doError and tell them to configure it
			doConfigError(request, response);
			return;
		}
		
		// otherwise continue to the view page
		request.setAttribute("userSecret", userSecret); 
		request.setAttribute("userToken", userToken);
		
		// dispatch view page
		dispatch(request, response, viewUrl);   
	}  
	

	/**
	 * Process any portlet actions
	 */
	public void processAction (ActionRequest request, ActionResponse response) 
	        throws PortletException, IOException 
    {
		String consumerKey = (String) request.getPreferences().getValue("consumerKey", null);
		String consumerSecret = (String) request.getPreferences().getValue("consumerSecret", null);
		
		twitterLogic.setOAuthConsumer(consumerKey, consumerSecret);
		
		PortletPreferences pref = request.getPreferences();
		String portletMode = request.getPortletMode().toString();
		
		if (StringUtils.equalsIgnoreCase(portletMode, "config")) {
			String operation = request.getParameter("operation");
			
            if (StringUtils.equalsIgnoreCase (operation, "save")) {
            
                consumerKey = request.getParameter("consumerKey");
                consumerSecret = request.getParameter("consumerSecret");
    
                try {
                    // validate here
                    twitterLogic.setOAuthConsumer(consumerKey, consumerSecret);
                    if (twitterLogic.validate()) {
                        // Try to set the prefs
                        pref.setValue("consumerKey", consumerKey);
                        pref.setValue("consumerSecret", consumerSecret);
                        log.info("Changing consumer key and secret to ****");
                        
                    } else {
                        log.error ("Invalid consumer key and secret");
                        response.setRenderParameter ("errorMessage", Messages.getString("config_error_register"));
                        return;
                    }
                    
                } catch (ReadOnlyException e) {
                    response.setRenderParameter ("errorMessage", Messages.getString("error_form_readonly"));
                    log.error(e);
                    return;
                }
                
                // save the prefs
                try {
                    pref.store();
                    response.setPortletMode(PortletMode.VIEW);
                } catch (ValidatorException e) {
                	response.setRenderParameter("errorMessage", 
                            e.getMessage());
                    log.error(e);
                } catch (IOException e) {
                	response.setRenderParameter("errorMessage", 
                            Messages.getString("error_form_save"));
                    log.error(e);
                } catch (PortletModeException e) {
                    e.printStackTrace();
                    log.error (e);
                }

            } else if (StringUtils.equalsIgnoreCase (operation, "cancel")) {
            	response.setPortletMode(PortletMode.VIEW);
            }

		} else if (request.getPortletMode().equals(PortletMode.EDIT)) {
            // when user in edit mode
        	String operation = request.getParameter("operation");
        	
			if (operation.equalsIgnoreCase("save")) {

				String pin = StringUtils.stripToNull(request.getParameter("authCode"));
				
				if (StringUtils.isBlank(pin)) {
					response.setRenderParameter("errorMessage", Messages.getString("error_invalid_pin_empty"));
					log.error("Empty pin");
					return;
				}

				AccessToken accessToken = twitterLogic.getAccessToken(requestToken, pin);
				
				if (accessToken == null) {
					//inline error - check your pin and try again
					response.setRenderParameter("errorMessage", Messages.getString("error_invalid_pin"));
					log.error("AccessToken was null possibly due to an invalid pin");
					return;
				}
				
				// persist to the accessToken for future reference.
				String userToken = accessToken.getToken();
				String userSecret = accessToken.getTokenSecret();

				// save them to preferences
				pref.setValue("userToken", userToken);
				pref.setValue("userSecret", userSecret);
				
                // save the prefs
                try {
                    pref.store();
                    // redirect to view page
                    response.setPortletMode(PortletMode.VIEW);
                } catch (ValidatorException e) {
                    response.setRenderParameter("errorMessage", 
                            e.getMessage());
                    log.error(e);
                } catch (IOException e) {
                    response.setRenderParameter("errorMessage", 
                            Messages.getString("error_form_save"));
                    log.error(e);
                } catch (PortletModeException e) {
                    e.printStackTrace();
                    log.error(e);
                }

			} else if (operation.equalsIgnoreCase("cancel")) {
				response.setPortletMode(PortletMode.VIEW);

			} else if (operation.equalsIgnoreCase("remove")) {
				//remove stuff
				removeUserPreferences(request);
				// redirect to view page
				response.setPortletMode(PortletMode.VIEW);
				
			} else {
				//log error, unsupported operation
			    log.error("Unsupported operation");
            }
		}
	}

	/**
	 * Render the edit page
	 */
	protected void doEdit(RenderRequest request, RenderResponse response) 
	        throws PortletException, IOException 
    {
		String consumerKey = (String) request.getPreferences().getValue("consumerKey", null);
		String consumerSecret = (String) request.getPreferences().getValue("consumerSecret", null);
		twitterLogic.setOAuthConsumer (consumerKey, consumerSecret);
		
		PortletURL viewModeUrl = response.createRenderURL();
		viewModeUrl.setPortletMode(PortletMode.VIEW);
		request.setAttribute("viewUrl", viewModeUrl);
		
		//get any error message that is in the request and pass it on
		request.setAttribute("errorMessage", request.getParameter("errorMessage"));
		
		//TODO get preferences and see if this user has already stored their token and secret, if so, alreadyConfigured=true
		String userToken = (String)request.getPreferences().getValue("userToken",null);
        request.setAttribute("userTokenAttr", userToken);

        String userSecret = (String)request.getPreferences().getValue("userSecret",null);
        request.setAttribute("userSecretAttr", userSecret);

        if (userToken != null && userSecret != null) {
         	String screenName = twitterLogic.getScreenName(userToken, userSecret);
         	if (screenName != null) {
         	    request.setAttribute("screenName", screenName);
         	}

		} else {
			requestToken = twitterLogic.getRequestToken();
			if(requestToken == null) {
				doError("error_no_remote_data", "error_heading_general", request, response);
				return;
			}
			
			request.setAttribute("authUrl", requestToken.getAuthorizationURL());
		}
    	
		dispatch(request, response, editUrl);
	}
	
	
	/**
	 * Render the help view
	 */
	protected void doHelp(RenderRequest request, RenderResponse response) 
	        throws PortletException, IOException 
    {
		PortletURL viewModeUrl = response.createRenderURL();
		viewModeUrl.setPortletMode(PortletMode.VIEW);
		request.setAttribute("viewUrl", viewModeUrl);

		PortletRequestDispatcher requestDispatcher = getPortletContext()
				.getRequestDispatcher(helpUrl);
		requestDispatcher.include(request, response);
	}
	

	/**
	 * Dispatch to a JSP or servlet
	 * @param request
	 * @param response
	 * @param path
	 * @throws PortletException
	 * @throws IOException
	 */
	protected void dispatch (RenderRequest request, RenderResponse response, String path) 
	        throws PortletException, IOException 
    {
		//response.setContentType("text/html"); //$NON-NLS-1$
		response.setContentType(request.getResponseContentType());  
		PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher(path);
		dispatcher.include(request, response);
	}

	
	/**
	 * Remove user token and secret
	 * @param request
	 * @throws PortletException
	 * @throws IOException
	 */
	public void removeUserPreferences (ActionRequest request) 
            throws PortletException, IOException 
    {
		PortletPreferences pref = request.getPreferences();
		pref.reset("userToken");
		pref.reset("userSecret");
		pref.store();
	}
	
	
	/**
	 * Helper to handle error messages
	 * @param messageKey	Message bundle key
	 * @param headingKey	optional error heading message bundle key, if not specified, the general one is used
	 * @param request
	 * @param response
	 */
	private void doError(String messageKey, String headingKey, 
	        RenderRequest request, RenderResponse response) 
	{
		//message
		request.setAttribute("errorMessage", Messages.getString(messageKey));
		
		//optional heading
		if(StringUtils.isNotBlank(headingKey)){
			request.setAttribute("errorHeading", Messages.getString(headingKey));
		} else {
			request.setAttribute("errorHeading", Messages.getString("error_heading_general"));
		}
		
		//dispatch
		try {
			dispatch(request, response, errorUrl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
    /**
     * Show configuration error messages
     * @param request
     * @param response
     * @throws PortletModeException 
     */
    private void doConfigError (RenderRequest request, RenderResponse response) 
            throws PortletModeException 
    {
        PortletURL editModeUrl = response.createRenderURL();
        editModeUrl.setPortletMode(PortletMode.EDIT);
        
        // message
        request.setAttribute("errorHeading", Messages.getString("error_heading_config"));
        request.setAttribute("errorMessage", 
                String.format(" <a href=\"%s\">%s</a> ", editModeUrl, 
                        Messages.getString("error_no_config_2_link"), Messages.getString("")));
        
        // dispatch
        try {
            dispatch(request, response, errorUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }	
    
    public void destroy() {
		log.info("destroy()");
		CacheManager.getInstance().shutdown();
	}
    
}