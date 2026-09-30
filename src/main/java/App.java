
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.sql.*;
import java.util.List;
import java.util.*;

public class App {
  static final Color OR = new Color(0xFC8019), GR = new Color(0x1BA672), BG = new Color(0xF4F4F6),
                     LG = new Color(0xE8F7F0), LO = new Color(0xFFF0E0);
  static int uid; static String uname, uaddr;

  public static void main(String[] a) { SwingUtilities.invokeLater(Login::new); }

  interface Act { void run() throws Exception; }

  static class Btn extends JButton {
    Color bg;
    Btn(String t, Color bg) {
      super(t); this.bg = bg;
      setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
      setForeground(Color.WHITE); setFont(new Font("SansSerif", Font.BOLD, 13));
      setCursor(new Cursor(Cursor.HAND_CURSOR)); setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }
    protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(isEnabled() ? bg : Color.LIGHT_GRAY);
      g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14); g2.dispose();
      super.paintComponent(g);
    }
  }
  static Btn btn(String t, Color c, Act a) {
    Btn b = new Btn(t, c);
    b.addActionListener(e -> { try { a.run(); } catch (Exception ex) { msg(b, ex.getMessage()); } });
    return b;
  }
  static void msg(Component c, String m) { JOptionPane.showMessageDialog(c, m); }
  static JLabel lbl(String t, int size, boolean bold, Color fg) {
    JLabel l = new JLabel(t); l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size)); l.setForeground(fg); return l;
  }
  static Border pad(int v, int h) { return BorderFactory.createEmptyBorder(v, h, v, h); }
  static String fmt(double d) { return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d); }
  static double step(String u) { return u.equals("gram") ? 100 : u.equals("piece") ? 1 : .5; }
  static String esc(Object s) { return String.valueOf(s).replace("<", "&lt;"); }
  static JTextField tf(String... s) { return new JTextField(s.length > 0 ? s[0] : "", 22); }
  static String tx(JComponent c) { return ((JTextComponent) c).getText().trim(); }
  static void onChange(JTextComponent c, Runnable r) {
    c.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) { r.run(); }
      public void removeUpdate(DocumentEvent e) { r.run(); }
      public void changedUpdate(DocumentEvent e) { r.run(); }
    });
  }
  static boolean form(Component parent, String title, String[] labels, JComponent[] fs) {
    JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
    for (int i = 0; i < labels.length; i++) { p.add(new JLabel(labels[i])); p.add(fs[i]); }
    return JOptionPane.showConfirmDialog(parent, p, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
  }
  static DefaultTableModel model(List<Map<String, Object>> rows) {
    Vector<String> cols = new Vector<>();
    if (!rows.isEmpty()) cols.addAll(rows.get(0).keySet());
    DefaultTableModel m = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
    for (var r : rows) m.addRow(new Vector<>(r.values()));
    return m;
  }
  static JTable table() {
    JTable t = new JTable(); t.setRowHeight(28); t.setSelectionBackground(LO); t.setSelectionForeground(Color.BLACK);
    t.setAutoCreateRowSorter(false); t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13)); return t;
  }
  static int selId(JTable t) {
    if (t.getSelectedRow() < 0) { msg(t, "Select a row first"); return -1; }
    return ((Number) t.getValueAt(t.getSelectedRow(), 0)).intValue();
  }
  static JTextArea mono() { JTextArea a = new JTextArea(); a.setEditable(false); a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); a.setMargin(new Insets(8, 8, 8, 8)); return a; }
  static String dump(List<Map<String, Object>> rows) {
    if (rows.isEmpty()) return "  (none)\n";
    StringBuilder b = new StringBuilder("  " + String.join("  |  ", rows.get(0).keySet()) + "\n");
    for (var r : rows) { StringJoiner j = new StringJoiner("  |  "); r.values().forEach(v -> j.add(String.valueOf(v))); b.append("  ").append(j).append("\n"); }
    return b.toString();
  }
  static String orderDetail(int id) throws SQLException {
    var o = Db.query("SELECT o.*, s.slot_date, s.start_time, s.end_time FROM orders o JOIN delivery_slots s ON s.slot_id=o.slot_id WHERE o.order_id=?", id).get(0);
    String st = (String) o.get("order_status");
    StringBuilder b = new StringBuilder("Order #" + id + "\n");
    if (st.equals("CANCELLED")) b.append("Status: CANCELLED\n");
    else {
      String[] S = {"PENDING", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED"};
      int cur = Arrays.asList(S).indexOf(st);
      for (int k = 0; k < 4; k++) b.append(k <= cur ? "● " : "○ ").append(S[k]).append(k < 3 ? "  →  " : "\n");
    }
    b.append("Delivery: ").append(o.get("slot_date")).append(" ").append(o.get("start_time")).append("-").append(o.get("end_time"))
     .append("\nPayment: ").append(o.get("payment_method")).append("\nAddress: ").append(o.get("delivery_address")).append("\n\nITEMS\n");
    for (var i : Db.query("SELECT p.title, oi.quantity q, p.unit_type u, oi.unit_price pr, oi.subtotal sb, oi.batch_id bt, b.expiry_date ex " +
        "FROM order_items oi JOIN products p ON p.product_id=oi.product_id JOIN product_batches b ON b.batch_id=oi.batch_id WHERE oi.order_id=?", id))
      b.append(String.format("  %-20s %s %s x %s = %s  (batch #%s, exp %s)\n", i.get("title"), fmt(Db.num(i.get("q"))), i.get("u"),
          Db.money(Db.num(i.get("pr"))), Db.money(Db.num(i.get("sb"))), i.get("bt"), i.get("ex")));
    b.append("\nDelivery fee: ").append(Db.money(Db.num(o.get("delivery_fee")))).append("   Packaging: ").append(Db.money(Db.num(o.get("packaging_fee"))))
     .append("\nTOTAL: ").append(Db.money(Db.num(o.get("total_amount")))).append("\n\nHISTORY\n");
    for (var h : Db.query("SELECT status, changed_at FROM order_status_history WHERE order_id=? ORDER BY history_id", id))
      b.append("  ").append(h.get("changed_at")).append("  ").append(h.get("status")).append("\n");
    return b.toString();
  }
}

class Login extends JFrame {
  JTextField email = new JTextField("demo@example.com");
  JPasswordField pw = new JPasswordField("demo123");
  Login() {
    super("FreshMart – Login");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBackground(Color.WHITE); p.setBorder(App.pad(30, 40));
    p.add(App.lbl("⚡ FreshMart", 32, true, App.OR));
    p.add(App.lbl("Groceries delivered in 10 minutes", 13, false, Color.GRAY));
    p.add(Box.createVerticalStrut(20));
    for (JComponent c : new JComponent[]{email, pw}) { c.setMaximumSize(new Dimension(400, 36)); c.setPreferredSize(new Dimension(320, 36)); }
    p.add(App.lbl("Email", 12, true, Color.DARK_GRAY)); p.add(email); p.add(Box.createVerticalStrut(10));
    p.add(App.lbl("Password", 12, true, Color.DARK_GRAY)); p.add(pw); p.add(Box.createVerticalStrut(16));
    JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); r.setOpaque(false);
    r.add(App.btn("Login", App.OR, this::login)); r.add(App.btn("Create account", Color.DARK_GRAY, this::register));
    p.add(r); p.add(Box.createVerticalStrut(14));
    p.add(App.lbl("Admin: admin@freshcart.com / admin123", 11, false, Color.GRAY));
    for (Component c : p.getComponents()) ((JComponent) c).setAlignmentX(0);
    add(p); pack(); setLocationRelativeTo(null); setVisible(true);
  }
  void login() throws Exception {
    var r = Db.query("SELECT customer_id, full_name, role, address FROM customers WHERE email=? AND password_hash=?",
        email.getText().trim(), Db.hash(new String(pw.getPassword())));
    if (r.isEmpty()) { App.msg(this, "Wrong email or password"); return; }
    var u = r.get(0);
    App.uid = (Integer) u.get("customer_id"); App.uname = (String) u.get("full_name"); App.uaddr = (String) u.get("address");
    dispose();
    if ("ADMIN".equals(u.get("role"))) new Admin(); else new Shop();
  }
  void register() throws Exception {
    JTextField n = App.tf(), e = App.tf(), ph = App.tf(), ad = App.tf(); JPasswordField p = new JPasswordField();
    if (!App.form(this, "Create account", new String[]{"Name", "Email", "Phone", "Address", "Password"}, new JComponent[]{n, e, ph, ad, p})) return;
    if (App.tx(n).isEmpty() || App.tx(e).isEmpty() || p.getPassword().length < 4) { App.msg(this, "Fill all fields (password 4+ chars)"); return; }
    Db.update("INSERT INTO customers(full_name,email,phone,address,password_hash,role) VALUES(?,?,?,?,?,'CUSTOMER')",
        App.tx(n), App.tx(e), App.tx(ph), App.tx(ad), Db.hash(new String(p.getPassword())));
    email.setText(App.tx(e)); App.msg(this, "Account created. Please login.");
  }
}