<%-- 
    Document   : BookStorePage
    Created on : 26-Aug-2017, 4:28:53 PM
    Author     : peiran
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.*,bookstore.Book"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Book Store</title>
    </head>
    <body>
        <form method="POST" action="books">
            Name: <input type ="text" name="name" />
            Inventory: <input type ="text" name="inventory" />
            <input type="submit"  value="Add" />
        </form>
        <hr><ol> <%
            @SuppressWarnings("unchecked")
            List<Book> books = (List<Book>)request.getAttribute("books");
            if (books != null) {
                for (Book book : books) { %>
                    <li> <%= book.getName()%> </li> 
                    <li> <%= book.getInventory()%> </li><%
                }
            } %>
        <h1>Hello World!</h1>
    </body>
</html>
