# Okean Visuals — editions (draft)

Planned editions for Minecraft Fabric 1.21.4. This document is a product specification, not an activated payment or licensing system.

| Edition | Access | Features |
| --- | --- | --- |
| Free | Free forever | HUD, FPS, coordinates, basic Zoom and Fullbright; small optional menu ad |
| Plus Mini | Paid | No ads; limited themes, sky presets, crosshair options |
| Plus Max | Paid | All Plus features; more themes, sky presets, HUD and profiles |
| Pro | Paid | Everything in Plus Max plus custom sky/theme editor, animated GUI and visual effects |

## Draft prices (UAH)

| Term | Plus Mini | Plus Max | Pro |
| --- | ---: | ---: | ---: |
| 1 day | 3 | 5 | — |
| 3 days | 5 | 9 | Free trial (proposed) |
| 7 days | 10 | 19 | 29 |
| 14 days | 18 | 35 | — |
| 30 days | 29 | 59 | 79 |
| 90 days | 79 | 149 | — |
| 365 days | 249 | 399 | 399 |
| Lifetime | 349 | 449 | 599 |

## Implementation notes

- First implement an edition selector/preview with no payment processing.
- Do not trust a client-only edition switch for real paid access; future entitlements need secure server-side validation.
- Never store payment card information in the mod.
- No automatic charge after free trials.
- Keep ads unobtrusive and only in the menu, subject to platform rules.
- Validate build in GitHub Actions before merging.
