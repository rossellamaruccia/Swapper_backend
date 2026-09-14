package com.example.swappie_be.Services;

import com.cloudinary.utils.ObjectUtils;
import com.example.swappie_be.Entities.Category;
import com.example.swappie_be.Entities.Item;
import com.example.swappie_be.Entities.User;
import com.example.swappie_be.Exceptions.NotFoundException;
import com.example.swappie_be.Exceptions.UnauthorizedException;
import com.example.swappie_be.Payloads.ItemDTO;
import com.example.swappie_be.Payloads.ItemGetResponseDTO;
import com.example.swappie_be.Repositories.ItemRepo;
import com.example.swappie_be.config.CloudinaryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
    private final ItemRepo itemRepo;
    private final CloudinaryConfig cloudinaryConfig;

    @Autowired
    public ItemService(ItemRepo itemRepo, CloudinaryConfig cloudinaryConfig) {
        this.itemRepo = itemRepo;
        this.cloudinaryConfig = cloudinaryConfig;
    }

    public void save(ItemDTO payload, User user, MultipartFile[] files) {
        List<String> imageUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    Map uploadResult = cloudinaryConfig.cloudinary().uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
                    imageUrls.add((String) uploadResult.get("secure_url"));
                }
            }
            Item item = new Item();
            item.setTitle(payload.title());
            item.setDescription(payload.description());
            item.setType(payload.itemType());
            item.setCategory(payload.category());
            item.setPics(imageUrls);
            item.setUser(user);
            item.setLocation(user.getLocation());
            this.itemRepo.save(item);

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    public void editItem(ItemDTO payload, long itemID, UUID userID, MultipartFile[] files) {
        List<String> imageUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    Map uploadResult = cloudinaryConfig.cloudinary().uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
                    imageUrls.add((String) uploadResult.get("secure_url"));
                }
            }
            Item item = this.itemRepo.findById(itemID).orElseThrow();
            if (item.getUser().getUser_id().equals(userID)) {
                item.setTitle(payload.title());
                item.setDescription(payload.description());
                item.setType(payload.itemType());
                item.setCategory(payload.category());
                item.setPics(imageUrls);
                this.itemRepo.save(item);
            } else throw new UnauthorizedException("You cannot edit this item");

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    public ArrayList<ItemGetResponseDTO> findItemsPerUser(User user) {
        Optional<ArrayList<Item>> optional = this.itemRepo.findAllByUser(user);
        if (optional.isPresent()) {
            ArrayList<Item> array = optional.get();
            return array.stream()
                    .map(item -> new ItemGetResponseDTO(
                            item.getItem_id(),
                            item.getTitle(),
                            item.getDescription(),
                            item.getType(),
                            item.getCategory(),
                            item.getUser().getUser_id(),
                            item.getPics(),
                            item.getLocation().getX(),
                            item.getLocation().getY()
                    ))
                    .collect(Collectors.toCollection(ArrayList::new));
        } else throw new NotFoundException(user.getUser_id());
    }

    public List<ItemGetResponseDTO> findAllItems(User user, int radius) {
        double userLon = user.getLocation().getX();
        double userLat = user.getLocation().getY();
        UUID uuid = user.getUser_id();

        return this.itemRepo.findItemsWithinRadius(userLat, userLon, radius)
                .stream()
                .filter(item -> !item.getUser().getUser_id().equals(uuid))
                .map(item -> new ItemGetResponseDTO(
                        item.getItem_id(),
                        item.getTitle(),
                        item.getDescription(),
                        item.getType(),
                        item.getCategory(),
                        item.getUser().getUser_id(),
                        item.getPics(),
                        item.getLocation().getX(),
                        item.getLocation().getY()
                ))
                .collect(Collectors.toList());
    }// questa funzione deve ritornare tutti gli item tranne quelli dell'user che fa la richiesta

    public Item findByID(long id) {
        return this.itemRepo.findById(id).orElseThrow();
    }

    public ItemGetResponseDTO findItemById(long id) {
        Item found = this.itemRepo.findById(id).orElseThrow();
        double lng = found.getLocation().getX();
        double lat = found.getLocation().getY();
        return new ItemGetResponseDTO(found.getItem_id(), found.getTitle(), found.getDescription(), found.getType(), found.getCategory(), found.getUser().getUser_id(), found.getPics(), lng, lat);
    }

    public List<ItemGetResponseDTO> findAllByCategory(User user, Category category, int radius) {
        double userLon = user.getLocation().getX();
        double userLat = user.getLocation().getY();
        UUID uuid = user.getUser_id();

        return this.itemRepo.findItemsWithinRadius(userLat, userLon, radius)
                .stream()
                .filter(item -> !item.getUser().getUser_id().equals(uuid))
                .filter(item -> item.getCategory() == category)
                .map(item -> new ItemGetResponseDTO(
                        item.getItem_id(),
                        item.getTitle(),
                        item.getDescription(),
                        item.getType(),
                        item.getCategory(),
                        item.getUser().getUser_id(),
                        item.getPics(),
                        item.getLocation().getX(),
                        item.getLocation().getY()
                ))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
