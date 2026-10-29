---
name: solen-site-deploy
description: Put a SolenLabs website/PWA (Hostinger-hosted, e.g. drava.app) under GitHub with auto-deploy, or ship a change to one that already is. Use when Cameron asks to deploy, publish, update the live site, set up hosting for a new app, or connect Hostinger.
---

# SolenLabs site deploy (GitHub → Hostinger auto-deploy)

Cameron is solo and not a sysadmin. Walk him through **one step at a time**, name every field and button, and wait for "done" before the next step. Never ask him to use FTP or share hosting passwords.

## Already set up?
Check for a repo named `<app>-site` under `solenlabsllc-ui` (e.g. `solenlabsllc-ui/drava-site`). If it exists:
1. Clone it (add it to the session with push access first).
2. Edit, commit to `main`, push. Hostinger deploys `main` to `public_html` in ~2 seconds.
3. Ask Cameron to refresh hPanel → Websites → `<domain>` → Advanced → **GIT**. **Last deployment → Commit** should show your commit message. That is the proof it's live.

## First-time setup for a new site (~15 min)

**A. Create the repo (Cameron, GitHub Desktop).** Claude can't create repos (403), so Cameron does it:
| Field | Value |
|---|---|
| Name | `<app>-site` |
| Description | `<domain> website` |
| Local path | default |
| Initialize with README | unchecked |
| Git ignore / License | None / None |
Then **Create repository** → **Publish repository** → keep **Keep this code private** checked → **Publish**. The "No local changes / Open in Android Studio / Show in Explorer / View on GitHub" screen means success. Ignore those buttons.

**B. Back up the live site (Cameron).** hPanel → **Websites** → domain → **Manage** → **Files** → **File Manager** → `public_html` → Ctrl+A → right-click **Compress** → Zip `<app>-backup` → right-click the zip → **Download** → upload it to Claude.

**C. Claude fills the repo.** Copy every live file (skip `.trash/`), apply the change, run a local http server and check that every page and link returns 200, then commit and push to `main`. Never put secrets in a site repo; only public keys (e.g. the Supabase anon key) belong there.

**D. Make the repo public.** GitHub → repo → **Settings** → **General** → bottom **Danger Zone** → **Change visibility** → **Change to public**. This avoids SSH deploy keys, which hPanel often doesn't show. It's safe because site files are already public via View Source.

**E. Empty public_html (Cameron).** File Manager → `public_html` → Ctrl+A → **Delete**, including hidden files. The site is blank until F.

**F. Connect (Cameron).** hPanel → domain → **Advanced** → **GIT** → repository `https://github.com/solenlabsllc-ui/<app>-site.git`, branch `main`, directory empty (it shows as `public_html`) → **Create** → **Deploy**. Newer hPanel shows a card with **Connected with GitHub** and **Auto-deployment ✅**. No webhook is needed.

**G. Prove auto-deploy.** Push a tiny real change (e.g. sitemap `<lastmod>`), then have Cameron refresh the GIT page and confirm the commit shows under **Last deployment**.

## Gotchas
- Hostinger only deploys into an EMPTY folder.
- Non-site files (Android sources etc.) go on a separate branch (e.g. `android`). Only `main` deploys.
- Something else may ping an old endpoint (UptimeRobot keep-alive). When retiring one, keep answering GET with 200.
