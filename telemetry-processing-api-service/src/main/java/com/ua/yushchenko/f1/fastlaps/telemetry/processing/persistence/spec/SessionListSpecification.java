package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.spec;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for session list filter and sort.
 * Supports filters: sessionType, trackId, search (display name + type + track), state, date range.
 * Sort: startedAt, finishingPosition (join), bestLap (join session_summary).
 * Plan: block-b-session-list-filters.md Step 7.5.
 */
public final class SessionListSpecification {

    private SessionListSpecification() {
    }

    /**
     * Resolved filter: explicit session type/track (AND) + search clause (display name OR type OR track, OR-combined).
     */
    public static class Resolved {
        /** Explicit filter from dropdown. Null = all. */
        public final Short explicitSessionType;
        /** Explicit filter from dropdown. Null = all. */
        public final Integer explicitTrackId;
        /** Search: display name LIKE pattern (for OR with type/track). */
        public final String searchDisplayNameLike;
        /** Search: session type codes whose display name matches search (OR). */
        public final List<Short> searchSessionTypeCodes;
        /** Search: track IDs whose display name matches search (OR). */
        public final List<Integer> searchTrackIds;
        public final String state; // ACTIVE | FINISHED | null
        public final LocalDate dateFrom;
        public final LocalDate dateTo;
        public final String sort;

        public Resolved(Short explicitSessionType, Integer explicitTrackId,
                        String searchDisplayNameLike, List<Short> searchSessionTypeCodes, List<Integer> searchTrackIds,
                        String state, LocalDate dateFrom, LocalDate dateTo, String sort) {
            this.explicitSessionType = explicitSessionType;
            this.explicitTrackId = explicitTrackId;
            this.searchDisplayNameLike = searchDisplayNameLike;
            this.searchSessionTypeCodes = searchSessionTypeCodes != null ? searchSessionTypeCodes : List.of();
            this.searchTrackIds = searchTrackIds != null ? searchTrackIds : List.of();
            this.state = state;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
            this.sort = sort == null || sort.isBlank() ? "startedAt_desc" : sort;
        }
    }

    public static Specification<Session> withFilters(Resolved resolved) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Explicit session type (from dropdown)
            if (resolved.explicitSessionType != null) {
                predicates.add(cb.equal(root.get("sessionType"), resolved.explicitSessionType));
            }

            // Explicit track (from dropdown)
            if (resolved.explicitTrackId != null) {
                predicates.add(cb.equal(root.get("trackId"), resolved.explicitTrackId));
            }

            // Search: (display name LIKE OR session type IN codes OR track ID IN ids)
            boolean hasSearch = (resolved.searchDisplayNameLike != null && !resolved.searchDisplayNameLike.isEmpty())
                    || !resolved.searchSessionTypeCodes.isEmpty()
                    || !resolved.searchTrackIds.isEmpty();
            if (hasSearch) {
                List<Predicate> searchOr = new ArrayList<>();
                if (resolved.searchDisplayNameLike != null && !resolved.searchDisplayNameLike.isEmpty()) {
                    searchOr.add(cb.like(cb.lower(root.get("sessionDisplayName")),
                            "%" + resolved.searchDisplayNameLike.toLowerCase() + "%"));
                }
                if (!resolved.searchSessionTypeCodes.isEmpty()) {
                    searchOr.add(root.get("sessionType").in(resolved.searchSessionTypeCodes));
                }
                if (!resolved.searchTrackIds.isEmpty()) {
                    searchOr.add(root.get("trackId").in(resolved.searchTrackIds));
                }
                if (!searchOr.isEmpty()) {
                    predicates.add(cb.or(searchOr.toArray(new Predicate[0])));
                }
            }

            // State: ACTIVE = endedAt null, FINISHED = endedAt non-null
            if ("ACTIVE".equalsIgnoreCase(resolved.state)) {
                predicates.add(cb.isNull(root.get("endedAt")));
            } else if ("FINISHED".equalsIgnoreCase(resolved.state)) {
                predicates.add(cb.isNotNull(root.get("endedAt")));
            }

            // Date range: startedAt in [dateFrom, dateTo]; and (endedAt in [dateFrom, dateTo] or endedAt null)
            if (resolved.dateFrom != null) {
                Instant fromStart = resolved.dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), fromStart));
            }
            if (resolved.dateTo != null) {
                Instant toEnd = resolved.dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                predicates.add(cb.lessThan(root.get("startedAt"), toEnd));
            }
            if (resolved.dateFrom != null && resolved.dateTo != null) {
                Instant fromStart = resolved.dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant toEnd = resolved.dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                Predicate endedInRange = cb.and(
                        cb.greaterThanOrEqualTo(root.get("endedAt"), fromStart),
                        cb.lessThan(root.get("endedAt"), toEnd));
                predicates.add(cb.or(endedInRange, cb.isNull(root.get("endedAt"))));
            }

            Predicate and = cb.and(predicates.toArray(new Predicate[0]));

            // Order
            String sort = resolved.sort;
            if (sort != null && !sort.equals("startedAt_desc")) {
                if ("startedAt_asc".equals(sort)) {
                    query.orderBy(cb.asc(root.get("startedAt")));
                } else if ("finishingPosition_asc".equals(sort)) {
                    Join<Session, SessionFinishingPosition> fp = root.join("finishingPositions", JoinType.LEFT);
                    fp.on(cb.equal(fp.get("carIndex"), root.get("playerCarIndex")));
                    query.orderBy(cb.asc(fp.get("finishingPosition")), cb.desc(root.get("startedAt")));
                } else if ("bestLap_asc".equals(sort)) {
                    Join<Session, SessionSummary> sum = root.join("summaries", JoinType.LEFT);
                    sum.on(cb.equal(sum.get("carIndex"), root.get("playerCarIndex")));
                    query.orderBy(cb.asc(sum.get("bestLapTimeMs")), cb.desc(root.get("startedAt")));
                } else if ("bestLap_desc".equals(sort)) {
                    Join<Session, SessionSummary> sum = root.join("summaries", JoinType.LEFT);
                    sum.on(cb.equal(sum.get("carIndex"), root.get("playerCarIndex")));
                    query.orderBy(cb.desc(sum.get("bestLapTimeMs")), cb.desc(root.get("startedAt")));
                } else {
                    query.orderBy(cb.desc(root.get("startedAt")));
                }
            } else {
                query.orderBy(cb.desc(root.get("startedAt")));
            }

            return and;
        };
    }
}
