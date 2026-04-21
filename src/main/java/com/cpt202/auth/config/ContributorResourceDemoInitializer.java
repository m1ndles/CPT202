package com.cpt202.auth.config;

import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.repository.AdminArchiveRepository;
import com.cpt202.auth.repository.ResourceAppealMessageRepository;
import com.cpt202.auth.repository.ResourceRepository;
import com.cpt202.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds contributor-owned demo resources for key workflow states.
 */
@Component
@Order(3)
public class ContributorResourceDemoInitializer implements CommandLineRunner {

    /**
     * Default contributor account used for the seeded resources.
     */
    private static final String DEFAULT_CONTRIBUTOR_EMAIL = "contributor@heritagehub.com";

    /**
     * Repository used to load the contributor account.
     */
    private final UserRepository userRepository;

    /**
     * Repository used to create the demo resources.
     */
    private final ResourceRepository resourceRepository;

    /**
     * Repository used to seed archive feedback for rejected resources.
     */
    private final AdminArchiveRepository adminArchiveRepository;

    /**
     * Repository used to seed the appeal conversation thread.
     */
    private final ResourceAppealMessageRepository resourceAppealMessageRepository;

    public ContributorResourceDemoInitializer(UserRepository userRepository,
                                              ResourceRepository resourceRepository,
                                              AdminArchiveRepository adminArchiveRepository,
                                              ResourceAppealMessageRepository resourceAppealMessageRepository) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.adminArchiveRepository = adminArchiveRepository;
        this.resourceAppealMessageRepository = resourceAppealMessageRepository;
    }

    /**
     * Seeds contributor demo resources after the demo users exist.
     */
    @Override
    public void run(String... args) {
        userRepository.findByEmail(DEFAULT_CONTRIBUTOR_EMAIL.toLowerCase(Locale.ROOT))
                .filter(user -> user.role().canUpload())
                .ifPresent(this::seedMissingStatuses);
    }

    /**
     * Ensures the contributor has one resource for each major moderation state.
     */
    private void seedMissingStatuses(UserAccount contributor) {
        ensureResource(
                contributor,
                "DRAFT",
                "Suzhou Silk Loom Notes",
                "Working draft for a resource that documents the restoration workflow of a traditional silk weaving loom.",
                "Traditional Craft",
                "Suzhou",
                "Late Qing Dynasty",
                "silk weaving,loom,workshop",
                "https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?auto=format&fit=crop&w=1200&q=80",
                null,
                0,
                LocalDateTime.now().minusDays(1)
        );

        ensureResource(
                contributor,
                "PENDING",
                "Qinhuai Lantern Workshop Archive",
                "A contributor submission about lantern-making records and oral histories collected from Qinhuai artisans.",
                "Folk Custom",
                "Nanjing",
                "Republic Era",
                "lantern,festival,oral history",
                "https://images.unsplash.com/photo-1513151233558-d860c5398176?auto=format&fit=crop&w=1200&q=80",
                trackingIdFor(contributor.id(), "PENDING"),
                0,
                LocalDateTime.now().minusDays(3)
        );

        ensureResource(
                contributor,
                "APPROVED",
                "Yangzhou Canal Storytelling Stage",
                "Published contributor resource describing the canal-side performance stage and its role in local narrative traditions.",
                "Performing Heritage",
                "Yangzhou",
                "Ming Dynasty",
                "storytelling,canal,culture",
                "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80",
                trackingIdFor(contributor.id(), "APPROVED"),
                182,
                LocalDateTime.now().minusDays(8)
        );

        ensureResource(
                contributor,
                "REJECTED",
                "Yixing Kiln Workshop Ledger",
                "Returned submission containing kiln workshop notes that still need clearer provenance and attachment cleanup before review can continue.",
                "Industrial Heritage",
                "Yixing",
                "Early 20th Century",
                "kiln,ceramics,ledger",
                "https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80",
                trackingIdFor(contributor.id(), "REJECTED"),
                0,
                LocalDateTime.now().minusDays(5)
        );

        seedRejectedRevisionContext(contributor);
    }

    /**
     * Creates a demo resource for the given status when none exists yet.
     */
    private void ensureResource(UserAccount contributor,
                                String status,
                                String title,
                                String description,
                                String category,
                                String place,
                                String period,
                                String tags,
                                String thumbnail,
                                String trackingId,
                                int viewCount,
                                LocalDateTime createdAt) {
        List<HeritageResource> existing = resourceRepository.findByOwner(contributor.id(), status);
        if (!existing.isEmpty()) {
            return;
        }

        HeritageResource resource = new HeritageResource(
                null,
                title,
                null,
                category,
                period,
                place,
                description,
                thumbnail,
                "Demo contributor dataset",
                trackingId,
                status,
                viewCount,
                createdAt,
                contributor.id(),
                contributor.username()
        );

        HeritageResource saved = resourceRepository.insert(resource);
        resourceRepository.replaceTags(saved.id(), splitTags(tags));
    }

    /**
     * Builds a stable tracking id for seeded resources.
     */
    private String trackingIdFor(Long contributorId, String status) {
        String suffix = switch (status) {
            case "PENDING" -> "PD";
            case "APPROVED" -> "AP";
            case "REJECTED" -> "RJ";
            default -> "DF";
        };
        return "TRK-DEMO-" + contributorId + "-" + suffix;
    }

    /**
     * Splits the demo tag string into distinct tag values.
     */
    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    /**
     * Adds archive feedback and a starter appeal thread for rejected resources.
     */
    private void seedRejectedRevisionContext(UserAccount contributor) {
        resourceRepository.findByOwner(contributor.id(), "REJECTED").forEach(resource -> {
            adminArchiveRepository.upsert(
                    resource.id(),
                    contributor.username(),
                    "Admin Console",
                    "Please clarify the provenance of this material, tidy the supporting files, and resubmit with cleaner evidence.",
                    "Submission returned for contributor revision.",
                    "Tracking ID: " + (resource.trackingId() == null ? "N/A" : resource.trackingId()),
                    LocalDateTime.now().minusDays(2)
            );

            if (resourceAppealMessageRepository.findByResourceId(resource.id()).isEmpty()) {
                resourceAppealMessageRepository.insert(
                        resource.id(),
                        "ADMIN",
                        "Review Admin",
                        "Use this thread if you need to clarify the rejection before revising the submission.",
                        LocalDateTime.now().minusDays(2).plusHours(1)
                );
            }
        });
    }
}
