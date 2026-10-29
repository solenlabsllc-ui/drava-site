---
name: solen-play-console
description: Do Google Play Console work for SolenLabs apps (upload a release, fix policy issues, create subscriptions/in-app products, testers, data safety, content rating, submit for review), in the order Google allows. Use when Cameron mentions Play Console, Play Store, a rejection, policy issue, testing track, subscriptions or releasing an app.
---

# SolenLabs Play Console

Developer account: **Solen Labs LLC** (signed in as `solenlabsllc@gmail.com`). Apps: Drava (`com.solenlabs.drava`), Bonva (`app.bonva`), Bonva devoted to you (`app.yourdevu`), Fovu Date Mode (`app.fovu`), Sammy (`com.animaltranslator.app`). Bonva/Devu/Novu/Auro are parked unless Cameron brings them up.

## How to operate
- The **cloud Claude can't see Play Console.** Cameron's local Claude can, through the Claude in Chrome extension: start it with `claude --chrome` (or `claude --continue --chrome`); `/chrome` should show Status: Enabled. Chrome must be signed into `solenlabsllc@gmail.com`.
- **Read before writing:** first send a read-only prompt (policy status, track versions, rejection text word for word), then decide.
- **Stop before any "Publish" / "Send for review" / "Save and publish"** and let Cameron click it.
- Give Cameron one step at a time with exact menu paths and button names. If his screen differs, ask for a screenshot.

## Order Google enforces (don't skip ahead; this already cost Cameron an hour)
1. **Check policy status and existing tracks.** Fix rejected or policy issues in the next build if possible.
2. **Upload an .aab that contains Play Billing** (see `solen-android-release`) to the track the app already uses (Drava is on **Closed testing**, not Internal).
   Test and release → Testing → <track> → **Create new release** → Upload → release notes → **Next** → review (yellow warnings are OK, red errors are not) → Cameron publishes.
3. **Merchant account (one-time, whole developer account):** if Subscriptions shows "You need to set up a Google Payments merchant account", Cameron must do it (it needs legal, tax and bank info, so Claude can't): Subscriptions → **Set up a merchant account** (or All apps → Setup → **Payments profile**). Organization type, legal name `Solen Labs LLC`, EIN, W-9, business checking account (micro-deposit verify in 2–5 days; products usually unlock before that). Check this FIRST on a new account, before building anything that sells.
4. **Only then create products** (Monetize with Play → Products):
   - Subscriptions → **Create subscription** → Product ID (permanent) → **Add base plan** (`monthly` 1 month / `yearly` 1 year, Auto-renewing) → **Set prices** (all countries, USD) → **Save** → **Activate**.
   - One-time products (older consoles: "In-app products") → **Create one-time product** → ID, name, description, price → **Activate**.
   - Don't add Play free-trial offers when the app already gives a no-card trial.
5. **Server verification:** Google Cloud service account with the **Google Play Android Developer API** enabled → JSON key → Play Console (All apps level) **Users and permissions** → invite the SA email → app access → *View financial data* + *Manage orders and subscriptions*. Store the JSON as Supabase secret `GOOGLE_PLAY_SERVICE_ACCOUNT`. It can take hours to start working.
6. **License testing** (All apps → Setup → License testing) → add Cameron's Gmail → free test purchases.
7. **App content:** Data safety (Device or other IDs; Purchase history; collected, not shared), Content rating / IARC with the correct app name (Drava's still said "Fovu Date Mode").
8. **Publishing overview → Send changes for review** (Cameron clicks).

## Paywall policy checklist (apps sell digital subscriptions)
- In the Android app, use **Google Play Billing only**: no Stripe links, no promo-code unlocks (hide them in-app; web may keep them).
- Visible ✕ dismiss + "No thanks", price per period, auto-renew statement, how to cancel (Google Play → Payments & subscriptions → Subscriptions).
- Trial copy must match reality (e.g. "free 72-hour trial, no payment info needed").
- Restore purchases + Manage subscription (`https://play.google.com/store/account/subscriptions?package=<pkg>`) in Settings.
