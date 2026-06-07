package com.ne.wasac.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ne.wasac.model.Bill;
import com.ne.wasac.model.Payment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds a PDF payment receipt attached to the bill-fully-paid email.
 */
@Service
public class BillReceiptPdfService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generate(Bill bill, Payment payment) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("WASAC Payment Receipt", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16f);
            document.add(title);

            document.add(new Paragraph("Thank you for your payment. This receipt confirms your bill is fully paid.",
                    VALUE_FONT));
            document.add(new Paragraph(" ", VALUE_FONT));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(8f);

            addRow(table, "Customer", bill.getCustomer().getFullName());
            addRow(table, "Bill reference", bill.getBillReference());
            addRow(table, "Meter", bill.getMeter().getMeterNumber());
            addRow(table, "Billing period", bill.getBillingMonth() + "/" + bill.getBillingYear());
            addRow(table, "Consumption", bill.getConsumption().toPlainString() + " units");
            addRow(table, "Amount before tax", bill.getAmountBeforeTax().toPlainString() + " FRW");
            addRow(table, "Tax", bill.getTaxAmount().toPlainString() + " FRW");
            addRow(table, "Penalty", bill.getPenaltyAmount().toPlainString() + " FRW");
            addRow(table, "Total amount", bill.getTotalAmount().toPlainString() + " FRW");
            addRow(table, "Amount paid", bill.getPaidAmount().toPlainString() + " FRW");
            addRow(table, "Outstanding balance", bill.getOutstandingBalance().toPlainString() + " FRW");
            addRow(table, "Last payment amount", payment.getAmountPaid().toPlainString() + " FRW");
            addRow(table, "Payment method", payment.getPaymentMethod().name());
            addRow(table, "Payment date", payment.getPaymentDate().toString());
            addRow(table, "Status", bill.getStatus().name());
            addRow(table, "Receipt generated", LocalDateTime.now().format(GENERATED_AT));

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Failed to generate payment receipt PDF", ex);
        }
    }

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPaddingBottom(6f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, VALUE_FONT));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPaddingBottom(6f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }
}
