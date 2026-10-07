package com.abhishek.eventbooking.specification;

import com.abhishek.eventbooking.dto.request.EventSearchCriteria;
import com.abhishek.eventbooking.entity.Event;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> withFilters(EventSearchCriteria criteria) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // =================================================
            // NAME
            // =================================================

            if (criteria.getName() != null && !criteria.getName().isBlank()) {

                String searchName = "%" + criteria.getName().trim().toLowerCase(Locale.ROOT) + "%";

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                searchName
                        )
                );
            }

            // =================================================
            // CATEGORY
            // =================================================

            if (criteria.getCategory() != null) {

                predicates.add(
                        criteriaBuilder.equal(root.get("category"), criteria.getCategory())
                );
            }

            // =================================================
            // STATUS
            // =================================================

            if (criteria.getStatus() != null) {

                predicates.add(
                        criteriaBuilder.equal(root.get("status"), criteria.getStatus())
                );
            }

            // =================================================
            // HALL ID
            // =================================================

            if (criteria.getHallId() != null) {

                predicates.add(
                        criteriaBuilder.equal(root.get("hall").get("id"), criteria.getHallId())
                );
            }

            /*
             * We only create Hall/Venue joins when one of
             * these filters actually needs them.
             */

            Join<Object, Object> hallJoin = null;
            Join<Object, Object> venueJoin = null;

            // =================================================
            // VENUE ID / CITY
            // =================================================

            if (criteria.getVenueId() != null || (criteria.getCity() != null && !criteria.getCity().isBlank())) {

                hallJoin = root.join("hall", JoinType.INNER);

                venueJoin = hallJoin.join("venue", JoinType.INNER);
            }

            // =================================================
            // VENUE ID
            // =================================================

            if (criteria.getVenueId() != null) {

                predicates.add(
                        criteriaBuilder.equal(venueJoin.get("id"), criteria.getVenueId())
                );
            }

            // =================================================
            // CITY
            // =================================================

            if (criteria.getCity() != null && !criteria.getCity().isBlank()) {

                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(venueJoin.get("city")),
                                criteria.getCity().trim().toLowerCase(Locale.ROOT)
                        )
                );
            }

            // =================================================
            // START FROM
            // =================================================

            if (criteria.getStartFrom() != null) {

                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), criteria.getStartFrom())
                );
            }

            // =================================================
            // START TO
            // =================================================

            if (criteria.getStartTo() != null) {

                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("startTime"), criteria.getStartTo())
                );
            }

            // =================================================
            // FINAL WHERE
            // =================================================

            return criteriaBuilder.and(predicates.toArray(new Predicate[0])
            );
        };
    }
}