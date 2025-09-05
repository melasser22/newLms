package com.ejada.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class FeatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(length = 255)
    private String description;
}
