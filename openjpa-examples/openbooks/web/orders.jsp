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
<%@page import="openbook.domain.PurchaseOrder"%>
<%@page import="openbook.domain.LineItem"%>
<%@page import="openbook.util.JSPUtility"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>

<%@include file="header.jsp"%>

<div id="content" style="display: block">

<% 
   OpenBookService service = (OpenBookService)session.getAttribute(KEY_SERVICE); 
   if (service == null) {
%>
       <jsp:forward page="<%= PAGE_HOME %>"></jsp:forward>
<%
   }
   if (ACTION_DELIVER.equals(request.getParameter(KEY_ACTION))) {
       String oid = request.getParameter(KEY_OID);
       PurchaseOrder order = (PurchaseOrder)session.getAttribute(oid);
       service.deliver(order);
   }
   
   Customer customer = (Customer)session.getAttribute(KEY_USER);
   List<PurchaseOrder> orders = service.getOrders(null, customer);
%>
All <%= orders.size() %> Purchase Order placed by <%= customer.getName() %> is shown.
The <code>Deliver</code> button will deliver the pending order. Delivery of a pending
order <br>
<OL>
<LI>decrements inventory of all associated LineItems</LI>
<LI>changes status to ORDERED and finally </LI>
<LI>nullifies the association between Purchase Order and Line Items. Nullifying the
association has the important side-effect of deleting the line items from the database
because Purchase Order and Line Items relation is annotated as orphan delete.</LI>
</OL>
<br>
  
<table border="0">
  <caption><%= customer.getName() %> placed <%= orders.size() %> orders</caption>
  <thead>
    <tr>
      <th>ID</th> <th>Total</th> <th>Placed On</th> <th>Status</th> <th>Delivered On</th> 
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
  for (PurchaseOrder order : orders) {
      session.setAttribute(""+order.getId(), order);
%>
   <TR style="<%= JSPUtility.getRowStyle(i++) %>">
      <TD> <A HREF="<%= 
          JSPUtility.encodeURL(PAGE_ORDERS, 
              KEY_ACTION, ACTION_DETAILS, 
              KEY_OID, order.getId()) %>"> <%= order.getId() %></A></TD>
      <TD> <%= order.getTotal() %> </TD>
      <TD> <%= JSPUtility.format(order.getPlacedOn()) %> </TD>
      <TD> <%= order.getStatus() %> </TD>
<% 
    if (order.getStatus().equals(PurchaseOrder.Status.PENDING)) {
%>        
      <TD>  </TD>
      <TD> <A HREF="<%= 
          JSPUtility.encodeURL(PAGE_ORDERS, KEY_ACTION, ACTION_DELIVER, 
                  KEY_OID, order.getId()) %>">Deliver</A></TD>
<%
    } else {
%>
      <TD> <%= JSPUtility.format(order.getDeliveredOn()) %> </TD>
      <TD> </TD>
<%        
    }
%>
   </TR>
<%
  }
%>
  </tbody>
</table>


<%
  if (ACTION_DETAILS.equals(request.getParameter(KEY_ACTION))) {
      String oid = request.getParameter(KEY_OID);
      PurchaseOrder order = (PurchaseOrder)session.getAttribute(oid);
      List<LineItem> items = order.getItems();
      if (items != null && order.getStatus().equals(PurchaseOrder.Status.PENDING)) {
%>
<table border="0">
  <caption><%= items.size() %> line items of Order <%= order.getId() %></caption>
  <thead>
    <tr>
      <th>Title</th> <th>Price</th> <th>Quantity</th> <th>Cost</th>
    </tr>
  </thead>
  <tbody>
<%
  int j = 0;
  for (LineItem item : items) {
%>
   <TR style="<%= JSPUtility.getRowStyle(j++) %>">
      <TD> <%= item.getBook().getTitle() %> </TD>
      <TD> <%= JSPUtility.format(item.getBook().getPrice()) %> </TD>
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
<%
      }
  }
%>
       


</div>


<%@include file="footer.jsp"%>
