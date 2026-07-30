/**
 * Preset catalog for known social networks in the Hall of Fame link editor,
 * in display order. Picking a preset pre-fills the link's icon and label; the
 * custom-link flow (free label + icon + url) remains available alongside.
 *
 * Icons verified against Bootstrap Icons 1.11.3 (the CDN version pinned in
 * src/index.html) — when adding a network, check the icon exists there.
 * Labels are brand names, identical in every language (no i18n needed).
 */
export interface SocialLinkPreset {
  /** Stable identifier of the preset. */
  key: string;
  /** Visible label — a brand name (language-neutral). */
  label: string;
  /** Bootstrap Icons class, e.g. "bi-instagram". */
  icon: string;
  /** Hostnames (without "www.") whose URLs map to this preset. */
  hosts: string[];
}

export const SOCIAL_LINK_PRESETS: readonly SocialLinkPreset[] = [
  { key: 'instagram', label: 'Instagram', icon: 'bi-instagram', hosts: ['instagram.com'] },
  { key: 'tiktok', label: 'TikTok', icon: 'bi-tiktok', hosts: ['tiktok.com'] },
  { key: 'youtube', label: 'YouTube', icon: 'bi-youtube', hosts: ['youtube.com', 'youtu.be'] },
  { key: 'x', label: 'X (Twitter)', icon: 'bi-twitter-x', hosts: ['x.com', 'twitter.com'] },
  { key: 'facebook', label: 'Facebook', icon: 'bi-facebook', hosts: ['facebook.com', 'fb.com'] },
  { key: 'linkedin', label: 'LinkedIn', icon: 'bi-linkedin', hosts: ['linkedin.com'] },
  { key: 'twitch', label: 'Twitch', icon: 'bi-twitch', hosts: ['twitch.tv'] },
  { key: 'discord', label: 'Discord', icon: 'bi-discord', hosts: ['discord.gg', 'discord.com'] },
  { key: 'telegram', label: 'Telegram', icon: 'bi-telegram', hosts: ['t.me', 'telegram.me'] },
  { key: 'whatsapp', label: 'WhatsApp', icon: 'bi-whatsapp', hosts: ['wa.me', 'whatsapp.com'] },
  { key: 'threads', label: 'Threads', icon: 'bi-threads', hosts: ['threads.net', 'threads.com'] },
  { key: 'snapchat', label: 'Snapchat', icon: 'bi-snapchat', hosts: ['snapchat.com'] },
  { key: 'spotify', label: 'Spotify', icon: 'bi-spotify', hosts: ['spotify.com'] },
  { key: 'pinterest', label: 'Pinterest', icon: 'bi-pinterest', hosts: ['pinterest.com', 'pinterest.es'] },
  { key: 'medium', label: 'Medium', icon: 'bi-medium', hosts: ['medium.com'] },
  { key: 'substack', label: 'Substack', icon: 'bi-substack', hosts: ['substack.com'] },
  { key: 'github', label: 'GitHub', icon: 'bi-github', hosts: ['github.com'] },
  { key: 'wechat', label: 'WeChat', icon: 'bi-wechat', hosts: ['weixin.qq.com'] },
  { key: 'weibo', label: 'Weibo', icon: 'bi-sina-weibo', hosts: ['weibo.com'] },
  { key: 'website', label: 'Website', icon: 'bi-globe2', hosts: [] },
  { key: 'email', label: 'Email', icon: 'bi-envelope-fill', hosts: [] }
];

/**
 * Maps a URL to its network preset (null if unknown or unparseable).
 * "mailto:" maps to the email preset; URLs pasted without a scheme are retried
 * with "https://" so "instagram.com/user" still detects.
 */
export function detectSocialPreset(url: string): SocialLinkPreset | null {
  const value = (url || '').trim().toLowerCase();
  if (!value) return null;
  if (value.startsWith('mailto:')) {
    return SOCIAL_LINK_PRESETS.find(p => p.key === 'email') ?? null;
  }
  let hostname: string;
  try {
    hostname = new URL(value.includes('://') ? value : `https://${value}`).hostname;
  } catch {
    return null;
  }
  if (hostname.startsWith('www.')) hostname = hostname.slice(4);
  return SOCIAL_LINK_PRESETS.find(p =>
    p.hosts.some(h => hostname === h || hostname.endsWith('.' + h))
  ) ?? null;
}
