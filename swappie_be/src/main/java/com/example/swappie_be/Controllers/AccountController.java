package com.example.swappie_be.Controllers;

import com.example.swappie_be.Entities.Item;
import com.example.swappie_be.Entities.User;
import com.example.swappie_be.Exceptions.BadRequestException;
import com.example.swappie_be.Exceptions.UnauthorizedException;
import com.example.swappie_be.Exceptions.ValidationException;
import com.example.swappie_be.Payloads.LocationDTO;
import com.example.swappie_be.Payloads.UserDTO;
import com.example.swappie_be.Payloads.UserGetResponseDTO;
import com.example.swappie_be.Services.ItemService;
import com.example.swappie_be.Services.UserService;
import com.example.swappie_be.config.Geometry;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class AccountController {
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final UserService userService;
    private final ItemService itemService;
    private final Geometry geometry;

    @Autowired
    public AccountController(UserService userService, ItemService itemService, Geometry geometry) {
        this.geometry = geometry;
        this.itemService = itemService;
        this.userService = userService;
    }

    @GetMapping("/details")
    public UserGetResponseDTO getUserDetails(@AuthenticationPrincipal User user, @RequestParam(name = "id", required = false) String id) {
        if (id == null) {
            UUID userId = user.getUser_id();
            return this.userService.findUserDetailsById(userId);
        } else {
            UUID userID = UUID.fromString(id);
            return this.userService.findUserDetailsById(userID);
        }
    }


    @PutMapping(value = "/me/edit")
    public void getUserByIdAndUpdate(@AuthenticationPrincipal User user, @RequestBody @Validated UserDTO payload, BindingResult validationResult) {
        if (validationResult.hasErrors()) {
            List<String> errorList = validationResult.getFieldErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new ValidationException(errorList);
        }
        if (user == null) {
            throw new UnauthorizedException("Log in again");
        } else this.userService.findByIdAndUpdate(user.getUser_id(), payload);
    }


    @PutMapping(value = "/me/edit/profile_pic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String getUserByIdAndUpdateProfilePic(@AuthenticationPrincipal User user, @RequestParam("profilePic") MultipartFile profilePic) {
        if (user == null) {
            throw new UnauthorizedException("Log in again");
        }
        if (profilePic.isEmpty()) {
            throw new ValidationException(List.of("Profile picture cannot be empty"));
        } else return this.userService.findByIdAndUpdateProfilePic(user.getUser_id(), profilePic);
    }

    @PutMapping("/me/edit/location")
    public void receiveLocation(@AuthenticationPrincipal User user, @RequestBody LocationDTO location) {
        if (location == null || location.lng() == null || location.lat() == null) {
            throw new BadRequestException("Invalid geolocation");
        } else {
            Point userPoint = geometry.createPoint(location.lng(), location.lat());
            this.userService.findByIdAndSetLocation(user, userPoint);
        }
    }

    @PutMapping("/me/fav/add")
    public ResponseEntity<Void> addFavItem(@AuthenticationPrincipal User user, @RequestParam(name = "id") long itemID) {
        Item fav = this.itemService.findByID(itemID);
        this.userService.saveFavourite(user, fav);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/me/fav/remove")
    public ResponseEntity<Void> removeFavItem(@AuthenticationPrincipal User user, @RequestParam(name = "id") long itemID) {
        Item fav = this.itemService.findByID(itemID);
        this.userService.removeFavourite(user, fav);
        return ResponseEntity.ok().build();
    }
}
