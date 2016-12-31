import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;

public abstract class JRoundButton extends JComponent implements MouseListener{
	
    private static final long serialVersionUID = 1L;
    
    public static final int DIAMETER = 20;
    
    protected int diameter = DIAMETER; 
    
    protected Color pColor = Color.BLACK;
    
    private ArrayList<ActionListener> listeners = new ArrayList<>();
    
    protected boolean mouseEntered = false;
    protected boolean mousePressed = false;
    
	public JRoundButton(Color color) {
		super();
		
		pColor = color;
		
		enableInputMethods(true);
		addMouseListener(this);
		
		setSize(DIAMETER, DIAMETER);
		setFocusable(true);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if(!isEnabled())	return;
		
		this.diameter = Math.min(getWidth(), getHeight());

		if(mousePressed){
			g.setColor(new Color(210, 210, 210));
		}else{
			g.setColor(Color.WHITE);
		}
		g.fillOval((getWidth() - diameter)/2, (getHeight() - diameter)/2, diameter, diameter);
		
		Graphics2D g2d = (Graphics2D) g;
		g2d.setStroke(new BasicStroke((float) Math.max(diameter/120.0, 1)));
		if(diameter >= 60 ^ !mouseEntered){
			g.setColor(new Color(225, 225, 225));
		}else{
			g.setColor(Color.BLACK);
		}			
		g.drawOval((getWidth() - diameter)/2, (getHeight() - diameter)/2, diameter, diameter);
		
		if(mousePressed)
			paintClickedContent(g);
		else if(mouseEntered)
			paintEnteredContent(g);
		else
			paintContent(g);
	}
	
	protected abstract void paintContent(Graphics g);
	protected abstract void paintEnteredContent(Graphics g);
	protected abstract void paintClickedContent(Graphics g);
	
	public Dimension getPreferredSize()
    {
        return new Dimension(DIAMETER, DIAMETER);
    }
	
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }
    
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

	
	private void notifyListeners(MouseEvent e)
    {
		ActionEvent evt;
		if(e != null){
			evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, new String(), e.getWhen(), e.getModifiers());
		}else{
			evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, new String(), System.currentTimeMillis(), 0);
		}
        synchronized(listeners)
        {
            for (int i = 0; i < listeners.size(); i++)
            {
                ActionListener tmp = listeners.get(i);
                tmp.actionPerformed(evt);
            }
        }
    }
	
	public void addActionListener(ActionListener l){
		listeners.add(l);
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mousePressed = true;
		repaint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mousePressed = false;
		if(!isEnabled())	return;
		notifyListeners(e);
		repaint();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if(!isEnabled())	return;
		mouseEntered = true;
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		repaint();
	}

	@Override
	public void mouseExited(MouseEvent e) {
		mouseEntered = false;
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		repaint();
	}
	
	public void doClick(){
		notifyListeners(null);
	}
	
}
