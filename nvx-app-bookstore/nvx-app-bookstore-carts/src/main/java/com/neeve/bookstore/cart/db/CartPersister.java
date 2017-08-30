/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * Copyright Version 1.0
 */
package com.neeve.bookstore.cart.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.eaio.uuid.UUID;

import com.neeve.trace.Tracer;

import com.neeve.service.cdc.DbEntityPersister;
import com.neeve.service.cdc.main.DbPersister;
import com.neeve.bookstore.cart.service.repository.Cart;

/**
 * The Bookstore cart persister
 */
final public class CartPersister extends DbEntityPersister {
    final private static String CART_TABLE_NAME = "CARTS";
    final private static String CART_ID_INDEX_NAME = "CART_ID_IDX";
    final private static String CART_STOREID_INDEX_NAME = "CART_STOREID_IDX";
    final private static String CART_ID_FIELD_NAME = "ID";
    final private static String CART_STOREID_FIELD_NAME = "STOREID";

    final private static int CART_STOREID_FIELD_MAX_LENGTH = 36;

    private PreparedStatement _insertCart;
    private PreparedStatement _updateCart;
    private PreparedStatement _deleteCart;
    private PreparedStatement _selectCart;
    private PreparedStatement _cartExists;
    private PreparedStatement _selectCartsByStoreId;

    CartPersister(final DbPersister persister) {
        super(persister);
    }

    /**
     * create Cart Table
     * @return
     */
    final private String createCartsTableStatement() {
        return String.format("CREATE TABLE %s(%s %s, %s %s)",
                             CART_TABLE_NAME,
                             CART_STOREID_FIELD_NAME,
                             "VARCHAR2(36)",
                             CART_ID_FIELD_NAME,
                             "NUMERIC(38,0)");
    }

    /**
     * This method will create the Cart table.
     * @param sb
     * @return
     * @throws Exception
     */
    final private boolean createCartTable(final StringBuilder sb) throws Exception {
        sb.append(CART_TABLE_NAME);
        if (!tableExists(CART_TABLE_NAME)) {
            if (tracer().debug) tracer().log(createCartsTableStatement(), Tracer.Level.DEBUG);
            _rdbmsConnection.createStatement().execute(createCartsTableStatement());
            return true;
        }
        return false;
    }

    /**
     * create Cart Table Id Index
     * @return
     */
    final private String createCartsTableIdIndexStatement() {
        return String.format("CREATE INDEX %s on %s(%s)",
                             CART_ID_INDEX_NAME,
                             CART_TABLE_NAME,
                             CART_ID_FIELD_NAME);
    }

    /**
     * This method will create the Cart Id Index.
     * @param sb
     * @return
     * @throws Exception
     */
    final private boolean createCartsTableIdIndex(final StringBuilder sb) throws Exception {
        sb.append(CART_ID_INDEX_NAME);
        if (!indexExists(CART_TABLE_NAME, CART_ID_INDEX_NAME)) {
            _rdbmsConnection.createStatement().execute(createCartsTableIdIndexStatement());
            return true;
        }
        return false;
    }

    /**
     * create Cart Table StoreId Index
     * @return
     */
    final private String createCartsTableStoreIdIndexStatement() {
        return String.format("CREATE INDEX %s on %s(%s)",
                             CART_STOREID_INDEX_NAME,
                             CART_TABLE_NAME,
                             CART_STOREID_FIELD_NAME);
    }

    /**
     * This method will create the Cart StoreId Index.
     * @param sb
     * @return
     * @throws Exception
     */
    final private boolean createCartsTableStoreIdIndex(final StringBuilder sb) throws Exception {
        sb.append(CART_STOREID_INDEX_NAME);
        if (!indexExists(CART_TABLE_NAME, CART_STOREID_INDEX_NAME)) {
            _rdbmsConnection.createStatement().execute(createCartsTableStoreIdIndexStatement());
            return true;
        }
        return false;
    }

    /**
     * insert statement for cart table.
     * @return
     * @throws Exception
     */
    final private String createCartInsertStatement() throws Exception {
        return String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                             CART_TABLE_NAME,
                             CART_STOREID_FIELD_NAME,
                             CART_ID_FIELD_NAME);
    }

    /**
     * This method will insert a cart table row into database.
     * @throws Exception
     */
    final private void createCartInsertPreparedStatement() throws Exception {
        _insertCart = _rdbmsConnection.prepareStatement(createCartInsertStatement());
    }

    /**
     * update statement for cart table.
     * @return
     * @throws Exception
     */
    final private String createCartUpdateStatement() throws Exception {
        return String.format("UPDATE %s SET %s=? where %s=?",
                             CART_TABLE_NAME,
                             CART_ID_FIELD_NAME,
                             CART_STOREID_FIELD_NAME);
    }

    /**
     * This method will update a cart table row into database.
     * @throws Exception
     */
    final private void createCartUpdatePreparedStatement() throws Exception {
        _updateCart = _rdbmsConnection.prepareStatement(createCartUpdateStatement());
    }

    /**
     * delete statement for cart table.
     * @return
     * @throws Exception
     */
    final private String createCartDeleteStatement() throws Exception {
        return String.format("DELETE FROM %s WHERE %s=?",
                             CART_TABLE_NAME,
                             CART_STOREID_FIELD_NAME);
    }

    /**
     * This method will delete a cart table row from database.
     * @throws Exception
     */
    final private void createCartDeletePreparedStatement() throws Exception {
        _deleteCart = _rdbmsConnection.prepareStatement(createCartDeleteStatement());
    }

    // select a particular cart table based on id.
    final private String createCartSelectStatement() {
        return String.format("SELECT * FROM %s WHERE %s = ?",
                             CART_TABLE_NAME,
                             CART_ID_FIELD_NAME);
    }

    /**
     * This method will select cart table row.
     * @throws Exception
     */
    final private void createCartSelectPreparedStatement() throws Exception {
        _selectCart = _rdbmsConnection.prepareStatement(createCartSelectStatement());
    }

    // select a particular cart table based on id.
    final private String createCartSelectByStoreIdStatement() {
        return String.format("SELECT * FROM %s WHERE %s = ?",
                             CART_TABLE_NAME,
                             CART_STOREID_FIELD_NAME);
    }

    /**
     * This method will select cart table row.
     * @throws Exception
     */
    final private void createCartSelectByStoreIdPreparedStatement() throws Exception {
        _selectCartsByStoreId = _rdbmsConnection.prepareStatement(createCartSelectByStoreIdStatement());
    }

    final private String createCartExistsStatement() {
        return String.format("SELECT COUNT(*) AS N1 FROM %s WHERE %s = ?",
                             CART_TABLE_NAME,
                             CART_STOREID_FIELD_NAME);
    }

    final private void createCartExistsPreparedStatement() throws Exception {
        _cartExists = _rdbmsConnection.prepareStatement(createCartExistsStatement());
    }

    @Override
    final protected void createTables(final boolean createIndexes) throws Exception {
        StringBuilder sb;

        // Cart Table
        sb = new StringBuilder("...");
        sb.append(createCartTable(sb) ? "...created" : "...exists");
        tracer().log(sb.toString(), Tracer.Level.INFO);
        if (createIndexes) {
            sb = new StringBuilder("...");
            sb.append(createCartsTableIdIndex(sb) ? "...created" : "...exists");
            tracer().log(sb.toString(), Tracer.Level.INFO);
            sb = new StringBuilder("...");
            sb.append(createCartsTableStoreIdIndex(sb) ? "...created" : "...exists");
            tracer().log(sb.toString(), Tracer.Level.INFO);
        }
    }

    @Override
    final protected void createPreparedStatements() throws Exception {
        createCartInsertPreparedStatement();
        createCartUpdatePreparedStatement();
        createCartDeletePreparedStatement();
        createCartSelectPreparedStatement();
        createCartExistsPreparedStatement();
        createCartSelectByStoreIdPreparedStatement();
    }

    @Override
    final protected void closePreparedStatements() throws SQLException {
        _insertCart.close();
        _updateCart.close();
        _deleteCart.close();
        _selectCart.close();
        _cartExists.close();
        _selectCartsByStoreId.close();
    }

    final private boolean cartExists(final String storeId) throws Exception {
        _cartExists.setString(1, storeId);
        ResultSet rs = _cartExists.executeQuery();
        try {
            return rs.next() && rs.getInt("N1") > 0;
        }
        finally {
            rs.close();
        }
    }

    /**
     * Insert into cart table
     * @param cart
     * @throws Exception
     */
    final private void insertCart(final Cart cart) throws Exception {
        if (cart != null) {
            _insertCart.setString(1, cart.getId().toString());
            _insertCart.setLong(2, cart.getCartId());
            _insertCart.execute();

            if (tracer().debug) tracer().log("...inserted cart , id=" + cart.getCartId(), Tracer.Level.DEBUG);
        }
    }

    /**
     * Update cart table
     * @param cart
     * @throws Exception
     */
    final private void updateCart(final Cart cart) throws Exception {
        if (cart != null) {
            _updateCart.setLong(1, cart.getCartId());
            _updateCart.setString(2, cart.getId().toString());
            _updateCart.execute();

            if (tracer().debug) tracer().log("...updated cart , id=" + cart.getCartId(), Tracer.Level.DEBUG);
        }
    }

    /**
     * This method will call the update/insert method of corresponding object.
     */
    final public void update(Cart cart) throws Exception {
        if (cartExists(cart.getId().toString())) {
            updateCart(cart);
        }
        else {
            insertCart(cart);
        }
    }

    public int delete(final String storeId) throws Exception {
        if (storeId != null) {
            _deleteCart.setString(1, storeId);
            final int count = _deleteCart.executeUpdate();
            if (count > 0 && tracer().debug) {
                tracer().log("...deleted cart -- store id=" + storeId, Tracer.Level.DEBUG);
            }
            return count;
        }
        return 0;
    }

    final private Cart getCart(final ResultSet rs) throws SQLException {
        final String storeId = rs.getString(CART_STOREID_FIELD_NAME);
        final Cart cart = Cart.create(new UUID(storeId));
        cart.setCartId(rs.getLong(CART_ID_FIELD_NAME));
        return cart;
    }

    public Cart read(final String id) throws Exception {
        _selectCart.setString(1, id);
        ResultSet rs = _selectCart.executeQuery();
        try {
            if (rs.next()) {
                return getCart(rs);
            }
        }
        finally {
            rs.close();
        }
        return null;
    }

    public List<Cart> readByStoreId(final String storeId) throws Exception {
        List<Cart> carts = new ArrayList<Cart>();
        _selectCartsByStoreId.setString(1, storeId);
        ResultSet rs = _selectCartsByStoreId.executeQuery();
        try {
            while (rs.next()) {
                carts.add(getCart(rs));
            }
        }
        finally {
            rs.close();
        }
        return carts;
    }

    // validations
    /**
     * Validate a Cart against persistence constraints
     */
    final static void validateConstraints(final Cart cart) {
    }

    final public void printStatements() throws Exception {
        // create
        System.out.println("Cart: create statements...");
        System.out.println("\n" + createCartsTableStatement() + ";");
        System.out.println(createCartsTableIdIndexStatement() + ";");
        System.out.println(createCartsTableStoreIdIndexStatement() + ";");
        System.out.println();

        // insert/update/delete/exists
        System.out.println("Cart: insert/update/delete/exists/other statements...");

        System.out.println("\n" + createCartSelectStatement());
        System.out.println(createCartSelectByStoreIdStatement());
        System.out.println(createCartInsertStatement());
        System.out.println(createCartUpdateStatement());
        System.out.println(createCartDeleteStatement());
        System.out.println(createCartExistsStatement());
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        new CartPersister(null).printStatements();
    }
}
