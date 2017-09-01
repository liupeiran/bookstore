/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bookstore;

/**
 *
 * @author peiran
 */
public class Book {
    public String name;
    public int inventory;
    public Book(String name, int inventory) {
        this.name = name;
        this.inventory = inventory;
    }
    public String getName() {
        return name;
    }
    public void setName(String value) {
        name = value;
    }
    public int getInventory() {
        return inventory;
    }
    public void setInventory (int value) {
        inventory = value;
    }
}
