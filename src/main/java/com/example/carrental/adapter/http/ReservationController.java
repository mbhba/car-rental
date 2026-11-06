package com.example.carrental.adapter.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.carrental.application.InvalidReservationException;
import com.example.carrental.application.ReserveByType;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReserveByType reserveByType;

    public ReservationController(ReserveByType reserveByType) {
        this.reserveByType = reserveByType;
    }

    // Other API operations remain outside the core scope and are listed as nice-to-have tasks.
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@RequestBody CreateReservationRequest request) {
        if (request == null) {
            throw new InvalidReservationException("Reservation details are required");
        }
        var reservation = reserveByType.reserve(request.carType(), request.start(), request.days());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }
}
