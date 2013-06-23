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

package au.edu.anu.portal.portlets.tweetal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.DateFormat;
import java.util.Calendar;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import au.edu.anu.portal.portlets.tweetal.logic.TwitterLogic;

@CommonsLog
public class TweetalServlet extends HttpServlet {
	
	//private final Log log = LogFactory.getLog(getClass().getName());
	private static final long serialVersionUID = 1L;
	
	private transient TwitterLogic twitterLogic;
	private String consumerKey;
	private String consumerSecret;
	private String lastRefreshed;
    /** cache for authenticated twitter object */
    private transient Cache tweetsCache;

    
    public void init (ServletConfig config) throws ServletException 
    {
		super.init(config);
		log.debug("TweetalServlet init()");
		
		twitterLogic = new TwitterLogic();

        // create cache, singleton method
        CacheManager.create();
        tweetsCache = CacheManager.getInstance().getCache("au.edu.anu.portal.portlets.tweetal.TweetsCache");
	}
	
    
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
	}
	
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		String path = request.getPathInfo();
        log.debug(path);
		
		if (path == null) {
			return;
		}
        String[] parts = path.split("/");
        
        if (path.length() < 1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Bad request");          
            return;
        }
		
        // for this validation, the key and secret are passed as POST parameters
        if (StringUtils.equals (parts[1], "validateOAuthConsumer")) {
            validateOAuthConsumer (request, response);
            return;
        }
        
		HttpSession session = request.getSession();
		String consumerKey = (String) session.getAttribute("consumerKey");
		String consumerSecret = (String) session.getAttribute("consumerSecret");

        if (!checkOAuthConsumer (consumerKey, consumerSecret) ) {
            // no valid key in session, no access
            log.error("Request without valid key from " + request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "You are not allowed to access the servlet!");          
		    return;
		}
		
        if (StringUtils.equals (parts[1], "deleteTweet")) {
			deleteTweets (request, response);

		} else if (StringUtils.equals (parts[1], "updateUserStatus")) {
			updateUserStatus (request, response);
		
        } else if (StringUtils.equals (parts[1], "retweet")) {
            retweet (request, response);
        
		} else if (StringUtils.equals (parts[1], "verifyCredentials")) {
			verifyCredentials (request, response);
		
		} else if (StringUtils.equals (parts[1], "getTweets")) {
			getTweets (request, response);

		} else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Bad request");          
            return;
		}
	}	

	
	public boolean checkOAuthConsumer(String consumerKey, String consumerSecret) 
	{
		if (StringUtils.isEmpty(consumerKey) || StringUtils.isEmpty(consumerSecret) ) {
            // no key in session, no access
		    log.debug ("no key");
		    return false;
		}
		
		if (StringUtils.equals(consumerKey, this.consumerKey) 
		        && StringUtils.equals(consumerSecret, this.consumerSecret)) 
		{
		    //log.debug("using the same valid consumer key and secret");
		    return true;
		    
		} else {
    		twitterLogic.setOAuthConsumer(consumerKey, consumerSecret);
    		if (twitterLogic.validate()) {
    		    // if validated, save them for later check
    		    this.consumerKey = consumerKey;
    		    this.consumerSecret = consumerSecret;
    		    return true;
    		}
		}
		
		return false;
	}
	
	
	public void deleteTweets(HttpServletRequest request, HttpServletResponse response) 
	        throws ServletException, IOException 
	{
		String userToken = request.getParameter("u");
		String userSecret = request.getParameter("s");
		long statusID = Long.parseLong(request.getParameter("d"));
		
		log.debug ("Deleting tweet " + statusID);
		
		Twitter twitter = twitterLogic.getTwitterAuthForUser(userToken, userSecret);
        if (twitter == null) {
            // no connection
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        
		try {
			Status s = twitter.destroyStatus(statusID);
			if (s != null) {
				// success
				response.setStatus(HttpServletResponse.SC_OK);
				
		        // remove tweets from cache
		        String cacheKey = userToken;
		        tweetsCache.remove(cacheKey);
		        
			} else {
			    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			}
			
		} catch (TwitterException e) {
			// problem in deleting the tweet
			log.error("Delete Tweet: " + e.getStatusCode() + ": " + e.getClass() + e.getMessage());
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
	}
	
	
	public void updateUserStatus(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException 
    {
        response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		String userToken = request.getParameter("u");
		String userSecret = request.getParameter("s");
		String userStatus = request.getParameter("t");
        String statusId = request.getParameter("d");

		log.debug ("userStatus: " + userStatus);
        log.debug ("statusId: " + statusId);
		
        Twitter twitter = twitterLogic.getTwitterAuthForUser(userToken, userSecret);
        if (twitter == null) {
            // no connection
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

		try {
		    Status status = null;

		    // update user status
            if(StringUtils.isNotBlank(statusId)) {
                status = twitter.updateStatus(userStatus, Long.parseLong(statusId));
            } else {
                status = twitter.updateStatus(userStatus);
            }
	        if (status == null) {
	            log.error("Status is null.");
	            // general error
	            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	            return;
	        }
	            
            JSONObject json = new JSONObject ();
            JSONObject statusJSON = getStatusJSON(twitter, status);
            
            User currentUser = twitter.showUser(twitter.getId());
            Status lastUserStatus = currentUser.getStatus();
            
            // return as an array even though only it contains only one element, 
            // so we can reuse the same Trimpath template (Denny)
            JSONArray statusList = new JSONArray ();
            statusList.add(statusJSON);
            json.put("statusList", statusList);
            lastRefreshed = Calendar.getInstance().getTime().toString();
            
            if (lastRefreshed == null) {
                json.element("lastRefreshed",
                        "unable to retrieve last refreshed");
            } else {
                json.element("lastRefreshed",
                		lastRefreshed.toString());
            }
            
            if (lastUserStatus == null) {
                json.element("lastStatusUpdate",
                        "unable to retrieve last status");
            } else {
                Date lastStatusUpdate = lastUserStatus.getCreatedAt();
                json.element("lastStatusUpdate",
                        lastStatusUpdate.toString());
 
            }

            if (log.isDebugEnabled()) {
                log.debug(json.toString(2));
            }
            
			out.print(json.toString());
			
		} catch (TwitterException e) {
			log.error("GetTweets: " + e.getStatusCode() + ": " + e.getClass() + e.getMessage());
			
			if(e.getStatusCode() == 401) {
				//invalid credentials
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}
			else if(e.getStatusCode() == -1) {
				//no connection
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			}
			else {
				//general error
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}


    public void retweet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException 
    {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String userToken = request.getParameter("u");
        String userSecret = request.getParameter("s");
        long statusId = Long.parseLong(request.getParameter("d"));
        
        log.debug("statusId: " + statusId);

        Twitter twitter = twitterLogic.getTwitterAuthForUser(userToken,
                userSecret);
        if (twitter == null) {
            // no connection
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        try {
            Status status = null;

            // update user status
            status = twitter.retweetStatus(statusId);
            if (status == null) {
                log.error("Status is null.");
                // general error
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            JSONObject json = new JSONObject();
            JSONObject statusJSON = getStatusJSON(twitter, status);

            // return as an array even though only it contains only one element,
            // so we can reuse the same Trimpath template (Denny)
            JSONArray statusList = new JSONArray();
            statusList.add(statusJSON);
            json.put("statusList", statusList);

            if (log.isDebugEnabled()) {
                log.debug(json.toString(2));
            }

            out.print(json.toString());

        } catch (TwitterException e) {
            log.error("GetTweets: " + e.getStatusCode() + ": " + e.getClass()
                    + e.getMessage());

            if (e.getStatusCode() == 401) {
                // invalid credentials
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else if (e.getStatusCode() == -1) {
                // no connection
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } else {
                // general error
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public void validateOAuthConsumer (HttpServletRequest request,
            HttpServletResponse response) 
            throws ServletException, IOException 
    {
        String consumerKey = request.getParameter("k");
        String consumerSecret = request.getParameter("s");
        
        log.debug ("validating consumer key and secret: " + consumerKey + " " + consumerSecret);
        
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        if (checkOAuthConsumer (consumerKey, consumerSecret)) {
            log.info ("Consumer key and secret verified");
            out.print("valid");
        } else {
            log.error ("Consumer key and secret are invalid");
            out.print("invalid");
        }
    }
    
    
	public void verifyCredentials(HttpServletRequest request, HttpServletResponse response) 
	        throws ServletException, IOException 
    {
        response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		
		String userToken = request.getParameter("u");
		String userSecret = request.getParameter("s");
	
		if (twitterLogic.verifyCredentials(userToken, userSecret)){
			log.debug ("logged in successfully");
			out.println("1");
		} else {
			out.println("0");
		} 
	}
	

    public void getTweets(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException 
    {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String userToken = request.getParameter("u");
        String userSecret = request.getParameter("s");

        Twitter twitter = twitterLogic.getTwitterAuthForUser(userToken,
                userSecret);
        if (twitter == null) {
            // no connection
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        String cacheKey = userToken;
        Element element = null;

        // force refresh
        boolean force = Boolean.parseBoolean(request.getParameter("force"));
        if (force) {
            log.debug("force refresh for " + userToken);
            // remove tweets cache
            tweetsCache.remove(cacheKey);
        } else {
            element = tweetsCache.get(cacheKey);
        }

        if (element == null) {
            synchronized (tweetsCache) {
                // if it is still null after acquiring lock
                element = tweetsCache.get(cacheKey);

                if (element == null) {
                    log.debug("cache miss: getting tweets for " + userToken);
                    System.out.println("Last refreshed: " + Calendar.getInstance().getTime());
                    
                    try {
                        ResponseList<Status> friendStatus = twitter
                                .getFriendsTimeline();

                        long maxId = Long.MIN_VALUE;

                        JSONObject json = new JSONObject();

                        lastRefreshed = Calendar.getInstance().getTime().toString();
                        
                        if (lastRefreshed == null) {
                            json.element("lastRefreshed",
                                    "unable to retrieve last refreshed");
                        } else {
                            json.element("lastRefreshed",
                            		lastRefreshed.toString());
                        }
                        
                        User currentUser = twitter.showUser(twitter.getId());
                        Status lastUserStatus = currentUser.getStatus();
                        if (lastUserStatus == null) {
                            json.element("lastStatusUpdate",
                                    "unable to retrieve last status");
                        } else {
                            Date lastStatusUpdate = lastUserStatus.getCreatedAt();
                            json.element("lastStatusUpdate",
                                    lastStatusUpdate.toString());
                        }

                        for (Status status : friendStatus) {
                            maxId = Math.max(maxId, status.getId());
                            json.accumulate("statusList",
                                    getStatusJSON(twitter, status));
                        }

                        if (log.isDebugEnabled()) {
                            log.debug(json.toString(2));
                        }

                        out.print(json.toString());

                        element = new Element(cacheKey, new TweetsCacheElement(
                                System.currentTimeMillis(), maxId, json));

                        tweetsCache.put(element);
      
                        
                        return;

                    } catch (TwitterException e) {
                        log.error("GetTweets: " + e.getStatusCode() + ": "
                                + e.getClass() + e.getMessage());

                        if (e.getStatusCode() == 401) {
                            // invalid credentials
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        } else if (e.getStatusCode() == -1) {
                            // no connection
                            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        } else {
                            // general error
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                        
                        return;
                    }
                }
            }
        }

        // tweets available in cache
        log.debug("cache hit: getting tweets for " + userToken);

        TweetsCacheElement tweets = (TweetsCacheElement) element
                .getObjectValue();
       
        // if just refreshed too quickly, don't request update, just use
        // whatever in cache
        long period = System.currentTimeMillis() - tweets.getLastRefresh();
        System.out.println("Already refreshed: " + (period / 1000) + " second(s) ago");
       
        if (period < 2 * 60 * 1000) {
            log.debug("refreshed too quickly: " + (period / 1000) + " seconds");
            JSONObject json = tweets.getResult();
            lastRefreshed = Calendar.getInstance().getTime().toString();
            json.element("lastRefreshed",
            		lastRefreshed.toString());           
            out.print(json.toString());
            return;
        }

        // get new updates since the last id
        long maxId = tweets.lastId;
        try {
            JSONObject json = tweets.getResult();
            
            ResponseList<Status> friendStatus = twitter
                    .getFriendsTimeline(new Paging(maxId));

            tweets.setLastRefresh(System.currentTimeMillis());

            log.debug(String.format("Got %d new tweets", friendStatus.size()));
            
            if (friendStatus.size() > 0) {
                JSONArray newTweets = new JSONArray ();
    
                lastRefreshed = Calendar.getInstance().getTime().toString();
                json.element("lastRefreshed",
                		lastRefreshed.toString());
                
                for (Status status : friendStatus) {
                    maxId = Math.max(maxId, status.getId());
                    newTweets.add (getStatusJSON(twitter, status));
                }
                
                if (log.isDebugEnabled()) {
                    log.debug("new tweets:\n" + newTweets.toString(2));
                }
    
                json.getJSONArray("statusList").addAll(0, newTweets);
                
                tweets.setLastId(maxId);
                
                User currentUser = twitter.showUser(twitter.getId());
                Status lastUserStatus = currentUser.getStatus();
                if (lastUserStatus == null) {
                    json.element("lastStatusUpdate",
                            "unable to retrieve last status");
                } else {
                    Date lastStatusUpdate = lastUserStatus.getCreatedAt();
                    json.element("lastStatusUpdate",
                            lastStatusUpdate.toString());
                }
            }

            out.print(json.toString());

        } catch (TwitterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            log.error(e);
        }
    }
    

    private JSONObject getStatusJSON (Twitter twitter, Status status)
            throws TwitterException 
    {
        JSONObject result = new JSONObject ();
        
        result.put("tweetId", Long.toString(status.getId()));
        result.put("isOwnTweet", status.getUser().getId() == twitter.getId());
        result.put("createdAt", status.getCreatedAt().toString());
        result.put("screenName", status.getUser().getScreenName()); 
        result.put("name", status.getUser().getName());
        result.put("profileImageURL", status.getUser().getProfileImageURL().toString());
        
        String statusText = status.getText();
        
        // escape html to prevent XSS attack
        statusText = StringEscapeUtils.escapeHtml(statusText);

        result.put("statusText", statusText);

        // if this tweet addresses the current user
        result.put("isMentionedMe", StringUtils.contains(statusText, "@" + twitter.getScreenName()));

        // replace links with link tags
        statusText = replaceLinks (statusText);
        // replace @username with link to the user page
        statusText = replaceUserReference (statusText);
        // replace #hashtag with link
        statusText = replaceHashTag (statusText);
        
        result.put("statusTextFormatted", statusText);
        
        return result;
    }
    

    private String replaceUserReference(String statusText) 
    {
        Pattern p = Pattern.compile("@[a-zA-Z0-9_]+");
        Matcher m = p.matcher(statusText);
        
        // if we have a match anywhere in the string
        StringBuffer buf = new StringBuffer();
        while (m.find()) {
        	//replace that portion that matched, with the link
        	String linkId = StringUtils.stripStart(m.group(), "@");
        	m.appendReplacement(buf, String.format(
        	        "<a target=\"_blank\" href=\"http://twitter.com/%s\">%s</a>", linkId, m.group()));
        }
        m.appendTail(buf);
        return buf.toString();
    }	

    
    private String replaceLinks (String statusText) 
    {
        Pattern p = Pattern.compile("http[s]{0,1}://[A-Za-z0-9\\._\\-~#/]+");
        Matcher m = p.matcher(statusText);
        
        // if we have a match anywhere in the string
        StringBuffer buf = new StringBuffer();
        while(m.find()) {
            //replace that portion that matched, with the link
            String matched = m.group();
            m.appendReplacement(buf, String.format ("<a target=\"_blank\" href=\"%s\">%s</a>", matched, matched));
        }
        m.appendTail(buf);
        return buf.toString();
    }   

   
    private String replaceHashTag (String statusText) 
    {
        Pattern p = Pattern.compile("(\\s|\\A)#([^\\s]+)");
        Matcher m = p.matcher(statusText);
        
        // if we have a match anywhere in the string
        StringBuffer buf = new StringBuffer();
        while(m.find()) {
            //replace that portion that matched, with the link
            String matched = m.group();
            m.appendReplacement(buf, String.format ("<a target=\"_blank\" href=\"http://twitter.com/search?q=%%23%s\">%s</a>", StringUtils.stripStart(matched.substring(1), "#"), matched));
        }
        m.appendTail(buf);
        return buf.toString();
    }   
   
    /* 
    private String replaceHashTag(String text) {
		Pattern pattern = Pattern.compile("(\\s|\\A)#([^\\s]+)");
		Matcher matcher = pattern.matcher(text);
		String result = "";
		int start = 0;
    	while(matcher.find()) {
    		result += text.substring(start, matcher.start()) + "<a href=\"http://twitter.com/search?q=#" + matcher.group(2) + "\">#" + matcher.group(2) + "</a>";
    		start = matcher.end();
    	}
    	
    	result += text.substring(start);
    	
    	return result;
	}
*/
}

@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
class TweetsCacheElement {
    long lastRefresh;
    long lastId;
    JSONObject result;
}