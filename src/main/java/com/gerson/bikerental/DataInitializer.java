package com.gerson.bikerental;

import com.gerson.bikerental.entity.Bicycle;
import com.gerson.bikerental.enums.BicycleStatus;
import com.gerson.bikerental.enums.BicycleType;
import com.gerson.bikerental.repository.BicycleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final BicycleRepository bicycleRepository;

    public DataInitializer(BicycleRepository bicycleRepository) {
        this.bicycleRepository = bicycleRepository;
    }

    @Override
    public void run(String... args) {
        if (bicycleRepository.count() > 0) return;

        bicycleRepository.save(Bicycle.builder().code("BIC-001").type(BicycleType.URBANA).status(BicycleStatus.DISPONIBLE).build());
        bicycleRepository.save(Bicycle.builder().code("BIC-002").type(BicycleType.MONTANA).status(BicycleStatus.DISPONIBLE).build());
        bicycleRepository.save(Bicycle.builder().code("BIC-003").type(BicycleType.ELECTRICA).status(BicycleStatus.DISPONIBLE).build());
        bicycleRepository.save(Bicycle.builder().code("BIC-004").type(BicycleType.MONTANA).status(BicycleStatus.EN_MANTENIMIENTO).build());
        bicycleRepository.save(Bicycle.builder().code("BIC-005").type(BicycleType.URBANA).status(BicycleStatus.DISPONIBLE).build());
    }
}