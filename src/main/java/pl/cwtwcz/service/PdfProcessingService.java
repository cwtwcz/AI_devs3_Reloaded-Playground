package pl.cwtwcz.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

@Service
public class PdfProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(PdfProcessingService.class);

    /**
     * Wyciąga tekst ze stron 1-18 PDF
     */
    public String extractTextFromPages(String pdfPath, int startPage, int endPage) {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setStartPage(startPage);
            textStripper.setEndPage(endPage);

            String extractedText = textStripper.getText(document);
            logger.info("Extracted text from pages {}-{}: {} characters", startPage, endPage, extractedText.length());

            return extractedText;
        } catch (IOException e) {
            logger.error("Error extracting text from PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract text from PDF", e);
        }
    }

    /**
     * Konwertuje określoną stronę PDF do obrazu w formacie base64
     */
    public String convertPageToBase64Image(String pdfPath, int pageNumber) {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);

            BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, 100);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            logger.info("Converted page {} to base64 image: {} bytes", pageNumber, imageBytes.length);

            return base64Image;
        } catch (IOException e) {
            logger.error("Error converting PDF page to image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert PDF page to image", e);
        }
    }

    public int getPageCount(String pdfPath) {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            int pageCount = document.getNumberOfPages();
            logger.info("PDF has {} pages", pageCount);
            return pageCount;
        } catch (IOException e) {
            logger.error("Error getting page count from PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get page count from PDF", e);
        }
    }
}