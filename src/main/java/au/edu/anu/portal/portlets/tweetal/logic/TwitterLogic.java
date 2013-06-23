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

package au.edu.anu.portal.portlets.tweetal.logic;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;

public class TwitterLogic {

	private String consumerKey;
	private String consumerSecret;
	
	private final Log log = LogFactory.getLog(getClass().getName());

	/** cache for authenticated users' twitter object */
    private Cache twitterCache;

    /** Twitter OAuth authorized consumer instance */
    private Twitter twitterConsumer;
    
	/**
	 * no arg constructor
	 */
	public TwitterLogic() 
	{
	    // create cache, singleton
        CacheManager.create();
        twitterCache = CacheManager.getInstance().getCache("au.edu.anu.portal.portlets.tweetal.TweetalCache");
	}

	/**
	 * Authenticate User to Twitter
	 * @param userToken
	 * @param userSecret
	 * @return OAuth authenticated instance of Twitter object
	 */
	public Twitter getTwitterAuthForUser (String userToken, String userSecret) 
	{
        if (StringUtils.isBlank(userToken) || StringUtils.isBlank(userSecret)) {
            return null;
        }
        
        String cacheKey = userToken + "|" + userSecret;
        //String cacheKey = userToken;
        
        Element element = twitterCache.get(cacheKey);

        // caching authenticated oauth objects
	    if (element == null) {
            synchronized (twitterCache) {
                // if it is still null after acquiring lock
                element = twitterCache.get(cacheKey);
                
                if (element == null) {
                    // log.debug("cache miss: getting authorized instance for " + userToken);
                    Twitter twitter = new TwitterFactory().getOAuthAuthorizedInstance(
                            consumerKey, consumerSecret, 
                            new AccessToken(userToken, userSecret));
                    
                    element = new Element (cacheKey, twitter);
                    twitterCache.put (element);
                }
            }
	    // } else {
        //     log.debug("cache hit: getting authorized instance for " + userToken);
	    }
	    
	    return (Twitter) element.getObjectValue();
	}
	
	
	/**
	 * Get the screen name of the Twitter user (eg KRuddPM)
	 * @param userToken
	 * @param userSecret
	 * @return the screen name, or null
	 */
	public String getScreenName (String userToken, String userSecret) 
	{
		Twitter twitter = getTwitterAuthForUser(userToken, userSecret);
        if (twitter == null) {
            return null;
        }
     	
		try {
			return twitter.verifyCredentials().getScreenName();
			
		} catch (TwitterException e) {
			log.error("Error getting screen name: " + e.getClass() + ": " + e.getMessage());
		}
		return null;
	}
	
	
	/**
	 * Get a RequestToken for this Twitter application. This token MUST be captured so that it can 
	 * be used for future requests in the same process.
	 * @return RequestToken or null if error
	 */
	public RequestToken getRequestToken() 
	{
        if (twitterConsumer == null) {
            log.error ("No OAuth instance");
            return null;
        }
		
		try {
			return twitterConsumer.getOAuthRequestToken();
			
		} catch (TwitterException e) {
			log.error("Error getting RequestToken: " + e.getClass() + ": " + e.getMessage());
		}
		
		return null;
	}
	
	
	/**
	 * Get an AccessToken for the given RequestToken and user entered pin.
	 * @param requestToken
	 * @param pin
	 * @return AccessToken or null if error
	 */
	public AccessToken getAccessToken(RequestToken requestToken, String pin) 
	{
        if (twitterConsumer == null) {
            log.error ("No OAuth instance");
            return null;
        }
		
		try {
			return twitterConsumer.getOAuthAccessToken(requestToken, pin);
			
		} catch (TwitterException e) {
			log.error("Error getting AccessToken: " + e.getClass() + ": " + e.getMessage());
		}
		
		return null;
	}
	
	
	/**
	 * Set consumer key and secret, and get OAuth authorized instance
	 */
	public void setOAuthConsumer(String consumerKey, String consumerSecret)
	{
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;

        if (StringUtils.isEmpty(consumerKey) || StringUtils.isEmpty(consumerSecret) ) {
            // no key in session, no access
            log.error ("Empty consumer secret and key");
            twitterConsumer = null;
            return;
        }
        
        twitterConsumer = new TwitterFactory().getOAuthAuthorizedInstance (consumerKey, consumerSecret);
	}
	

	/**
	 * Validate that twitter consumer is not null (i.e. key and secret are not null)
	 * @return false if error
	 */
	public boolean validate() 
	{
	    if (twitterConsumer == null) {
	        log.error ("No OAuth instance");
	        return false;
	    }
        
		try {
		    // TODO: it seems that this method does not verify consumer key and secret (Denny)
			return twitterConsumer.test();
		} catch (TwitterException e) {
			log.error("Error in testing API: " + e.getClass() + ": " + e.getMessage());
		}
		
		return false;
	}
	
	
	public boolean verifyCredentials (String userToken, String userSecret) 
	{
		Twitter twitter = getTwitterAuthForUser(userToken, userSecret);
     	
		try {
			twitter.verifyCredentials();
			return true;
			
		} catch (TwitterException e) {
			log.error("Error: Credentials are invalid.\n" + e.getClass() + ":\n" + e.getMessage());
		}
		return false;
	}

}
