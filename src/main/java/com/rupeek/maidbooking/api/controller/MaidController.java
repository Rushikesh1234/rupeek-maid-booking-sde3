package com.rupeek.maidbooking.api.controller;

import com.rupeek.maidbooking.api.dto.ApiDtos.AvailabilityWindowDto;
import com.rupeek.maidbooking.api.dto.ApiDtos.CreateMaidRequest;
import com.rupeek.maidbooking.api.dto.ApiDtos.MaidResponse;
import com.rupeek.maidbooking.api.dto.ApiDtos.MaidSearchRequest;
import com.rupeek.maidbooking.application.service.MaidDiscoveryAndOnboardingService;
import com.rupeek.maidbooking.domain.model.AvailabilityWindow;
import com.rupeek.maidbooking.domain.model.Location;
import com.rupeek.maidbooking.domain.model.Maid;
import com.rupeek.maidbooking.domain.model.TimeSlot;
import com.rupeek.maidbooking.domain.specification.MaidSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/maids")
@Tag(name = "1. Maids & Discovery", description = "Onboard maids and search available maids by criteria")
public class MaidController {

    private final MaidDiscoveryAndOnboardingService maidService;

    public MaidController(MaidDiscoveryAndOnboardingService maidService) {
        this.maidService = maidService;
    }

    @PostMapping
    @Operation(summary = "Onboard a new maid with skills, pricing, and weekly availability windows")
    public ResponseEntity<MaidResponse> onboardMaid(@Valid @RequestBody CreateMaidRequest request) {
        List<AvailabilityWindow> windows = request.availabilityWindows().stream()
                .map(w -> new AvailabilityWindow(w.dayOfWeek(), w.startTime(), w.endTime()))
                .toList();

        Maid maid = new Maid(
                UUID.randomUUID(),
                request.name(),
                request.gender(),
                new Location(request.locality(), request.city(), request.pincode(), request.latitude(), request.longitude()),
                request.skills(),
                request.baseHourlyRate(),
                request.rating(),
                windows,
                true);

        Maid saved = maidService.onboardMaid(maid);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping("/{maidId}")
    @Operation(summary = "Fetch maid profile by ID")
    public ResponseEntity<MaidResponse> getMaid(@PathVariable UUID maidId) {
        return ResponseEntity.ok(toResponse(maidService.getMaidById(maidId)));
    }

    @PostMapping("/search")
    @Operation(summary = "Discover maids matching filters and verified free for the requested time slot")
    public ResponseEntity<List<MaidResponse>> searchMaids(@Valid @RequestBody MaidSearchRequest request) {
        MaidSearchCriteria criteria = new MaidSearchCriteria(
                request.locality(),
                request.requiredSkill(),
                new TimeSlot(request.startTime(), request.endTime()),
                request.minRating(),
                request.maxHourlyRate(),
                request.genderPreference());

        List<MaidResponse> results = maidService.searchAvailableMaids(criteria).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(results);
    }

    private MaidResponse toResponse(Maid maid) {
        List<AvailabilityWindowDto> windows = maid.getAvailabilityWindows().stream()
                .map(w -> new AvailabilityWindowDto(w.getDayOfWeek(), w.getStartTime(), w.getEndTime()))
                .toList();
        return new MaidResponse(
                maid.getId(),
                maid.getName(),
                maid.getGender(),
                maid.getLocation().getLocality(),
                maid.getLocation().getCity(),
                maid.getLocation().getPincode(),
                maid.getSkills(),
                maid.getBaseHourlyRate(),
                maid.getRating(),
                windows,
                maid.isActive());
    }
}
