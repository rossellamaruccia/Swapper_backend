package com.example.swappie_be.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"username", "password", "accountNonExpired", "accountNonLocked", "authorities", "credentialsNonExpired", "enabled"})
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID user_id;
    private String name;
    private String surname;
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    @JsonIgnore
    private String password;
    private String city;
    private String profilePic;
    @Column(columnDefinition = "geography(Point, 4326)")
    private Point location;
    @Transient
    private Double distanceMeters;
    @OneToMany(mappedBy = "user")
    private List<Item> items;
    @ManyToMany
    @JoinTable(
            name = "Favourites",
            joinColumns = @JoinColumn(name = "user_id"),  // Foreign key in join table for Student
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    private Set<Item> favouriteItems = new HashSet<>();


    public User(String name, String surname, String username, String email, String password, String city) {
        this.name = name;
        this.surname = surname;
        this.username = username;
        this.email = email;
        this.password = password;
        this.city = city;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
