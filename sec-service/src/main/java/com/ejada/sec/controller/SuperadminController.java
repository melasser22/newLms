package com.ejada.sec.controller;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.http.BaseResponseController;
import com.ejada.sec.dto.admin.*;
import com.ejada.sec.service.SuperadminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admins")
@RequiredArgsConstructor
@Tag(name = "Superadmin Management", description = "APIs for managing superadmin accounts")
@PreAuthorize("hasRole('EJADA_OFFICER')")
public class SuperadminController extends BaseResponseController {

    private final SuperadminService superadminService;
    
    @PostMapping
    @Operation(summary = "Create new superadmin", 
               description = "Creates a new superadmin account. Only existing superadmins can perform this action.")
    public ResponseEntity<BaseResponse<SuperadminDto>> createSuperadmin(
            @Valid @RequestBody CreateSuperadminRequest request) {
        return respond(() -> superadminService.createSuperadmin(request), HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update superadmin", 
               description = "Updates an existing superadmin account")
    public ResponseEntity<BaseResponse<SuperadminDto>> updateSuperadmin(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSuperadminRequest request) {
        return respond(() -> superadminService.updateSuperadmin(id, request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete superadmin", 
               description = "Disables a superadmin account. Cannot delete your own account or the last active superadmin.")
    public ResponseEntity<BaseResponse<Void>> deleteSuperadmin(@PathVariable Long id) {
        return respond(() -> superadminService.deleteSuperadmin(id));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get superadmin details", 
               description = "Retrieves details of a specific superadmin")
    public ResponseEntity<BaseResponse<SuperadminDto>> getSuperadmin(@PathVariable Long id) {
        return respond(() -> superadminService.getSuperadmin(id));
    }
    
    @GetMapping
    @Operation(summary = "List all superadmins", 
               description = "Retrieves a paginated list of all superadmin accounts")
    public ResponseEntity<BaseResponse<Page<SuperadminDto>>> listSuperadmins(Pageable pageable) {
        return respond(() -> superadminService.listSuperadmins(pageable));
    }
    

    @PostMapping("/first-login")
    @PreAuthorize("hasRole('EJADA_OFFICER')")
    public ResponseEntity<BaseResponse<Void>> completeFirstLogin(@Valid @RequestBody FirstLoginRequest request) {
      return respond(() -> superadminService.completeFirstLogin(request));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('EJADA_OFFICER')")
    public ResponseEntity<BaseResponse<Void>> changeSuperadminPassword(@Valid @RequestBody ChangePasswordRequest request) {
      return respond(() -> superadminService.changePassword(request));
    }
}