import { SOCIAL_LINK_PRESETS, detectSocialPreset } from './social-link-presets';

describe('SOCIAL_LINK_PRESETS', () => {
  it('has unique keys', () => {
    const keys = SOCIAL_LINK_PRESETS.map(p => p.key);
    expect(new Set(keys).size).toBe(keys.length);
  });

  it('only uses Bootstrap Icons classes', () => {
    for (const p of SOCIAL_LINK_PRESETS) {
      expect(p.icon).toMatch(/^bi-[a-z0-9-]+$/);
    }
  });

  it('has a non-empty brand label per preset', () => {
    for (const p of SOCIAL_LINK_PRESETS) {
      expect(p.label.trim().length).toBeGreaterThan(0);
    }
  });
});

describe('detectSocialPreset', () => {
  it('detects known networks from their hostnames', () => {
    expect(detectSocialPreset('https://instagram.com/chinesereadsapp')?.key).toBe('instagram');
    expect(detectSocialPreset('https://www.instagram.com/chinesereadsapp')?.key).toBe('instagram');
    expect(detectSocialPreset('https://www.tiktok.com/@user')?.key).toBe('tiktok');
    expect(detectSocialPreset('https://youtu.be/abc123')?.key).toBe('youtube');
  });

  it('maps both x.com and twitter.com to the X preset', () => {
    expect(detectSocialPreset('https://x.com/user')?.key).toBe('x');
    expect(detectSocialPreset('https://twitter.com/user')?.key).toBe('x');
  });

  it('detects subdomains of a known host', () => {
    expect(detectSocialPreset('https://open.spotify.com/show/xyz')?.key).toBe('spotify');
  });

  it('maps mailto: to the email preset', () => {
    expect(detectSocialPreset('mailto:hola@chinesereads.com')?.key).toBe('email');
  });

  it('detects URLs pasted without a scheme', () => {
    expect(detectSocialPreset('instagram.com/user')?.key).toBe('instagram');
  });

  it('returns null for unknown or invalid URLs', () => {
    expect(detectSocialPreset('https://example.com/whatever')).toBeNull();
    expect(detectSocialPreset('not a url')).toBeNull();
    expect(detectSocialPreset('')).toBeNull();
  });
});
