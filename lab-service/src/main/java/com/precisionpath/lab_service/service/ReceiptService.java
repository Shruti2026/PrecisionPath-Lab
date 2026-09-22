package com.precisionpath.lab_service.service;

import com.precisionpath.lab_service.dto.AppointmentItemResponse;
import com.precisionpath.lab_service.dto.ReceiptResponse;
import com.precisionpath.lab_service.entity.Appointment;
import com.precisionpath.lab_service.entity.AppointmentStatus;
import com.precisionpath.lab_service.entity.LabTest;
import com.precisionpath.lab_service.entity.Payment;
import com.precisionpath.lab_service.exception.InvalidStateException;
import com.precisionpath.lab_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final Set<AppointmentStatus> RECEIPT_STATUSES =
            EnumSet.of(AppointmentStatus.APPROVED, AppointmentStatus.COMPLETED);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ReceiptResponse buildReceipt(Appointment appointment) {

        if (!RECEIPT_STATUSES.contains(appointment.getStatus())) {
            throw new InvalidStateException("A receipt is available once the appointment is approved");
        }

        Payment payment = paymentRepository.findByAppointmentId(appointment.getId()).orElse(null);

        List<ReceiptResponse.TestDetail> tests = AppointmentService.testsOf(appointment).stream()
                .map(test -> new ReceiptResponse.TestDetail(
                        test.getCode(),
                        test.getName(),
                        test.getPurpose(),
                        test.getSampleType(),
                        List.copyOf(test.getPrerequisites())
                ))
                .toList();

        return new ReceiptResponse(
                receiptNumber(appointment),
                LocalDateTime.now(clock),
                new ReceiptResponse.Patient(
                        appointment.getPatientId(),
                        appointment.getPatientName(),
                        appointment.getPatientEmail(),
                        appointment.getPatientPhone(),
                        appointment.getPatientAge(),
                        appointment.getPatientGender()
                ),
                new ReceiptResponse.Appointment(
                        appointment.getId(),
                        appointment.getAppointmentDate(),
                        appointment.getStatus()
                ),
                appointment.getItems().stream().map(AppointmentItemResponse::from).toList(),
                tests,
                appointment.getTotalAmount(),
                payment == null
                        ? new ReceiptResponse.Payment(null, null, null, null)
                        : new ReceiptResponse.Payment(
                                payment.getStatus(),
                                payment.getMethod(),
                                payment.getTransactionReference(),
                                payment.getPaidAt()
                        ),
                new ReceiptResponse.Queue(
                        appointment.getTokenNumber(),
                        appointment.getEstimatedWaitMinutes()
                )
        );
    }

    @Transactional(readOnly = true)
    public byte[] buildReceiptPdf(Appointment appointment) {

        return renderPdf(buildReceipt(appointment));
    }

    public static String receiptNumber(Appointment appointment) {
        return "PPL-" + appointment.getId().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    // ---------- PDF ----------

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 9);

    private byte[] renderPdf(ReceiptResponse receipt) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        PdfWriter.getInstance(document, out);
        document.open();

        Paragraph title = new Paragraph("PrecisionPath Lab", TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Digital Receipt", HEADING);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(12);
        document.add(subtitle);

        PdfPTable summary = keyValueTable();
        addRow(summary, "Receipt No.", receipt.receiptNumber());
        addRow(summary, "Generated", receipt.generatedAt().format(DATE_TIME));
        addRow(summary, "Appointment Date", receipt.appointment().appointmentDate().format(DATE));
        addRow(summary, "Status", receipt.appointment().status().name());
        addRow(summary, "Token No.", receipt.queue().tokenNumber() == null
                ? "Issued at check-in" : String.valueOf(receipt.queue().tokenNumber()));
        addRow(summary, "Estimated Wait", receipt.queue().estimatedWaitMinutes() == null
                ? "-" : receipt.queue().estimatedWaitMinutes() + " minutes");
        document.add(summary);

        section(document, "Patient");
        PdfPTable patient = keyValueTable();
        addRow(patient, "Name", receipt.patient().name());
        addRow(patient, "Email", receipt.patient().email());
        addRow(patient, "Phone", receipt.patient().phone());
        addRow(patient, "Age / Gender", receipt.patient().age() + " / " + receipt.patient().gender());
        document.add(patient);

        section(document, "Billed Items");
        PdfPTable items = new PdfPTable(new float[]{4, 1.5f, 1.5f});
        items.setWidthPercentage(100);
        headerCell(items, "Item");
        headerCell(items, "Type");
        headerCell(items, "Amount");
        for (AppointmentItemResponse item : receipt.items()) {
            items.addCell(cell(item.name(), BODY));
            items.addCell(cell(item.itemType().name(), BODY));
            items.addCell(amountCell(item.price(), BODY));
        }
        PdfPCell totalLabel = cell("Total", LABEL);
        totalLabel.setColspan(2);
        items.addCell(totalLabel);
        items.addCell(amountCell(receipt.totalAmount(), LABEL));
        document.add(items);

        section(document, "Payment");
        PdfPTable payment = keyValueTable();
        ReceiptResponse.Payment paid = receipt.payment();
        addRow(payment, "Status", paid.status() == null ? "UNPAID" : paid.status().name());
        if (paid.status() != null) {
            addRow(payment, "Method", paid.method().name());
            addRow(payment, "Transaction Ref.", paid.transactionReference());
            addRow(payment, "Paid At", paid.paidAt().format(DATE_TIME));
        }
        document.add(payment);

        section(document, "Tests & Prerequisites");
        for (ReceiptResponse.TestDetail test : receipt.tests()) {
            Paragraph name = new Paragraph(test.name() + " (" + test.code() + ")", LABEL);
            name.setSpacingBefore(6);
            document.add(name);
            document.add(new Paragraph(test.purpose(), SMALL));
            if (test.sampleType() != null) {
                document.add(new Paragraph("Sample: " + test.sampleType(), SMALL));
            }
            if (test.prerequisites().isEmpty()) {
                document.add(new Paragraph("Prerequisites: none", SMALL));
            } else {
                org.openpdf.text.List prerequisites =
                        new org.openpdf.text.List(org.openpdf.text.List.UNORDERED);
                prerequisites.setIndentationLeft(12);
                test.prerequisites().forEach(item -> prerequisites.add(new ListItem(item, SMALL)));
                document.add(prerequisites);
            }
        }

        Paragraph footer = new Paragraph(
                "Please carry this receipt and arrive 15 minutes before your turn.", SMALL);
        footer.setSpacingBefore(18);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();

        return out.toByteArray();
    }

    private PdfPTable keyValueTable() {
        PdfPTable table = new PdfPTable(new float[]{1.3f, 3});
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        return table;
    }

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = cell(label, LABEL);
        labelCell.setBorder(Rectangle.NO_BORDER);
        PdfPCell valueCell = cell(value == null ? "-" : value, BODY);
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void section(Document document, String text) {
        Paragraph heading = new Paragraph(text, HEADING);
        heading.setSpacingBefore(12);
        heading.setSpacingAfter(4);
        document.add(heading);
    }

    private void headerCell(PdfPTable table, String text) {
        PdfPCell cell = cell(text, LABEL);
        cell.setGrayFill(0.9f);
        table.addCell(cell);
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell amountCell(BigDecimal amount, Font font) {
        PdfPCell cell = cell("Rs. " + amount.setScale(2, RoundingMode.HALF_UP), font);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}
