package com.example.swappie_be.Controllers;

import com.example.swappie_be.Entities.Category;
import com.example.swappie_be.Entities.User;
import com.example.swappie_be.Exceptions.UnauthorizedException;
import com.example.swappie_be.Exceptions.ValidationException;
import com.example.swappie_be.Payloads.ItemDTO;
import com.example.swappie_be.Payloads.ItemGetResponseDTO;
import com.example.swappie_be.Services.ItemService;
import com.example.swappie_be.Services.UserService;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/items")
public class ItemController {
    private final ItemService itemService;
    private final UserService userService;

    public ItemController(ItemService itemService, UserService userService) {
        this.itemService = itemService;
        this.userService = userService;
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void createNewItem(
            @AuthenticationPrincipal User user,
            @ModelAttribute @Validated ItemDTO payload,
            BindingResult validationResult,
            @RequestParam("files") MultipartFile[] files
    ) {
        if (validationResult.hasErrors()) {
            List<String> errorList = validationResult.getFieldErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new ValidationException(errorList);
        }

        if (user == null) {
            throw new UnauthorizedException("Log in again");
        }

        this.itemService.save(payload, user, files);
    }

    @GetMapping("")
    public ArrayList<ItemGetResponseDTO> getItemsPerUser(@AuthenticationPrincipal User user, @RequestParam(name = "user") UUID uuid) {
        if (user.getUser_id() == uuid) {
            return this.itemService.findItemsPerUser(user);
        } else {
            User user1 = this.userService.findById(uuid);
            return this.itemService.findItemsPerUser(user1);
        }
    }

    @GetMapping("/details")
    public ItemGetResponseDTO getItemDetails(@AuthenticationPrincipal User user, @RequestParam(name = "id", required = false) long itemID) {
        return itemService.findItemById(itemID);
    }

    //TODO: aggiungere parametro che passa la distanza massima con cui filtrare i risultati di questa GET
    @GetMapping("/feed")
    public List<ItemGetResponseDTO> findAllItems(@AuthenticationPrincipal User user,
                                                 @RequestParam(name = "radius", required = false) int radius,
                                                 @RequestParam(name = "category", required = false) Category category) {
        if (category != null) {
            return itemService.findAllByCategory(user, category, radius);
        } else {
            return itemService.findAllItems(user, radius);
        }
    }

    @PostMapping(value = "/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void editItem(@AuthenticationPrincipal User user,
                         @RequestParam(name = "id", required = false) long itemID,
                         @ModelAttribute @Validated ItemDTO payload,
                         BindingResult validationResult,
                         @RequestParam("files") MultipartFile[] files) {
        if (validationResult.hasErrors()) {
            List<String> errorList = validationResult.getFieldErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new ValidationException(errorList);
        }

        if (user == null) {
            throw new UnauthorizedException("Log in again");
        }

        this.itemService.editItem(payload, itemID, user.getUser_id(), files);
    }
}
