/* Frame: Pattern Editor
 * @author Arnold Lin
 * @date first commit 2016/12/31
 * 
 * The frame in charge of changing password pattern
 */
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.NumberFormatter;

class PatternEditor extends JFrame implements ListDataListener, WindowListener{
	
	private static final long serialVersionUID = 1L;

	private Connection conn;
	private FullStructPane leftPane;
	private FullCardPane rightPane;
	private JScrollPane scrollLeft;
	private BackendDataManager dataManager;
	private JFrame parent;
	
	public PatternEditor(JFrame parent, PasswordPattern p, Connection conn){
		super("Password Structure Editor");
		
		this.parent = parent;
		addWindowListener(this);
		
		this.conn = conn;
		
		dataManager = new BackendDataManager(p);
		dataManager.addListDataListener(this);

		// structPane | tagPane
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		add(scrollLeft = new JScrollPane(leftPane = new FullStructPane(dataManager)), gbc);
		
		gbc.gridx = 2;
		add(rightPane = new FullCardPane(dataManager), gbc);
		
		leftPane.apply(p == null ? null : p.probTuple);
		
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.VERTICAL;
		add(new JSeparator(JSeparator.VERTICAL), gbc);
		
		setLocation(200, 200);
		pack();
		setVisible(true);
		setSize(750, 500);
	}
	
	private void dataChangeDealer(){
		leftPane.updateUI();
		rightPane.updateUI();
	}
	
	@Override
	public void intervalAdded(ListDataEvent e) {
		dataChangeDealer();
	}

	@Override
	public void intervalRemoved(ListDataEvent e) {
		dataChangeDealer();
	}

	@Override
	public void contentsChanged(ListDataEvent e) {
		dataChangeDealer();
	}

	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		// On closing, append the new version to DB
		try {
			this.leftPane.saveToSQL(conn);
			this.dataManager.saveToSQL(conn);
		} catch (SQLException e1) {
			JOptionPane.showMessageDialog(null, String.format("%s - Error occurred while saving data.", e1.getClass().toString()), "SQL ERROR", JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace(System.err);
		}
		this.parent.setVisible(true);
	}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}
}

/* class BackendDataManager
 * The data manager in charge of PassPhrases and groups
 */
class BackendDataManager {
	
	private DefaultListModel<String> lparts;
	private Map<String, String> detail;
	private int version;
	
	public BackendDataManager(PasswordPattern p){
		lparts = new DefaultListModel<String>();
		detail = new LinkedHashMap<>();
		if(p == null){
			lparts.addElement("Default");
			detail.put("Default", "Default");
			version = 0;
		}else{
			version = p.version + 1;
			for (Map.Entry<String, List<String>> e : p.candidateMap.entrySet()) {
				lparts.addElement(e.getKey());
				detail.put(e.getKey(), String.join(":", e.getValue()));
			}
		}
	}
	
	public void addPart(String p){
		if(detail.containsKey(p))	return;
		lparts.addElement(p);
		detail.put(p, "Default");
	}
	
	public String removePart(String p){
		if(!detail.containsKey(p))	return null;
		lparts.removeElement(p);
		return detail.remove(p);
	}
	
	public void renamePart(String p, String np){
		if(!detail.containsKey(p) && detail.containsKey(np)) return;
		lparts.setElementAt(np, lparts.indexOf(p));
		detail.put(np, detail.remove(p));
	}
	
	public void updatePart(String p, String val){
		if(!detail.containsKey(p)) return;
		detail.put(p, val);
	}
	
	public int dataSize(){
		return lparts.getSize();
	}
	
	public String query(String key){
		return detail.get(key);
	}
	
	public boolean contains(String key){
		return detail.containsKey(key);
	}
	
	public DefaultListModel<String> getModel(){
		return lparts;
	}
	
	public DefaultComboBoxModel<String> getComboBoxModel(){
		String[] ah = new String[lparts.size()];
		lparts.copyInto(ah);
		return new DefaultComboBoxModel<>(ah);
	}
	
	public void addListDataListener(ListDataListener l){
		lparts.addListDataListener(l);
	}
	
	public int getVersion(){
		return this.version;
	}
	
	public void saveToSQL(Connection conn) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement(String.format("insert into EveList values (%d,1,?,?)", this.version));
		for(Map.Entry<String, String> e : detail.entrySet()){
			stmt.setString(1, e.getKey());
			stmt.setString(2, e.getValue());
			stmt.addBatch();
		}
		stmt.executeBatch();
	}
}

/* class FullCardPane
 * the UI in charge of changing PassPhrases
 */
class FullCardPane extends JPanel implements ListSelectionListener, ActionListener{
	JScrollPane scrollParts;
	JList<String> parts;
	JGraphicsRB add, change, remove;
	JButton apply;
	JScrollPane scrollText;
	JTextArea editField;
	BackendDataManager backend;
	
	private static Font BTN = new Font("SansSerif", Font.PLAIN, 12);
	private static Font CORE = new Font("SansSerif", Font.PLAIN, 15);
	
	public FullCardPane(BackendDataManager dataManager) {
		backend = dataManager;
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		JLabel left = new JLabel("PassPhrase Groups:");
		left.setFont(CORE);
		gbc.gridx = gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		add(left,gbc);
		
		JLabel right = new JLabel("PassPhrase candidates (one per line):");
		right.setFont(CORE);
		gbc.gridx = 1;
		add(right, gbc);
		
		parts = new JList<>(dataManager.getModel());
		parts.setFont(CORE);
		parts.addListSelectionListener(this);
		scrollParts = new JScrollPane(parts);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(5, 5, 5, 5);
		add(scrollParts, gbc);
		
		editField = new JTextArea();
		editField.setFont(CORE);
		editField.setLineWrap(true);
		scrollText = new JScrollPane(editField);
		gbc.gridx = 1;
		gbc.weightx = 0.5;
		add(scrollText, gbc);
		
		JPanel threeButtons = new JPanel(new GridLayout(1, 3, 10, 0));
		threeButtons.add(add = new JGraphicsRB(JGraphicsRB.PLUS));
		threeButtons.add(change = new JGraphicsRB(JGraphicsRB.PEN));
		threeButtons.add(remove = new JGraphicsRB(JGraphicsRB.CROSS));
		add.addActionListener(this);
		remove.addActionListener(this);
		change.addActionListener(this);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.BOTH;
		add(threeButtons, gbc);
		
		apply = new JButton("Apply");
		apply.setFont(BTN);
		gbc = new GridBagConstraints();
		gbc.gridy = 2;
		gbc.gridx = 1;
		gbc.ipadx = 10;
		gbc.anchor = GridBagConstraints.NORTHEAST;
		gbc.insets = new Insets(5, 5, 5, 5);
		apply.addActionListener((e) -> {
			String result = Utils.blockContentCleanseAndPack(editField.getText());
			backend.updatePart(parts.getSelectedValue(), result);
			editField.setText(Utils.unpackBlockContent(result));
		});
		add(apply, gbc);
		
		updateUI();
	}
	
	@Override
	public void updateUI() {
		if(parts != null){
			remove.setEnabled(!parts.isSelectionEmpty() && backend.dataSize() > 1);
			change.setEnabled(!parts.isSelectionEmpty());
			apply.setEnabled(!parts.isSelectionEmpty());
		}
		super.updateUI();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		editField.setText(Utils.unpackBlockContent(backend.query(parts.getSelectedValue())));
		updateUI();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object s = e.getSource();
		String result = null;
		if(s == remove){
			backend.removePart(parts.getSelectedValue());
			return;
		}else if(s == add){
			do{
				if((result = JOptionPane.showInputDialog("Please enter the name of the new component:", "DefaultName")) == null)
					return;
				result = Utils.titleSafeCleanse(result);
				if(result.length() == 0)
					JOptionPane.showMessageDialog(null, "Invalid component name", "Error", JOptionPane.ERROR_MESSAGE);
				else if(backend.contains(result)){
					JOptionPane.showMessageDialog(null, "Repetitive component name", "Error", JOptionPane.ERROR_MESSAGE);
					result = "";
				}
			}while(result.length() == 0);
			backend.addPart(result);
		}else if(s == change){
			do{
				if((result = JOptionPane.showInputDialog("Please enter a new name of the component:", parts.getSelectedValue())) == null)
					return;
				result = Utils.titleSafeCleanse(result);
				if(result.length() == 0)
					JOptionPane.showMessageDialog(null, "Invalid component name", "Error", JOptionPane.ERROR_MESSAGE);
				else if(backend.contains(result) && !result.equals(parts.getSelectedValue())){
					JOptionPane.showMessageDialog(null, "Repetitive component name", "Error", JOptionPane.ERROR_MESSAGE);
					result = "";
				}
			}while(result.length() == 0);
			
			backend.renamePart(parts.getSelectedValue(), result);
		}
	}
}

/* class FullStructPane
 * the dynamic UI in charge of creating password structure
 */
class FullStructPane extends JPanel implements ActionListener{
	private static final long serialVersionUID = 1L;
	static final int MAXI_OUT = 6;
	static final String[] TITLES = Pattern.compile(":").split("First:Second:Third:Fourth:Fifth:Sixth");
	
	JPanel structsPane;
	JGraphicsRB plus;
	Map<StructPane, Integer> panes;
	BackendDataManager data;
	
	public FullStructPane(BackendDataManager data){
		this.data = data;
		setLayout(new GridBagLayout());
		
		structsPane = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.NORTH;
		add(structsPane, gbc);
		
		gbc = new GridBagConstraints();
		JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
		gbc.gridy = 1;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(sep, gbc);
		
		gbc = new GridBagConstraints();
		plus = new JGraphicsRB(JGraphicsRB.PLUS);
		plus.addActionListener(this);
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.SOUTH;
		add(plus, gbc);
	
		panes = new LinkedHashMap<>();
	}
	
	public void apply(List<List<Tuple>> guide){
		if(guide == null){
			addOne();
			return;
		}
		for(List<Tuple> l: guide){
			StructPane block = addOne();
			for(Tuple t: l){
				block.addEntry(((Double)t.get(0)).doubleValue(), Math.max(data.getModel().indexOf(t.get(1)), 0));
			}
			block.entries.get(0).del.doClick();
		}
	}
	
	public StructPane addOne(){
		// Expedient
		String[] things = new String[10];
		for(int i = 0; i < things.length; things[i] = String.format("%02d", i++));
		
		StructPane block = new StructPane(this.data);
		block.minus.addActionListener(this);
		block.setParent(this);
		GridBagConstraints gbc = new GridBagConstraints();
		// Find a proper key
		gbc.gridy = (panes.keySet().size() == 0) ? 0 : (Utils.max(panes.values()).intValue() + 1);
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.NORTH;
		panes.put(block, gbc.gridy);
		structsPane.add(block, gbc);
		revalidate();
		
		return block;
	}
	
	public void removeOne(StructPane toRemove){
		panes.remove(toRemove);
		structsPane.remove(toRemove);
		
		if(panes.keySet().size() == 1){
			StructPane onlyOne = (StructPane) panes.keySet().toArray()[0];
			GridBagLayout gbl = (GridBagLayout) structsPane.getLayout();
			GridBagConstraints gbc = gbl.getConstraints(onlyOne);
			gbc.gridy = 0;
			panes = new HashMap<>();
			panes.put(onlyOne, 0);
			gbl.setConstraints(onlyOne, gbc);
		}
		revalidate();
	}

	@Override
	public void updateUI() {
		if(panes != null){
			int elems = panes.keySet().size();
			plus.setEnabled(elems < MAXI_OUT);
			for(StructPane p : panes.keySet()){
				p.minus.setEnabled(elems > 1);
				p.updateThings();
				p.title.setText(TITLES[panes.get(p)]);
			}
		}
		super.updateUI();
	}
	
	// If minus or plus button is clicked
	@Override
	public void actionPerformed(ActionEvent e) {
		JGraphicsRB init = (JGraphicsRB) e.getSource();
		if(init.isEnabled()){ 
			Container initP = init.getParent(); 
			if(initP == this) {	// Add
				this.addOne();
			}else{
				initP = initP.getParent();
				if(initP instanceof StructPane){	// Remove 
					this.removeOne((StructPane) initP);
				}
			}
		}
		updateUI();
	}
	
	
	public void saveToSQL(Connection conn) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement(String.format("insert into EveList values (%d, 0, ?, ?)", data.getVersion()));
		List<StructPane> l = new ArrayList<>(panes.keySet());
		String[] parts = new String[l.size()];
		for(int i = 0; i < parts.length; i++){
			StringBuffer sb = new StringBuffer();
			for(EntryPane ep: l.get(i).entries){
				sb.append(ep);
				sb.append(",");
			}
			parts[i] = sb.substring(0, sb.length()-1);
		}
		stmt.setString(1, "prob");
		stmt.setString(2, String.join(":", parts));
		stmt.executeUpdate();
	}
}

/* class StructPane
 * the dynamic UI contains up to 6 EntryPanes.
 * The sequence of EntryPanes will be the sequence in the password generating step.
 */
class StructPane extends JPanel implements ActionListener, FocusListener{
	
	private static final long serialVersionUID = 1L;
	private static Font titleFont = new Font("SansSerif", Font.PLAIN, 15);
	
	JRoundButton minus; 
	EntryPane plusPane;
	JLabel title;
	ArrayList<EntryPane> entries;
	JPanel parent;
	BackendDataManager data;
	
	public StructPane(BackendDataManager data) {
		this.data = data;
		setLayout(new GridLayout(0,1));
		JPanel titleP = new JPanel(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		minus = new JGraphicsRB(JGraphicsRB.MINUS);
		minus.setSize(10, 10);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(5,5,10,5);
		minus.setEnabled(false);
		titleP.add(minus, gbc);
		
		gbc = new GridBagConstraints();
		title = new JLabel("First");
		title.setFont(titleFont);
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;
		gbc.insets = new Insets(0, 5, 0, 25);
		gbc.anchor = GridBagConstraints.WEST;
		titleP.add(title, gbc);
		
		add(titleP);
		
		
		entries = new ArrayList<>();
		addEntry();
		
	}
	
	@Override
	public void updateUI(){
		super.updateUI();
		if(parent != null)
			parent.updateUI();
		if(entries != null)
			for(EntryPane pane : entries)
				pane.del.setEnabled(entries.size() > 1);
	}
	
	public void updateThings(){
		for(EntryPane pane: entries){
			pane.updateThings(data.getComboBoxModel());
		}
		if(plusPane != null)
			plusPane.updateThings(data.getComboBoxModel());
	}
	
	private void addEntry(EntryPane e){
		e.del.addActionListener(this);
		e.p.addFocusListener(this);
		add(e);
		updateUI();
		revalidate();
	}
	
	public void addEntry(){
		addEntry(0, 0);
	}
	
	public void addEntry(double p, int sid){
		EntryPane nPane;
		if(entries.isEmpty()){
			addEntry(nPane = new EntryPane(data.getComboBoxModel()));
		}else{
			(nPane = plusPane).enable();
		}
		nPane.setP(p);
		nPane.setSid(sid);
		nPane.enable();
		entries.add(nPane);
		
		addEntry(plusPane = new EntryPane(data.getComboBoxModel()));
		plusPane.disable();
	}
	
	void removeEntry(EntryPane e){
		remove(e);
		entries.remove(e);
		updateUI();
		revalidate();
	}
	
	public void setParent(JPanel parent){
		this.parent = parent;
	}

	
	// If a button (minus / plus) is triggered
	@Override
	public void actionPerformed(ActionEvent e) {
		EntryPane toRemove = (EntryPane) ((JGraphicsRB) e.getSource()).getParent();
		if(toRemove.p.isEnabled()){ // Delete
			this.removeEntry(toRemove);
		}else{	// Add
			this.addEntry();
		}
	}

	@Override
	public void focusGained(FocusEvent e) {
		// TODO Auto-generated method stub
		;
	}

	@Override
	public void focusLost(FocusEvent e) {
		Double[] things = new Double[entries.size()];
		for(int i = 0; i < things.length; things[i] = Double.parseDouble(entries.get(i++).p.getText()));
		double sum = Utils.sum(things);
		if(sum <= 100) return; 
		// Normalization
		for(int i = 0; i < things.length; i++){
			entries.get(i).p.setText(String.format("%.2f", things[i] / sum * 100));
		}
	}
	
}

/* class EntryPane 
 * the dynamic UI of an "entry": a pack of probablity this entry will be used in password generating step, the PassPhrase group the step will be used.
 */
class EntryPane extends JPanel{
	
	private static final long serialVersionUID = 1L;
	
	private static Font mono = new Font("Monospace", Font.PLAIN, 12);
	
	JGraphicsRB del;
	JFormattedTextField p; 
	JLabel perc;
	JComboBox<String> comps;
	
	public EntryPane(ComboBoxModel<String> model){
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		// Place holder
		JLabel ph = new JLabel("");
		gbc.gridx = 0;
		gbc.ipadx = gbc.ipady = JRoundButton.DIAMETER;
		gbc.insets = new Insets(2, 5, 2, 5);
		add(ph, gbc);
		
		// delete
		del = new JGraphicsRB(JGraphicsRB.CROSS);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(2, 5, 2, 5);
		add(del, gbc);
		
		// P = ? %
		NumberFormat format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(2);
		format.setMaximumIntegerDigits(3);
		NumberFormatter formatter = new NumberFormatter(format);
		p = new JFormattedTextField(formatter);
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.weightx = 0.25;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 5, 0, 5);
		p.setHorizontalAlignment(SwingConstants.RIGHT);
		p.setValue(100);
		p.setColumns(5);
		p.setFont(mono);
		add(p, gbc);
		
		// Percent char
		perc = new JLabel("%");
		perc.setFont(mono);
		gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.ipadx = 10;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.anchor = GridBagConstraints.WEST;
		add(perc, gbc);
		
		// Dropdown
		comps = new JComboBox<String>(model);
		comps.setFont(mono);
		gbc = new GridBagConstraints();
		gbc.gridx = 4;
		gbc.weightx = 0.75;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 5, 0, 5);
		add(comps, gbc);
	}
	
	public EntryPane(ComboBoxModel<String> model, double p, int sid){
		this(model);
		setP(p);
		setSid(sid);
		
	}
	
	public void setP(double p){
		this.p.setText(String.format("%.1f", p));
	}
	
	public void setSid(int sid){
		sid = Math.min(sid, comps.getItemCount());
		this.comps.setSelectedIndex(sid);
	}
	
	public void updateThings(ComboBoxModel<String> model){
		int selected = comps.getSelectedIndex();
		comps.setModel(model);
		comps.setSelectedIndex(Math.min(selected, model.getSize() - 1));
	}

	private void setable(boolean able){
		del.setGraphic(able ? JGraphicsRB.CROSS : JGraphicsRB.PLUS);
		p.setEnabled(able);
		comps.setEnabled(able);
	}
	
	public void disable(){
		setable(false);
	}
	
	public void enable() {
		setable(true);
	}
	
	public String toString(){
		return String.format("%s,%s", p.getText(), comps.getSelectedItem());
	}
	
}