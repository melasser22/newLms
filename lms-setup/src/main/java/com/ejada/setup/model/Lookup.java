package com.ejada.setup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "lookup")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lookup {

    @Id
    @Column(name = "lookup_item_id", unique = true, nullable = false)
    @EqualsAndHashCode.Include
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

    public Boolean isActive() { return isActive != null ? isActive : Boolean.FALSE; }
}
