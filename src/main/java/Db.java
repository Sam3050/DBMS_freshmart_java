import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

public class Db {
  static String env(String k, String d) { String v = System.getenv(k); return v == null ? d : v; }
  static Connection get() throws SQLException {
    return DriverManager.getConnection(env("DB_URL",
      "jdbc:mysql://localhost:3306/grocery?useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&useSSL=false"),
      env("DB_USER", "root"), env("DB_PASSWORD", ""));
  }
  static List<Map<String, Object>> query(String sql, Object... p) throws SQLException {
    try (Connection c = get(); PreparedStatement s = c.prepareStatement(sql)) {
      for (int i = 0; i < p.length; i++) s.setObject(i + 1, p[i]);
      try (ResultSet r = s.executeQuery()) {
        List<Map<String, Object>> out = new ArrayList<>();
        ResultSetMetaData m = r.getMetaData();
        while (r.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= m.getColumnCount(); i++) row.put(m.getColumnLabel(i), r.getObject(i));
          out.add(row);
        }
        return out;
      }
    }
  }
  static int update(String sql, Object... p) throws SQLException {
    try (Connection c = get(); PreparedStatement s = c.prepareStatement(sql)) {
      for (int i = 0; i < p.length; i++) s.setObject(i + 1, p[i]);
      return s.executeUpdate();
    }
  }
  static String hash(String s) {
    try {
      StringBuilder b = new StringBuilder();
      for (byte x : MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))) b.append(String.format("%02x", x));
      return b.toString();
    } catch (Exception e) { throw new RuntimeException(e); }
  }
  static double num(Object o) { return o == null ? 0 : ((Number) o).doubleValue(); }
  static boolean yes(Object o) { return o instanceof Boolean ? (Boolean) o : o != null && ((Number) o).intValue() != 0; }
  static String money(double d) { return String.format("₹%.2f", d); }
}