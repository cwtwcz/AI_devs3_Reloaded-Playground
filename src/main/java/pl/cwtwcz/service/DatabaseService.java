package pl.cwtwcz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    /**
     * Wykonuje zapytanie DDL (CREATE, DROP, ALTER) na bazie SQLite
     */
    public void executeDDL(String dbPath, String ddlQuery) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddlQuery);
            logger.debug("DDL executed successfully: {}", ddlQuery.substring(0, Math.min(ddlQuery.length(), 50)));
        } catch (SQLException e) {
            logger.error("Error executing DDL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute DDL: " + e.getMessage(), e);
        }
    }

    /**
     * Wykonuje zapytanie INSERT/UPDATE/DELETE na bazie SQLite
     */
    public int executeUpdate(String dbPath, String query, Object... parameters) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            setParameters(stmt, parameters);
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Update executed, rows affected: {}", rowsAffected);
            return rowsAffected;
        } catch (SQLException e) {
            logger.error("Error executing update: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute update: " + e.getMessage(), e);
        }
    }

    /**
     * Wykonuje zapytanie SELECT i zwraca listę map (kolumna -> wartość)
     */
    public List<Map<String, Object>> executeQuery(String dbPath, String query, Object... parameters) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            setParameters(stmt, parameters);
            
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
            
            logger.debug("Query executed, {} rows returned", results.size());
            return results;
        } catch (SQLException e) {
            logger.error("Error executing query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute query: " + e.getMessage(), e);
        }
    }

    /**
     * Wykonuje zapytanie SELECT i zwraca pierwszą wartość z pierwszego wiersza
     */
    public Object executeSingleValue(String dbPath, String query, Object... parameters) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            setParameters(stmt, parameters);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1);
                }
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error executing single value query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute single value query: " + e.getMessage(), e);
        }
    }

    /**
     * Sprawdza czy tabela istnieje w bazie danych
     */
    public boolean tableExists(String dbPath, String tableName) {
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        Object result = executeSingleValue(dbPath, query, tableName);
        return result != null;
    }

    /**
     * Sprawdza czy baza danych zawiera jakiekolwiek dane w podanej tabeli
     */
    public boolean hasData(String dbPath, String tableName) {
        if (!tableExists(dbPath, tableName)) {
            return false;
        }
        String query = "SELECT COUNT(*) FROM " + tableName;
        Object count = executeSingleValue(dbPath, query);
        return count != null && ((Number) count).longValue() > 0;
    }

    /**
     * Wykonuje operację w transakcji
     */
    public <T> T executeInTransaction(String dbPath, TransactionCallback<T> callback) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            conn.setAutoCommit(false);
            try {
                T result = callback.execute(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Error executing transaction: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Ustawia parametry w PreparedStatement
     */
    private void setParameters(PreparedStatement stmt, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            if (param == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else if (param instanceof String) {
                stmt.setString(i + 1, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(i + 1, (Integer) param);
            } else if (param instanceof Long) {
                stmt.setLong(i + 1, (Long) param);
            } else if (param instanceof Boolean) {
                stmt.setBoolean(i + 1, (Boolean) param);
            } else if (param instanceof Double) {
                stmt.setDouble(i + 1, (Double) param);
            } else {
                stmt.setObject(i + 1, param);
            }
        }
    }

    /**
     * Interface dla operacji w transakcji
     */
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute(Connection connection) throws SQLException;
    }
} 