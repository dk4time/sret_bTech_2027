# Walkthrough 1 — Egmore → Tambaram (the canonical flow, Manual captain mode)

```bash
python run.py --cli < walkthroughs/01_egmore_to_tambaram.txt
```

**Goal:** show that a captain's location changes only through ride events, and that matching is
decided by **where the captain is now**.

| Step | Input | What happens | What to point out |
|---|---|---|---|
| 1 | `4` `3` `09:10` `0` | Clock → Jump to 09:10 | The status line now says **Peak hour**. `SimulatedClock.advance_to()` would refuse an earlier time. |
| 2 | `3` `3` `egmore` `3` `0` | Ops → Nearby captains for Egmore, Cab Economy | **Senthil ✔ eligible, 0.0 km** away, and Balaji ✔ at Royapettah. Every ✘ reason comes from `MatchingService` — the CLI has no rules of its own. |
| 3 | `1` `3` `4` … `y` | Customer app → Selvi → Book: pickup Enter (= Egmore), drop `tambaram`, vehicle 3 (Cab Economy), 1 passenger, preferred payment, no coupon, confirm | The estimate (₹754.22, 1.1x peak surge) is shown **before** confirming. The OTP is shown after booking (**2951** — seeded, identical every run). Status: `REQUESTED — offer waiting for captain Senthil`. |
| 4 | `5` `0` | Track the ride | The timeline so far. Nothing happens until the captain answers — this is **Manual mode**. |
| 5 | `2` `2` `1` | Captain app → Senthil (● marker) → profile | The ● in the captain list means an offer is waiting on his phone. |
| 6 | `3` `a` | View offer → accept | Offer screen: pickup distance, drop, estimated fare. Accepting calls `RideService.accept_offer()`. |
| 7 | `4` | Mark arrived | `captain_arrives()` moves the clock by the ETA **before** moving Senthil — no teleporting. |
| 8 | `5` `0000` | Start with a wrong OTP | ✘ `InvalidOtpError … 2 attempt(s) left`. The OTP is `Ride.__otp` — private, name-mangled. |
| 9 | `5` `2951` | Start with the right OTP | Ride is `IN_PROGRESS`. |
| 10 | `6` | Complete | Clock jumps **82 minutes** to 10:33 AM (peak-hour drive). Receipt shows ₹754.22; wallet pays. *"Selvi and Senthil are at Tambaram."* |
| 11 | `1` `0` | Senthil's profile | Location: **Tambaram**. |
| 12 | `3` `3` `egmore` `3` | Ops → Nearby for Egmore again | **Senthil ✘ 24.0 km away (radius 8 km)**. A new Egmore request can never reach him. |
| 13 | `3` `chromepet` `3` | Nearby for Chromepet | **Senthil ✔ eligible, 5.8 km** — the captain now at Tambaram is the right one for Chromepet. |
| 14 | `0` `0` | Back, leave | "Interactive mode closed — goodbye!" |

**Discussion question:** which single line of code decides that Senthil is at Tambaram?
(`Captain.finish_ride()` sets `self._location = drop`, called from `RideService._settle_trip()`.)
