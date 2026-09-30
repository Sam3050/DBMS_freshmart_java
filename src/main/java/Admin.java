
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.List;
import java.util.*;

class Admin extends JFrame {
  JTable inv = App.table(), ord = App.table();
  JTextArea det = App.mono(), rep = App.mono();

  Admin() {
    super("FreshMart Admin");
    setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1200, 720); setLocationRelativeTo(null);
    JPanel top = new JPanel(new BorderLayout()); top.setBackground(App.OR); top.setBorder(App.pad(12, 18));
    top.add(App.lbl("⚡ FreshMart  •  Admin Console", 22, true, Color.WHITE), BorderLayout.WEST);
    top.add(App.btn("Logout", Color.DARK_GRAY, () -> { dispose(); new Login(); }), BorderLayout.EAST);
    JTabbedPane tabs = new JTabbedPane();
    tabs.add("Inventory", inventoryTab()); tabs.add("Orders", ordersTab()); tabs.add("Reports", new JScrollPane(rep));
    tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 2) reports(); });
    add(top, BorderLayout.NORTH); add(tabs);
    loadInv(); loadOrd(); setVisible(true);
  }

  JPanel inventoryTab() {
    JPanel p = new JPanel(new BorderLayout());
    JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    b.add(App.btn("+ Add item", App.GR, this::addItem));
    b.add(App.btn("Edit item", App.OR, this::editItem));
    b.add(App.btn("Add stock", new Color(0x2C7BE5), this::addStock));
    b.add(App.btn("Hide / Show", Color.DARK_GRAY, this::toggle));
    b.add(App.btn("Refresh", Color.GRAY, this::loadInv));
    p.add(new JScrollPane(inv)); p.add(b, BorderLayout.SOUTH); return p;
  }
  void loadInv() throws RuntimeException {
    try {
      inv.setModel(App.model(Db.query("SELECT p.product_id AS ID, p.sku AS SKU, p.title AS Item, c.name AS Category, p.unit_type AS Unit, " +
        "p.price_per_unit AS Price, v.stock AS Available, CASE WHEN NOT p.is_active THEN 'Hidden' WHEN v.stock>0 THEN 'In stock' ELSE 'Out of stock' END AS Status, " +
        "v.next_expiry AS `Next expiry`, p.description AS Description FROM products p JOIN categories c ON c.category_id=p.category_id " +
        "JOIN v_catalog v ON v.product_id=p.product_id ORDER BY p.product_id")));
    } catch (SQLException e) { throw new RuntimeException(e.getMessage()); }
  }

  void addItem() throws Exception {
    var cats = Db.query("SELECT category_id, name FROM categories");
    JComboBox<String> cb = new JComboBox<>(); for (var c : cats) cb.addItem((String) c.get("name"));
    JComboBox<String> unit = new JComboBox<>(new String[]{"kg", "gram", "liter", "piece"});
    JTextField name = App.tf(), sku = App.tf(), price = App.tf(), qty = App.tf(), days = App.tf("7"), emoji = App.tf("🛒"), desc = App.tf();
    if (!App.form(this, "Add item", new String[]{"Name", "SKU", "Category", "Unit", "Price (Rs)", "Opening stock", "Shelf life (days)", "Emoji", "Description"},
        new JComponent[]{name, sku, cb, unit, price, qty, days, emoji, desc})) return;
    int shelf = Integer.parseInt(App.tx(days));
    try (Connection c = Db.get()) {
      c.setAutoCommit(false);
      try {
        PreparedStatement s = c.prepareStatement("INSERT INTO products(category_id,sku,title,emoji,unit_type,price_per_unit,is_perishable,shelf_life_days,description) VALUES(?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        s.setInt(1, (Integer) cats.get(cb.getSelectedIndex()).get("category_id")); s.setString(2, App.tx(sku)); s.setString(3, App.tx(name));
        s.setString(4, App.tx(emoji)); s.setString(5, (String) unit.getSelectedItem()); s.setBigDecimal(6, new java.math.BigDecimal(App.tx(price)));
        s.setBoolean(7, shelf < 30); s.setInt(8, shelf); s.setString(9, App.tx(desc)); s.executeUpdate();
        ResultSet k = s.getGeneratedKeys(); k.next(); int pid = k.getInt(1);
        PreparedStatement b = c.prepareStatement("INSERT INTO product_batches(product_id,stock_quantity,expiry_date) VALUES(?,?,DATE_ADD(CURDATE(),INTERVAL ? DAY))");
        b.setInt(1, pid); b.setBigDecimal(2, new java.math.BigDecimal(App.tx(qty))); b.setInt(3, shelf); b.executeUpdate();
        c.commit();
      } catch (Exception e) { c.rollback(); throw e; }
    }
    loadInv();
  }
  void editItem() throws Exception {
    int id = App.selId(inv); if (id < 0) return;
    var p = Db.query("SELECT title, price_per_unit, description, emoji FROM products WHERE product_id=?", id).get(0);
    JTextField n = App.tf(String.valueOf(p.get("title"))), pr = App.tf(String.valueOf(p.get("price_per_unit"))),
               d = App.tf(String.valueOf(p.get("description"))), em = App.tf(String.valueOf(p.get("emoji")));
    if (!App.form(this, "Edit item #" + id, new String[]{"Name", "Price (Rs)", "Description", "Emoji"}, new JComponent[]{n, pr, d, em})) return;
    Db.update("UPDATE products SET title=?, price_per_unit=?, description=?, emoji=? WHERE product_id=?", App.tx(n), new java.math.BigDecimal(App.tx(pr)), App.tx(d), App.tx(em), id);
    loadInv();
  }
  void addStock() throws Exception {
    int id = App.selId(inv); if (id < 0) return;
    JTextField q = App.tf(), days = App.tf("7");
    if (!App.form(this, "Receive new batch for item #" + id, new String[]{"Quantity received", "Expires in (days)"}, new JComponent[]{q, days})) return;
    Db.update("INSERT INTO product_batches(product_id,stock_quantity,expiry_date) VALUES(?,?,DATE_ADD(CURDATE(),INTERVAL ? DAY))",
        id, new java.math.BigDecimal(App.tx(q)), Integer.parseInt(App.tx(days)));
    loadInv();
  }
  void toggle() throws Exception {
    int id = App.selId(inv); if (id < 0) return;
    Db.update("UPDATE products SET is_active = NOT is_active WHERE product_id=?", id); loadInv();
  }

  JPanel ordersTab() {
    JPanel p = new JPanel(new BorderLayout());
    ord.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && ord.getSelectedRow() >= 0)
        try { det.setText(App.orderDetail(((Number) ord.getValueAt(ord.getSelectedRow(), 0)).intValue())); } catch (Exception ex) { App.msg(this, ex.getMessage()); }
    });
    JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    b.add(App.btn("Advance status", App.GR, () -> {
      int id = App.selId(ord); if (id < 0) return;
      String st = (String) ord.getValueAt(ord.getSelectedRow(), 2);
      String nx = st.equals("PENDING") ? "PACKED" : st.equals("PACKED") ? "OUT_FOR_DELIVERY" : st.equals("OUT_FOR_DELIVERY") ? "DELIVERED" : null;
      if (nx == null) { App.msg(this, "Cannot advance a " + st + " order"); return; }
      Db.update("UPDATE orders SET order_status=? WHERE order_id=?", nx, id); loadOrd();
    }));
    b.add(App.btn("Cancel order", new Color(0xC0392B), () -> {
      int id = App.selId(ord); if (id < 0) return;
      try (Connection c = Db.get(); CallableStatement s = c.prepareCall("{CALL cancel_order(?)}")) { s.setInt(1, id); s.execute(); }
      loadOrd(); loadInv();
    }));
    b.add(App.btn("Refresh", Color.GRAY, this::loadOrd));
    JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(ord), new JScrollPane(det)); sp.setDividerLocation(260);
    p.add(sp); p.add(b, BorderLayout.SOUTH); return p;
  }
  void loadOrd() {
    try {
      ord.setModel(App.model(Db.query("SELECT o.order_id AS ID, c.full_name AS Customer, o.order_status AS Status, o.total_amount AS Total, o.payment_method AS Payment, " +
        "CONCAT(s.slot_date,' ',s.start_time) AS Slot, o.created_at AS Placed FROM orders o JOIN customers c ON c.customer_id=o.customer_id " +
        "JOIN delivery_slots s ON s.slot_id=o.slot_id ORDER BY o.order_id DESC")));
    } catch (SQLException e) { throw new RuntimeException(e.getMessage()); }
  }

  void reports() {
    try {
      rep.setText("EXPIRING WITHIN 48 HOURS (spoilage radar)\n" + App.dump(Db.query(
        "SELECT p.sku, p.title, b.batch_id, b.stock_quantity, b.expiry_date, ROUND(b.stock_quantity*p.price_per_unit,2) AS capital_at_risk " +
        "FROM product_batches b JOIN products p ON p.product_id=b.product_id " +
        "WHERE b.expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND b.stock_quantity>0 ORDER BY b.expiry_date")) +
        "\nLOW / OUT OF STOCK (5 or fewer)\n" + App.dump(Db.query("SELECT title, unit_type, stock FROM v_catalog WHERE stock<=5 ORDER BY stock")) +
        "\nFREQUENTLY BOUGHT TOGETHER\n" + App.dump(Db.query(
        "SELECT p1.title AS item_a, p2.title AS item_b, COUNT(DISTINCT a.order_id) AS times FROM order_items a " +
        "JOIN order_items b ON a.order_id=b.order_id AND a.product_id<b.product_id JOIN orders o ON o.order_id=a.order_id AND o.order_status<>'CANCELLED' " +
        "JOIN products p1 ON p1.product_id=a.product_id JOIN products p2 ON p2.product_id=b.product_id GROUP BY p1.title,p2.title ORDER BY times DESC LIMIT 10")));
    } catch (Exception e) { rep.setText(e.getMessage()); }
  }
}