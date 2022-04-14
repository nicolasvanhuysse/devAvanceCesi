package com.cesi.apireservation.service;

import com.cesi.apireservation.dto.ReservationDTO;
import com.cesi.apireservation.model.Concert;
import com.cesi.apireservation.model.Reservation;
import com.cesi.apireservation.model.ReservationStatus;
import com.cesi.apireservation.model.UserApp;
import com.cesi.apireservation.repository.ConcertRepository;
import com.cesi.apireservation.repository.ReservationRepository;
import com.cesi.apireservation.repository.UserAppRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private UserAppRepository userAppRepository;

    private ModelMapper modelMapper;

    public ReservationService() {
        modelMapper = new ModelMapper();
    }

    @Transactional
    public ReservationDTO save(ReservationDTO reservationDTO, Long concertId) throws Exception {
        Reservation reservation = modelMapper.map(reservationDTO, Reservation.class);
        reservation.setReservationStatus(ReservationStatus.pending);
        UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication();
        UserApp userApp = userAppRepository.findUserAppByUsername(userDetails.getUsername());
        if(userApp == null) {
            throw new Exception("user not found");
        }
        reservation.setUser(userApp);
        Concert concert = concertRepository.findById(concertId).get();
        if(concert == null) {
            throw new Exception("concert not found");
        }
        reservation.setConcert(concert);
        reservation = reservationRepository.save(reservation);
        if(reservation == null) {
            throw  new Exception("Error when reservation added to database");
        }
        return modelMapper.map(reservation, ReservationDTO.class);
    }

    //Méthode pour récupérer les réservations

    public List<ReservationDTO> getAll() throws Exception {
        List<ReservationDTO> reservations= new ArrayList<>();
        UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication();
        UserApp userApp = userAppRepository.findUserAppByUsername(userDetails.getUsername());
        if(userApp == null) {
            throw new Exception("user not found");
        }
        if(userApp.isAdmin()) {
            reservationRepository.findAll().forEach(r -> {
                reservations.add(modelMapper.map(r, ReservationDTO.class));
            });
        }else {
            reservationRepository.findAllByUser(userApp).forEach(r -> {
                reservations.add(modelMapper.map(r, ReservationDTO.class));
            });
        }
        return reservations;
    }

    //Méthode pour mettre à jour le status (uniqument pour l'admin)
    public void updateReservation(Long reservationId, ReservationStatus status) throws Exception {
        UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication();
        UserApp userApp = userAppRepository.findUserAppByUsername(userDetails.getUsername());
        if(userApp == null) {
            throw new Exception("user not found");
        }
        if(userApp.isAdmin()) {
            Reservation reservation = reservationRepository.findById(reservationId).get();
            if(reservation == null) {
                throw  new Exception("Error when reservation added to database");
            }
            reservation.setReservationStatus(status);
            reservationRepository.save(reservation);
        }
        else {
            throw new Exception("Not allowed exception");
        }
    }

}
