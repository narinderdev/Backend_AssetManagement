package com.example.eam.Common;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;

@Service
public class PdfGeneratorService {

    public byte[] generatePdf(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF generation failed", e);
        }
    }
}
