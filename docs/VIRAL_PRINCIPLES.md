# Ondes vs. the "31 Principles of a Viral Product"

Marc Lou's list is a set of **landing-page and go-to-market** rules. The surface
they apply to in a mobile app is its *shop window*: the README (the GitHub
landing page), the Play Store listing, and the first-impression copy inside the
app — not the playback engine.

This note records how Ondes's copy was revised to respect the principles, and —
just as importantly — which principles were **deliberately not applied** because
they conflict with what Ondes is: an ad-free, tracker-free, no-login,
open-source player that's **free to build and a one-time purchase on Play**.
Forcing the rest would make the product worse, not more viral.

## Applied

| # | Principle | How Ondes applies it |
|---|-----------|----------------------|
| 3 | Numbers, not adjectives | "0 ads · 0 trackers · 0 login", "0.8×–3× speed", "finish a 60-minute show in 40", "5 languages" — replacing "clean, fast, ad-free". |
| 4 | A footer worth sharing | README closes with the name's meaning ("Ondes = waves") and an explicit share nudge. |
| 6 | One idea per screen | Hero leads with a single promise (privacy) before the feature list. |
| 7 | A headline a fifth-grader gets | "Podcasts without the ads watching you back." No jargon. |
| 9 | Copy only you could write | The "make you the product" framing and the *waves* wordplay are specific to Ondes, not paste-able onto a competitor. |
| 10 | Show before you tell | Screenshots stay at the very top of the README, ahead of prose. |
| 11 | Do one thing | Positioned squarely as a *podcast player* — not a media suite. |
| 13 | Ride a wave | Leans on the privacy / "own your data" wave people already care about. |
| 18 | Emotional headline | "ads watching you back" is a feeling, not a feature. |
| 20 | Sell from the hero alone | Hero now carries the promise, the proof (0/0/0), the outcome, and the CTA. |
| 21 | Empathy before selling | Opens by naming the problem (ads, trackers, forced accounts) before the fix. |
| 22 | One call to action | Hero has a single primary CTA — "Get the APK". |
| 23 | A memorable name | Kept "Ondes" and explained it ("French for *waves*") instead of hiding it. |
| 24 | Sell the desire, not the feature | "finish a 60-minute show in 40", "your commute, your flight, your dead zones". |
| 26 | No weak words | Removed "& more" and bare adjectives from the short description and hero. |
| 28 | CTA says what happens next | "Get the APK — installs in under a minute, no store account required." |
| 30 | Describe it in under 10 words | Tagline: "Your podcasts. 0 ads, 0 trackers, 0 account." |
| 27 | One-time payment, not a subscription | The Play Store version is a **one-time purchase** (€2.99 / $2.99 launch price), and the price is sold *as a feature*: "Pay once — yours for good. No subscription, no ads, no upsells, ever." |
| 25 | Let people try before they pay | The free, open-source sideload build **is** the trial: "Try it free here first; buy it on Play if it earns a spot on your phone." |

## Monetization model

Ondes uses a **two-path** model that lets it honor the pricing principles without
betraying the open-source ethos:

- **Free** — clone, build and sideload the open-source APK (also published as a CI
  artifact / GitHub Release). This is the try-before-you-buy path (#25).
- **Paid** — a **one-time purchase** on Google Play (**€2.99 / $2.99 launch price**,
  with Google Play local pricing left on so other markets auto-adjust). Same app,
  no subscription, no ads, no in-app purchases — the convenience of one-tap
  install, and it funds the work (#27).

**Why €2.99 (the pricing-debate verdict).** A free, open-source build already
exists, so the Play price buys *convenience + support*, not the software — which
caps how high it can go against free rivals like AntennaPod. €2.99 clears the
"why not just use the free one?" objection with margin to spare, matches charm
pricing (#3), and is paired with the one-time-vs-subscription story ("less than
two months of a podcast subscription; buy once, not €40 every year"). The plan is
to **launch at €2.99 and raise to €3.99 after ~50+ ratings** — you can always
raise a price, but cutting one at launch reads as desperation.

## Deliberately not applied (and why)

| # | Principle | Why it's wrong for Ondes |
|---|-----------|--------------------------|
| 1 | No free plan | The open-source build stays free on purpose — a *privacy* player you can't inspect or self-host would undercut its own pitch. The Play version is paid; the free build is the "plan." |
| 8 | Hard paywall | The Play listing is paid (a soft paywall), but we won't wall off the open-source build or gate features behind a second payment. |
| 12 / 16 | Pricing tiers / "Pricing" in the header | One price, every feature, forever — inventing Good/Better/Best tiers would add decisions, not sales (the opposite of the goal). |
| 31 | Be more expensive than competitors | A one-time ~€3 against free-with-ads / subscription rivals is *cheaper*, and that contrast is the selling point. "Charge more" doesn't fit the privacy-for-everyone positioning. |
| 29 | Don't launch without testimonials | We won't fabricate quotes. Real Play Store reviews belong on the listing once they exist. |
| 5 / 15 | OG image as a thumbnail / founder's face | Design and personal-brand assets, out of scope for a copy pass; the feature graphic lives in `docs/store-assets/`. |

## Why the in-app copy was left mostly as-is

The first-run copy (onboarding, empty states, CTAs like "Find podcasts") already
follows the spirit of these rules — specific CTAs that say what happens next,
benefit-framed empty states — and it is translated across all five locales.
Rewriting it would risk five-language parity for little gain, so the high-leverage
changes were concentrated on the README and the store listing, where the
principles bite hardest.
