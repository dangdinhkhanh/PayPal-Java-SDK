package estatio.prototype.database;

public enum InvoiceColumnName {
	INVOICE_ID(1), RENTEE_NAME(2), PAYMENT_METHOD(3), STATUS(4), AMOUNT(5), CURRENCY(6), CREATED_DATE(7),
	UPDATED_DATE(8);
	int columnIndex;

	InvoiceColumnName(int index) {
		this.columnIndex = index;
	}

	public static InvoiceColumnName getColumnName(int index) {
		for (InvoiceColumnName columnName : InvoiceColumnName.values()) {
            if (columnName.columnIndex == index) {
                return columnName;
            }
        }
        return null;
	}
}
