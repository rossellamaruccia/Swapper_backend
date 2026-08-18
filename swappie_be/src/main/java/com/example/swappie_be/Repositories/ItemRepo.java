package com.example.swappie_be.Repositories;

import com.example.swappie_be.Entities.Item;
import com.example.swappie_be.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepo extends JpaRepository<Item, Long> {

    Optional<ArrayList<Item>> findAllByUser(User user);

    @Query(value = "SELECT * FROM items i " +
            "WHERE ST_DWithin(i.location, ST_MakePoint(:lon, :lat)::geography, :radius * 1000) ", nativeQuery = true)
    List<Item> findItemsWithinRadius(double lat, double lon, @Param("radius") int radius);
}
