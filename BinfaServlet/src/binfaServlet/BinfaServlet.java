package binfaServlet;

//import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Servlet implementation class BinfaServlet
 */
@WebServlet
@MultipartConfig(fileSizeThreshold=0,
				 maxFileSize=1024*1024*300,
				 maxRequestSize=1024*1024*600
				 //location="C:\\Program Files\\GlassFish4\\glassfish\\domains\\domain1\\eclipseApps\\BinfaServlet\\upload"
				 )
public class BinfaServlet extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
        processRequest(request, response, "GET");
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		processRequest(request, response, "POST");
	}
	
	protected void processRequest(HttpServletRequest request, HttpServletResponse response, String method) throws ServletException, IOException
	{
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html>");
        out.println("<head>");
        out.println("<title>Binfa Servlet</title>");  
        out.println("</head>");
        out.println("<body>");
		switch (method)
		{
			case "GET":
			{	
				System.out.println(request.getPathInfo());
				String url = request.getRequestURI();
				url = url.substring((url.lastIndexOf("/") + 1));
				byte[] ba = new byte[url.length()];
				for (int i=0; i<url.length(); i++)
				{
					if (url.charAt(i) == '0')
						ba[i] = 0;
					else
						ba[i] = 1;			
				}				
		        new Binfa(ba, out);
		        out.println("</body>");
		        out.println("</html>"); 
		        
		        out.close();
			}
			case "POST":
			{
				for (Part part : request.getParts())
				{
					InputStream in = part.getInputStream();
					new Binfa(in, out);
				}
		        out.println("</body>");
		        out.println("</html>"); 
		        
		        out.close();
			}
		}
	}
}
