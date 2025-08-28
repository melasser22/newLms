package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.model.SystemParameter;
import com.lms.setup.service.SystemParameterService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/setup/system-parameters")
@Validated
public class SystemParameterController {

    private final SystemParameterService systemParameterService;
    public SystemParameterController(SystemParameterService systemParameterService) {
        this.systemParameterService = systemParameterService;
    }

    @PostMapping
    public ResponseEntity<BaseResponse<SystemParameter>> add(@Valid @RequestBody SystemParameter body) {
        return ResponseEntity.ok(systemParameterService.add(body));
    }

    @PutMapping("/{paramId}")
    public ResponseEntity<BaseResponse<SystemParameter>> update(@PathVariable Integer paramId,
                                                                @Valid @RequestBody SystemParameter body) {
        return ResponseEntity.ok(systemParameterService.update(paramId, body));
    }

    @GetMapping("/{paramId}")
    public ResponseEntity<BaseResponse<SystemParameter>> get(@PathVariable Integer paramId) {
        return ResponseEntity.ok(systemParameterService.get(paramId));
    }

    @GetMapping
    public ResponseEntity<?> list(@PageableDefault(size = 20) Pageable pageable,
                                  @RequestParam(required = false) String group,
                                  @RequestParam(required = false) Boolean onlyActive) {
        return ResponseEntity.ok(systemParameterService.list(pageable, group, onlyActive));
    }

    // Fetch by keys (cached inside service). POST to accept a JSON array payload.
    @PostMapping("/by-keys")
    public ResponseEntity<BaseResponse<List<SystemParameter>>> getByKeys(@RequestBody List<String> keys) {
        return ResponseEntity.ok(systemParameterService.getByKeys(keys));
    }
}
