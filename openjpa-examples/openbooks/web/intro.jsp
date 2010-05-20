<%-- 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
--%>

<%@include file="header.jsp"%>

<div id="content" style="display: block">
<b>OpenBooks</b> is a sample web application demonstrating some new features in JPA 2.0 API.
<br>
Please enter your name below to start a OpenBooks session.
<br> 
<% 
    Object service = session.getAttribute(KEY_SERVICE);
    if (service == null) {
%>
      <form method="get" action="<%= PAGE_LOGIN %>">
        Your Name :<br> <input type="text" name="<%= KEY_USER %>" size="40">  <br>
        <input type="SUBMIT" value="Enter">
      </form>
<%
    } 
%>
</div>

<%@include file="footer.jsp"%>
