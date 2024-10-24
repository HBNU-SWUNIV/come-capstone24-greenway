package com.example.demo.repository;

import com.example.demo.model.Favorite;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 기존 ParkingLot 객체를 사용한 메서드를 제거하고, parkingLotCode를 사용한 메서드를 사용합니다.
    Favorite findByUserAndParkingLotCode(User user, String parkingLotCode);
    List<Favorite> findByUser(User user);
    List<Favorite> findByUserId(Long userId);
    List<Favorite> findByUserAndParkingLotCode(String parkingLotCode, Long userId); // 수정된 부분
}
