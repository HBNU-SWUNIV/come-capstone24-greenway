package com.example.demo.service;

import com.example.demo.model.Favorite;
import com.example.demo.model.ParkingLot;
import com.example.demo.model.User;
import com.example.demo.payload.response.FavoriteDTO;
import com.example.demo.payload.response.ParkingLotDetailDTO;
import com.example.demo.repository.FavoriteRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.ParkingLotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    public Favorite addFavorite(Long userId, String parkingLotCode) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        ParkingLot parkingLot = parkingLotRepository.findByCode(parkingLotCode).orElseThrow(() -> new RuntimeException("Parking Lot not found"));

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setParkingLotCode(parkingLotCode);
        return favoriteRepository.save(favorite);
    }

    public void removeFavorite(Long userId, String parkingLotCode) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Favorite favorite = favoriteRepository.findByUserAndParkingLotCode(user, parkingLotCode);
        if (favorite != null) {
            favoriteRepository.delete(favorite);
        } else {
            throw new RuntimeException("Favorite not found");
        }
    }

    public List<FavoriteDTO> getUserFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        return favorites.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private FavoriteDTO convertToDTO(Favorite favorite) {
        FavoriteDTO dto = new FavoriteDTO();
        dto.setId(favorite.getId());
        dto.setParkingLotCode(favorite.getParkingLotCode());

        // parkingLotCode로 주차장 정보 조회
        ParkingLot parkingLot = parkingLotRepository.findByCode(favorite.getParkingLotCode())
                .orElseThrow(() -> new RuntimeException("Parking Lot not found"));

        dto.setParkingLotName(parkingLot.getName());
        dto.setParkingLotAddress(parkingLot.getAddress());

        return dto;
    }

    public ParkingLotDetailDTO getParkingLotDetails(String parkingLotCode) {
        ParkingLot parkingLot = parkingLotRepository.findByCode(parkingLotCode)
                .orElseThrow(() -> new RuntimeException("Parking Lot not found"));
        return convertToDetailDTO(parkingLot);
    }

    private ParkingLotDetailDTO convertToDetailDTO(ParkingLot parkingLot) {
        ParkingLotDetailDTO dto = new ParkingLotDetailDTO();
        dto.set주차장명(parkingLot.getName());
        dto.set주소(parkingLot.getAddress());
        dto.set전화번호(parkingLot.getPhoneNumber());
        dto.set총주차면(parkingLot.getTotalParkingSpots());
        dto.set현재주차차량수(parkingLot.getCurrentParkingVehicles());
        dto.set유무료구분명(parkingLot.getFeeTypeName());
        dto.set토요일유무료구분명(parkingLot.getSaturdayFeeTypeName());
        dto.set공휴일유무료구분명(parkingLot.getHolidayFeeTypeName());
        dto.set월정기권금액(parkingLot.getMonthlySubscriptionFee());
        dto.set기본주차요금(parkingLot.getBaseParkingFee());
        dto.set기본주차시간분단위(parkingLot.getBaseParkingTime());
        dto.set추가단위요금(parkingLot.getAdditionalUnitFee());
        dto.set추가단위시간분단위(parkingLot.getAdditionalUnitTime());
        dto.set일최대요금(parkingLot.getDailyMaxFee());
        dto.set평일운영시작시각HHMM(parkingLot.getWeekdayStartTime());
        dto.set평일운영종료시각HHMM(parkingLot.getWeekdayEndTime());
        dto.set주말운영시작시각HHMM(parkingLot.getWeekendStartTime());
        dto.set주말운영종료시각HHMM(parkingLot.getWeekendEndTime());
        dto.set공휴일운영시작시각HHMM(parkingLot.getHolidayStartTime());
        dto.set공휴일운영종료시각HHMM(parkingLot.getHolidayEndTime());
        dto.set주차장위치좌표위도(parkingLot.getLatitude());
        dto.set주차장위치좌표경도(parkingLot.getLongitude());
        return dto;
    }
}