# tweetal
=======

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
Osama Alkadi
Denny Denny
Steve Swinsburg

### Installation


### Configuration
Starting with Tweetal 1.3.0, all deployments need to register an application with Twitter and provide the settings to the portlet. Simply fill out this form: http://twitter.com/oauth_clients/new
Be sure to choose 'client' as the Application type, NOT 'browser'. Once you have filled out the form, add the key and secret supplied to your portlet using Config mode.

DO NOT reset these keys without updating the values in the portlet, as the integration will no longer function if they are out of sync (they are essentially your application's username and password).
When user's link their account they will be prompted to enter a code into Tweetal, that will then be verified and their own access token and secret stored as preferences. 