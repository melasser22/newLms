package com.ejada.mapstruct.starter.config;

import org.mapstruct.Builder;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingInheritanceStrategy;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    componentModel = "spring",
    // Constructor injection for nested/used mappers
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,

    // Prefer adders for collections (good for JPA aggregates)
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,

    // Null-handling defaults
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,   // for patch/update methods
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,          // null List/Map -> empty

    // Strictness
    unmappedTargetPolicy = ReportingPolicy.ERROR,  // fail when you forget to map a target prop
    unmappedSourcePolicy = ReportingPolicy.WARN,   // noise control for unused source props

    // Be explicit about config inheritance (safer in larger codebases)
    mappingInheritanceStrategy = MappingInheritanceStrategy.EXPLICIT,

    // Lombok builders: disable if you prefer property accessors; flip to configure if you rely on builders
    builder = @Builder(disableBuilder = true)
)
public interface SharedMapstructConfig {}
