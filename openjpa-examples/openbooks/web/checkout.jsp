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
<!--      This JSP page demonstrates usage of OpenBookService to purchase Books.       -->
<!-- ===============================================================================================      -->
<%@page import="openbook.server.OpenBookService"%>
<%@page import="openbook.domain.Book"%>
<%@page import="openbook.domain.ShoppingCart"%>
<%@page import="openbook.domain.PurchaseOrder"%>
<%@page import="openbook.domain.LineItem"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="openbook.util.JSPUtility"%>

<%@include file="header.jsp"%>



<div id="content" style="display: block">
<h2>Thank you for buying books from OpenBooks!</h2>
<% 
   OpenBookService service = (OpenBookService)session.getAttribute(KEY_SERVICE); 
   ShoppingCart cart = (ShoppingCart)session.getAttribute(KEY_CART);
   PurchaseOrder order = service.placeOrder(cart);
   
%>
Order : <%= order.getId() %> <br>
Placed on <%= JSPUtility.format(order.getPlacedOn()) %>

<table border="0">
  <caption><%= order.getItems().size() %> items</caption>
  <thead>
    <tr>
      <th>Title</th> <th>Price</th> <th>Quantity</th>
    </tr>
  </thead>
  <tfoot>
    <tr>
      <td><A HREF="<%= PAGE_SEARCH %>">Continue Shopping</A></td>
    </tr>
  </tfoot>
  <tbody>
<%
  int i = 0;
  List<LineItem> items = order.getItems();
  for (LineItem item : items) {
%>
   <TR style="<%= JSPUtility.getRowStyle(i++) %>">
      <TD> <%= item.getBook().getTitle() %> </TD>
      <TD> <%= item.getQuantity() %> </TD>
      <TD> <%= JSPUtility.format(item.getBook().getPrice() * item.getQuantity()) %> </TD>
   </TR>
<%
  }
%>
  <TR>
  <TD>Total</TD><TD><%= JSPUtility.format(order.getTotal()) %></TD>
  </TR>
  </tbody>
</table>


</div>
<%@include file="footer.jsp"%>
