# CREST brand guide

## Identity

**Name:** CREST, always capitalized. Use “CREST” in navigation, app listings, and the wordmark. Do not switch between “Crest,” “CRESTV2,” and “CREST App” in product copy.

**Existing full name:** Compiled Research as an Educational Application for Students and Teachers. Retain this expansion in About, project documentation, and formal school materials. It is too long to repeat in everyday interface headings.

**Product descriptor:** School research library.

**Suggested tagline:** School research, shared.

**One-sentence introduction:** CREST helps students and teachers find, submit, review, and preserve school research.

**School attribution:** Bonifacio D. Borebor Sr. High School. Show this in About and appropriate sign-in/footer contexts. The tagline and refreshed identity are proposals; they do not imply a new school seal or institutional endorsement.

## Character

- **Clear:** papers, authors, submission status, and next actions get attention first.
- **Credible:** careful typography, accurate labels, and predictable interactions.
- **Welcoming:** plain language suitable for students and teachers.
- **Connected:** the same visual vocabulary across web and Android, with layouts suited to each device.

The interface should feel like an organized library. Favor quiet surfaces, readable titles, and useful structure. Keep decorative effects secondary to research content.

## Color direction

| Name | Value | Role |
| --- | --- | --- |
| CREST blue | `#2457D6` | Light-theme primary actions, selection, and brand emphasis |
| Ink navy | `#14233B` | Light-theme headings and primary text; wordmark on pale surfaces |
| Research teal | `#0E7075` | Secondary actions and supporting identity accents |
| Library mist | `#F5F7FB` | Light page background |
| Paper white | `#FFFFFF` | Light content surfaces and text on primary buttons |

Use semantic theme roles from the token file in actual components. For example, dark mode uses a lighter blue for `primary` so labels remain readable. Brand swatches identify the identity; they are not a substitute for theme-aware colors.

Rules:

- Use blue for the principal action and active destination. Use teal sparingly for secondary emphasis.
- Use ink/neutral text for most headings and body copy. A page does not need every section title colored blue.
- Keep backgrounds neutral. Avoid full-screen saturated gradients, repeated circles, and heavy shadows.
- Use status-specific paired text/background tokens. Never use strand colors to mean approval or failure.
- Never communicate a status with color alone. Pair it with a label and, where helpful, an icon.
- Preserve the same semantic meanings in both themes and platforms.

## Typography: Inter

Use **Inter** throughout the interface and for the proposed text wordmark. It gives the app a clear, contemporary identity while keeping long research titles, metadata, and form labels legible.

Use four bundled upright weights: Regular 400, Medium 500, Semibold 600, and Bold 700. Web uses the bundled variable WOFF2; Android uses static TTF files so the family can work on the project's API 24 minimum. Do not use synthetic italics as a substitute for an italic font. The papers inside the PDF reader keep their original typography.

| Role | Starting size / line height | Weight | Usage |
| --- | --- | --- | --- |
| Page heading (`headlineLarge`) | 28 / 36 | 700 | Main screen title |
| Section heading (`titleLarge`) | 20 / 28 | 600 | Major content section |
| Research title (`titleMedium`) | 16 / 24 | 600 | List item title |
| Body (`bodyLarge`) | 16 / 24 | 400 | Descriptions, forms, reading interface |
| Supporting copy (`bodyMedium`) | 14 / 20 | 400 | Authors, dates, help text |
| Action label (`labelLarge`) | 14 / 20 | 600 | Buttons and filters |
| Small label (`labelMedium`) | 12 / 16 | 500 | Short nonessential metadata |

All fifteen Material typography roles are defined in the token file for complete platform mapping. Use the roles above for normal product screens; reserve display sizes for marketing/introductory content. Avoid text smaller than 12 for essential information. Respect system font scaling; use rem on web and sp on Android.

For the text wordmark, use Inter Bold with modest tracking (`0.06em`). Keep it as “CREST,” not five disconnected initials. This is a typographic treatment, not final master logo artwork.

Font source: [Inter official release](https://github.com/rsms/inter/releases/tag/v4.1). License and source hashes are kept beside the font files. Retain the bundled license when redistributing them.

## Logo direction

The existing app icon combines a document, magnifying glass, and a detailed connected outer ring. Keep the document/search idea as continuity with the current identity. A proposed refresh should simplify the outer ring and reduce fine details so the mark is recognizable at small sizes.

Prepare these deliverables in a future logo pass:

1. A simple document/search symbol with strong silhouette and even stroke weight.
2. A horizontal symbol-plus-CREST lockup, using the Inter wordmark treatment.
3. One-color ink and reversed white versions.
4. A favicon/small-symbol export verified at 16px, 24px, and 32px.
5. Android adaptive foreground/background layers and monochrome artwork, verified under launcher masks.

This kit does not replace or redraw the current logo. The preview labels its existing icon as a legacy reference. Do not treat the image as an approved simplified mark.

For the proposed lockup, leave clear space of at least half the symbol's height on all sides. Start with a minimum 24px/dp symbol and a 112px/dp horizontal lockup, then validate with final artwork. Do not stretch, rotate, add glow/shadow, recolor arbitrarily, or put the logo over busy content. Use a text wordmark when a symbol is too detailed at its intended size.

Keep school seals separate from the product mark. Use only approved school artwork when attribution requires it.

## Shape, rhythm, and imagery

- Spacing follows 4, 8, 12, 16, 24, 32, 48, and 64 units.
- Use 16-unit compact-screen gutters and 24-unit section gaps.
- Inputs and buttons use a 12-unit corner radius; cards use 16; sheets use 24. Use pills for compact statuses and navigation indicators, not every container.
- Prefer borders and surface separation over shadows. Reserve visible elevation for overlays.
- Use simple, consistent icons. Filled icons can identify selected destinations; outlined icons work for inactive destinations.
- Use school/research imagery only when it supports the content. Avoid stock images that imply an unrelated university or institution.
- Use brief functional transitions. Honor reduced motion on web and accessibility animation preferences on Android; the logo should not pulse indefinitely.

## Voice and vocabulary

Use sentence case, concise labels, and direct verbs. State what happened and the next useful action. Keep tone calm during errors and approvals.

| Context | Preferred copy |
| --- | --- |
| Main navigation | Home · Research · Documents · Groups |
| Research collection heading | Research papers |
| Read action | Read paper |
| Submission action | Submit research |
| Unreviewed submission | Pending review |
| Requested changes | Changes requested |
| Accepted submission | Approved |
| Empty search | No papers match your search. Try another title or clear the filters. |
| Upload failure | Your file could not be uploaded. Check your connection and try again. |
| Upload completion | Research submitted for review. |
| Offline notice | You're offline. Saved papers are available. |

“Upload” refers to transferring a file; “Submit” refers to sending research for review. Keep backend status values unchanged and map them to accurate display labels. “Documents” means school resources/templates; “Research” means research papers. Spell out qualitative and quantitative rather than using QL/QN. Do not label a download count as views unless it actually measures viewing.

Avoid grand claims such as “academic excellence,” ambiguous “Success!” messages, and technical exception details in the interface. Localized versions should preserve meaning rather than translate labels word for word.
