package com.gerson.bikerental.repository;

import com.gerson.bikerental.entity.Bicycle;
import com.gerson.bikerental.entity.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {
    List<Rental> findByBicycle(Bicycle bicycle);
}