# AasPas Project Backlog

## P0

- CURSOR-038 Customer Notification physical E2E with 3rd test phone (POCO customer + Samsung admin + real-time badge/banner/navigation)

## P1

- Nearby offer broadcast notifications
- Expiring-soon offer scheduler / background job
- Offer/customer notification improvements (merge duplicate shop+offer alerts, preferences)
- Shop suspend / deactivate lifecycle (not in current backend — requires product decision before implementation)
- FCM / background push notifications
- Favorites: show inactive/expired favorited offers with clear status (currently only active favorites returned by API)

## P2

- Admin reports and analytics dashboard
- Admin user detail screen and account status controls
- Email/SMS notifications
- HTTPS / production deployment hardening
- Payment / subscription features

## Completed / In Progress Notes

- CURSOR-039: Admin Shop Management + User Management foundation (list/search/filter/detail read-only)
- CURSOR-040: Customer discovery UX polish (screen-specific errors, location consistency, shared photo loading)
- CURSOR-041B: Beta deployment package (Fly.io config, isolated beta env, Android BETA buildType) — cloud deploy pending credit-card approval
- CURSOR-041D-PREP: Render + Neon S3 media backend (`MEDIA_STORAGE_BACKEND=s3`), `deploy/beta/render.yaml`, docs/BETA_DEPLOYMENT_RENDER.md
- Shop verification and offer verification remain the authoritative approval flows
