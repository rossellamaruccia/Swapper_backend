package com.example.swappie_be.Services;

import com.cloudinary.utils.ObjectUtils;
import com.example.swappie_be.Entities.Item;
import com.example.swappie_be.Entities.User;
import com.example.swappie_be.Exceptions.NotFoundException;
import com.example.swappie_be.Payloads.ItemGetResponseDTO;
import com.example.swappie_be.Payloads.LocationDTO;
import com.example.swappie_be.Payloads.UserDTO;
import com.example.swappie_be.Payloads.UserGetResponseDTO;
import com.example.swappie_be.Repositories.UserRepo;
import com.example.swappie_be.config.CloudinaryConfig;
import com.example.swappie_be.config.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryConfig cloudinaryConfig;
    private final Geometry geometry;

    @Autowired
    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder, CloudinaryConfig config, Geometry geometry) {

        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryConfig = config;
        this.geometry = geometry;
    }

    public void save(UserDTO payload, Point userPoint) {
        User newUser = new User(payload.name(), payload.surname(), payload.username(), payload.email(), passwordEncoder.encode(payload.password()), payload.city());
        newUser.setLocation(userPoint);
        this.userRepo.save(newUser);
    }

    public User findById(UUID id) {
        Optional<User> op = this.userRepo.findById(id);
        if (op.isPresent()) return op.get();
        else throw new NotFoundException(id);
    }

    @Transactional(readOnly = true)
    public UserGetResponseDTO findUserDetailsById(UUID id) {
        User user = this.userRepo.findById(id).orElseThrow();
        Set<ItemGetResponseDTO> favouriteItemsDTO = user.getFavouriteItems()
                .stream()
                .map(item -> new ItemGetResponseDTO(
                        item.getItem_id(),
                        item.getTitle(),
                        item.getDescription(),
                        item.getType(),
                        item.getCategory(),
                        item.getUserId(),
                        item.getPics(),
                        item.getLocation().getY(),
                        item.getLocation().getX()
                ))
                .collect(Collectors.toSet());
        return new UserGetResponseDTO(user.getUser_id(), user.getName(), user.getSurname(), user.getUsername(), user.getEmail(), user.getCity(), user.getProfilePic(), new LocationDTO(user.getLocation().getX(), user.getLocation().getY()), favouriteItemsDTO);
    }

    public UUID returnID(String email) {
        User user = this.userRepo.findByEmail(email).orElseThrow();
        return user.getUser_id();
    }


    public User findByEmail(String email) {
        Optional<User> op = this.userRepo.findByEmail(email);
        if (op.isPresent()) return op.get();
        else throw new NotFoundException("Email non registrata.");
    }

    public void findByIdAndUpdate(UUID id, UserDTO payload) {
        Optional<User> op = this.userRepo.findById(id);
        try {
            if (op.isPresent()) {
                User user = op.get();
                user.setName(payload.name());
                user.setSurname(payload.surname());
                user.setEmail(payload.email());
                user.setCity(payload.city());
                this.userRepo.save(user);
            } else throw new NotFoundException(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update profile", e);
        }
    }

    public String findByIdAndUpdateProfilePic(UUID id, MultipartFile profilePic) {
        Optional<User> op = this.userRepo.findById(id);
        try {
            Map uploadResult = cloudinaryConfig.cloudinary().uploader().upload(profilePic.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
            String profilePicUrl = uploadResult.get("secure_url").toString();
            if (op.isPresent()) {
                User user = op.get();
                user.setProfilePic(profilePicUrl);
                this.userRepo.save(user);
                return user.getProfilePic();
            } else throw new NotFoundException(id);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    public void findByIdAndSetLocation(User user, Point userPoint) {
        Optional<User> op = this.userRepo.findById(user.getUser_id());
        try {
            if (op.isPresent()) {
                User found = op.get();
                found.setLocation(userPoint);
                this.userRepo.save(found);
            } else throw new NotFoundException(user.getUser_id());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update your location", e);
        }
    }

    @Transactional
    public void saveFavourite(User user, Item item) {
        User activeUser = this.userRepo.findById(user.getUser_id())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));
        activeUser.getFavouriteItems().add(item);
    }

    @Transactional
    public void removeFavourite(User user, Item item) {
        User activeUser = this.userRepo.findById(user.getUser_id())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));
        activeUser.getFavouriteItems().remove(item);
    }
}
