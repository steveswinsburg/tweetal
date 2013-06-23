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

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ taglib prefix="rs" uri="http://www.jasig.org/resource-server" %>


<portlet:defineObjects /> 
<fmt:setBundle basename="au.edu.anu.portal.portlets.tweetal.util.messages" />

<script type="text/javascript" src="<rs:resourceURL value="/rs/jquery/1.4.2/jquery-1.4.2.min.js" />"></script>


<!-- Set the namespace variable -->
<c:set var="namespace"><portlet:namespace/></c:set>


<script>
var Tweetal${namespace} = Tweetal${namespace} || {};
Tweetal${namespace}.jQuery = jQuery.noConflict(true);

Tweetal${namespace}.jQuery(function(){

	var loader = "<img src='/tweetal/images/ajax-loader-small.gif' />";
	
	var $ = Tweetal${namespace}.jQuery;

	//disable pin field until they click the button
	$("#${namespace}authCode").attr('disabled', true);

	//fn() bind events
	Tweetal${namespace}.bindEvents = function() {

		//open twitter window
		$("#${namespace}authButton").click(function(event) {
			//enable  field
			$("#${namespace}authCode").attr('disabled', false);
			$("#${namespace}tweetError").hide();
			window.open('${authUrl}');
		});

		//form submission
		/*
		$("#authSubmit").click(function(event) {

			var code = $("#authCode").val();

		});
		*/

	}

	Tweetal${namespace}.bindEvents();
});
</script>

<div class="tweetal">

	<c:if test="${not empty errorMessage}">
		<div id="${namespace}tweetError" class="portlet-msg-error"> 
		<c:out value="${errorMessage}" />
		</div>
	</c:if>

	<c:choose>
		<c:when test="${not empty screenName}">
			<h3><fmt:message key="heading_unlink" /></h3>
		
			<fmt:message key="text_linked" />&nbsp;<b>${screenName}</b>
			<form method="POST" id="${namespace}remove" action="<portlet:actionURL />">
				<input type="hidden" name="operation" value="remove"/>
				
				<br />
				<div>
					<button type="submit"><fmt:message key="action_unlink" /></button>
					<a href="${viewUrl}"><fmt:message key="action_back" /></a>
				</div>
				
			</form>
		</c:when>

		<c:otherwise>
			<h3><fmt:message key="heading_link" /></h3>
			<form method="POST" id="${namespace}save" action="<portlet:actionURL />">
				<input type="hidden" name="operation" value="save" />
				<table>
					<tr>
						<td> 
							<fmt:message key="step_1" />
						</td>
						<td>
							<button type="button" id="${namespace}authButton"><fmt:message key="get_auth_code" /></button>
						</td>
					</tr>
					<tr>
						<td> 
							<fmt:message key="step_2" />
						</td>
						<td>
							<fmt:message key="enter_auth_code" />
							<input type="text" id="${namespace}authCode" name="authCode" />
						</td>
					</tr>
					<tr>
						<td> 
							<fmt:message key="step_3" />
						</td>
						<td>
							<button type="submit" id="${namespace}authSubmit"><fmt:message key="action_save" /></button>
							<a href="${viewUrl}"><fmt:message key="action_back" /></a>
						</td>
					</tr>
				</table>
			</form>
			
		</c:otherwise>
	</c:choose>
</div>

