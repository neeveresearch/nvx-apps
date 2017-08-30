package com.neeve.bookstore.cart.service;

import com.neeve.ci.XRuntime;
import com.neeve.bookstore.cart.service.messages.AddBookRequest;
import com.neeve.service.messages.Credentials;
import com.neeve.bookstore.cart.service.messages.CreateCartRequest;
import com.neeve.bookstore.cart.service.messages.GetCartRequest;
import com.neeve.bookstore.cart.service.messages.RemoveBookRequest;
import com.neeve.util.UtlProps;

final public class Driver {
    final public static void main(final String args[]) throws InterruptedException {
        final Client client = new Client("carts-driver");
        Credentials credentials = Credentials.create();
        credentials.setUsername("Tsomething");
        credentials.setPassword("doesntmatter");
        client.open(credentials);

        final int numThreads = (int) UtlProps.getValue(XRuntime.getProps(), "bookstore.driver.numthreads", 1);
        final int addBookInterval = (int) UtlProps.getValue(XRuntime.getProps(), "bookstore.driver.addbook.interval", 100);
        final int createCartInterval = (int) UtlProps.getValue(XRuntime.getProps(), "bookstore.driver.createcart.interval", 100);
        final int numCarts = (int) UtlProps.getValue(XRuntime.getProps(), "bookstore.driver.numcarts", 1000);

        final Thread[] threads = new Thread[numThreads];
        for (int t = 0; t < threads.length; t++) {
            final int num = t;
            threads[t] = new Thread() {
                public void run() {
                    for (int i = 0; i < numCarts; i++) {
                        try {
                            final long cartId = client.createCart(CreateCartRequest.create()).getCartId();
                            System.out.println(num + ": Created cart (id=" + cartId + ")");
                            String isbn = null;
                            for (int j = 0; j < 2; j++) {
                                final AddBookRequest addBookRequest = AddBookRequest.create();
                                addBookRequest.setCartId(cartId);
                                addBookRequest.setTitle("book " + cartId + "-" + j);
                                System.out.println(num + ":   ...added book to cart (isbn=" + (isbn = client.addBook(addBookRequest).getIsbn()) + ")");
                                Thread.currentThread().sleep(addBookInterval);
                            }
                            if (i % 3 == 0) {
                                final RemoveBookRequest removeBookRequest = RemoveBookRequest.create();
                                removeBookRequest.setCartId(cartId);
                                removeBookRequest.setIsbn(isbn);
                                client.removeBook(removeBookRequest);
                                System.out.println(num + ":   ...removed book '" + isbn + "' from cart '" + cartId + "'");
                            }

                            // get cart
                            final GetCartRequest getRequest = GetCartRequest.create();
                            getRequest.setCartId(cartId);
                            final int numBooks = client.getCart(getRequest).getBooksEmptyIfNull().length;
                            System.out.println(num + ":   ...fetched cart " + cartId + " - #books = " + numBooks);
                        }
                        catch (Throwable e) {
                            e.printStackTrace();
                        }
                        finally {
                            try {
                                Thread.currentThread().sleep(createCartInterval);
                            }
                            catch (InterruptedException ex) {
                            }
                        }
                    }
                }
            };
            threads[t].start();
        }
        Thread.sleep(5000);
        for (int t = 0; t < threads.length; t++) {
            threads[t].join();
        }
    }
}
