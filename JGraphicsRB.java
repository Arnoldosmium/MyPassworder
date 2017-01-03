/** JGraphicsRB: An extension of JRoundButton, supporting 4 basic graphics: plus, minus, cross signs, and a pen graphic
 * @author Arnold Lin
 * @date first commit 2016/12/31
 */
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JFrame;

public class JGraphicsRB extends JRoundButton {

	protected int halfLen;
	private int graphic;
	public static final int MINUS = -1;
	public static final int PLUS = 1;
	public static final int CROSS = 0;
	public static final int PEN = 2;
	
	public JGraphicsRB(int graphicType) {
		super(Color.BLACK);
		graphic = graphicType;
	}

	public void setGraphic(int graph){
		if(graph < -1 || graph > 1)	return;
		this.graphic = graph;
	}
	
	public int getGraphic(){
		return this.graphic;
	}
	
	@Override
	protected void paintContent(Graphics g) {
		this.halfLen = this.diameter / 4;
		
		Graphics2D g2d = (Graphics2D) g;		
		Stroke org = g2d.getStroke();
		float strkWidth = (float) (diameter/30.0);
		g2d.setStroke(new BasicStroke(strkWidth));
		g.setColor(pColor);
		switch (graphic) {
		case PLUS:
			g.drawLine(getWidth()/2, getHeight()/2 - halfLen, getWidth()/2, getHeight()/2 + halfLen);
		case MINUS:
			g.drawLine(getWidth()/2 - halfLen, getHeight()/2, getWidth()/2 + halfLen, getHeight()/2);
			break;
			
		case CROSS:
			int move = (int) (halfLen / Math.sqrt(2));
			g.drawLine(getWidth()/2 - move, getHeight()/2 - move, getWidth()/2 + move, getHeight()/2 + move);
			g.drawLine(getWidth()/2 - move, getHeight()/2 + move, getWidth()/2 + move, getHeight()/2 - move);
			break;
			
		case PEN:
			drawPen(g, getWidth()/2, getHeight()/2, halfLen*2, (int) (strkWidth*3), halfLen/2, Math.toRadians(60), Math.toRadians(30));
		default:
			break;
		}
		
		
		g2d.setStroke(org);
	}
	
	private static void drawPen(Graphics g, int x, int y, int l, int w, int t, double angle, double penAngle){
		double theta = Math.atan(w / (double)l);
		double rho = Math.sqrt(l*l + w*w) / 2;
		
		double[] newThetas = {theta - angle, -theta - angle, Math.PI + theta - angle, Math.PI - theta - angle};
		int[] xPoints = new int[4];
		int[] yPoints = new int[4];
		
		for(int i = 0; i < 4; i++){
			xPoints[i] = (int) (x + rho * Math.cos(newThetas[i]));
			yPoints[i] = (int) (y - rho * Math.sin(newThetas[i]));
		}
		
		g.drawPolygon(xPoints, yPoints, 4);
		
		double halfPenAngle = penAngle / 2.0;
		double topHeight = t / Math.cos(halfPenAngle);
		double dropX = x + (rho + topHeight) * Math.cos(-angle);
		double dropY = y - (rho + topHeight) * Math.sin(-angle);
		
		g.drawPolygon(new int[]{(int) dropX, xPoints[0], xPoints[1]}, 
				new int[]{(int) dropY, yPoints[0], yPoints[1]}, 3);
		
		int[] triX = {0, (int) dropX, 0};
		int[] triY = {0, (int) dropY, 0};
		for(int j = -1; j <= 1; j+=2){
			triX[j+1] = (int) (dropX + (t / 1.5) * Math.cos(Math.PI - angle + j * halfPenAngle));
			triY[j+1] = (int) (dropY - (t / 1.5) * Math.sin(Math.PI - angle + j * halfPenAngle));
		}
		
		g.fillPolygon(triX, triY, 3);
	}

	@Override
	protected void paintEnteredContent(Graphics g) {
		paintContent(g);
	}

	@Override
	protected void paintClickedContent(Graphics g) {
		paintContent(g);
	}

	public static void main(String[] args) {
		JFrame test = new JFrame();
		
		test.setLayout(new GridLayout(1,0));
		for(int i = -1; i <= 2; i++){	// Constant from -1 to 1
			JRoundButton minus = new JGraphicsRB(i);
			minus.setBorder(BorderFactory.createLineBorder(Color.BLUE));
			test.add(minus, BorderLayout.CENTER);			
		}
		
		test.pack();
		test.setVisible(true);
		test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
