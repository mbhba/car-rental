package com.example.carrental.adapter.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.carrental.application.CancelReservation;
import com.example.carrental.application.FindReservation;
import com.example.carrental.application.InvalidReservationException;
import com.example.carrental.application.ReserveCar;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReserveCar reserveCar;
    private final FindReservation findReservation;
    private final CancelReservation cancelReservation;

    public ReservationController(
            ReserveCar reserveCar,
            FindReservation findReservation,
            CancelReservation cancelReservation) {
        this.reserveCar = reserveCar;
        this.findReservation = findReservation;
        this.cancelReservation = cancelReservation;
    }

    // Customer, payment, authentication, and fleet-management operations are outside this project's scope.
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@RequestBody CreateReservationRequest request) {
        if (request == null) {
            throw new InvalidReservationException("Reservation details are required");
        }
        var reservation = reserveCar.reserve(
                request.carType(), request.carId(), request.start(), request.days());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }

    @GetMapping("/{id}")
    public ReservationResponse find(@PathVariable long id) {
        return ReservationResponse.from(findReservation.find(id));
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable long id) {
        return ReservationResponse.from(cancelReservation.cancel(id));
    }
}
