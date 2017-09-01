/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bookstore;
import java.sql.*;
import java.util.Collection;
import java.util.ArrayList;

/**
 *
 * @author peiran
 */
public class BooksModel {
    
    public void add(Book book) {
        Connection myConn = null;
        PreparedStatement prpStmt = null;
        try {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                myConn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/bookstore",
                        "root", "root");
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            String query = "INSERT INTO books (name, inventory) values (?, ?)";
            prpStmt = myConn.prepareStatement(query);
            prpStmt.setString(1, book.getName());
            prpStmt.setInt(2, book.getInventory());
            prpStmt.execute();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (prpStmt != null) {
                try {
                    prpStmt.close();
                } catch (Exception e) {                    
                }
            }
            if (myConn != null) {
                try {
                    myConn.close();
                } catch (Exception e) {                    
                }
            }
        }

    }

    public Collection<Book> getBooks() {
        Connection myConn = null;
        Statement myStmt = null;
        ResultSet myRs = null;
        ArrayList<Book> books = new ArrayList<>(); 
        try {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                myConn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/bookstore",
                        "root", "root");
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            myStmt = myConn.createStatement();
            myRs = myStmt.executeQuery("SELECT name, inventory FROM books");
            while (myRs.next()) {
                books.add(
                        new Book(
                                myRs.getString("name"), 
                                myRs.getInt("inventory"))
                        );
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myRs != null) {
                try {
                    myRs.close();
                } catch (Exception e) {                    
                }
            }
            if (myStmt != null) {
                try {
                    myStmt.close();
                } catch (Exception e) {                    
                }
            }
            if (myConn != null) {
                try {
                    myConn.close();
                } catch (Exception e) {                    
                }
            }
        }
        return books;
    } 
            
}
