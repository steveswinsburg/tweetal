# tweetal

Tweetal is a full featured Twitter portlet.

This is currently being migrated from SVN and needs cleanup.

### Features
* Renders your Twitter feed as a portlet
* Rich AJAX interface, using JSON to communicate to servlet
* Allows posting messages, replies, retweets and the deletion of messages
* Uses OAuth to keep your credentials safe
* Uses config page to set OAuth consumer key and secret which allow binary deployment
* Caching tweets on servlet to reduce traffic to Tweetal

### Developers
* Osama Alkadi
* Denny Denny
* Steve Swinsburg

### Installation

TODO

### Configuration
Starting with Tweetal 1.3.0, all deployments need to register an application with Twitter and provide the settings
to the portlet. Simply fill out this form:
http://twitter.com/oauth_clients/new

Be sure to choose 'client' as the Application type, NOT 'browser'. 

Once you have filled out the form, get the key and secret supplied by Twitter, and add them to your portlet using Config mode.

DO NOT reset these keys without updating the values in the portlet, as the integration will no longer function
if they are out of sync (they are essentially your application's username and password).

Note that when user's link their account they will be prompted to enter a code into Tweetal, that will then be verified and their own access token and secret stored as preferences.

=====

### Project Roadmap

##### June 2013

Migrated to github. Note that only trunk has been migrated (1.5) and the source for all earlier versions is unavailable.

##### Late 2012
Maintenance releases of 1.4 series

##### 2012
1.4.0 released

##### Late 2011
Maintenance releases of 1.3 series

##### May 2011
1.3.0 released
Add config mode for administrators to set OAuth consumer key and secret. This allows binary deployment of Tweetal.
HTML tags are escaped
Tweets are cached on Tweetal servlet thus reducing traffic to Twitter server
Servlet produces JSON that is processed with Trimpath on client's browser
Recognize links and hashtags in tweets, and format them as links
Replying tweets and retweets uses tweets id
Simplified servlets code

##### December 2010
1.2.3 released

##### November 2010
The 1.2 version brings better AJAX handling and bugfixes

##### August 2010
Early tags of the portlet were released locally and put through their paces
