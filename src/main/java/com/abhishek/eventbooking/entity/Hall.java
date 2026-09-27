package com.abhishek.eventbooking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "halls",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "venue_id",
                                "name"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "venue_id",
            nullable = false
    )
    private Venue venue;
}