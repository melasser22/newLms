package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.model.Country;
import com.lms.setup.service.CountryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/setup/countries")
@Validated
public class CountryController {

    private final CountryService countryService;
    public CountryController(CountryService countryService) { this.countryService = countryService; }

    @PostMapping
    public ResponseEntity<BaseResponse<Country>> add(@Valid @RequestBody Country body) {
        return ResponseEntity.ok(countryService.add(body));
    }

    @PutMapping("/{countryId}")
    public ResponseEntity<BaseResponse<Country>> update(@PathVariable Integer countryId,
                                                        @Valid @RequestBody Country body) {
        return ResponseEntity.ok(countryService.update(countryId, body));
    }

    @GetMapping("/{countryId}")
    public ResponseEntity<BaseResponse<Country>> get(@PathVariable Integer countryId) {
        return ResponseEntity.ok(countryService.get(countryId));
    }

    @GetMapping
    public ResponseEntity<?> list(@PageableDefault(size = 20) Pageable pageable,
                                  @RequestParam(required = false) String q, @RequestParam(required = false) boolean all) {
        return ResponseEntity.ok(countryService.list(pageable, q,all));
    }

    @GetMapping("/active")
    public ResponseEntity<?> listActive() {
        return ResponseEntity.ok(countryService.listActive());
    }
}
