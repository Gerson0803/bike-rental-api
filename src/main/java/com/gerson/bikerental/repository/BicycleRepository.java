package com.gerson.bikerental.repository;

import com.gerson.bikerental.entity.Bicycle;
import com.gerson.bikerental.enums.BicycleStatus;
import com.gerson.bikerental.enums.BicycleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BicycleRepository extends JpaRepository<Bicycle, Long> {

    Optional<Bicycle> findByCode(String code);

    List<Bicycle> findByStatus(BicycleStatus status);

    List<Bicycle> findByStatusAndType(
            BicycleStatus status,
            BicycleType type
    );

}
