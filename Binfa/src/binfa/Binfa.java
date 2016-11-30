package binfa;

import javax.swing.*;
import java.awt.*;
import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;


class Binfa extends JFrame 
{
	static final int MAXWIDTH = (int)GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getWidth();
	static final int MAXHEIGHT = (int)(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getHeight()*0.75);
	
	private int width = 500, height = 500;
	private int printmelyseg = 7;
	private short melyseg = 0, maxmelyseg = 0;
	private double atlag = 0, szoras = 0;
	private Csomopont gyoker;
	private Csomopont jelenlegi;
	private CoordCsomopont printgyoker;
	
	private JMenuBar menubar = new JMenuBar();
	private JMenu file = new JMenu("File");
	private JMenuItem open = new JMenuItem("Megnyitás");
	private JMenuItem save = new JMenuItem("Mentés");
	private JProgressBar bar = new JProgressBar(0, 100);
	
	private boolean isloading = true;
	private PrintWriter writer;
	private PrintPanel alap;
	private CollapseButton cbutton;
	private ArrayList<CoordCsomopont> returnCsomopont = new ArrayList<CoordCsomopont>();
	private ArrayList<ExpandButton> ebuttons = new ArrayList<ExpandButton>();
	
	private int tmpdb;
	private double tmpossz;
	private long starttime = 0;
	
	private class PrintPanel extends JPanel
	{
		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			
			if (!isloading)
			{
				Graphics2D g2d = (Graphics2D) g;
				if (printgyoker != null)
				{
					TreeToPrint(printgyoker, g2d);
				}		
				
				g2d.setColor(Color.BLACK);
				g2d.drawString("Mélység: " + maxmelyseg, 10, 20);
				g2d.drawString("Átlag: " + String.format("%-12.5f", atlag), 10, 40);
				g2d.drawString("Szórás: " + String.format("%-12.5f", szoras), 10, 60);
				g2d.drawString("Jelenlegi mélység: " + printmelyseg, 10, 80);
			}
		}
	}
	
	private void TreeToPrint(CoordCsomopont current, Graphics2D g2d)
	{
		Stroke defaultstroke = g2d.getStroke();
		g2d.setColor(Color.BLUE);
		//Bejárás
		if (current.GetCoordLeft() != null && current.melyseg < printmelyseg)
		{
			TreeToPrint(current.GetCoordLeft(), g2d);
		}
		if (current.GetCoordRight() != null && current.melyseg < printmelyseg)
		{
			TreeToPrint(current.GetCoordRight(), g2d);
		}
		//Elõre gombok
		if (current.melyseg == printmelyseg && (current.GetCoordLeft() != null || current.GetCoordRight() != null))
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
		//Vissza gomb
		if (printgyoker.melyseg != 0)
		{
			Stroke st = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0);
			Color tmpcolor = g2d.getColor();
			g2d.setColor(Color.BLACK);
			g2d.setStroke(st);
			g2d.drawLine(printgyoker.X, printgyoker.Y, cbutton.getX()+5, cbutton.getY()+5);
			g2d.setStroke(defaultstroke);
			g2d.setColor(tmpcolor);
		}
		//Vonalak
		if (current.GetCoordParent() != null && current != printgyoker)
		{
			g2d.setColor(Color.BLUE);
			g2d.drawLine(current.X, current.Y, current.GetCoordParent().X, current.GetCoordParent().Y);
		}
		//Szín érték szerint
		switch (current.ertek)
		{
			case '0' : g2d.setColor(Color.ORANGE); break;
			case '1' : g2d.setColor(Color.RED); break;
			default : g2d.setColor(Color.BLACK); break;
		}
		g2d.fillOval(current.X - 4, current.Y - 4, 8, 8);
	}
	
	private class Csomopont
	{
		protected char ertek;
		protected short melyseg;
		private Csomopont[] gyermekek;
		private Csomopont szulo;
		
		Csomopont()
		{
			this.ertek = '/';
			this.melyseg = 0;
			gyermekek = new Csomopont[2];
			this.szulo = null;
		}
		
		Csomopont(char ertek, Csomopont szulo)
		{
			this.ertek = ertek;
			gyermekek = new Csomopont[2];
			this.szulo = szulo;
		}
		
		protected Csomopont GetLeft() { return gyermekek[0]; }
		protected void SetLeft(Csomopont value) { gyermekek[0] = value; }
		protected Csomopont GetRight() { return gyermekek[1]; }
		protected void SetRight(Csomopont value) { gyermekek[1] = value; }
		protected Csomopont GetParent() { return szulo; }
		protected void SetParent(Csomopont value) { szulo = value; }
	}
	
	private class CoordCsomopont extends Csomopont
	{
		private int X, Y;
		private CoordCsomopont[] coordchilds;
		private CoordCsomopont coordparent;
		
		CoordCsomopont(Csomopont cs)
		{
			SetParent(cs.GetParent());
			SetLeft(cs.GetLeft());
			SetRight(cs.GetRight());
			melyseg = cs.melyseg;
			ertek = cs.ertek;
			coordparent = null;
			coordchilds = new CoordCsomopont[2];
		}
		
		CoordCsomopont(Csomopont cs, CoordCsomopont cparent, int x, int y)
		{
			SetParent(cs.GetParent());
			SetLeft(cs.GetLeft());
			SetRight(cs.GetRight());
			melyseg = cs.melyseg;
			ertek = cs.ertek;
			coordparent = cparent;
			coordchilds = new CoordCsomopont[2];
			X = x;
			Y = y;
		}
		
		protected void SetCoords(int x, int y)
		{
			X = x;
			Y = y;
		}
		
		protected CoordCsomopont GetCoordParent() { return coordparent; }
		protected CoordCsomopont GetCoordLeft() { return coordchilds[0]; }
		protected void SetCoordLeft(CoordCsomopont value) { coordchilds[0] = value; }
		protected CoordCsomopont GetCoordRight() { return coordchilds[1]; }
		protected void SetCoordRight(CoordCsomopont value) { coordchilds[1] = value; }
	}
	
	protected class ExpandButton extends RoundButton
	{
		protected CoordCsomopont expCsomopont;
		
		ExpandButton(CoordCsomopont cs)
		{
			expCsomopont = cs;
			this.setVisible(true);
			this.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
	            {
					ExpandButton tmp = (ExpandButton)e.getSource();
					if (cbutton != null)
					{
						alap.remove(cbutton);
					}
	                cbutton = new CollapseButton();
	                returnCsomopont.add(printgyoker);
	                printmelyseg += 7;
	                printgyoker = tmp.expCsomopont;
	                printgyoker.SetCoords(width/2, 30);
	                SetCoordinates(printgyoker, 0);
	                for (ExpandButton exbtn : ebuttons)
	        		{
	        			alap.remove(exbtn);
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
		
		CollapseButton()
		{
			this.setBounds(printgyoker.X - 5, printgyoker.Y - 30, 10, 10);
			this.setVisible(true);
			alap.add(this);
			this.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
	            {
	                printmelyseg -= 7;
	                printgyoker = returnCsomopont.get(returnCsomopont.size()-1);
	                printgyoker.SetCoords(width/2, 30);
	                returnCsomopont.remove(returnCsomopont.size()-1);
	                SetCoordinates(printgyoker, 0);
	                for (ExpandButton exbtn : ebuttons)
	        		{
	        			alap.remove(exbtn);;
	        		}
	                if (printmelyseg <= 7)
	                {
	                	alap.remove(cbutton);
	                }
	                ebuttons.clear();
	                alap.revalidate();
	                alap.repaint();
	            }
			});
		}
	}
	
	void SetCoordinates(CoordCsomopont cs, int m)
	{
		short x,y;
		if (cs.GetLeft() != null && m < 8)
		{
			if (cs != printgyoker)
				x = (short)(cs.X - Math.abs(cs.X - cs.GetCoordParent().X) / 2);
			else
				x = (short)(cs.X / 2);
			y = (short)(cs.Y + 30);
			Csomopont bal = cs.GetLeft();
			if (bal != null)
			{
				cs.SetCoordLeft(new CoordCsomopont(bal, cs, x, y));
				SetCoordinates(cs.GetCoordLeft(), m+1);
			}
		}
		if (cs.GetRight() != null && m < 8)
		{
			if (cs != printgyoker)
				x = (short)(cs.X + Math.abs(cs.X - cs.GetCoordParent().X) / 2);
			else
				x = (short)(cs.X + cs.X / 2);
			y = (short)(cs.Y + 30);
			Csomopont jobb = cs.GetRight();
			if (jobb != null)
			{
				cs.SetCoordRight(new CoordCsomopont(jobb, cs, x, y));
				SetCoordinates(cs.GetCoordRight(), m+1);
			}
		}
	}

	public Binfa() 
	{
		Container contentpane = this.getContentPane();
		this.setPreferredSize(new Dimension(width, height));
		contentpane.setPreferredSize(new Dimension(width, height));
		gyoker = new Csomopont('/', null);
		jelenlegi = gyoker;
		this.pack();
		Create();
	}
	
	
	private void Create()
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
		this.getContentPane().add(alap);
		alap.setLayout(null);
	}
	
	private void Reset()
	{
		melyseg = 0;
		maxmelyseg = 0;
		printmelyseg = 7;
		alap.removeAll();
		ebuttons.clear();
		gyoker.SetLeft(null);
		gyoker.SetRight(null);
		jelenlegi = gyoker;
		printgyoker = null;
		System.gc();
		alap.revalidate();
		alap.repaint();
	}

	private void Open()
	{
		JFileChooser fc = new JFileChooser();
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) 
		{
			String path = fc.getSelectedFile().getPath();
			Reset();
			Beolvasas(path);
		}
	}

	private void Save()
	{
		JFileChooser fc = new JFileChooser();
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) 
		{
			File f = fc.getSelectedFile();
			String path = f.getPath();
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
	}
	
	private class ReaderTask extends SwingWorker<Void, Void> 
	{
		String path;
		
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
    			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.defaultCharset()));
    			int r;

    			while ((r = br.read()) != -1) 
    			{
    				if (r == 0x0a || r == 0x0d || r == 0x4e) // \r, \n, N-t nem olvassa be
    				{
    					currlength++;
    					continue;
    				}
    				if (r == 0x3e) //> karakter, ugrás a sor végére
    				{
    					String line = br.readLine();
    					currlength += line.length();
    					continue;
    				}
    				//r = (r & 0xff); //2 byte-os karakterek miatt
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
            tmpossz = 0;
            tmpdb = 0;
            Atlag(gyoker);
            tmpossz = 0;
            tmpdb = 0;
    		Szoras(gyoker);
    		System.out.println((System.nanoTime() - starttime)/1000000000.0 + "s");
    		
    		width = (30+maxmelyseg*125 < MAXWIDTH) ? 30+maxmelyseg*125 : MAXWIDTH;
    		height= (30 + 8*30 < MAXHEIGHT) ? 30+8*30 : MAXHEIGHT;
    		Binfa outer = Binfa.this;
    		Container c = outer.getContentPane();
    		int borderwidth = outer.getWidth()-c.getWidth();
    		int borderheight = outer.getHeight()-c.getHeight();
    		c.setPreferredSize(new Dimension(width, height));
    		outer.setPreferredSize(new Dimension(width+borderwidth+10, height+borderheight+10));
    		printgyoker = new CoordCsomopont(gyoker);
    		printgyoker.SetCoords(width/2, 30);
    		SetCoordinates(printgyoker, 0);
    		alap.remove(bar);
    		outer.pack();
    		outer.revalidate();
    		outer.repaint();
        }
    }

	void Beolvasas(String path)
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
        starttime = System.nanoTime();
        task.execute();
	}

	void Kiir(Csomopont cs)
	{
		if (cs != null) 
		{
			try
			{
				Kiir(cs.GetRight());
				for (int i = 0; i < cs.melyseg; i++) 
				{
					writer.print("-");
				}
				writer.print(cs.ertek + "(" + cs.melyseg + ")" + System.lineSeparator());
				Kiir(cs.GetLeft());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	void Atlag(Csomopont cs)
	{
		if (cs.GetLeft() != null) 
			Atlag(cs.GetLeft());
		if (cs.GetRight() != null) 
			Atlag(cs.GetRight());
		if (cs.GetLeft() == null && cs.GetRight() == null) 
		{
			tmpossz += cs.melyseg;
			tmpdb++;
		}
		if (cs.melyseg == 0)
		{
			atlag = tmpossz / tmpdb;
		}
	}

	void Szoras(Csomopont cs)
	{
		if (cs.GetLeft() != null) 
			Szoras(cs.GetLeft());
		if (cs.GetRight() != null) 
			Szoras(cs.GetRight());
		if (cs.GetLeft() == null && cs.GetRight() == null) 
		{
			tmpossz += (cs.melyseg - atlag) * (cs.melyseg - atlag);
			tmpdb++;
		}
		if (cs.melyseg == 0)
		{
			if (tmpdb - 1 > 0) 
			{
				szoras = Math.sqrt(tmpossz / (tmpdb - 1));
			}
			else 
			{
				szoras = Math.sqrt(tmpossz);
			}
		}
	}

	void FaEpites(char c)
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
