# Interaction Model

## Default UX

The normal mode **makes the decision**: a game page shows the best verified route, its FPS and 1% low, a confidence level, the improvement over the current route, and an OPTIMISE action. The reasoning stays underneath.

## Expert UX

The expert mode exposes the machinery without random internals: candidates considered, how many were rejected, incompatible, benchmarked; a comparison table (average, 1% low, temperature); the selected route; and a "why" in structured reasons (`PERFORMANCE_GAIN`, `BETTER_FRAME_PACING`, `LOWER_THERMALS`, `HIGHER_VISUAL_QUALITY`, `STRONGER_EVIDENCE`, `BUILD_REGRESSION`).

Expert view increases trust; it does not dump implementation details.

## Controller-first

The entire primary flow is operable with touch, D-pad, controller analog focus navigation, and keyboard. Focus order is predictable, focus states are visible, targets are large enough, and there are no touch-only hidden interactions (see [Controller navigation](../06-design/controller-navigation.md)).
