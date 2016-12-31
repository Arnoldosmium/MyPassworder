import java.util.*;
import java.util.List;
import java.sql.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class Passworder {
	
	private static Connection conn;
	private static Statement stmt;
	
	public static void main(String[] args) throws Exception {
		conn = DriverManager.getConnection("jdbc:sqlite:data.db");
		stmt = conn.createStatement();
		// Prepare tables
		prepareTables();
		
		EventQueue.invokeLater(() -> {
			//new PatternEditor(stmt).setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			new MainFrame(conn);
		});
	}
	
	private static void prepareTables() throws Exception{
		ResultSet rstSet = stmt.executeQuery("select name from sqlite_master where type='table'");

		Set<String> names = new HashSet<>();
		while(rstSet.next()){
			names.add(rstSet.getString("name"));
		}
		
		// Snapshot history
		if(!names.contains("EveList")){
			stmt.executeUpdate("create table EveList("
					+ "actseq integer,"
					+ "acttype integer,"
					+ "identifier not null,"
					+ "arguments not null)");
		}
		
		// WebObjects
		if(!names.contains("WebObjects")){
			stmt.executeUpdate("create table WebObjects("
					+ "name not null,"
					+ "nameid primary key,"
					+ "stage integer not null,"
					+ "structVersion integer not null,"
					+ "init not null)");
		}
	}
	
}

class MainFrame extends JFrame implements WindowListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Font NORMAL = new Font("SansSerif", Font.PLAIN, 15);
	
	private DefaultListModel<WebObject> lwebs;
	private JList<WebObject> webs;
	private JScrollPane swebs;
	private JLabel currweb;
	private JLabel itera;
	private Button donext;
	private JPanel showTable;
	private JGraphicsRB plus;
	private JGraphicsRB minus;
	private JGraphicsRB change;
	
	private JPanel left, right;
	
	private JMenuBar mBar;
	private JMenu edit;
	private JMenuItem editItem;
	
	private Connection conn;
	private Statement stmt;
	private List<PasswordPattern> patterns;
	
	private WebObject currentSelection = null;
	
	public MainFrame(Connection conn){
		super("My Passworder Ver 0.1");
		this.conn = conn;
		try{
			stmt = conn.createStatement();
			initData();
		}catch (SQLException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		setLayout(new GridBagLayout());
		this.addWindowListener(this);
		GridBagConstraints gbc = new GridBagConstraints();
		
		// Menu
		mBar = new JMenuBar();
		mBar.add(edit = new JMenu("Structure"));
		edit.setMnemonic(KeyEvent.VK_S);
		edit.getAccessibleContext().setAccessibleDescription("Edit the password structure");
		edit.add(editItem = new JMenuItem("Edit"));
		editItem.setMnemonic(KeyEvent.VK_E);
		editItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
		editItem.addActionListener((e) -> {
			this.setVisible(false);
			new PatternEditor(this, patterns.isEmpty() ? null : patterns.get(patterns.size()-1), conn);
		});
		setJMenuBar(mBar);
		
		
		// Leftpane
		left = new JPanel(new GridBagLayout());
		
		// JList
		webs = new JList<>(lwebs);
		webs.setFont(NORMAL);
		webs.addListSelectionListener((e) -> refreshUI());
		swebs = new JScrollPane();
		swebs.setViewportView(webs);
		gbc.gridx = gbc.gridy = 0;
		gbc.weightx = gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(25,25,10,10);
		left.add(swebs, gbc);
		
		// Rightpane
		right = new JPanel(new GridBagLayout());
		
		// Website Label
		currweb = new JLabel();
		currweb.setFont(new Font("SansSerif", Font.BOLD, 25));
		currweb.setBorder(new MatteBorder(0,0,1,0, new Color(0, 0, 0)));
		gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;
		gbc.weighty = 0.1;
		gbc.insets = new Insets(20,20,5,20);
		gbc.anchor = GridBagConstraints.NORTH;
		right.add(currweb, gbc);
		
		// iteration
		itera = new JLabel("0");
		itera.setFont(new Font("Monospace", Font.PLAIN, 15));
		itera.setHorizontalAlignment(SwingConstants.RIGHT);
		gbc.gridy = 1;
		gbc.gridwidth = gbc.gridheight = 1;
		gbc.weightx = 0.2;
		gbc.weighty = 0.1;
		gbc.ipadx = 10;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(10,10,10,10);
		right.add(itera, gbc);
		
		// Do next
		donext = new Button("Next Version");
		donext.setFont(NORMAL);
		donext.setEnabled(false);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(10,10,10,10);
		right.add(donext, gbc);
		donext.addActionListener((e) -> {
			nextStage();
			refreshUI();
		});
		
		showTable = new JPanel(new GridLayout(2, 0, 10, 10));
		gbc = new GridBagConstraints();
		gbc.gridy = 2;
		gbc.gridheight = gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;
		gbc.weighty = 0.8;
		gbc.ipady = 20;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(20, 20, 20, 20);
		right.add(showTable, gbc);
		TitledBorder b = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),"Generated Password");
		b.setTitleFont(NORMAL);
		showTable.setBorder(b);
		
		JPanel threeBtn = new JPanel(new GridLayout(1,3,10,10));
		threeBtn.add(plus = new JGraphicsRB(JGraphicsRB.PLUS));
		plus.addActionListener((e) -> {
			String result = JOptionPane.showInputDialog(null, "Please enter a name of the new password instance", "New Password Instance", JOptionPane.INFORMATION_MESSAGE);
			if(result != null)
				addWebObject(result);
		});
		threeBtn.add(change = new JGraphicsRB(JGraphicsRB.PEN));
		change.addActionListener(e -> {
			String result = JOptionPane.showInputDialog(null, "Please enter new name of the password instance", currentSelection.name);
			if(result != null)
				renameWebObject(result);
			refreshUI();
		});
		threeBtn.add(minus = new JGraphicsRB(JGraphicsRB.CROSS));
		minus.addActionListener(e -> {
			if(JOptionPane.showConfirmDialog(null, "Removing password instance is not recoverable. Are you sure?", "Remove Password Instance", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
				removeWebObject();
			refreshUI();
		});
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		left.add(threeBtn, gbc);
		
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.ipadx = 20;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		add(left, gbc);
		
		gbc.gridx = 1;
		gbc.ipadx = 0;
		gbc.weightx = 1;
		add(right,gbc);
		
		pack();
		refreshUI();
		setLocation(100, 100);
		setSize(600,550);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setVisible(true);
		
		if(patterns.isEmpty()){
			JOptionPane.showMessageDialog(null, "Thanks for using MyPassworder.\nBefore you start, please specify your general password structure.",
					"First Time User", JOptionPane.INFORMATION_MESSAGE);
			editItem.doClick();
		}
	}

	private void initData() throws SQLException{
		// WebObjects
		lwebs = WebObject.compileQuery(stmt.executeQuery("select * from WebObjects"));
		if(lwebs.getSize() == 0){
			addWebObject("Default");
		}
		
		// Password Pattern
		refreshDB();
	}
	
	private void refreshDB() throws SQLException{
		patterns = PasswordPattern.compileQuery(stmt.executeQuery("select * from EveList order by actseq, acttype"));
	}
	
	private void addWebObject(String obj){
		String key = obj;
		int i = 0;
		while(lwebs.contains(key)){
			key = String.format("%s%03d", obj, i++);
		}
		try{
			PreparedStatement prp= conn.prepareStatement("insert into WebObjects values (?, ?, ?, ?, ?)");
			prp.setString(1, obj);
			prp.setString(2, key);
			prp.setInt(3, 0);
			prp.setInt(4, patterns.size() - 1);
			prp.setInt(5, 0);
			prp.executeUpdate();
		}catch (Exception e) {
			JOptionPane.showMessageDialog(null, String.format("%s - Failed to update database.", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		// updated in sql
		lwebs.addElement(new WebObject(obj, key, 0, patterns.size() - 1, 0));
	}
	
	private void renameWebObject(String name) {
		try{
			PreparedStatement prp = conn.prepareStatement("update WebObjects set name=? where nameid=?");
			prp.setString(1, name);
			prp.setString(2, currentSelection.nameid);
			prp.executeUpdate();
		}catch (SQLException e) {
			JOptionPane.showMessageDialog(null, String.format("%s - Failed to update the password instance.", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		currentSelection.name = name;
	}
	
	private void nextStage(){
		try{
			PreparedStatement prp = conn.prepareStatement("update WebObjects set stage=? where nameid=?");
			prp.setInt(1, currentSelection.stage+1);
			prp.setString(2, currentSelection.nameid);
			prp.executeUpdate();
		}catch (SQLException e) {
			JOptionPane.showMessageDialog(null, String.format("%s - Failed to update the password instance.", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		currentSelection.stage += 1;
	}
	
	private void removeWebObject() {
		try{
			PreparedStatement prp = conn.prepareStatement("delete from WebObjects where nameid=?");
			prp.setString(1, currentSelection.nameid);
			prp.executeUpdate();
		}catch (SQLException e) {
			JOptionPane.showMessageDialog(null, String.format("%s - Failed to remove the password instance.", e.getMessage()), "Database Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		lwebs.remove(webs.getSelectedIndex());
	}
	
	private void generatePassword(){
		showTable.removeAll();
		if(webs.isSelectionEmpty()){
			JLabel l = new JLabel("Waiting Selection");
			l.setFont(NORMAL);
			l.setHorizontalAlignment(SwingConstants.CENTER);
			showTable.add(l);
			return;
		}
		// When reaches, patterns should not be empty
		PasswordPattern patt = patterns.get(currentSelection.pwVersion);
		List<Tuple<String>> pw = patt.generatePassword(currentSelection.nameid, currentSelection.stage);
		
		for(Tuple<String> each: pw){
			JLabel t = new JLabel(each.get(0));
			t.setFont(NORMAL);
			t.setHorizontalAlignment(SwingConstants.CENTER);
			showTable.add(t);
		}
		for(Tuple<String> each: pw){
			JLabel t = new JLabel(each.get(1));
			t.setFont(new Font("SansSerif", Font.BOLD, 15));
			t.setHorizontalAlignment(SwingConstants.CENTER);
			showTable.add(t);
		}
	}
	
	private void refreshUI(){
		boolean selected = !webs.isSelectionEmpty();
		donext.setEnabled(selected);
		currweb.setText( selected ? 
				(currentSelection = webs.getSelectedValue()).toString() : 
				((lwebs.getSize() == 0 ? "Add a new" : "Click one") + " to start"));
		itera.setText(String.valueOf(selected ? currentSelection.stage : 0));
		change.setEnabled(selected);
		minus.setEnabled(selected && lwebs.getSize() > 1);
		generatePassword();
		left.updateUI();
		right.updateUI();
	}
	
	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		try {
			conn.close();
		} catch (SQLException e1) {
			//e1.printStackTrace();
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) {
		if(conn != null)
			try{
				this.refreshDB();
			}catch(Exception e1){
				JOptionPane.showMessageDialog(null, e1.getClass().toString() + " - Error occured while retrieving data", "DATA ERROR", JOptionPane.ERROR_MESSAGE);
			}
	}

	@Override
	public void windowDeactivated(WindowEvent e) {}
}

class WebObject{
	String name;
	String nameid;
	int stage;
	int pwVersion;
	int init;	// Reserved
	
	public WebObject(String name, String nameid, int stage, int pwVersion, int init){
		this.name = name;
		this.nameid = nameid;
		this.stage = stage;
		this.pwVersion = pwVersion;
		this.init = init;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
	
	public static DefaultListModel<WebObject> compileQuery(ResultSet r) throws SQLException{
		DefaultListModel<WebObject> rtn = new DefaultListModel<>();
		while(r.next()){
			rtn.addElement(new WebObject(r.getString(1), r.getString(2), r.getInt(3), r.getInt(4), r.getInt(5)));
		}
		return rtn;
	}
}

class PasswordPattern{
	
	List<List<Tuple>> probTuple;
	Map<String, List<String>> candidateMap;
	int version;
	
	public PasswordPattern(int version){
		probTuple = new ArrayList<>();
		candidateMap = new HashMap<>();
		this.version = version;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setPattern(String patternStr){
		String[] parts = patternStr.split(":");
		for(String s: parts){
			ArrayList<Tuple> part = new ArrayList<>();
			probTuple.add(part);
			String[] splited = s.split(",");
			for(int i = 0; i < splited.length; i+=2){
				if(i < splited.length - 1)
					part.add(new Tuple(Double.parseDouble(splited[i]), splited[i+1]));
				else
					part.add(new Tuple(splited[i]));
			}
		}
	}
	
	public List<Tuple<String>> generatePassword(String key, int stage){
		Random r = new Random((long)key.hashCode() * stage);
		ArrayList<Tuple<String>> pw = new ArrayList<>();
		for(List<Tuple> l : probTuple){
			double rand = r.nextFloat() * 100;
			double cumsum = 0;
			String qKey= null;
			String selection = null;
			for(Tuple t : l)
				if(rand <= (cumsum += ((Double) t.get(0)).doubleValue())){
					List<String> candidates = candidateMap.get(qKey = (String) t.get(1));
					selection = candidates.get(r.nextInt(candidates.size()));
					break;
				}
			pw.add(selection == null ? new Tuple<>("(Skip)", "") : new Tuple<>(qKey, selection));
		}
		return pw;
	}
	
	public static List<PasswordPattern> compileQuery(ResultSet r) throws SQLException{
		List<PasswordPattern> rtn = new ArrayList<>();
		while(r.next()){
			int seq = r.getInt("actseq");
			while(seq >= rtn.size()){
				rtn.add(new PasswordPattern(rtn.size()));
			}
			PasswordPattern p = rtn.get(seq);
			switch(r.getInt("acttype")){
			case 0: 	// guide string;
				p.setPattern(r.getString("arguments"));
				break;
				
			case 1:
				p.candidateMap.put(r.getString("identifier"), Arrays.asList(r.getString("arguments").split(":")));
				break;
				
			default:
				System.err.printf("DATA INTEGRITY ERROR: invalid type info \"%d\"\n", r.getInt("acttype"));
			}
		}
		return rtn;
	}
}

