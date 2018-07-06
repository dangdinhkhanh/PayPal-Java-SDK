package estatio.prototype.payment;

import static com.paypal.api.payments.util.SampleConstants.clientID;
import static com.paypal.api.payments.util.SampleConstants.clientSecret;
import static com.paypal.api.payments.util.SampleConstants.mode;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.api.payments.RedirectUrls;
import com.paypal.api.payments.Transaction;
import com.paypal.api.payments.util.ResultPrinter;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;

import estatio.prototype.database.DatabaseManager;

@WebServlet(name = "PaymentServlet", urlPatterns = { "payment" }, loadOnStartup = 1)
public class PaymentServlet extends HttpServlet {
	private static final Logger LOGGER = Logger.getLogger(PaymentServlet.class);
	Map<String, String> map = new HashMap<String, String>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// PrintWriter out = resp.getWriter();
		// out.println("Get Payment");
		LOGGER.info("Get message");
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PrintWriter out = resp.getWriter();
		out.println("POST Payment");
		for (int i = 1; i <= InvoiceColumnName.values().length; i++) {
			out.println(req.getParameter(InvoiceColumnName.getColumnName(i).toString()));
		}
		createPayment(req, resp);
		req.getRequestDispatcher("response.jsp").forward(req, resp);
	}

	public Payment createPayment(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		// ### Api Context
		// Pass in a `ApiContext` object to authenticate
		// the call and to send a unique request id
		// (that ensures idempotency). The SDK generates
		// a request id if you do not pass one explicitly.
		APIContext apiContext = new APIContext(clientID, clientSecret, mode);
		Payment createdPayment = null;

		if (req.getParameter("PayerID") != null) {
			Payment payment = new Payment();
			if (req.getParameter("invoiceID") != null) {
				payment.setId(map.get(req.getParameter("invoiceID")));
			}

			PaymentExecution paymentExecution = new PaymentExecution();
			paymentExecution.setPayerId(req.getParameter("PayerID"));
			try {

				createdPayment = payment.execute(apiContext, paymentExecution);
				ResultPrinter.addResult(req, resp, "Executed The Payment", Payment.getLastRequest(),
						Payment.getLastResponse(), null);
				// TODO need to update database here
				String state = createdPayment.getState();
				DatabaseManager dbm = new DatabaseManager();
				Connection conn = dbm.connect();
				String query = "UPDATE invoice SET status = '" + state + " WHERE' INVOICEID = "
						+ req.getParameter("invoiceID");
				if (conn != null) {
					try {
						Statement stmt = conn.createStatement();
						stmt.executeUpdate(query);
						conn.close();
						System.out.println("Disconnected from database");
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						LOGGER.info(query);
					}
				}
			} catch (PayPalRESTException e) {
				ResultPrinter.addResult(req, resp, "Executed The Payment", Payment.getLastRequest(), null,
						e.getMessage());
			}
		} else {

			String invoiceID = req.getParameter(InvoiceColumnName.INVOICE_ID.name()).trim();
			String amount = req.getParameter(InvoiceColumnName.AMOUNT.name()).trim();
			String currency = req.getParameter(InvoiceColumnName.CURRENCY.name()).trim();
			String renteeName = req.getParameter(InvoiceColumnName.RENTEE_NAME.name()).trim();
			Amount ppAmount = new Amount();
			ppAmount.setTotal(amount);
			ppAmount.setCurrency(currency);

			// ###Transaction
			// A transaction defines the contract of a
			// payment - what is the payment for and who
			// is fulfilling it. Transaction is created with
			// a `Payee` and `Amount` types
			Transaction transaction = new Transaction();
			transaction.setAmount(ppAmount);
			transaction.setDescription(
					"Pay for rent with invoiceID" + req.getParameter(InvoiceColumnName.getColumnName(1).toString())
							+ " of rentee's name" + renteeName);

			// ###Payer
			// A resource representing a Payer that funds a payment
			// Payment Method
			// as 'paypal'
			Payer payer = new Payer();
			payer.setPaymentMethod("paypal");

			// ###Payment
			// A Payment Resource; create one using
			// the above types and intent as 'sale'
			Payment payment = new Payment();
			payment.setIntent("sale");
			payment.setPayer(payer);

			// The Payment creation API requires a list of
			// Transaction; add the created `Transaction`
			// to a List
			List<Transaction> transactions = new ArrayList<Transaction>();
			transactions.add(transaction);
			payment.setTransactions(transactions);

			// ###Redirect URLs
			RedirectUrls redirectUrls = new RedirectUrls();
			// String guid = UUID.randomUUID().toString().replaceAll("-", "");
			redirectUrls.setCancelUrl(req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
					+ req.getContextPath() + "/payment?invoiceID=" + invoiceID);
			redirectUrls.setReturnUrl(req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
					+ req.getContextPath() + "/payment?invoiceID=" + invoiceID);
			payment.setRedirectUrls(redirectUrls);

			// Create a payment by posting to the APIService
			// using a valid AccessToken
			// The return object contains the status;
			try {
				createdPayment = payment.create(apiContext);
				LOGGER.info("Created payment with id = " + createdPayment.getId() + " and status = "
						+ createdPayment.getState());
				// ###Payment Approval Url
				Iterator<Links> links = createdPayment.getLinks().iterator();
				while (links.hasNext()) {
					Links link = links.next();
					if (link.getRel().equalsIgnoreCase("approval_url")) {
						req.setAttribute("redirectURL", link.getHref());
					}
				}
				ResultPrinter.addResult(req, resp, "Payment with PayPal", Payment.getLastRequest(),
						Payment.getLastResponse(), null);
				map.put(invoiceID, createdPayment.getId());
			} catch (PayPalRESTException e) {
				ResultPrinter.addResult(req, resp, "Payment with PayPal", Payment.getLastRequest(), null,
						e.getMessage());
			}
		}

		return createdPayment;

	}

}
