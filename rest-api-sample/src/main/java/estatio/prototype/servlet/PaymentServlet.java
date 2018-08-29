package estatio.prototype.servlet;

import static com.paypal.api.payments.util.SampleConstants.clientID;
import static com.paypal.api.payments.util.SampleConstants.clientSecret;
import static com.paypal.api.payments.util.SampleConstants.mode;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import estatio.prototype.database.InvoiceColumnName;

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

		DatabaseManager dbm = new DatabaseManager();
		Connection conn = dbm.connect();
		
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
				String paymentState = createdPayment.getState();
				
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				
				
				//invoice state is No Pay,  Fully Paid, Partial Paid, ... 
				//TODO the state of invoice is not the state of payment
				String transactionState = createdPayment.getTransactions().get(0).getRelatedResources().get(0).getSale().getState();
				String transactionID = createdPayment.getTransactions().get(0).getRelatedResources().get(0).getSale().getId();
				
				//Update the state of payment (created, approved)
				//State of transaction is completed -> thinking of adding transaction ID to the payment table 
				String queryPayment = "UPDATE payment SET status = '" + paymentState + "', updateddate ='"+ dateFormat.format(new Date()) + "', transaction_id = '"+ transactionID+ "' WHERE INVOICEID = "
						+ req.getParameter("invoiceID");
				String queryInvoice = null;
				if("completed".equals(transactionState)) {
					queryInvoice = "UPDATE invoice SET status = 'fully paid', updateddate ='"+ dateFormat.format(new Date()) + "' WHERE INVOICEID = "
						+ req.getParameter("invoiceID");
				} 
				if (conn != null) {
					try {
						Statement stmt = conn.createStatement();
						if(queryInvoice != null) {
							stmt.executeUpdate(queryInvoice);
						}
						stmt.executeUpdate(queryPayment);
						conn.close();
						System.out.println("Disconnected from database");
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						LOGGER.info(queryInvoice);
						LOGGER.info(queryPayment);
					}
				}
			} catch (PayPalRESTException e) {
				ResultPrinter.addResult(req, resp, "Executed The Payment", Payment.getLastRequest(), null,
						e.getMessage());
			}
		} else {

			String invoiceID = req.getParameter(InvoiceColumnName.INVOICE_ID.name());
			invoiceID = (invoiceID != null) ? invoiceID.trim() : invoiceID;
			String amount = req.getParameter(InvoiceColumnName.AMOUNT.name());
			amount = (amount != null) ? amount.trim() : amount;
			String currency = req.getParameter(InvoiceColumnName.CURRENCY.name());
			currency = (currency != null) ? currency.trim() : currency;
			String renteeName = req.getParameter(InvoiceColumnName.RENTEE_NAME.name());
			renteeName = (renteeName != null) ? renteeName.trim() : renteeName;
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
				//### Update the status of Payment in Payment table
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String query = "INSERT INTO Payment (InvoiceID, paymentmethod, amount, currency, status, ExternalID, createddate) VALUES ("
						+ invoiceID + ", 'paypal'," + amount + ",'" + currency + "','" + createdPayment.getState()
						+ "','" + createdPayment.getId() + "','" + dateFormat.format(new Date()) + "')";

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
