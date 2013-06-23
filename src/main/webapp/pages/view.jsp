<%--

    Copyright 2009-2011 The Australian National University

       Licensed under the Apache License, Version 2.0 (the "License");
       you may not use this file except in compliance with the License.
       You may obtain a copy of the License at

           http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing, software
       distributed under the License is distributed on an "AS IS" BASIS,
       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       See the License for the specific language governing permissions and
       limitations under the License.

--%>

<%@ page contentType="text/html" isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ taglib prefix="rs" uri="http://www.jasig.org/resource-server" %>

<portlet:defineObjects/>
<fmt:setBundle basename="au.edu.anu.portal.portlets.tweetal.util.messages" />

<link type="text/css" rel="stylesheet"  href="<%=request.getContextPath()%>/style/stylesheet.css" />
<rs:resourceURL var="jQueryPath" value="/rs/jquery/1.4.2/jquery-1.4.2.min.js"/>
<rs:resourceURL var="jqueryi18n" value="/rs/jquery/jquery.i18n/jquery.i18n.properties-min.js"/>
<rs:resourceURL var="apTextCounter" value="/rs/jquery/apTextCounter/jquery.apTextCounter.min.js"/>
<rs:resourceURL var="trimpathPath" value="/rs/trimpath/1.0.38/template.js"/>

<script type="text/javascript" language="javascript" src="${jQueryPath}"></script>
<script type="text/javascript" language="javascript" src="${jqueryi18n}"></script>
<script type="text/javascript" language="javascript" src="${apTextCounter}"></script>
<script type="text/javascript" language="javascript" src="${trimpathPath}"></script>

<!-- Set the namespace variable -->
<c:set var="namespace"><portlet:namespace/></c:set>

<script>
var Tweetal${namespace} = Tweetal${namespace} || {};
Tweetal${namespace}.jQuery = jQuery.noConflict(true);

Tweetal${namespace}.jQuery(function(){
	var contextPath = '<%=request.getContextPath()%>';
	var token = '${userToken}';
	var secret = '${userSecret}';
	var loader = '<img src="' + contextPath + '/images/ajax-loader-small.gif" />';
	var statusHighlightedId = '';
	
        var templateStatusList = TrimPath.parseDOMTemplate("${namespace}statusList_jst");
        
	var $ = Tweetal${namespace}.jQuery;
	
	var unauthorized = false;

	//init
        $("#${namespace}tweet-list").html(loader);
	
	$("#${namespace}tweet-error").hide();
	$("#${namespace}update-wrapper").hide();


	Tweetal${namespace}.registerRefresh = function(interval) {
		setInterval(function() {
			if (!unauthorized) {
		                Tweetal${namespace}.getTweets(false);
		        }
		}, interval);

		// setup refresh link
		$("#${namespace}refresh").click(function(event){
			Tweetal${namespace}.getTweets(true);	
		});
	}


	Tweetal${namespace}.getTweets = function(force) {
		
		$.ajax({
			url: contextPath + "/tweetalServlet/getTweets",
			type: "POST",
			data: "u="+token+"&s="+secret+"&force="+force,
			cache: false,
			dataType: "json",
			timeout: 5000,
			success: function(data) {
                                // generate html 
                                var result = templateStatusList.process(data);
                                if(result == '') {
                					// service unavailable
                					$('#${namespace}tweet-error').html('<fmt:message key="tweet_error_service" />').show();
                                }
                                
                                $('#${namespace}tweet-list').html (result);
                                
                                $('#${namespace}tweet-error').hide();
				$('#${namespace}update-wrapper').show();
				$(".status-tools").hide();
				
			        $('#${namespace}last-updated').fadeIn()
			             .html('<fmt:message key="view_update" />' ).append(" ")
			             .append(" " + data.lastRefreshed);
				
			        unauthorized = false;
			},
			error: function(xhr, status) {
				var errorCode = xhr.status;
				
				if(errorCode == '-1') {
					// service unavailable
					$('#${namespace}tweet-error').html('<fmt:message key="tweet_error_service" />').show();
					
				} else if (errorCode == '400') {
					// unauthorised
					$('#${namespace}tweet-error').html('<fmt:message key="no_internet_connection" />').show();
					unauthorized = true;
					
				} else if (errorCode == '401') {
					// unauthorised
					$('#${namespace}tweet-error').html('<fmt:message key="tweet_error_unauthorised" />').show();
					unauthorized = true;
				} else {
					// general error
				        $('#${namespace}tweet-error').html('<fmt:message key="tweet_error_forbidden" />').show();
				        $('#${namespace}update-wrapper').hide();
				        $(".status-tools").hide();
				}
	
				$('#${namespace}tweet-list').html(null);
			}
		});
	}


	Tweetal${namespace}.bindEvents = function() {

		$("div.status").live('click', function(event) {
			//remove highlight from all elements
			$("div.status").removeClass("highlight-selected-tweet")
		      
       		        //highlight just this one
        		$(this).addClass("highlight-selected-tweet");
			
			statusHighlightedId = 
				Tweetal${namespace}.trimTweetId ($(this).attr('id'));
 
			//hide all the status boxes
			$(".status-tools").hide();

			//show just this one  
			$(this).children(".status-content-wrapper").children(".status-tools").show();

			return true;
		});

		// remove the default message when the status field is clicked
		$("#${namespace}status-field").live('click', function(event) {

			if($(this).val() == '<fmt:message key="default_message" />') {
				$(this).val('');
			}
		});
	}

	Tweetal${namespace}.setupLinks = function() {
		// setup reply links
		$(".reply").live('click', function(event){
			
			 // to avoid two Tweetal portlet on a page firing live methods twice
			if (Tweetal${namespace}.getElemNamespace($(this)) != "${namespace}") {
				return;
			}
			var reply = $(this).attr("rel");
			var tweetId = Tweetal${namespace}.getTweetId($(this));
			
			$("#${namespace}status-field").val(reply);
			// trigger/fire events programmatically to refresh app counter 
			$('#${namespace}status-field').trigger('keyup');
			
			$("#${namespace}status-id").val(tweetId);
		});

		// setup retweet links
		$(".retweet").live('click', function(event){
			
			// to avoid two Tweetal portlet on a page firing live methods twice
			if (Tweetal${namespace}.getElemNamespace($(this)) != "${namespace}") {
				return;
				}
			
		    var id = Tweetal${namespace}.getTweetId($(this));
			var rt = $(this).attr("rel");
			
                        var msg = confirm('<fmt:message key="tweet_confirm_retweet"/>\n' + rt)
                        
                        if (msg){
                            $.ajax({
                                url: contextPath + "/tweetalServlet/retweet",
                                data: "u="+token+"&s="+secret+"&d="+id,
                                type: "POST",
                                cache: false,
                                dataType: "json",
                                timeout: 5000,
                                success: function(data) {
                                    // generate html 
                                    var result = templateStatusList.process(data);
                                    $('#${namespace}tweet-list').prepend (result);
                                    
                                    $('#${namespace}status-field').val('');
                                    // trigger/fire events programmatically to refresh app counter 
                                    $('#${namespace}status-field').trigger('keyup');
                                    
                                    $(".status-tools").hide();
                                },
                                error: function(xhr, status) {
                                    $('#${namespace}tweet-error').html('<fmt:message key="tweet_error_retweet" />').show();
                                }
                            });
                        }
		});

		// setup delete links
		$(".delete").live('click', function(event){
			 	// to avoid two Tweetal portlet on a page firing live methods twice
			 	if (Tweetal${namespace}.getElemNamespace($(this)) != "${namespace}") {
			 	return;
			 	} 
			 	
				var id = Tweetal${namespace}.getTweetId($(this));
				Tweetal${namespace}.deleteTweets(id);
		});

	}
	
	Tweetal${namespace}.initLocalisation = function() {
		$.i18n.properties({
		    name:'messages',
		    path: contextPath + '/bundle/',
		    mode:'vars'
		});
	}
	

	Tweetal${namespace}.deleteTweets = function(id) {

		var msg = confirm('<fmt:message key="tweet_confirm_delete" />')
		
		if (msg){
			$.ajax({
				url: contextPath + "/tweetalServlet/deleteTweet",
				data: "u="+token+"&s="+secret+"&d="+id,
				type: "POST",
				cache: false,
				dataType: "json",
				timeout: 5000,
				success: function(data) {
					$('#${namespace}'+id).slideUp();
				},
				error: function(xhr, status) {
					$('#${namespace}tweet-error').html('<fmt:message key="tweet_error_delete" />').show();
				}
			});
		}
	}
	
	Tweetal${namespace}.getElemNamespace = function(elem) {
		//get the parent div id
		elementId = $(elem).parents(".status").attr("id");
		return elementId.substring (0, "${namespace}".length);
		 	 }

	Tweetal${namespace}.getTweetId = function(elem) {
		//get the parent div id
		elementId = $(elem).parents(".status").attr("id");
		return Tweetal${namespace}.trimTweetId (elementId);
	}


        Tweetal${namespace}.trimTweetId = function(elementId) {
            namespace = "${namespace}";
            return elementId.substring (namespace.length, elementId.length); 
        }

	
	Tweetal${namespace}.setupCounter = function(field, tracker) {
		$(field).apTextCounter({
 	        maxCharacters: 140,
	        direction: "down",
 	        tracker: "#<portlet:namespace/>tracker",
 	        trackerTemplate: "%s",
 	         
 	        onTrackerUpdated: function(msg) {
				if (msg.count <= 10) {
					$(msg.config.tracker).css("color", "red");
				} else if (msg.count <= 20) {
					$(msg.config.tracker).css("color", "maroon");
				} else {
					$(msg.config.tracker).css("color", "#999999");
				}
 	        }
 	    });
	}

	
	Tweetal${namespace}.bindForm = function () {

		$("#${namespace}status-form").submit(function(event){
			var text = $("#${namespace}status-field").val();
			if(text != '') {
				$.ajax({
					url: contextPath + "/tweetalServlet/updateUserStatus",
					data: "u=" + token + "&s=" + secret + "&t=" + text +"&d=" + statusHighlightedId,
					type: "POST",
					cache: false, 
					dataType: "json",
					timeout: 5000,
					success: function(data) {
	                                        // generate html 
	                                        var result = templateStatusList.process(data);
	                                        $('#${namespace}tweet-list').prepend (result);
	                                        
						$('#${namespace}status-field').val('');
						// trigger/fire events programmatically to refresh app counter 
						$('#${namespace}status-field').trigger('keyup');
						
						$(".status-tools").hide();
						//Tweetal${namespace}.getTweets(true);
						$('#${namespace}last-updated').fadeIn()
			             .html('<fmt:message key="view_update" />' ).append(" ")
			             .append(" " + data.lastRefreshed);
					},
					error: function(xhr, status) {
						$("#${namespace}tweet-list").html('<fmt:message key="tweet_error_status" />');
					}
				});
			}
			return false;
		});
	}

	Tweetal${namespace}.initLocalisation();
	Tweetal${namespace}.bindForm();
	Tweetal${namespace}.getTweets();
	Tweetal${namespace}.registerRefresh(5*60*1000);
	Tweetal${namespace}.setupCounter('#${namespace}status-field', '#${namespace}tracker');

	// bind once with .live rather than re-bind with .click every updates
	Tweetal${namespace}.setupLinks();
	Tweetal${namespace}.bindEvents();	
});


</script>

<div class="tweetal">

	<div id="${namespace}tweet-error" class="portlet-msg-error" style="display:none;"></div>

	<div id="${namespace}tweet-list" class="status-wrapper"></div>

	<div id="${namespace}update-wrapper" class="update-wrapper">
		<form id="${namespace}status-form" method="POST" action="">
			<input type="text" id="${namespace}status-field" value="<fmt:message key="default_message" />"   onfocus="this.style.borderColor='#AAD9FA'" onblur="this.style.borderColor=''"      />			
		</form>
		<div class="refresh-wrapper">
			<span id="${namespace}last-updated" ></span>&nbsp;(<a href="#" id="${namespace}refresh"><fmt:message key="action_refresh" /></a>)			
		</div>
		<div class="status-word-count" id="${namespace}tracker"></div>
	</div>
</div>


<textarea id="${namespace}statusList_jst" style="display:none;">
    {for s in statusList}
    <div id="${namespace}\${s.tweetId}" class="status {if s.isMentionedMe} highlight-mine{/if}">
        <div class="status-image-wrap">
        	<img src="\${s.profileImageURL}"/>
        </div>
        <div class="status-content-wrapper">
            <div class="status-tools" style="display: none;">
                <a rel="@\${s.screenName}" class="reply">
                    <img src="<%=request.getContextPath()%>/images/arrow_turn_left.png">
                </a>
                  {if s.isOwnTweet ==false}
                <a rel="RT @\${s.screenName} \${s.statusText}" class="retweet">
                    <img src="<%=request.getContextPath()%>/images/arrow_rotate_anticlockwise.png">
                </a>
                 {/if} 
                {if s.isOwnTweet}
                    <a class="delete"><img src="<%=request.getContextPath()%>/images/cross.png"/></a>
                {/if}                
            </div>
            <div class="status-body-wrapper">
                <div class="status-body">
                    <span><a target="_blank"
                        href="http://twitter.com/\${s.screenName}">\${s.name}</a>
                    </span><span class="status-text">\${s.statusTextFormatted}</span>
                </div>
                <div class="status-timestamp">
                    <span>\${s.createdAt}</span>
                </div>
            </div>
        </div>
    </div>    
    {/for}
</textarea>
    