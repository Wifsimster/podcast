# Play Console — Data Safety form answers

Carne collects no data, so the form is short.

## Data collection & sharing
- **Does your app collect or share any of the required user data types?** → **No**
  (No personal info, no location, no identifiers, no usage/analytics — nothing is
  collected by the developer or shared with third parties.)

## Security practices
- **Is all of the user data encrypted in transit?** → Not applicable (no user data
  is collected). Network requests to feeds / iTunes search use HTTPS where available.
- **Do you provide a way for users to request that their data be deleted?** → Data
  is stored only on-device; users can clear it in-app (backup/restore, uninstall).

## Other declarations
- **Ads:** No.
- **Account creation / sign-in:** No.
- **Target audience:** General (not designed for children).
- **Privacy policy URL:** _host `privacy-policy.md` and paste the URL here._

## Foreground service (separate Play declaration)
The app uses `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. In the Play Console "App content
→ Foreground service permissions" section, declare:
- **Use case:** Media playback — continue playing podcasts while the app is in the
  background and show transport controls in the notification / lock screen / Android Auto.
