package ec.edu.epn.fis.aeis.rental.service;

import ec.edu.epn.fis.aeis.rental.util.ExcelGenerator;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    public byte[] exportToExcel(List<Map<String, Object>> data) {
        try {
            return ExcelGenerator.generateExcelFromMapList(data);
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el archivo Excel", e);
        }
    }
}
