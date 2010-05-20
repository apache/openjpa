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
<%@page import="openbook.domain.Customer"%>
<%@page import="openbook.domain.ShoppingCart"%>
<%@page import="java.util.Map"%>
<%@page import="openbook.util.JSPUtility"%>

<%@include file="header.jsp"%>

<div id="content" style="display: block">

<% 
   OpenBookService service = (OpenBookService)session.getAttribute(KEY_SERVICE); 
   Customer customer = (Customer)session.getAttribute(KEY_USER);
   ShoppingCart cart = (ShoppingCart)session.getAttribute(KEY_CART);
   if (ACTION_ADD.equals(request.getParameter(KEY_ACTION))) {
       String isbn = request.getParameter(KEY_ISBN);
       Book book = (Book)session.getAttribute(isbn);  
       cart.addItem(book, 1);
   }
   if (cart.isEmpty()) {
%>
   <h3><%= customer.getName() %>, your Shopping Cart is empty.</h3><br>
   <A HREF="<%= PAGE_SEARCH %>">Continue Shopping</A>
<%    
   } else {
%>


<table border="0">
  <caption><%= customer.getName() %>, your Shopping Cart has <%= cart.getTotalCount() %> books</caption>
  <thead>
    <tr>
      <th>Title</th> <th>Price</th> <th>Quantity</th>
    </tr>
  </thead>
  <tfoot>
    <tr>
      <td><A HREF="<%= PAGE_SEARCH %>">Continue Shopping</A></td>
      <td><A HREF="<%= PAGE_CHECKOUT %>">Proceed to CheckOut</A></td>
    </tr>
  </tfoot>
  <tbody>
<%
   }
   Map<Book,Integer> books = cart.getItems();
   int i = 0;
   for (Book b : books.keySet()) {
%>
   <TR style="<%= JSPUtility.getRowStyle(i++) %>">
      <TD> <%= b.getTitle() %> </TD>
      <TD> <%= JSPUtility.format(b.getPrice()) %> </TD>
      <TD> <%= books.get(b) %> </TD>
   </TR>
<%
   }
%>
  </tbody>
</table>


</div>
<%@include file="footer.jsp"%>
