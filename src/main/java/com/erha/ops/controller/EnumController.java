package com.erha.ops.controller;

import com.erha.ops.entity.*;
import com.erha.ops.rfq.enums.ProjectType;
import com.erha.ops.rfq.enums.RfqStatus;
import com.erha.quote.model.QuoteStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/enums")
@CrossOrigin(origins = "*")
public class EnumController {

    @GetMapping("/all")
    public ResponseEntity<Map<String, List<Map<String, String>>>> getAllEnums() {
        Map<String, List<Map<String, String>>> enums = new HashMap<>();
        
        enums.put("rfqStatus", convertEnumToMap(RfqStatus.values()));
        enums.put("projectType", convertEnumToMap(ProjectType.values()));
        enums.put("quoteStatus", convertEnumToMap(QuoteStatus.values()));
        enums.put("assignmentStatus", convertEnumToMap(AssignmentStatus.values()));
        enums.put("jobAssignmentStatus", convertEnumToMap(JobAssignmentStatus.values()));
        enums.put("workshopStatus", convertEnumToMap(WorkshopStatus.values()));
        enums.put("deliveryNoteStatus", convertEnumToMap(DeliveryNoteStatus.values()));
        enums.put("holdingPointStatus", convertEnumToMap(HoldingPointStatus.values()));
        enums.put("signoffStatus", convertEnumToMap(SignoffStatus.values()));
        enums.put("timeEntryStatus", convertEnumToMap(TimeEntryStatus.values()));
        
        return ResponseEntity.ok(enums);
    }
    
    @GetMapping("/rfq-status")
    public ResponseEntity<List<Map<String, String>>> getRfqStatus() {
        return ResponseEntity.ok(convertEnumToMap(RfqStatus.values()));
    }
    
    @GetMapping("/project-type")
    public ResponseEntity<List<Map<String, String>>> getProjectType() {
        return ResponseEntity.ok(convertEnumToMap(ProjectType.values()));
    }
    
    private <E extends Enum<E>> List<Map<String, String>> convertEnumToMap(E[] enumValues) {
        return Arrays.stream(enumValues)
                .map(e -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("value", e.name());
                    map.put("label", formatLabel(e.name()));
                    return map;
                })
                .collect(Collectors.toList());
    }
    
    private String formatLabel(String enumName) {
        return Arrays.stream(enumName.split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}