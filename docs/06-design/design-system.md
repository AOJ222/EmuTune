# Design System

The creative direction is **precision instrumentation + optical glass + restrained technical futurism**: extremely precise, tactile, fast, calm, sophisticated, technically powerful.

Material 3 is the behavioural/accessibility substrate — it is **not** the visual identity. The system defines tokens centrally (colour, typography, spacing, radius, elevation, motion, focus) so screens never scatter arbitrary `18.dp` or `Color(0x…)`.

## Primitives

- `OpticSurface` — the layered glass surface (subtle vertical gradient + hairline border, no heavy blur/glow).
- `Metric` — a measured value + label, monospace for data.
- `StatusBadge` — pill-shaped status chip.
- `ConfidenceIndicator` — four-segment confidence readout.

## Presentation mapping

`StatusPresentation` is the single mapping from domain status/confidence to label, colour and icon. Screens never invent status strings.

## Rules

- "Premium" means coherent detail, not decoration — no overused blur, glow or gradients.
- Use visual spectacle only where it improves comprehension.
