package com.lms.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "lookup")
public class Lookup {

    @Id
    @Column(name = "lookup_item_id", unique = true, nullable = false)
    private Integer lookupItemId;

    @Column(name = "lookup_item_cd")
    private String lookupItemCd;

    @Column(name = "lookup_item_en_nm")
    private String lookupItemEnNm;

    @Column(name = "lookup_item_ar_nm")
    private String lookupItemArNm;

    @Column(name = "lookup_group_code")
    private String lookupGroupCode;

    @Column(name = "parent_lookup_id")
    private String parentLookupId;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "item_en_description")
    private String itemEnDescription;

    @Column(name = "item_ar_description")
    private String itemArDescription;

    // --- getters & setters ---
    public Integer getLookupItemId() { return lookupItemId; }
    public void setLookupItemId(Integer lookupItemId) { this.lookupItemId = lookupItemId; }

    public String getLookupItemCd() { return lookupItemCd; }
    public void setLookupItemCd(String lookupItemCd) { this.lookupItemCd = lookupItemCd; }

    public String getLookupItemEnNm() { return lookupItemEnNm; }
    public void setLookupItemEnNm(String lookupItemEnNm) { this.lookupItemEnNm = lookupItemEnNm; }

    public String getLookupItemArNm() { return lookupItemArNm; }
    public void setLookupItemArNm(String lookupItemArNm) { this.lookupItemArNm = lookupItemArNm; }

    public String getLookupGroupCode() { return lookupGroupCode; }
    public void setLookupGroupCode(String lookupGroupCode) { this.lookupGroupCode = lookupGroupCode; }

    public String getParentLookupId() { return parentLookupId; }
    public void setParentLookupId(String parentLookupId) { this.parentLookupId = parentLookupId; }

    public Boolean getIsActive() { return isActive; }
    public Boolean isActive() { return isActive != null ? isActive : Boolean.FALSE; }
    public void setIsActive(Boolean active) { isActive = active; }

    public String getItemEnDescription() { return itemEnDescription; }
    public void setItemEnDescription(String itemEnDescription) { this.itemEnDescription = itemEnDescription; }

    public String getItemArDescription() { return itemArDescription; }
    public void setItemArDescription(String itemArDescription) { this.itemArDescription = itemArDescription; }
}
