package com.example.demo.controller;

import com.example.demo.model.Favorite;
import com.example.demo.model.ParkingLot;
import com.example.demo.model.User;
import com.example.demo.payload.response.FavoriteDTO;
import com.example.demo.payload.response.ParkingLotDetailDTO;
import com.example.demo.repository.FavoriteRepository;
import com.example.demo.repository.ParkingLotRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtils;
import com.example.demo.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/add")
    public ResponseEntity<?> addFavorite(@RequestBody FavoriteRequest favoriteRequest) {
        User user = userRepository.findById(favoriteRequest.getUserId()).orElseThrow(() -> new IllegalArgumentException("User doesn't exist"));
        ParkingLot parkingLot = parkingLotRepository.findByCode(favoriteRequest.getParkingLotCode()).orElseThrow(() -> new IllegalArgumentException("Parking lot doesn't exist"));

        Favorite favorite = favoriteRepository.findByUserAndParkingLotCode(user, favoriteRequest.getParkingLotCode());
        if (favorite == null) {
            favoriteService.addFavorite(favoriteRequest.getUserId(), favoriteRequest.getParkingLotCode());
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return ResponseEntity.badRequest().body("Duplicated favorite");
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeFavorite(@RequestParam Long userId, @RequestParam String parkingLotCode) {
        favoriteService.removeFavorite(userId, parkingLotCode);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FavoriteDTO>> getUserFavorites(@PathVariable Long userId) {
        return ResponseEntity.ok(favoriteService.getUserFavorites(userId));
    }

    @GetMapping("/parkingLot/{parkingLotCode}")
    public ResponseEntity<ParkingLotDetailDTO> getParkingLotDetails(@PathVariable String parkingLotCode) {
        return ResponseEntity.ok(favoriteService.getParkingLotDetails(parkingLotCode));
    }
}