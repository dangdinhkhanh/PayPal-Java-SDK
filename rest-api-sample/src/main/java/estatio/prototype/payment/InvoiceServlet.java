package estatio.prototype.payment;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import estatio.prototype.database.DatabaseManager;

@WebServlet(name = "InvoiceServlet", urlPatterns = { "invoice" }, loadOnStartup = 1)
public class InvoiceServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DatabaseManager dbm = new DatabaseManager();
		Connection conn = dbm.connect();
		String query = "SELECT * FROM Invoice LIMIT 10";
		PrintWriter out = resp.getWriter();
		out.println("<html>");
		out.println("<head>");
		out.println("<h1>Invoice Page</h1>");
		out.println("<style>");
		out.println("table, th, td {border: 1px solid black;}");
		out.println("</style>");
		out.println("</head>");
		out.println("<body>");
		out.println("<table style=\"width:100%\">");
		out.println("<tr>");
		out.println("<th>Invoice ID</th>");
		out.println("<th>Renter Name</th>");
		out.println("<th>Payment Method</th>");
		out.println("<th>Status</th>");
		out.println("<th>Amount</th>");
		out.println("<th>Currency</th>");
		out.println("<th>Created Date</th>");
		out.println("<th>Updated Date</th>");
		out.println("<th>Create Payment Request</th>");
		out.println("</tr>");
		if (conn != null) {
			try {
				PreparedStatement ps = conn.prepareStatement(query);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					out.println("<tr>");
					for (int i = 1; i <= InvoiceColumnName.values().length; i++) {
						out.println("<th>" + rs.getString(i) + "</th>");
					}
					// out.println("<th><button type=\"button\">Create</button></th>");
					out.println("<th><form action=\"payment\" method=\"POST\" >");
					out.println("<input type=\"submit\" value=\"Creat Payment\" />");
					for (int i = 1; i <= InvoiceColumnName.values().length; i++) {
						out.println("<input type=\"hidden\" name=\"" + InvoiceColumnName.getColumnName(i).toString()
								+ "\" + value=\" " + rs.getString(i) + "\"/>");
					}
					out.println("</form></th>");
					out.println("</tr>");
				}
				conn.close();
				System.out.println("Disconnected from database");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		out.println("</table>");
		out.println("</body>");
		out.println("</html>");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	}

	
}
