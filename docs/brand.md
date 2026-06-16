# Mycellis Brand

**Tagline:** Watch your services breathe.

---

## Voice

Direct, calm, slightly biological. Not cute. Not corporate. Engineers
should feel like the tool is on their side without trying too hard.

- ✅ "Watch your services breathe."
- ✅ "Your stalks are healthy."
- ❌ "We've got you covered!"
- ❌ "Welcome to the family!"

Prefer active voice. Prefer short sentences. Prefer biological metaphors
(breathe, grow, healthy, stressed, dormant) over corporate ones
(monitor, observe, track, analyze).

---

## Colors

### Brand
| Token                    | Hex       | Usage                             |
|--------------------------|-----------|-----------------------------------|
| `--brand-primary`        | `#0d7377` | Main brand, CTA buttons, logo bar |
| `--brand-primary-hover`  | `#0a5d60` | Button hover state                |
| `--brand-accent`         | `#7fffd4` | Bioluminescent mark, highlights   |

### Text
| Token              | Hex       | Usage                       |
|--------------------|-----------|-----------------------------|
| `--text-primary`   | `#0a2e2f` | Headings, important text    |
| `--text-secondary` | `#4a6566` | Body copy                   |
| `--text-muted`     | `#8aa6a7` | Footer, hints, fine print   |

### Backgrounds
| Token           | Hex       | Usage                       |
|-----------------|-----------|-----------------------------|
| `--bg-page`     | `#f1f5f5` | Page background             |
| `--bg-card`     | `#ffffff` | Card backgrounds            |
| `--bg-subtle`   | `#f5faf9` | Subtle background sections  |
| `--border`      | `#d8e8e8` | Dividers, borders           |

### Status (dashboard UI — kept distinct from brand colors)
| Token                | Hex       | Meaning             |
|----------------------|-----------|---------------------|
| `--status-healthy`   | `#22c55e` | Service is healthy  |
| `--status-stressed`  | `#f59e0b` | Service is stressed |
| `--status-down`      | `#ef4444` | Service is down     |
| `--status-dormant`   | `#94a3b8` | Service is dormant  |

---
## Mark

A 14×14px aquamarine circle (`#7fffd4`) with a soft bioluminescent glow,
placed left of the "Mycellis" wordmark **on dark backgrounds only**
(brand bar, dashboard nav, marketing site dark mode).

On light/editorial layouts, omit the mark — the wordmark and accent
line carry the brand on their own. Adding the mark on light backgrounds
weakens both elements.

- Mark color:           `#7fffd4`
- Mark size:            14px diameter (8px when scaled with smaller wordmark)
- Spacing to wordmark:  10px
- Glow:                 `box-shadow: 0 0 8px #7fffd4, 0 0 14px rgba(127,255,212,0.6)`
- Light background:     don't use

In email clients that strip `box-shadow` (notably Outlook desktop), the
mark falls back to a solid aquamarine circle — still on-brand, no
layout breakage.

Use the mark ONLY in the brand bar — one mark per surface. Repeating it
saturates the metaphor.

---

## Typography

Until a custom font is locked, use the OS native stack for consistency
and speed:

`-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`

Wordmark uses `font-weight: 600` and `letter-spacing: -0.3px` for a
slightly tightened, modern feel.

---

## Application

- **Verification email:** brand bar with mark + wordmark on `--brand-primary`,
  CTA button in `--brand-primary`, footer in `--text-muted`.
- **Dashboard UI:** brand color reserved for nav and primary actions.
  Status colors used liberally in service health displays. Never mix
  brand teal with status colors in the same component.
- **Marketing site:** TBD when frontend lands.

---

## What NOT to do

- Don't add illustrations (flowers, mushrooms, fungi) to transactional emails
- Don't use brand teal as a status color
- Don't use status colors (red/amber/green) as brand accents
- Don't add multiple marks per surface
- Don't animate the mark (won't work in email; visual noise in app UI)
- Don't change brand color casually — every change cascades across email
  templates, dashboard, marketing site, and customer trust