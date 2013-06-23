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
<rs:resourceURL var="jQueryValidation" value="/rs/jquery/validate/jquery.validate.min.js"/>

<script type="text/javascript" language="javascript" src="${jQueryPath}"></script>
<script type="text/javascript" language="javascript" src="${jqueryi18n}"></script>
<script type="text/javascript" language="javascript" src="${jQueryValidation}"></script>
<!-- Set the namespace variable -->
<c:set var="namespace"><portlet:namespace/></c:set>

<script>
var Tweetal${namespace} = Tweetal${namespace} || {};
Tweetal${namespace}.jQuery = jQuery.noConflict(true);

Tweetal${namespace}.jQuery(function(){
	var contextPath = '<%=request.getContextPath()%>';
	
	var $ = Tweetal${namespace}.jQuery;
	
	/*
	$("#${namespace}form").submit (function (event) {
                var success = true;
                
		$.ajax({
			url: contextPath + "/tweetalServlet/validateOAuthConsumer",
			data: "k=" + $("#${namespace}consumerKey").val() + "&s=" + $("#${namespace}consumerSecret").val(),
			type: "POST",
			async: false,
			cache: false,
			dataType: "text",
			timeout: 5000,
			success: function(data) {
				if (data == null) {
					success = false;
				} else 	if (data == "1") {
					success = true;
				} else {
					//failed to validate consumer key and secret
					$("#${namespace}tweet-error").html('<fmt:message key="config_error_register" />').show();
					success = false;
				}
			},
			error: function(xhr, status) {
				//failed
				$("#${namespace}tweet-error").html('<fmt:message key="config_error_general" />').show();
				success = false;
			}
		});
		
		return success;
	});
	*/
	
	$("#${namespace}form").validate();

	
});

</script>

<div class="tweetal">

<div id="${namespace}tweet-error" class="portlet-msg-error" style="display:none;"></div>


	<div id="${namespace}update-key" class="tweetal-config">	

		<form method="POST" id="<portlet:namespace/>form" name="<portlet:namespace/>form" action="<portlet:actionURL />">
                        
                        <fieldset>
                                <legend><fmt:message key="config_heading" /></legend>
                                
                                <c:if test="${not empty errorMessage}">
                                        <p class="portlet-msg-error">${errorMessage}</p>
                                </c:if>
                                
                                <p>
    				<label for="${namespace}consumerKey"><fmt:message key="enter_consumer_key" /></label>
    				<em>*</em><input id="${namespace}consumerKey" type="text" name="consumerKey" value="${consumerKey}" class="required" style="width: 15em;" minlength="10"/>
                                </p>
                                <p>
                                <label for="${namespace}consumerSecret"><fmt:message key="enter_consumer_secret" /></label>
    				<em>*</em><input id="${namespace}consumerSecret" type="text" name="consumerSecret" value="${consumerSecret}" class="required" style="width: 30em;" minlength="10"/>
                                </p>
                                <p>
    				<input id="${namespace}keySubmit" class="submit" type="submit" name="operation" value="Save"/>
    				<input id="${namespace}keyCancel" class="cancel" type="submit" name="operation" value="Cancel"/>
                                </p>
                        </fieldset>
		</form>
	</div>
</div>