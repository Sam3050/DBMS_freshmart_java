
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.*;

class Shop extends JFrame {
  List<Map<String, Object>> prods = new ArrayList<>(), cats = new ArrayList<>();
  Map<Integer, Map<String, Object>> known = new HashMap<>();
  Map<Integer, Double> cart = new LinkedHashMap<>();
  Integer cat; JDialog dlg;
  JTextField search = new JTextField(28);
  JPanel grid = new JPanel(new GridLayout(0, 4, 12, 12)), side = new JPanel(), bar = new JPanel(new BorderLayout());
  JLabel barTxt = App.lbl("", 15, true, Color.WHITE);

  Shop() {
    super("FreshMart");
    setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1180, 760); setLocationRelativeTo(null);
    JPanel top = new JPanel(new BorderLayout(24, 0)); top.setBackground(App.OR); top.setBorder(App.pad(12, 18));
    JPanel l = new JPanel(new GridLayout(2, 1)); l.setOpaque(false);
    l.add(App.lbl("⚡ FreshMart", 24, true, Color.WHITE));
    l.add(App.lbl("Delivery in 10 minutes  •  " + App.uaddr, 12, false, Color.WHITE));
    search.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.WHITE, 1, true), App.pad(8, 12)));
    JPanel sp = new JPanel(new BorderLayout()); sp.setOpaque(false);
    sp.add(App.lbl("🔍 Search:  ", 13, true, Color.WHITE), BorderLayout.WEST); sp.add(search);
    JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4)); r.setOpaque(false);
    r.add(App.lbl("Hi, " + App.uname, 13, true, Color.WHITE));
    r.add(App.btn("My Orders", Color.DARK_GRAY, this::showOrders));
    r.add(App.btn("Logout", Color.DARK_GRAY, () -> { dispose(); new Login(); }));
    top.add(l, BorderLayout.WEST); top.add(sp, BorderLayout.CENTER); top.add(r, BorderLayout.EAST);

    side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS)); side.setBackground(Color.WHITE);
    side.setBorder(App.pad(10, 10)); side.setPreferredSize(new Dimension(180, 0));
    JPanel wrap = new JPanel(new BorderLayout()); wrap.setBackground(App.BG); wrap.setBorder(App.pad(14, 14));
    grid.setOpaque(false); wrap.add(grid, BorderLayout.NORTH);
    JScrollPane sc = new JScrollPane(wrap); sc.setBorder(null); sc.getVerticalScrollBar().setUnitIncrement(20);
    bar.setBackground(App.GR); bar.setBorder(App.pad(12, 20)); bar.setVisible(false);
    bar.add(barTxt, BorderLayout.WEST); bar.add(App.btn("View Cart  ›", new Color(0x14875C), this::showCart), BorderLayout.EAST);
    add(top, BorderLayout.NORTH); add(side, BorderLayout.WEST); add(sc, BorderLayout.CENTER); add(bar, BorderLayout.SOUTH);

    try { cats = Db.query("SELECT * FROM categories ORDER BY category_id"); } catch (Exception e) { App.msg(this, e.getMessage()); }
    App.onChange(search, this::load);
    renderSide(); load(); setVisible(true);
  }

  String emoji(String n) {
    switch (n) { case "Produce": return "🥬"; case "Dairy": return "🥛"; case "Pantry": return "🌾"; case "Bakery": return "🍞"; case "Frozen": return "🧊"; default: return "🛒"; }
  }
  void renderSide() {
    side.removeAll(); addCat("🛒  All", null);
    for (var c : cats) addCat(emoji((String) c.get("name")) + "  " + c.get("name"), (Integer) c.get("category_id"));
    side.revalidate(); side.repaint();
  }
  void addCat(String t, Integer id) {
    boolean on = Objects.equals(cat, id);
    App.Btn b = new App.Btn(t, on ? App.LO : Color.WHITE); b.setForeground(on ? App.OR : Color.DARK_GRAY);
    b.setMaximumSize(new Dimension(160, 40)); b.setAlignmentX(0);
    b.addActionListener(e -> { cat = id; renderSide(); load(); });
    side.add(b); side.add(Box.createVerticalStrut(4));
  }
  void load() {
    try {
      String sql = "SELECT * FROM v_catalog WHERE is_active=1"; List<Object> p = new ArrayList<>();
      if (cat != null) { sql += " AND category_id=?"; p.add(cat); }
      String q = search.getText().trim();
      if (!q.isEmpty()) { sql += " AND title LIKE ?"; p.add("%" + q + "%"); }
      prods = Db.query(sql + " ORDER BY (stock<=0), title", p.toArray());
      for (var m : prods) known.put((Integer) m.get("product_id"), m);
      cart.keySet().removeIf(id -> Db.num(known.get(id).get("stock")) <= 0);
      render();
    } catch (Exception e) { App.msg(this, e.getMessage()); }
  }
  double total() { double t = 0; for (var e : cart.entrySet()) t += e.getValue() * Db.num(known.get(e.getKey()).get("effective_price")); return t; }
  void render() {
    grid.removeAll();
    for (var p : prods) grid.add(card(p));
    if (prods.isEmpty()) grid.add(App.lbl("No items found", 15, false, Color.GRAY));
    grid.revalidate(); grid.repaint();
    bar.setVisible(!cart.isEmpty());
    barTxt.setText(cart.size() + " item" + (cart.size() > 1 ? "s" : "") + "   |   " + Db.money(total()));
  }
  void chg(int id, int dir) {
    var p = known.get(id);
    double n = Math.round((cart.getOrDefault(id, 0.0) + dir * App.step((String) p.get("unit_type"))) * 100) / 100.0, stock = Db.num(p.get("stock"));
    if (n > stock) { App.msg(this, "Only " + App.fmt(stock) + " available"); return; }
    if (n <= 0) cart.remove(id); else cart.put(id, n);
    render();
  }
  App.Btn mini(String t, int id, int d, Act2 after) {
    App.Btn b = new App.Btn(t, App.GR); b.addActionListener(e -> { chg(id, d); after.go(); }); return b;
  }
  interface Act2 { void go(); }

  JPanel card(Map<String, Object> p) {
    int id = (Integer) p.get("product_id");
    double stock = Db.num(p.get("stock")), price = Db.num(p.get("price_per_unit")), eff = Db.num(p.get("effective_price")), q = cart.getOrDefault(id, 0.0);
    boolean off = Db.yes(p.get("expiring_soon")), out = stock <= 0;
    JPanel c = new JPanel(new BorderLayout(0, 6)); c.setBackground(Color.WHITE);
    c.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(0xE5E5E5), 1, true), App.pad(12, 12)));
    c.setToolTipText(String.valueOf(p.get("description")));
    JLabel em = new JLabel(String.valueOf(p.get("emoji")), SwingConstants.CENTER); em.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
    JPanel tp = new JPanel(new BorderLayout()); tp.setOpaque(false); tp.add(em, BorderLayout.CENTER);
    if (off) {
      JLabel t = App.lbl("20% OFF", 11, true, Color.WHITE); t.setOpaque(true); t.setBackground(App.GR); t.setBorder(App.pad(2, 6));
      JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); f.setOpaque(false); f.add(t); tp.add(f, BorderLayout.NORTH);
    } else tp.add(Box.createVerticalStrut(20), BorderLayout.NORTH);
    c.add(tp, BorderLayout.NORTH);
    c.add(new JLabel("<html><b>" + App.esc(p.get("title")) + "</b><br><span style='color:gray'>1 " + p.get("unit_type") + "</span>" +
        (!out && stock <= 5 ? "<br><span style='color:#E08A1E'>Only " + App.fmt(stock) + " left</span>" : "") + "</html>"), BorderLayout.CENTER);
    JPanel s = new JPanel(new BorderLayout()); s.setOpaque(false);
    s.add(new JLabel("<html>" + (off ? "<s style='color:gray'>" + Db.money(price) + "</s>" : "&nbsp;") + "<br><b>" + Db.money(eff) + "</b></html>"), BorderLayout.WEST);
    if (out) { App.Btn b = new App.Btn("SOLD OUT", Color.LIGHT_GRAY); b.setEnabled(false); s.add(b, BorderLayout.EAST); }
    else if (q == 0) { App.Btn b = new App.Btn("ADD", App.LG); b.setForeground(App.GR); b.addActionListener(e -> chg(id, 1)); s.add(b, BorderLayout.EAST); }
    else {
      JPanel st = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); st.setOpaque(false);
      st.add(mini("−", id, -1, () -> {})); st.add(App.lbl(App.fmt(q), 13, true, App.GR)); st.add(mini("+", id, 1, () -> {}));
      s.add(st, BorderLayout.EAST);
    }
    c.add(s, BorderLayout.SOUTH);
    return c;
  }

  void showCart() {
    dlg = new JDialog(this, "Your cart", true); dlg.setSize(540, 720); dlg.setLocationRelativeTo(this);
    JPanel root = new JPanel(); root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS)); root.setBorder(App.pad(14, 16)); root.setBackground(Color.WHITE);
    root.add(App.lbl("Your cart", 20, true, Color.BLACK)); root.add(Box.createVerticalStrut(8));
    double sub = 0;
    for (var e : cart.entrySet()) {
      int id = e.getKey(); var p = known.get(id); double q = e.getValue(), eff = Db.num(p.get("effective_price")); sub += q * eff;
      JPanel row = new JPanel(new BorderLayout(8, 0)); row.setOpaque(false); row.setMaximumSize(new Dimension(2000, 54)); row.setBorder(App.pad(4, 0));
      row.add(new JLabel("<html><b>" + p.get("emoji") + " " + App.esc(p.get("title")) + "</b><br><span style='color:gray'>" + Db.money(eff) + " / " + p.get("unit_type") + "</span></html>"), BorderLayout.WEST);
      JPanel ea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0)); ea.setOpaque(false);
      Act2 re = () -> { dlg.dispose(); if (!cart.isEmpty()) showCart(); };
      ea.add(mini("−", id, -1, re)); ea.add(App.lbl(App.fmt(q), 13, true, App.GR)); ea.add(mini("+", id, 1, re));
      ea.add(App.lbl(Db.money(q * eff), 13, true, Color.BLACK)); row.add(ea, BorderLayout.EAST); root.add(row);
    }
    double dfee = sub >= 500 ? 0 : 40, pack = 10, pay = sub + dfee + pack;
    root.add(Box.createVerticalStrut(8));
    root.add(App.lbl("Items " + Db.money(sub) + "   |   Delivery " + (dfee == 0 ? "FREE" : Db.money(dfee)) + "   |   Packaging " + Db.money(pack), 12, false, Color.GRAY));
    JTextArea addr = new JTextArea(App.uaddr, 2, 30); addr.setLineWrap(true); addr.setBorder(new LineBorder(Color.LIGHT_GRAY));
    JComboBox<String> date = new JComboBox<>(), slot = new JComboBox<>(), payc = new JComboBox<>(new String[]{"UPI / Wallet", "Card", "Cash on Delivery"});
    for (int i = 1; i <= 7; i++) date.addItem(LocalDate.now().plusDays(i).toString());
    List<Integer> sids = new ArrayList<>(); List<String> states = new ArrayList<>();
    Runnable ls = () -> {
      slot.removeAllItems(); sids.clear(); states.clear();
      try {
        for (var s : Db.query("SELECT * FROM v_slots WHERE slot_date=? ORDER BY start_time", date.getSelectedItem())) {
          sids.add((Integer) s.get("slot_id")); states.add((String) s.get("state"));
          slot.addItem(s.get("start_time").toString().substring(0, 5) + " – " + s.get("end_time").toString().substring(0, 5) + "   (" + s.get("state") + ")");
        }
      } catch (Exception ex) { App.msg(dlg, ex.getMessage()); }
    };
    date.addActionListener(e -> ls.run()); ls.run();
    root.add(Box.createVerticalStrut(10)); root.add(App.lbl("Delivery address", 12, true, Color.DARK_GRAY)); root.add(addr);
    root.add(Box.createVerticalStrut(8)); root.add(App.lbl("Delivery date", 12, true, Color.DARK_GRAY)); root.add(date);
    root.add(Box.createVerticalStrut(8)); root.add(App.lbl("Time slot", 12, true, Color.DARK_GRAY)); root.add(slot);
    root.add(Box.createVerticalStrut(8)); root.add(App.lbl("Payment", 12, true, Color.DARK_GRAY)); root.add(payc);
    root.add(Box.createVerticalStrut(14));
    String[] pm = {"UPI", "CARD", "COD"};
    root.add(App.btn("Place order  •  " + Db.money(pay), App.GR, () -> {
      int i = slot.getSelectedIndex();
      if (i < 0 || "Full".equals(states.get(i))) { App.msg(dlg, "Please choose an available slot"); return; }
      placeOrder(sids.get(i), pm[payc.getSelectedIndex()], addr.getText().trim());
    }));
    for (Component c : root.getComponents()) if (c instanceof JComponent) ((JComponent) c).setAlignmentX(0);
    dlg.add(new JScrollPane(root)); dlg.setVisible(true);
  }

  void placeOrder(int slotId, String pay, String addr) throws SQLException {
    StringBuilder j = new StringBuilder("[");
    for (var e : cart.entrySet()) j.append("{\"product_id\":").append(e.getKey()).append(",\"quantity\":").append(e.getValue()).append("},");
    j.setLength(j.length() - 1); j.append("]");
    try (Connection c = Db.get()) {
      try (PreparedStatement s = c.prepareStatement("CALL place_order(?,?,?,?,CAST(? AS JSON),@oid)")) {
        s.setInt(1, App.uid); s.setInt(2, slotId); s.setString(3, pay); s.setString(4, addr); s.setString(5, j.toString()); s.execute();
      }
      int oid; try (Statement st = c.createStatement(); ResultSet r = st.executeQuery("SELECT @oid")) { r.next(); oid = r.getInt(1); }
      cart.clear(); dlg.dispose(); load();
      App.msg(this, "Order #" + oid + " placed! Track it in My Orders.");
    } catch (SQLException ex) { App.msg(dlg, ex.getMessage()); load(); }
  }

  void showOrders() {
    JDialog d = new JDialog(this, "My orders", true); d.setSize(780, 580); d.setLocationRelativeTo(this);
    JTable t = App.table(); JTextArea det = App.mono();
    Runnable rl = () -> { try { t.setModel(App.model(Db.query("SELECT order_id AS ID, order_status AS Status, total_amount AS Total, created_at AS Placed FROM orders WHERE customer_id=? ORDER BY order_id DESC", App.uid))); } catch (Exception e) { App.msg(d, e.getMessage()); } };
    rl.run();
    t.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && t.getSelectedRow() >= 0)
        try { det.setText(App.orderDetail(((Number) t.getValueAt(t.getSelectedRow(), 0)).intValue())); } catch (Exception ex) { App.msg(d, ex.getMessage()); }
    });
    JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(t), new JScrollPane(det)); sp.setDividerLocation(180);
    d.add(sp, BorderLayout.CENTER);
    d.add(App.btn("Cancel selected order", new Color(0xC0392B), () -> {
      int id = App.selId(t); if (id < 0) return;
      try (Connection c = Db.get(); CallableStatement s = c.prepareCall("{CALL cancel_order(?)}")) { s.setInt(1, id); s.execute(); }
      rl.run(); det.setText(""); load(); App.msg(d, "Order cancelled");
    }), BorderLayout.SOUTH);
    d.setVisible(true);
  }
}