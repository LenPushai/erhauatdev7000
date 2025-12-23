package com.erha.ops.controller;

import com.erha.ops.service.JobCardPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@CrossOrigin(origins = "*")
public class JobCardController {

    @Autowired
    private JobCardPdfService jobCardPdfService;

    /**
     * Generate and download Job Card PDF
     * GET /api/v1/jobs/{id}/job-card-pdf
     */
    @GetMapping("/{id}/job-card-pdf")
    public ResponseEntity<byte[]> downloadJobCardPdf(@PathVariable Long id) {
        try {
            byte[] pdfBytes = jobCardPdfService.generateJobCardPdf(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ERHA_JobCard_" + id + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}