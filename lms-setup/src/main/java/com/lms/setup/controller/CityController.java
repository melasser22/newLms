package com.lms.setup.controller;

import com.common.dto.BaseResponse;
import com.lms.setup.dto.CityDto;
import com.lms.setup.service.CityService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/setup/cities")
@Validated
public class CityController {

  private final CityService service;

  public CityController(CityService service) { this.service = service; }

  @PostMapping
  public ResponseEntity<BaseResponse<CityDto>> create(@Valid @RequestBody CityDto request) {
    return ResponseEntity.ok(service.add(request));
  }

  @PutMapping("/{id}")
  public ResponseEntity<BaseResponse<CityDto>> update(@PathVariable Integer id,
                                                      @Valid @RequestBody CityDto request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Integer id) {
    return ResponseEntity.ok(service.delete(id));
  }

  @GetMapping("/{id}")
  public ResponseEntity<BaseResponse<CityDto>> get(@PathVariable Integer id) {
    return ResponseEntity.ok(service.get(id));
  }

  @GetMapping
  public ResponseEntity<BaseResponse<?>> list(
      @ParameterObject
      @PageableDefault(size = 20, sort = "cityEnNm", direction = Sort.Direction.ASC) Pageable pageable,
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "all", defaultValue = "false") boolean all
  ) {
    return ResponseEntity.ok(service.list(pageable, q, all));
  }

  @GetMapping("/countries/{countryId}/active")
  public ResponseEntity<BaseResponse<List<CityDto>>> listActiveByCountry(@PathVariable Integer countryId) {
    return ResponseEntity.ok(service.listActiveByCountry(countryId));
  }
}
