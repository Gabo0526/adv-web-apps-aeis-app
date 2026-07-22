package ec.edu.epn.fis.aeis.rental.controller;

import ec.edu.epn.fis.aeis.rental.service.ExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * El gateway exige rol ADMIN para /api/excel/** (ver AccessRules del gateway);
 * este servicio no vuelve a verificar el rol (ver SecurityConfig).
 */
@RestController
@RequestMapping("/excel")
public class ExcelController {

    private final ExcelExportService excelExportService;

    public ExcelController(ExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateExcel(
            @RequestBody List<Map<String, Object>> data,
            @RequestParam(defaultValue = "reporte") String filename) {

        if (data == null || data.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        byte[] excelBytes = excelExportService.exportToExcel(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".xlsx");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
