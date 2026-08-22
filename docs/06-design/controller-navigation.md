# Controller Navigation

Controller interaction is first-class. The primary application is operable with touch, D-pad, controller/analog focus navigation, and keyboard.

## Principles

- Correct, predictable focus order.
- Visible focus states (a focus ring, not just selection colour).
- Predictable directional navigation.
- Large enough targets.
- No touch-only hidden interactions.

## Implementation

- Tab items in `MainScreen` use `Modifier.focusRequester` + `onFocusChanged` to draw a visible accent border when focused, and are `clickable` (focusable + activatable via D-pad).
- `IconButton` (back) and list items are natively focusable.
- A user holding a handheld should not need to touch the screen for ordinary navigation.

## Adaptive layout

A phone shows Library → Game → Recommendation as a stack; a landscape handheld/tablet can sensibly show game list | detail | evidence side by side where space permits. Milestone 1 uses a single-column stack with the adaptive substrate in place (Material 3 Adaptive dependency) for the future split layout.
