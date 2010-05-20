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
<!-- ===============================================================================================      -->
<!--      This JSP page demonstrates usage of OpenBookService to browse, select and purchase Books.       -->
<!-- ===============================================================================================      -->
<%@page import="openbook.server.OpenBookService"%>
<%@page import="openbook.domain.Book"%>

<%@include file="header.jsp"%>

<div id="content" style="display: block">

<% 
   OpenBookService service = (OpenBookService)session.getAttribute(KEY_SERVICE); 
   if (service == null) {
%>
       <jsp:forward page="<%= PAGE_HOME %>"></jsp:forward>
<%
   }
%>
OpenBooks database contains <%= service.count(Book.class) %> books.
<br>
Fill in the details for a book you are searching for. 
<br>
You can leave one, more or all fields empty.
<hr>
<form method="GET" action="<%= PAGE_BOOKS %>">
  Title : <br> <input type="text" name="<%= FORM_TITLE %>"><br>
  Author: <br> <input type="text" name="<%= FORM_AUTHOR %>"><br>
  Price : <br> from <input type="text" name="<%= FORM_PRICE_MIN %>"> to 
  <input type="text" name="<%= FORM_PRICE_MAX %>"><br>
  <hr>
<input type="submit" value="Search">
</form>
</div>
<%@include file="footer.jsp"%>
