package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.chinesereads.backend.Model.HallOfFameBadges;
import com.chinesereads.backend.Model.HallOfFameEntry;
import com.chinesereads.backend.Repository.HallOfFameEntryRepository;
import com.chinesereads.backend.Repository.HallOfFameSocialRepository;
import com.chinesereads.backend.Service.HallOfFameService;
import com.chinesereads.backend.dto.HallOfFameEntryDTO;
import com.chinesereads.backend.dto.HallOfFameMapper;

/**
 * Hall of Fame pure logic: slug derivation/uniqueness, partial updates
 * (null = unchanged) and the badge whitelist. Persistence is mocked — the
 * saved entity is inspected through a captor.
 */
public class HallOfFameServiceTest {

    private HallOfFameEntryRepository entryRepository;
    private HallOfFameSocialRepository socialRepository;
    private HallOfFameService hallOfFameService;

    @BeforeEach
    public void setUp() {
        entryRepository = mock(HallOfFameEntryRepository.class);
        socialRepository = mock(HallOfFameSocialRepository.class);
        hallOfFameService = new HallOfFameService(entryRepository, socialRepository,
                mock(HallOfFameMapper.class));
        when(entryRepository.save(any(HallOfFameEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private HallOfFameEntryDTO dto(String name, String slug, String tagline, List<String> badges) {
        return new HallOfFameEntryDTO(null, name, slug, tagline, null, null, null,
                null, null, badges, null);
    }

    @Test
    @DisplayName("create derives a clean slug from a name with diacritics and spaces")
    public void createDerivesSlug() {
        when(entryRepository.count()).thenReturn(0L);
        when(entryRepository.existsBySlug("maria-lopez")).thenReturn(false);

        hallOfFameService.create(dto("María López", null, null, null));

        ArgumentCaptor<HallOfFameEntry> captor = ArgumentCaptor.forClass(HallOfFameEntry.class);
        org.mockito.Mockito.verify(entryRepository).save(captor.capture());
        assertEquals("maria-lopez", captor.getValue().getSlug());
    }

    @Test
    @DisplayName("create auto-suffixes the derived slug when it is already taken")
    public void createAutoSuffixesSlug() {
        when(entryRepository.count()).thenReturn(1L);
        when(entryRepository.existsBySlug("maria")).thenReturn(true);
        when(entryRepository.existsBySlug("maria-2")).thenReturn(false);

        hallOfFameService.create(dto("María", null, null, null));

        ArgumentCaptor<HallOfFameEntry> captor = ArgumentCaptor.forClass(HallOfFameEntry.class);
        org.mockito.Mockito.verify(entryRepository).save(captor.capture());
        assertEquals("maria-2", captor.getValue().getSlug());
        assertEquals(1, captor.getValue().getDisplayOrder());
    }

    @Test
    @DisplayName("create rejects an explicit slug that is already taken")
    public void createRejectsExplicitDuplicateSlug() {
        when(entryRepository.existsBySlug("maria")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> hallOfFameService.create(dto("Otra", "maria", null, null)));
    }

    @Test
    @DisplayName("create rejects a blank name")
    public void createRejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> hallOfFameService.create(dto("  ", null, null, null)));
    }

    @Test
    @DisplayName("update only overwrites non-null fields; empty badge list clears badges")
    public void updateIsPartial() {
        HallOfFameEntry entry = new HallOfFameEntry();
        entry.setName("Maria");
        entry.setSlug("maria");
        entry.setTagline("Old tagline");
        entry.setBioEn("Hello");
        entry.setDisplayOrder(3);
        entry.setBadges(new java.util.LinkedHashSet<>(List.of("star")));
        when(entryRepository.findById(1L)).thenReturn(Optional.of(entry));

        hallOfFameService.update(1L, dto(null, null, "New tagline", null));
        assertEquals("Maria", entry.getName());
        assertEquals("New tagline", entry.getTagline());
        assertEquals("Hello", entry.getBioEn());
        assertEquals(3, entry.getDisplayOrder());
        assertEquals(Set.of("star"), entry.getBadges());

        hallOfFameService.update(1L, dto(null, null, null, List.of()));
        assertTrue(entry.getBadges().isEmpty());
    }

    @Test
    @DisplayName("normalize de-duplicates and re-orders badges canonically; unknown key throws")
    public void badgesNormalize() {
        assertEquals(List.of("pioneer", "star"),
                List.copyOf(HallOfFameBadges.normalize(List.of("star", "pioneer", "star"))));
        assertThrows(IllegalArgumentException.class,
                () -> HallOfFameBadges.normalize(List.of("legend")));
    }
}
