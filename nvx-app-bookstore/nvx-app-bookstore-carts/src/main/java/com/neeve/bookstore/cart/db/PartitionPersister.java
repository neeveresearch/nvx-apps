/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * Copyright Version 1.0
 */
package com.neeve.bookstore.cart.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.eaio.uuid.UUID;

import com.neeve.trace.Tracer;

import com.neeve.service.cdc.DbEntityPersister;
import com.neeve.service.cdc.main.DbPersister;
import com.neeve.bookstore.cart.service.repository.Partition;

/**
 * The Bookstore partition persister
 */
final public class PartitionPersister extends DbEntityPersister {
    // partitions table
    final private static String PARTITIONS_TABLE_NAME = "CSPARTITIONS";
    final private static String PARTITIONS_ID_FIELD_NAME = "ID";
    final private static String PARTITIONS_STOREID_FIELD_NAME = "STOREID";
    final private static String PARTITIONS_NEXTCARTID_FIELD_NAME = "NEXTCARTID";

    // prepared statements
    private PreparedStatement _partitionExists;
    private PreparedStatement _insertPartition;
    private PreparedStatement _updatePartition;
    private PreparedStatement _deletePartition;
    private PreparedStatement _selectPartition;

    public PartitionPersister(final DbPersister persister) {
        super(persister);
    }

    final private String createPartitionsTableStatement() {
        return String.format("CREATE TABLE %s(%s %s, %s %s, %s %s)",
                             PARTITIONS_TABLE_NAME,
                             PARTITIONS_ID_FIELD_NAME,
                             "NUMERIC(38,0)",
                             PARTITIONS_STOREID_FIELD_NAME,
                             "VARCHAR(36)",
                             PARTITIONS_NEXTCARTID_FIELD_NAME,
                             "NUMERIC(38,0)");
    }

    final private boolean createPartitionsTable(final StringBuilder sb) throws Exception {
        sb.append(PARTITIONS_TABLE_NAME);
        if (!tableExists(PARTITIONS_TABLE_NAME)) {
            _rdbmsConnection.createStatement().execute(createPartitionsTableStatement());
            return true;
        }
        return false;
    }

    final private String truncatePartitionsTableStatement() {
        return String.format("TRUNCATE TABLE %s", PARTITIONS_TABLE_NAME);
    }

    final private void truncatePartitionsTable() throws Exception {
        if (tableExists(PARTITIONS_TABLE_NAME)) {
            _rdbmsConnection.createStatement().execute(truncatePartitionsTableStatement());
        }
    }

    final private String dropPartitionsTableStatement() {
        return String.format("DROP TABLE %s", PARTITIONS_TABLE_NAME);
    }

    final private void dropPartitionsTable() throws Exception {
        if (tableExists(PARTITIONS_TABLE_NAME)) {
            _rdbmsConnection.createStatement().execute(dropPartitionsTableStatement());
        }
    }

    final private String createPartitionInsertStatement() {
        return String.format("INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)",
                             PARTITIONS_TABLE_NAME,
                             PARTITIONS_ID_FIELD_NAME,
                             PARTITIONS_STOREID_FIELD_NAME,
                             PARTITIONS_NEXTCARTID_FIELD_NAME);
    }

    final private void createPartitionInsertPreparedStatement() throws Exception {
        _insertPartition = _rdbmsConnection.prepareStatement(createPartitionInsertStatement());
    }

    final private String createPartitionUpdateStatement() {
        return String.format("UPDATE %s SET %s=?, %s=? where %s=?",
                             PARTITIONS_TABLE_NAME,
                             PARTITIONS_ID_FIELD_NAME,
                             PARTITIONS_NEXTCARTID_FIELD_NAME,
                             PARTITIONS_STOREID_FIELD_NAME);
    }

    final private void createPartitionUpdatePreparedStatement() throws Exception {
        _updatePartition = _rdbmsConnection.prepareStatement(createPartitionUpdateStatement());
    }

    final private String createPartitionDeleteStatement() {
        return String.format("DELETE FROM %s WHERE %s=?",
                             PARTITIONS_TABLE_NAME,
                             PARTITIONS_STOREID_FIELD_NAME);
    }

    final private void createPartitionDeletePreparedStatement() throws Exception {
        _deletePartition = _rdbmsConnection.prepareStatement(createPartitionDeleteStatement());
    }

    final private String createPartitionExistsStatement() {
        return String.format("SELECT COUNT(*) AS N1 FROM %s WHERE %s = ?",
                             PARTITIONS_TABLE_NAME,
                             PARTITIONS_STOREID_FIELD_NAME);
    }

    final private void createPartitionExistsPreparedStatement() throws Exception {
        _partitionExists = _rdbmsConnection.prepareStatement(createPartitionExistsStatement());
    }

    final private String createPartitionSelectStatement() {
        return String.format("SELECT * FROM %s WHERE %s = ?",
                             PARTITIONS_TABLE_NAME,
                             PARTITIONS_ID_FIELD_NAME);
    }

    final private void createPartitionSelectPreparedStatement() throws Exception {
        _selectPartition = _rdbmsConnection.prepareStatement(createPartitionSelectStatement());
    }

    /*
     * Return if a partition exists with a store id
     */
    final private boolean partitionExistsByStoreId(final String storeId) throws Exception {
        _partitionExists.setString(1, storeId);
        ResultSet rs = _partitionExists.executeQuery();
        try {
            return rs.next() && rs.getInt("N1") > 0;
        }
        finally {
            rs.close();
        }
    }

    /*
     * Insert a partition object into the database
     */
    final private void insertPartition(final Partition partition) throws Exception {
        if (partition == null) {
            return;
        }
        _insertPartition.setInt(1, partition.getPartitionId());
        _insertPartition.setString(2, partition.getId().toString());
        _insertPartition.setLong(3, partition.getNextCartId());
        _insertPartition.execute();
        if (tracer().debug) tracer().log("Inserted partition [id=" + partition.getPartitionId() + ", storeid=" + partition.getId() + "]", Tracer.Level.DEBUG);
    }

    /*
     * Update a partition object in the database
     */
    final private void updatePartition(final Partition partition) throws Exception {
        if (partition == null) {
            return;
        }
        _updatePartition.setInt(1, partition.getPartitionId());
        _updatePartition.setLong(2, partition.getNextCartId());
        _updatePartition.setString(3, partition.getId().toString());
        _updatePartition.execute();

        int count = _updatePartition.getUpdateCount();
        if (count == 1) {
            if (tracer().debug) tracer().log("Updated partition [id=" + partition.getPartitionId() + ", storeid=" + partition.getId() + "]", Tracer.Level.DEBUG);
        }
        else if (count < 1) {
            tracer().log("*** Partition not found for update [id=" + partition.getPartitionId() + ", storeid=" + partition.getId() + "] ***", Tracer.Level.SEVERE);
        }
        else {
            tracer().log("*** Duplicate rows. Updated " + count + " partitions [id=" + partition.getPartitionId() + ", storeid=" + partition.getId() + "] ***", Tracer.Level.SEVERE);
        }
    }

    /*
     * Delete a partition object from the database
     */
    final private int deletePartition(final String storeId) throws Exception {
        _deletePartition.setString(1, storeId);
        final int count = _deletePartition.executeUpdate();
        if (count > 0 && tracer().debug) {
            tracer().log("Deleted partition with storeId=" + storeId, Tracer.Level.DEBUG);
    }
        return count;
    }

    final private Partition readPartition(final int id) throws Exception {
        _selectPartition.setInt(1, id);
        ResultSet rs = _selectPartition.executeQuery();
        try {
            if (rs.next()) {
                final Partition partition = Partition.create(new UUID(rs.getString(PARTITIONS_STOREID_FIELD_NAME)));
                partition.setPartitionId(rs.getInt(PARTITIONS_ID_FIELD_NAME));
                partition.setNextCartId(rs.getLong(PARTITIONS_NEXTCARTID_FIELD_NAME));
                return partition;
            }
        }
        finally {
            rs.close();
        }

        return null;
    }

    @Override
    final protected void createTables(final boolean createIndexes) throws Exception {
        StringBuilder sb = new StringBuilder("...");

        // partition master
        sb = new StringBuilder("...");
        sb.append(createPartitionsTable(sb) ? "...created" : "...exists");
        tracer().log(sb.toString(), Tracer.Level.INFO);
    }

    @Override
    final protected void createPreparedStatements() throws Exception {
        createPartitionInsertPreparedStatement();
        createPartitionUpdatePreparedStatement();
        createPartitionDeletePreparedStatement();
        createPartitionSelectPreparedStatement();
        createPartitionExistsPreparedStatement();
    }

    @Override
    final protected void closePreparedStatements() throws SQLException {
        if (_insertPartition != null) _insertPartition.close();
        if (_updatePartition != null) _updatePartition.close();
        if (_deletePartition != null) _deletePartition.close();
        if (_selectPartition != null) _selectPartition.close();
        if (_partitionExists != null) _partitionExists.close();
    }

    final public void dropTables() throws Exception {
        dropPartitionsTable();
    }

    final public void truncateTables() throws Exception {
        truncatePartitionsTable();
    }

    final public void update(final Partition partition) throws Exception {
        if (partitionExistsByStoreId(partition.getId().toString())) {
            updatePartition(partition);
        }
        else {
            insertPartition(partition);
        }
    }

    final public int delete(final String storeId) throws Exception {
        return deletePartition(storeId);
    }

    final public Partition read(int partitionId) throws Exception {
        return readPartition(partitionId);
    }

    final public void printStatements() {
        // create
        System.out.println("Partition: create statements...");
        System.out.println("\n" + createPartitionsTableStatement() + ";");
        System.out.println();

        // drop
        System.out.println("Partition: drop statements...");
        System.out.println(dropPartitionsTableStatement());
        System.out.println();

        // truncate
        System.out.println("Partition: truncate statements...");
        System.out.println(truncatePartitionsTableStatement());
        System.out.println();

        // insert/update/delete/exists
        System.out.println("Partition: insert/update/delete/exists/other statements...");

        // partition selection (during startup)
        System.out.println("\n" + createPartitionSelectStatement());
        System.out.println(createPartitionInsertStatement());
        System.out.println(createPartitionUpdateStatement());
        System.out.println(createPartitionDeleteStatement());
        System.out.println(createPartitionExistsStatement());
        System.out.println();
    }

    public static void main(String[] args) {
        new PartitionPersister(null).printStatements();
    }
}
