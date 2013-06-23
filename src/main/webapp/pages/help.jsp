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

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>

<portlet:defineObjects /> 
<fmt:setBundle basename="au.edu.anu.portal.portlets.tweetal.util.messages" />

<h4>Copyright� 2010 - Twitter Portlet for uPortal (tweetal)</h4>

<br/>

<input type="BUTTON" value="<fmt:message key="edit_back" />" ONCLICK="window.location.href='${viewUrl}'"/>
