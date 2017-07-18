package kepard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import krpc.client.Connection;

public class JWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = -3585786710376675786L;
	
	public MyPanel panel;
	public JPilot pilot;
	
	ClassComunicator cc = new ClassComunicator();
	
	public JButton button_launch;
	public JButton button_abort;
	public JTextArea text_ap;
	public ArrayList<String> paintText;
	Dimension log_box = new Dimension(400, 250);
	Dimension paintStartPos = new Dimension(10, 1);
	int paintDistance = 13;
	
	int telemetry_box_width = 250;
	public String[][] telemetry_booster;
	public dVector2 telemetry_booster_finalDistance = new dVector2(-999,-999);
	public boolean[] telemetry_booster_brakes = new boolean[4];
	public String[][] telemetry_capsule;
	
	boolean paintReady = false;
	
	public JWindow(String name)
	{
		super(name);
		 
		panel = new MyPanel(this);
		this.setContentPane(panel);
		
		setPreferredSize(log_box);
		
		setAlwaysOnTop(true);
        setVisible(true);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
		
        paintText = new ArrayList<String>();
        
		setLayout(null);
		
		button_launch = new JButton("LAUNCH");
		button_launch.addActionListener(this);
		add(button_launch);
		
		button_abort = new JButton("ABORT");
		button_abort.addActionListener(this);
		button_abort.setBounds(300, 0, 100, 220);
		
		Dimension size = new Dimension();
		size.width = 200;
		size.height = 86;
		Dimension pos = new Dimension(190, 100);
		button_launch.setBounds(pos.width-Math.round(size.width/2f), pos.height-Math.round(size.height/2f), size.width, size.height);
		
		text_ap = new JTextArea("75000");
		text_ap.setBounds(105, 170, 170, 15);
		
		add(text_ap);
		
		pack();
	}
	
	int num = 0;
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();
		
		if(src == button_launch)
		{
			if(hasOnlyNumbers(text_ap.getText()))
			{
				int h = Integer.parseInt(text_ap.getText());
				h = Math.min(Math.max(30000, h), 170000);
				
				try {
					Main.connection = Connection.newInstance();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				if(Main.connection != null)
				{
					remove(button_launch);
					remove(text_ap);
					getGraphics().clearRect(0, 0, getWidth(), getHeight());
					
					pilot.ascent_apoapsis = h;
					
					cc.start();
					
					setPreferredSize(new Dimension(telemetry_box_width + log_box.width, log_box.height));
					pack();
					
					add(button_abort);
					paintReady = true;
					repaint();
				}
				else
				{
					panel.paint_arg = MyPanel.PAINT_ARG_CONNECTION_RED;
					repaint();
				}
			}
		}
		else if(src == button_abort)
		{
			pilot.setBoosterProgram(JPilot.BOOSTER_PROGRAM_ABORT);
		}
	}
	
	public void PaintText(String text)
	{
		paintText.add(text);
		panel.paint_arg = MyPanel.PAINT_ARG_FLIGHT_LOG;
		repaint();
	}
	
	public void PaintTelemetry(boolean booster, boolean capsule, boolean align)
	{
		panel.paint_arg = MyPanel.PAINT_ARG_FLIGHT_TELEMETRY;
		panel.telemetry_isActive[0] = booster;
		panel.telemetry_isActive[1] = capsule;
		panel.telemetry_isActive[2] = align;
		repaint();
	}
	
	boolean hasOnlyNumbers(String s)
	{
		for(int i = 0; i < s.length(); i++)
		{
			String ns = s.substring(i, i+1);
			if(!(ns.equals("0") || ns.equals("1") || ns.equals("2") || ns.equals("3") || ns.equals("4") || ns.equals("5") || ns.equals("6") || ns.equals("7") || ns.equals("8") || ns.equals("9")))
				return false;
		}
		return true;
	}
}

class MyPanel extends JPanel
{
	private static final long serialVersionUID = 7154370044938037223L;
	
	public static int PAINT_ARG_NONE = 0;
	public static int PAINT_ARG_CONNECTION_RED = 1;
	public static int PAINT_ARG_FLIGHT_LOG = 2;
	public static int PAINT_ARG_FLIGHT_TELEMETRY = 3;
	public int paint_arg = PAINT_ARG_NONE;
	
	public boolean[] telemetry_isActive = new boolean[3];
	public static int[] telemetry_startPos = {10, 130};
	
	Rectangle telemetry_align_box = new Rectangle(90, 75, 135, 135);
	double telemetry_align_box_realSize = 65;
	double telemetry_align_pad_realSize = 8;
	double telemetry_align_rocket_realSize = 2;
	double telemetry_brakes_realDistance = 2;
	double telemetry_brakes_realWidth = 1.3;
	double telemetry_brakes_realHeight = 2.5;
	Point telemetry_pad_distance_pos = new Point(0, 15);
	
	JWindow window;
	
	public MyPanel(JWindow w)
	{
		window = w;
		setPreferredSize(w.getPreferredSize());
	}
	
	private void PaintFlightLog(Graphics g)
	{
		int paintsCount = -1;
		
		int textSize = 0;
		for(int i = 0; i < window.paintText.size(); i++)
			for(int k = 0; k < window.paintText.get(i).split("\n").length; k++)
				textSize++;
		
		g.clearRect(0, 0, window.button_abort.getBounds().x-1, getHeight());
		for(int i = 0; i < window.paintText.size(); i++)
		{
			String text = window.paintText.get(i);
			
			if(text.substring(0, 5).equals("ALERT")) g.setColor(Color.RED);
			
			String[] texts = text.split("\n");
			
			for(int k = 0; k < texts.length; k++)
			{
				paintsCount++;
				g.drawString(texts[k], window.paintStartPos.width, getHeight() - (window.paintStartPos.height + window.paintDistance*(textSize -paintsCount)));
			}
			
			g.setColor(Color.BLACK);
		}
	}
	
	private void PaintFlightTelemetry(Graphics g)
	{
		g.clearRect(window.log_box.width, 0, window.log_box.width + window.telemetry_box_width, window.log_box.height);
		g.drawString("Booster:", window.log_box.width + telemetry_startPos[0], 10);
		g.drawString("Capsule:", window.log_box.width + telemetry_startPos[1], 10);
		
		if(telemetry_isActive[0])
		{
			for(int i = 0; i < window.telemetry_booster.length; i++)
				g.drawString(window.telemetry_booster[i][0] + window.telemetry_booster[i][1], window.log_box.width + telemetry_startPos[0], 25 + 15*i);
		}
		else g.drawString("NO TELEMETRY", window.log_box.width + telemetry_startPos[0], 25);
		
		if(telemetry_isActive[1])
		{
			for(int i = 0; i < window.telemetry_capsule.length; i++)
				g.drawString(window.telemetry_capsule[i][0] + window.telemetry_capsule[i][1], window.log_box.width + telemetry_startPos[1], 25 + 15*i);
		}
		else g.drawString("NO TELEMETRY", window.log_box.width + telemetry_startPos[1], 25);
		
		if(telemetry_isActive[2])
		{
			double zoom = Math.max(2*Math.max(Math.abs(window.telemetry_booster_finalDistance.x), Math.abs(window.telemetry_booster_finalDistance.y)), telemetry_align_box_realSize);
			double zoomSize = Math.min(zoom, 150d);
			
			int boxStartW = telemetry_align_box.x + telemetry_align_box.width/2;
			int boxStartH = telemetry_align_box.y + telemetry_align_box.height/2;
			int padW = (int) ((telemetry_align_box.width * telemetry_align_pad_realSize) / zoomSize);
			int padH = (int) ((telemetry_align_box.height * telemetry_align_pad_realSize) / zoomSize);
			
			//pad drawing
			g.drawRect(window.log_box.width + boxStartW - padW, boxStartH - padH, 2*padW, 2*padH);
			g.drawLine(window.log_box.width + boxStartW, boxStartH - padH, window.log_box.width + boxStartW, boxStartH + padH);
			g.drawLine(window.log_box.width + boxStartW - padW, boxStartH, window.log_box.width + boxStartW + padH, boxStartH);
			String finalDistance = new DecimalFormat("#0.0").format(window.telemetry_booster_finalDistance.Magnitude());
			
			g.drawString(finalDistance, window.log_box.width + boxStartW + telemetry_pad_distance_pos.x - g.getFontMetrics().stringWidth(finalDistance)/2, boxStartH + padH + telemetry_pad_distance_pos.y);
			
			int rocketW = (int) (telemetry_align_box.width * telemetry_align_rocket_realSize / zoomSize);
			int rocketH = (int) (telemetry_align_box.height * telemetry_align_rocket_realSize / zoomSize);
			int rocketPosW = (int) (telemetry_align_box.width * window.telemetry_booster_finalDistance.x / zoom);
			int rocketPosH = (int) (telemetry_align_box.height * window.telemetry_booster_finalDistance.y / zoom);
			
			int brakeW = (int) (telemetry_align_box.width * telemetry_brakes_realWidth / zoomSize);
			int brakeH = (int) (telemetry_align_box.height * telemetry_brakes_realHeight / zoomSize);
			int brakeDist = (int) (telemetry_align_box.width * telemetry_brakes_realDistance / zoomSize);
			
			//rocket drawing
			drawRocketWithBrakes(g, window.log_box.width + boxStartW + rocketPosW, boxStartH - rocketPosH, rocketW, rocketH, brakeDist, brakeW, brakeH);
		}
	}
	
	private void drawRocketWithBrakes(Graphics g, int startX,int startY, int rW, int rH, int bDist, int bW, int bH)
	{
		g.drawOval(startX - rW, startY - rH, 2*rW, 2*rH);
		
		if(window.telemetry_booster_brakes[1])	//left
			g.fillRect(startX - rW - bDist - 2*bH, startY - bW, 2*bH, 2*bW);
		else g.drawRect(startX - rW - bDist - 2*bH, startY - bW, 2*bH, 2*bW);
		
		if(window.telemetry_booster_brakes[0])	//top
			g.fillRect(startX - bW, startY - rH - bDist - 2*bH, 2*bW, 2*bH);
		else g.drawRect(startX - bW, startY - rH - bDist - 2*bH, 2*bW, 2*bH);
		
		if(window.telemetry_booster_brakes[3])	//right
			g.fillRect(startX + rW + bDist, startY - bW, 2*bH, 2*bW);
		else g.drawRect(startX + rW + bDist, startY - bW, 2*bH, 2*bW);
		
		if(window.telemetry_booster_brakes[2])	//bottom
			g.fillRect(startX - bW, startY + rH + bDist, 2*bW, 2*bH);
		else g.drawRect(startX - bW, startY + rH + bDist, 2*bW, 2*bH);	
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		if(window.paintReady)
		{
			PaintFlightLog(g);
			
			if(paint_arg == PAINT_ARG_FLIGHT_TELEMETRY)
				PaintFlightTelemetry(g);
		}
		else
		{
			if(paint_arg == PAINT_ARG_CONNECTION_RED) g.setColor(Color.RED);
			g.drawString("Remember to start the server!", 110, 50);
			if(paint_arg == PAINT_ARG_CONNECTION_RED) g.setColor(Color.BLACK);
			
			g.drawString("Enter target apoapsis height:", 105, 165);
		}
		
	}
}
