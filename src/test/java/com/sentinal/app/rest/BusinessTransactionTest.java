package com.sentinal.app.rest;

import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.transaction.TransactionManager;
import com.sentinal.persistencefw.transaction.TransactionProxy;
import com.sentinal.roadrescue.jobs.BackgroundJob;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class BusinessTransactionTest
{
    public interface BusinessWork { Object invoke() throws Exception; }

    private static final String URL = "jdbc:h2:mem:persistence_test;DB_CLOSE_DELAY=-1";

    @BeforeEach void setup() throws Exception
    {
        PersistenceManager.initialize(URL, "sa", "");
        try (Connection c = DriverManager.getConnection(URL, "sa", ""); Statement s = c.createStatement())
        {
            s.execute("DROP ALL OBJECTS");
            s.execute("CREATE TABLE item (id INT PRIMARY KEY)");
        }
    }

    @AfterEach void cleanup()
    {
        assertFalse(TransactionManager.isActive());
    }

    private void insert() throws Exception
    {
        TransactionManager.withConnection(c -> {
            try (Statement s = c.createStatement()) { s.executeUpdate("INSERT INTO item VALUES (1)"); }
            return null;
        });
    }

    private int count() throws Exception
    {
        try (Connection c = DriverManager.getConnection(URL, "sa", ""); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM item"))
        {
            r.next(); return r.getInt(1);
        }
    }

    @Test void serviceBoundaryCommitsOnlyAfterBusinessInvocation() throws Exception
    {
        boolean[] published = {false};
        BusinessWork target = () -> {
            insert();
            assertEquals(0, count());
            TransactionManager.afterCommit(() -> published[0] = true);
            assertFalse(published[0]);
            return "ok";
        };
        assertEquals("ok", TransactionProxy.create(BusinessWork.class, target).invoke());
        assertEquals(1, count());
        assertTrue(published[0]);
    }

    @Test void serviceFailureRollsBackAndDiscardsPublication() throws Exception
    {
        boolean[] published = {false};
        BusinessWork target = () -> {
            insert();
            TransactionManager.afterCommit(() -> published[0] = true);
            throw new IOException("failed");
        };
        assertThrows(IOException.class, () -> TransactionProxy.create(BusinessWork.class, target).invoke());
        assertEquals(0, count());
        assertFalse(published[0]);
    }

    @Test void backgroundAttemptRollsBackOnCheckedFailure() throws Exception
    {
        BackgroundJob job = new BackgroundJob() {
            public String getName() { return "test"; }
            public void execute() throws Exception { insert(); throw new IOException("job failed"); }
        };
        assertThrows(IOException.class, () -> TransactionProxy.create(BackgroundJob.class, job).execute());
        assertEquals(0, count());
    }
}
