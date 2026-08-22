# Design Tokens

Central tokens in `:core:designsystem`.

## Colour

A dark, instrument-like base with one restrained teal accent and a cooler ice-blue for data.

- Background `#08090C`, Surface `#0F1218`, SurfaceElevated `#171B24`
- Text: `#F2F4F8` / `#9AA3B2` / `#5E6673`
- Accent teal `#4CC9B0`, Ice `#6EA8FE`
- Semantic: Success `#4ADE80`, Warning `#FACC15`, Danger `#F87171`, Neutral `#9AA3B2`

## Typography

Strong hierarchy with a monospace face for raw numbers so measurements read as data, not prose.

## Spacing / radius / elevation

- Spacing: XS 4 · S 8 · M 12 · L 16 · XL 24 · XXL 32
- Radius: S 10 · M 16 · L 24 · XL 32
- Elevation: Raised 2 · Card 4 · Overlay 8

## Motion

A single easing curve (`FastOutSlowInEasing`) and durations Quick 120 / Standard 220 / Emphasis 340 ms.

## Status colours (central)

`OPTIMAL` → success; `IMPROVEMENT_AVAILABLE`/`BETTER_ROUTE_AVAILABLE` → accent; `UPDATE_RECOMMENDED`/`HARDWARE_LIMITED` → warning; `REGRESSION_DETECTED` → danger; unverified/insufficient/unsupported → neutral.
