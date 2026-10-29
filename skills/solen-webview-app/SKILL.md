---
name: solen-webview-app
description: Blueprint for building or hardening a SolenLabs AI app (single-file PWA + Supabase backend + Android WebView wrapper with Play Billing). Use when starting a new app idea, adding AI features, paywalls, trials, promo codes or subscriptions, or auditing an existing app's security.
---

# SolenLabs AI app blueprint (PWA + Supabase + Play)

Stack defaults: single-file mobile-first PWA hosted on Hostinger (`solen-site-deploy`), **Supabase** backend, Android WebView wrapper (`solen-android-release`), Play Console (`solen-play-console`). Keep Cameron's voice in copy (warm, direct, not corporate). Ask before changing a visual direction.

## Non-negotiable security (learned from Drava)
- **Never send prompts, model or max_tokens from the client.** One Edge Function (`<app>-ai`) owns the system prompts, model, token limits and per-tool input validation. The page sends `{device_id, tool, fields}` only. An open proxy lets anyone use the Anthropic key.
- **Access is server-side.** Tables: `<app>_devices` (device_id, trial_started_at), `<app>_entitlements` (source play|stripe|code, product_id, purchase_ref, expires_at), `<app>_promo_codes`, `<app>_usage` (daily counter). RLS on, no policies; only Edge Functions (service role) touch them.
- **No unlock codes or secrets in page source** (base64 is not hiding).
- **Rate limit** per device and per IP with an atomic `bump_usage(key, limit)` SQL function.
- **Escape all AI output** before `innerHTML` (an `esc()` helper; a tiny `md()` for **bold**).
- Device id: Android = SHA-256 of ANDROID_ID via the JS bridge; web = random UUID in localStorage.

## Billing
- Android: `BillingBridge` exposed as `window.<App>Android` (purchase(productId, basePlanId), restorePurchases(), acknowledge(token), getDeviceId(), copy(text)). It calls back `window.on<App>Purchase({token, productId})`. The page verifies with `<app>-account` `verify_play`, then acknowledges. Restore on every launch.
- Web: Stripe Payment Links with `?client_reference_id=<device>` and an after-payment redirect to `https://<domain>/?checkout={CHECKOUT_SESSION_ID}`, verified by `verify_stripe` using a **restricted** key (Checkout Sessions + Subscriptions: Read) in secret `STRIPE_SECRET_KEY`.
- `body.in-app` class → `.only-app` / `.only-web` CSS to swap paywall UI.

## AI model
Use Cameron's chosen model per app (Drava: `claude-haiku-4-5-20251001`; don't change without asking). Stream chat replies (pass the SSE through). Allow chat screenshots (downscale client-side to ≤1600px JPEG).

## UX patterns that worked
5-tab bottom nav max (rest under "More"); copy-ready reply cards for quoted suggestions; persisted chat history; min text 11px; contrast ≥4.5:1; no redirect to a separate thank-you page (keep users in-app).

## Testing without the live backend
Playwright (preinstalled in cloud sessions) with `page.route` mocks for the functions, plus a stubbed `window.<App>Android` via `addInitScript` to cover app mode. Test Edge Functions from inside Postgres with `pg_net` (`net.http_post`, then read `net._http_response`) when the session's network blocks supabase.co.

## Reference implementation
`solenlabsllc-ui/claude` → `drava/` (`drava.html`, `supabase/functions/drava-ai`, `drava-account`, `_shared`, `android/`).
