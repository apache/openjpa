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
<%-- ====================================================================  --%>
<%-- Executes a query which is determined by the request parameter.        --%>
<%-- Displays the results                                                  --%>
<%-- ====================================================================  --%>
<%@include file="header.jsp"%>


<%@page import="openbook.server.OpenBookService"%>
<%@page import="openbook.domain.Book"%>
<%@page import="openbook.domain.Author"%>
<%@page import="openbook.util.JSPUtility"%>
<%@page import="java.util.List"%>

<div id="content" style="display: block">
<%!
     /**
      * Concatenates the names of the given list of Authors.
      *
      */
     public static String namesOf(List<Author> authors) {
       StringBuilder names = new StringBuilder();
       if (authors == null)
           return names.toString();
       for (Author a : authors) {
           if (names.length() != 0) names.append(", ");
           names.append(a.getName());
       }
       return names.toString();
   }
%>


<% 
   OpenBookService service = (OpenBookService)session.getAttribute(KEY_SERVICE); 
   if (service == null) {
%>
       <jsp:forward page="<%= PAGE_HOME %>"></jsp:forward>
<%
   }
%>

<%
  String title    = request.getParameter(FORM_TITLE);
  Double minPrice = JSPUtility.toDouble(request.getParameter(FORM_PRICE_MIN));
  Double maxPrice = JSPUtility.toDouble(request.getParameter(FORM_PRICE_MAX));
  String author   = request.getParameter(FORM_AUTHOR);
  
  List<Book> books = service.select(title, minPrice, maxPrice, author);
  String query = service.getQuery(title, minPrice, maxPrice, author);
%>
The query executed on the server was <br>
<pre><%= query %></pre>
<br>
This query selected <%= books.size() %> books.<br>
<table border="0">
  <thead>
    <tr>
      <th>ISBN</th> <th>Title</th> <th>Price</th> <th>Authors</th> 
      <th><img src="images/Add2Cart.jpg" border="0" width="20px" height="20px"></th>
    </tr>
  </thead>
  <tfoot>
    <tr>
      <td><A HREF="<%= PAGE_SEARCH %>">Search again</A></td>
    </tr>
  </tfoot>
  <tbody>
<%
  int i = 0;
  for (Book book : books) {
      session.setAttribute(book.getISBN(), book);
%>
   <TR style="<%= JSPUtility.getRowStyle(i++) %>">
      <TD> <%= book.getISBN() %> </TD>
      <TD> <%= book.getTitle() %> </TD>
      <TD> <%= JSPUtility.format(book.getPrice()) %> </TD>
      <TD> <%= namesOf(book.getAuthors()) %> </TD>
      <TD> <A HREF="<%= JSPUtility.encodeURL(PAGE_CART, 
             KEY_ACTION, ACTION_ADD, KEY_ISBN, book.getISBN()) %>">
             Add to Cart</A></TD>
   </TR>
<%
  }
%>
  </tbody>
</table>

</div>
<%@include file="footer.jsp"%>
