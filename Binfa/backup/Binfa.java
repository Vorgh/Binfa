import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;


public class Binfa extends JFrame 
{
	static final int MAXWIDTH = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
	static final int MAXHEIGHT = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight()*0.75);
	
	public int width = 500, height = 500;
	public Csomopont gyoker;
	public Csomopont jelenlegi;
	public int melyseg = 0, maxmelyseg = 0;
	public double atlag, szoras;
	public Csomopont printgyoker;
	public int printmelyseg = 7;
	public ArrayList<Csomopont> csomopontok = new ArrayList<Csomopont>();
	
	private JMenuBar menubar = new JMenuBar();
	private JMenu file = new JMenu("File");
	private JMenuItem open = new JMenuItem("Megnyitás");
	private JMenuItem save = new JMenuItem("Mentés");
	private JProgressBar bar = new JProgressBar(0, 100);
	
	private PrintWriter writer;
	private PrintPanel alap;
	private CollapseButton cbutton;
	private boolean isloading = true;
	private ArrayList<ExpandButton> ebuttons = new ArrayList<ExpandButton>();
	
	private void TreeToPrint(Csomopont cs, Graphics2D g2d)
	{
		Stroke defaultstroke = g2d.getStroke();
		g2d.setColor(Color.BLUE);
		Csomopont current = cs;
		if (current.GetLeft() != null && current.melyseg < printmelyseg)
		{
			TreeToPrint(current.GetLeft(), g2d);
		}
		if (current.GetRight() != null && current.melyseg < printmelyseg)
		{
			TreeToPrint(current.GetRight(), g2d);
		}
		if (current.melyseg == printmelyseg && (current.GetLeft() != null || current.GetRight() != null))
		{
			Stroke st = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0);
			Color tmpcolor = g2d.getColor();
			g2d.setColor(Color.BLACK);
			g2d.setStroke(st);
			g2d.drawLine(current.X, current.Y, current.X, current.Y + 20);
			g2d.setStroke(defaultstroke);
			g2d.setColor(tmpcolor);
			
			boolean createbtn = true;
			for (ExpandButton expbtn : ebuttons)
			{
				if (expbtn.expCsomopont == current)
				{
					expbtn.setVisible(true);
					createbtn = false;
					break;
				}
			}
			if (createbtn)
			{
				ExpandButton e = new ExpandButton(current);
				ebuttons.add(e);
				alap.add(e);
				e.setBounds(e.expCsomopont.X - 4, e.expCsomopont.Y + 20, 8, 8);
			}
		}
		if (printgyoker != gyoker)
		{
			Stroke st = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0);
			Color tmpcolor = g2d.getColor();
			g2d.setColor(Color.BLACK);
			g2d.setStroke(st);
			g2d.drawLine(printgyoker.X, printgyoker.Y, cbutton.getX()+5, cbutton.getY()+5);
			g2d.setStroke(defaultstroke);
			g2d.setColor(tmpcolor);
		}
		if (current.GetParent() != null && current != printgyoker)
		{
			g2d.setColor(Color.BLUE);
			g2d.drawLine(current.X, current.Y, current.GetParent().X, current.GetParent().Y);
		}
		switch (current.ertek)
		{
			case '0' : g2d.setColor(Color.ORANGE); break;
			case '1' : g2d.setColor(Color.RED); break;
			default : g2d.setColor(Color.BLACK); break;
		}
		g2d.fillOval(current.X - 4, current.Y - 4, 8, 8);	
	}
	
	protected class PrintPanel extends JPanel
	{
		
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			
			if (csomopontok.size() > 1 && !isloading)
			{
				Graphics2D g2d = (Graphics2D) g;
				TreeToPrint(printgyoker, g2d);			
				
				g2d.setColor(Color.BLACK);
				g2d.drawString("Mélység: " + maxmelyseg, 10, 20);
				g2d.drawString("Átlag: " + String.format("%-12.5f", atlag), 10, 40);
				g2d.drawString("Szórás: " + String.format("%-12.5f", szoras), 10, 60);
				g2d.drawString("Mélység (jelenlegi): " + (printmelyseg), 10, 80);
				
			}
			g.drawString("Csomópontok: " + (csomopontok.size()), 10, 100);
		}
	}
	
	protected class Csomopont
	{
		public char ertek;
		public int X, Y, melyseg;
		private Csomopont[] gyermekek;
		private Csomopont szulo;
		
		Csomopont(char ertek, Csomopont szulo)
		{
			this.ertek = ertek;
			gyermekek = new Csomopont[2];
			this.szulo = szulo;
		}

		Csomopont(char ertek, int x, int y, Csomopont szulo) 
		{
			this.ertek = ertek;
			gyermekek = new Csomopont[2];
			this.szulo = szulo;
			X = x;
			Y = y;
		}
		
		void SetCoords(int x, int y)
		{
			X = x;
			Y = y;
		}
		Csomopont GetLeft() { return gyermekek[0]; }
		void SetLeft(Csomopont value) { gyermekek[0] = value; }
		Csomopont GetRight() { return gyermekek[1]; }
		void SetRight(Csomopont value) { gyermekek[1] = value; }
		Csomopont GetParent() { return szulo; }
	}
	
	protected class ExpandButton extends RoundButton
	{
		public Csomopont expCsomopont;
		
		public ExpandButton(Csomopont cs)
		{
			expCsomopont = cs;
			this.setVisible(true);
			this.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
	            {
					ExpandButton tmp = (ExpandButton)e.getSource();
	                printmelyseg += 7;
	                printgyoker = tmp.expCsomopont;
	                printgyoker.SetCoords(gyoker.X, gyoker.Y);
	                SetCoordinates(printgyoker, 0);
	                cbutton = new CollapseButton(printgyoker);
	                for (ExpandButton exbtn : ebuttons)
	        		{
	        			alap.remove(exbtn);;
	        		}
	                ebuttons.clear();
	                alap.revalidate();
	                alap.repaint();
	            }
			});
		}
	}
	
	protected class CollapseButton extends RoundButton
	{
		private Csomopont returnCsomopont;
		
		public CollapseButton(Csomopont cs)
		{
			this.setBounds(cs.X - 5, cs.Y - 30, 10, 10);
			this.setVisible(true);
			alap.add(this);
			returnCsomopont = cs;
			for (int i=0; i<7; i++)
			{
				returnCsomopont = returnCsomopont.GetParent();
			}
			this.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
	            {
					CollapseButton tmp = (CollapseButton)e.getSource();
	                printmelyseg -= 7;
	                printgyoker = tmp.returnCsomopont;
	                printgyoker.SetCoords(gyoker.X, gyoker.Y);
	                SetCoordinates(printgyoker, 0);
	                alap.remove(tmp);
	                alap.revalidate();
	                alap.repaint();
	            }
			});
		}
	}

	private Binfa() 
	{
		Container contentpane = this.getContentPane();
		this.setPreferredSize(new Dimension(width, height));
		contentpane.setPreferredSize(new Dimension(width, height));
		gyoker = new Csomopont('/', null);
		csomopontok.add(gyoker);
		jelenlegi = gyoker;
		printgyoker = gyoker;
		this.pack();
		Create(contentpane);
		alap.setVisible(true);
	}
	
	private void SetCoordinates(Csomopont cs, int m)
	{
		int x,y;
		Csomopont bal = cs.GetLeft();
		Csomopont jobb = cs.GetRight();
		if (bal != null && m < 8)
		{
			if (cs != printgyoker)
				x = cs.X - Math.abs(cs.X - cs.GetParent().X) / 2;
			else
				x = cs.X / 2;
			y = cs.Y + 30;
			bal.SetCoords(x, y);
			SetCoordinates(bal, m+1);
		}
		if (jobb != null && m < 8)
		{
			if (cs != printgyoker)
				x = cs.X + Math.abs(cs.X - cs.GetParent().X) / 2;
			else
				x = cs.X + cs.X / 2;
			y = cs.Y + 30;
			jobb.SetCoords(x, y);
			SetCoordinates(jobb, m+1);
		}
	}
	
	private void Create(Container pane)
	{
		this.setJMenuBar(menubar);
		menubar.add(file);
		file.add(open);
		open.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
            {
                Open();
            }
		});
		file.add(save);
		save.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
            {
                Save();
            }
		});
		alap = new PrintPanel();
		pane.add(alap);
		alap.setLayout(null);
	}
	
	private void Reset()
	{
		melyseg = 0;
		maxmelyseg = 0;
		printmelyseg = 7;
		csomopontok.clear();
		csomopontok.add(gyoker);
		ebuttons.clear();
		gyoker.SetLeft(null);
		gyoker.SetRight(null);
		jelenlegi = gyoker;
	}

	private void Open()
	{
		JFileChooser fc = new JFileChooser();
		fc.addChoosableFileFilter(new FileTypeFilter(".txt", "Text files"));
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) 
		{
			String path = fc.getSelectedFile().getPath();
			if (path.toLowerCase().endsWith(".txt"))
			{
				Reset();
				Beolvasas(path);
			}
		}
	}

	private void Save()
	{
		JFileChooser fc = new JFileChooser();
		fc.addChoosableFileFilter(new FileTypeFilter(".txt", "Text files"));
		fc.addChoosableFileFilter(new FileTypeFilter(".jpg", "JPEG images"));
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) 
		{
			File f = fc.getSelectedFile();
			String path = f.getPath();
			if (path.toLowerCase().endsWith(".txt"))
			{
				try 
				{
					writer = new PrintWriter(path, "UTF-8");
					Kiir(gyoker);
					writer.close();
				} 
				catch (FileNotFoundException | UnsupportedEncodingException e) 
				{
					e.printStackTrace();
				}
			}
			else
				if (path.toLowerCase().endsWith(".jpg"))
				{
					try
			        {
			            BufferedImage image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
			            Graphics2D graphics2D = image.createGraphics();
			            this.paint(graphics2D);
			            ImageIO.write(image,"jpeg", new File(path));
			        }
			        catch(Exception e)
			        {
			            e.printStackTrace();
			        }
				}
				else
					JOptionPane.showMessageDialog(this,
					    "Csak JPG vagy TXT formátumban tudok menteni.",
					    "Error",
					    JOptionPane.ERROR_MESSAGE);
		} 
	}
	
	class ReaderTask extends SwingWorker<Void, Void> 
	{
		private String path;
		
		ReaderTask(String p)
		{
			path = p;
		}
		
        @Override
        public Void doInBackground() 
        {
            int progress = 0;
            setProgress(0);
            try 
    		{
    			File f = new File(path);
    			long maxlength = f.length();
    			long currlength = 0;
    			double percentlength = maxlength / 100f;
    			FileReader fr = new FileReader(f);
    			BufferedReader br = new BufferedReader(fr);
    			int r;
    			while ((r = br.read()) != -1) 
    			{
    				r = (r & 0xff);
    				for (int i = 0; i < 8; i++) 
    				{
    					if ((r&128) == 0)
    						FaEpites('0');
    					else
    						FaEpites('1');
    					r<<=1;
    				}
    				currlength++;
    				if (currlength >= percentlength)
    				{
    					currlength = 0;
    					progress++;
    					setProgress(progress);
    				}
    			}
    			if (progress < 100)
    			{
    				progress = 100;
    				setProgress(progress);
    			}
    			br.close();
    		}
    		catch (Exception e)
    		{
    			e.printStackTrace();
    		}

            return null;
        }

        @Override
        public void done() 
        {
            Toolkit.getDefaultToolkit().beep();
            isloading = false;
            Atlag();
    		Szoras();
    		
    		width = (30+maxmelyseg*125 < MAXWIDTH) ? 30+maxmelyseg*125 : MAXWIDTH;
    		height= (30 + maxmelyseg*30 < MAXHEIGHT) ? 30+maxmelyseg*30 : MAXHEIGHT;
    		Binfa outer = Binfa.this;
    		Container c = outer.getContentPane();
    		int borderwidth = outer.getWidth()-c.getWidth();
    		int borderheight = outer.getHeight()-c.getHeight();
    		c.setPreferredSize(new Dimension(width, height));
    		outer.setPreferredSize(new Dimension(width+borderwidth+10, height+borderheight+10));
    		gyoker.SetCoords(width/2, 30);
    		SetCoordinates(gyoker, 0);
    		alap.remove(bar);
    		outer.pack();
    		outer.revalidate();
    		outer.repaint();
        }
    }

	public void Beolvasas(String path)
	{
		bar.setValue(0);
		bar.setStringPainted(true);
		bar.setBounds(width/2-150, height/2, 300, 20);
		alap.add(bar);
		ReaderTask task = new ReaderTask(path);
        task.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
        	public void propertyChange(PropertyChangeEvent evt) 
			{
                if (evt.getPropertyName().equals("progress")) 
                {
                    int progress = (int)evt.getNewValue();
                    bar.setValue(progress);
                    alap.revalidate();
                    alap.repaint();
                } 
            }
		});
        task.execute();
	}

	public void Kiir(Csomopont cs)
	{
		if (cs != null) 
		{
			try
			{
				Kiir(cs.GetLeft());
				for (int i = 0; i < cs.melyseg; i++) 
				{
					writer.print("-");
				}
				writer.print(cs.ertek + "(" + cs.melyseg + ")" + System.lineSeparator());
				Kiir(cs.GetRight());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void Atlag()
	{
		int ossz = 0, db = 0;
		for (int i = 0; i < csomopontok.size(); i++) 
		{
			if (csomopontok.get(i).GetLeft() == null && csomopontok.get(i).GetRight() == null) 
			{
				ossz += csomopontok.get(i).melyseg;
				db++;
			}
		}
		atlag = (double) ossz / db;
	}

	public void Szoras()
	{
		int db = 0;
		double ossz = 0;
		for (int i = 0; i < csomopontok.size(); i++) 
		{
			if (csomopontok.get(i).GetLeft() == null && csomopontok.get(i).GetRight() == null) 
			{
				ossz += (csomopontok.get(i).melyseg - atlag) * (csomopontok.get(i).melyseg - atlag);
				db++;
			}
		}
		if (db - 1 > 0) 
		{
			szoras = Math.sqrt(ossz / (db - 1));
		} else 
		{
			szoras = Math.sqrt(ossz);
		}
	}

	public void FaEpites(char c)
	{
		if (c == '0') 
		{
			if (jelenlegi.GetLeft() != null) 
			{
				jelenlegi = jelenlegi.GetLeft();
				melyseg++;
			} 
			else 
			{
				jelenlegi.SetLeft(new Csomopont('0', jelenlegi));
				csomopontok.add(jelenlegi.GetLeft());
				melyseg++;
				jelenlegi.GetLeft().melyseg = melyseg;
				if (melyseg > maxmelyseg) 
				{
					maxmelyseg = melyseg;
				}
				jelenlegi = gyoker;
				melyseg = 0;
			}
		} 
		else 
		{
			if (jelenlegi.GetRight() != null) 
			{
				jelenlegi = jelenlegi.GetRight();
				melyseg++;
			} 
			else 
			{
				jelenlegi.SetRight(new Csomopont('1', jelenlegi));
				csomopontok.add(jelenlegi.GetRight());
				melyseg++;
				jelenlegi.GetRight().melyseg = melyseg;
				if (melyseg > maxmelyseg) {
					maxmelyseg = melyseg;
				}
				jelenlegi = gyoker;
				melyseg = 0;
			}
		}
	}
		
	public static void main(String[] args)
	{
		 SwingUtilities.invokeLater(new Runnable()
		 {
			 public void run() 
			 {        
				 Binfa window = new Binfa();
				 window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				 window.setVisible(true);
				 window.setTitle("Binfa");
			 }
	     });
	}
}
