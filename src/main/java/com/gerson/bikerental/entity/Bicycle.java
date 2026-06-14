package com.gerson.bikerental.entity;

import com.gerson.bikerental.enums.BicycleStatus;
import com.gerson.bikerental.enums.BicycleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Bicycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BicycleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BicycleStatus status;
}
