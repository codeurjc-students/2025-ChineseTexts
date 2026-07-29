/**
 * Closed catalog of Hall of Fame recognition badges, in display order.
 * Keys are the stable identifiers stored in the database; the visible labels
 * and tooltips live in the i18n dictionaries under hofBadges.*. Keep in sync
 * with HallOfFameBadges.ALLOWED in
 * backend/src/main/java/com/chinesereads/backend/Model/HallOfFameBadges.java.
 */
export interface HallOfFameBadge {
  /** Stable key stored in the database and used to build i18n keys. */
  key: string;
  /** Bootstrap Icons class, e.g. "bi-star-fill". */
  icon: string;
}

export const HALL_OF_FAME_BADGES: readonly HallOfFameBadge[] = [
  { key: 'pioneer', icon: 'bi-rocket-takeoff-fill' },
  { key: 'star', icon: 'bi-star-fill' },
  { key: 'prolific', icon: 'bi-journal-text' },
  { key: 'collaborator', icon: 'bi-people-fill' },
  { key: 'educator', icon: 'bi-mortarboard-fill' },
  { key: 'community', icon: 'bi-heart-fill' }
] as const;
