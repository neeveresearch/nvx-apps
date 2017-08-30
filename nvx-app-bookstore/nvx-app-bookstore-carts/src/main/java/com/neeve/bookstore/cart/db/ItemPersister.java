/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * Copyright Version 1.0
 */
package com.neeve.bookstore.cart.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.neeve.trace.Tracer;
import com.neeve.service.EServiceException;
import com.neeve.service.entities.ErrorType;
import com.neeve.service.entities.ErrorCode;

import com.neeve.service.cdc.DbEntityPersister;
import com.neeve.service.cdc.main.DbPersister;
import com.neeve.bookstore.cart.service.repository.Item;

/**
 * The Bookstore cart item persister
 */
final public class ItemPersister extends DbEntityPersister {
    final private static String ITEM_TABLE_NAME = "ITEMS";
    final private static String ITEM_CARTID_INDEX_NAME = "ITEM_CARTID_IDX";
    final private static String ITEM_STOREID_INDEX_NAME = "ITEM_STOREID_IDX";
    final private static String ITEM_STOREID_FIELD_NAME = "STOREID";
    final private static String ITEM_CARTID_FIELD_NAME = "CARTID";
    final private static String ITEM_ISBN_FIELD_NAME = "ISBN";
    final private static String ITEM_TITLE_FIELD_NAME = "TITLE";

    final private static int ITEM_ISBN_FIELD_MAX_LENGTH = 64;
    final private static int ITEM_TITLE_FIELD_MAX_LENGTH = 256;

    private PreparedStatement _insertItem;
    private PreparedStatement _updateItem;
    private PreparedStatement _deleteItem;
    private PreparedStatement _itemExists;

    ItemPersister(final DbPersister persister) {
        super(persister);
    }

    /**
     * create item table
     * @return
     */
    final private String createItemsTableStatement() {
        return String.format("CREATE TABLE %s(%s %s, %s %s, %s %s, %s %s)",
                             ITEM_TABLE_NAME,
                             ITEM_STOREID_FIELD_NAME,
                             "VARCHAR2(36)",
                             ITEM_CARTID_FIELD_NAME,
                             "NUMERIC(38,0)",
                             ITEM_ISBN_FIELD_NAME,
                             "VARCHAR2(64)",
                             ITEM_TITLE_FIELD_NAME,
                             "VARCHAR2(256)");
    }

    /**
     * This method will create the item table.
     * @param sb
     * @return
     * @throws Exception
     */
    final private boolean createItemTable(final StringBuilder sb) throws Exception {
        sb.append(ITEM_TABLE_NAME);
        if (!tableExists(ITEM_TABLE_NAME)) {
            if (tracer().debug) tracer().log(createItemsTableStatement(), Tracer.Level.DEBUG);
            _rdbmsConnection.createStatement().execute(createItemsTableStatement());
            return true;
        }
        return false;
    }

    /**
     * create item table cart id Index
     * @return
     */
    final private String createItemsTableCartIdIndexStatement() {
        return String.format("CREATE INDEX %s on %s(%s)",
                             ITEM_CARTID_INDEX_NAME,
                             ITEM_TABLE_NAME,
                             ITEM_CARTID_FIELD_NAME);
    }

    /**
     * This method will create the item cart id Index.
     * @param sb
     * @return
     * @throws Exception
     */
    final private boolean createItemTableCartIdIndex(final StringBuilder sb) throws Exception {
        sb.append(ITEM_CARTID_INDEX_NAME);
        if (!indexExists(ITEM_TABLE_NAME, ITEM_CARTID_INDEX_NAME)) {
            _rdbmsConnection.createStatement().execute(createItemsTableCartIdIndexStatement());
            return true;
        }
        return false;
    }

    /**
     * create item table store id Index
     * @return
     */
    final private String createItemsTableStoreIdIndexStatement() {
        return String.format("CREATE INDEX %s on %s(%s)",
                             ITEM_STOREID_INDEX_NAME,
                             ITEM_TABLE_NAME,
                             ITEM_STOREID_FIELD_NAME);
    }

    /**
     * This method will create the item store id Index.
     * @param sb
     * @return
     * @throws Exception
     */
    final private boolean createItemTableStoreIdIndex(final StringBuilder sb) throws Exception {
        sb.append(ITEM_STOREID_INDEX_NAME);
        if (!indexExists(ITEM_TABLE_NAME, ITEM_STOREID_INDEX_NAME)) {
            _rdbmsConnection.createStatement().execute(createItemsTableStoreIdIndexStatement());
            return true;
        }
        return false;
    }

    /**
     * insert statement for item table.
     * @return
     * @throws Exception
     */
    final private String createItemInsertStatement() throws Exception {
        return String.format("INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)",
                             ITEM_TABLE_NAME,
                             ITEM_STOREID_FIELD_NAME,
                             ITEM_CARTID_FIELD_NAME,
                             ITEM_ISBN_FIELD_NAME,
                             ITEM_TITLE_FIELD_NAME);
    }

    /**
     * This method will insert a item table row into database.
     * @throws Exception
     */
    final private void createItemInsertPreparedStatement() throws Exception {
        _insertItem = _rdbmsConnection.prepareStatement(createItemInsertStatement());
    }

    /**
     * update statement for item table.
     * @return
     * @throws Exception
     */
    final private String createItemUpdateStatement() throws Exception {
        return String.format("UPDATE %s SET %s=?, %s=?, %s=? where %s=?",
                             ITEM_TABLE_NAME,
                             ITEM_CARTID_FIELD_NAME,
                             ITEM_ISBN_FIELD_NAME,
                             ITEM_TITLE_FIELD_NAME,
                             ITEM_STOREID_FIELD_NAME);
    }

    /**
     * This method will update a item table row into database.
     * @throws Exception
     */
    final private void createItemUpdatePreparedStatement() throws Exception {
        _updateItem = _rdbmsConnection.prepareStatement(createItemUpdateStatement());
    }

    /**
     * delete statement for item table.
     * @return
     * @throws Exception
     */
    final private String createItemDeleteStatement() throws Exception {
        return String.format("DELETE FROM %s WHERE %s=?",
                             ITEM_TABLE_NAME,
                             ITEM_STOREID_FIELD_NAME);
    }

    /**
     * This method will delete a item table row from database.
     * @throws Exception
     */
    final private void createItemDeletePreparedStatement() throws Exception {
        _deleteItem = _rdbmsConnection.prepareStatement(createItemDeleteStatement());
    }

    final private String createItemExistsStatement() {
        return String.format("SELECT COUNT(*) AS N1 FROM %s WHERE %s = ?",
                             ITEM_TABLE_NAME,
                             ITEM_STOREID_FIELD_NAME);
    }

    final private void createItemExistsPreparedStatement() throws Exception {
        _itemExists = _rdbmsConnection.prepareStatement(createItemExistsStatement());
    }

    @Override
    final protected void createTables(final boolean createIndexes) throws Exception {
        StringBuilder sb;

        // item table
        sb = new StringBuilder("...");
        sb.append(createItemTable(sb) ? "...created" : "...exists");
        tracer().log(sb.toString(), Tracer.Level.INFO);
        if (createIndexes) {
            sb = new StringBuilder("...");
            sb.append(createItemTableCartIdIndex(sb) ? "...created" : "...exists");
            tracer().log(sb.toString(), Tracer.Level.INFO);
            sb = new StringBuilder("...");
            sb.append(createItemTableStoreIdIndex(sb) ? "...created" : "...exists");
            tracer().log(sb.toString(), Tracer.Level.INFO);
        }
    }

    @Override
    final protected void createPreparedStatements() throws Exception {
        createItemInsertPreparedStatement();
        createItemUpdatePreparedStatement();
        createItemDeletePreparedStatement();
        createItemExistsPreparedStatement();
    }

    @Override
    final protected void closePreparedStatements() throws SQLException {
        _insertItem.close();
        _updateItem.close();
        _deleteItem.close();
        _itemExists.close();
    }

    final private boolean itemExists(final String storeId) throws Exception {
        _itemExists.setString(1, storeId);
        ResultSet rs = _itemExists.executeQuery();
        try {
            return rs.next() && rs.getInt("N1") > 0;
        }
        finally {
            rs.close();
        }
    }

    /**
     * Insert into item table
     * @param item
     * @throws Exception
     */
    final private void insertCart(final Item item) throws Exception {
        if (item != null) {
            _insertItem.setString(1, item.getId().toString());
            _insertItem.setLong(2, item.getCartId());
            _insertItem.setString(3, item.getIsbn());
            _insertItem.setString(4, item.getTitle());
            _insertItem.execute();

            if (tracer().debug) tracer().log("...inserted item -- isbn=" + item.getIsbn(), Tracer.Level.DEBUG);
        }
    }

    /**
     * Update item table
     * @param item
     * @throws Exception
     */
    final private void updateCart(final Item item) throws Exception {
        if (item != null) {
            _updateItem.setLong(1, item.getCartId());
            _updateItem.setString(2, item.getIsbn());
            _updateItem.setString(3, item.getTitle());
            _updateItem.setString(4, item.getId().toString());
            _updateItem.execute();

            if (tracer().debug) tracer().log("...updated item -- isbn=" + item.getIsbn(), Tracer.Level.DEBUG);
        }
    }

    /**
     * This method will call the update/insert method of corresponding object.
     */
    final public void update(Item item) throws Exception {
        if (itemExists(item.getId().toString())) {
            updateCart(item);
        }
        else {
            insertCart(item);
        }
    }

    public int delete(final String storeId) throws Exception {
        if (storeId != null) {
            _deleteItem.setString(1, storeId);
            final int count = _deleteItem.executeUpdate();
            if (count > 0 && tracer().debug) {
                tracer().log("...deleted item -- store id=" + storeId, Tracer.Level.DEBUG);
            }
            return count;
        }
        return 0;
    }

    // validations
    /**
     * Validate a item against persistence constraints
     */
    final static void validateConstraints(final Item item) {
        if (item.getIsbn() != null && item.getIsbn().length() > ITEM_ISBN_FIELD_MAX_LENGTH) {
            throw new EServiceException(ErrorType.Functional,
                    ErrorCode.MalformedRequestDTO,
                    "Item isbn field exceeds length constraint (" + ITEM_ISBN_FIELD_MAX_LENGTH + ")",
                    null);
        }
        if (item.getTitle() != null && item.getTitle().length() > ITEM_TITLE_FIELD_MAX_LENGTH) {
            throw new EServiceException(ErrorType.Functional,
                    ErrorCode.MalformedRequestDTO,
                    "Item title field exceeds length constraint (" + ITEM_TITLE_FIELD_MAX_LENGTH + ")",
                    null);
        }
    }

    final public void printStatements() throws Exception {
        // create
        System.out.println("Item: create statements...");
        System.out.println("\n" + createItemsTableStatement() + ";");
        System.out.println(createItemsTableCartIdIndexStatement() + ";");
        System.out.println(createItemsTableStoreIdIndexStatement() + ";");
        System.out.println();

        // insert/update/delete/exists
        System.out.println("Item: insert/update/delete/exists/other statements...");

        System.out.println("\n" + createItemInsertStatement());
        System.out.println(createItemUpdateStatement());
        System.out.println(createItemDeleteStatement());
        System.out.println(createItemExistsStatement());
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        new ItemPersister(null).printStatements();
    }
}
