package binfaServlet;

import java.io.*;

class Binfa
{
	private short melyseg = 0, maxmelyseg = 0;
	private double atlag = 0, szoras = 0;
	private Csomopont gyoker;
	private Csomopont jelenlegi;
	
	private PrintWriter out;
	private int tmpdb;
	private double tmpossz;
	private long starttime;
	
	private class Csomopont
	{
		protected char ertek;
		protected short melyseg;
		private Csomopont[] gyermekek;
		
		Csomopont(char ertek)
		{
			this.ertek = ertek;
			gyermekek = new Csomopont[2];
		}
		
		protected Csomopont GetLeft() { return gyermekek[0]; }
		protected void SetLeft(Csomopont value) { gyermekek[0] = value; }
		protected Csomopont GetRight() { return gyermekek[1]; }
		protected void SetRight(Csomopont value) { gyermekek[1] = value; }
	}
	
	public Binfa(InputStream in, PrintWriter out) 
	{
		gyoker = new Csomopont('/');
		jelenlegi = gyoker;
		this.out = out;
		starttime = System.nanoTime();
		Beolvasas(in);
		Kiir(gyoker);
		out.print("Mélység: " + maxmelyseg + "<br/>");
		out.print("Átlag: " + atlag + "<br/>");
		out.print("Szórás: " + szoras + "<br/>");
		System.out.println((System.nanoTime() - starttime)/1000000000.0 + "s");
	}
	
	public Binfa(byte[] in, PrintWriter out)
	{
		gyoker = new Csomopont('/');
		jelenlegi = gyoker;
		this.out = out;
		starttime = System.nanoTime();
		Beolvasas(in);
		Kiir(gyoker);
		out.print("Mélység: " + maxmelyseg + "<br/>");
		out.print("Átlag: " + atlag + "<br/>");
		out.print("Szórás: " + szoras + "<br/>");
		System.out.println((System.nanoTime() - starttime)/1000000000.0 + "s");
	}

	void Beolvasas(InputStream in)
	{
		try 
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			int r;

			while ((r = br.read()) != -1) 
			{
				if (r == 0x0a || r == 0x0d || r == 0x4e) // \r, \n, N-t nem olvassa be
				{
					continue;
				}
				if (r == 0x3e) //> karakter, ugrás a sor végére
				{
					br.readLine();
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
			}
			
			br.close();
			in.close();
			
            AtlagSzoras();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	void Beolvasas(byte[] in)
	{
		for (byte b : in) 
		{
			if (b == 0)
				FaEpites('0');
			else
				FaEpites('1');
		}
			
        AtlagSzoras();
	}
	
	void AtlagSzoras()
	{
		tmpossz = 0;
        tmpdb = 0;
        Atlag(gyoker);
        tmpossz = 0;
        tmpdb = 0;
        Szoras(gyoker);
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
					out.print("---");
				}
				out.println(cs.ertek + "(" + cs.melyseg + ")<br/>");
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
				jelenlegi.SetLeft(new Csomopont('0'));
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
				jelenlegi.SetRight(new Csomopont('1'));
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
}
