package com.example.carrental.adapter.http;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.carrental.application.SearchAvailableCars;
import com.example.carrental.domain.Car;
import com.example.carrental.domain.CarType;

@RestController
@RequestMapping("/cars")
public class CarAvailabilityController {

    private final SearchAvailableCars searchAvailableCars;

    public CarAvailabilityController(SearchAvailableCars searchAvailableCars) {
        this.searchAvailableCars = searchAvailableCars;
    }

    @GetMapping("/available")
    public List<CarAvailabilityResponse> available(
            @RequestParam OffsetDateTime start,
            @RequestParam Integer days,
            @RequestParam(required = false) CarType carType) {
        List<Car> cars = carType == null
                ? searchAvailableCars.search(start, days)
                : searchAvailableCars.search(carType, start, days);
        return cars
                .stream().map(CarAvailabilityResponse::from).toList();
    }
}
