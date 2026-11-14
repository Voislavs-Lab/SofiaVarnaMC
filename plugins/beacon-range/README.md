# BeaconRange

Paper 26.3 plugin that sets each beacon pyramid tier's effect range.

| Tier | Range |
| --- | --- |
| 1 | 45 |
| 2 | 70 |
| 3 | 100 |
| 4 | 128 |

`/beaconrange` prints the ranges. `/beaconrange reload` rereads the config. Permission `beaconrange.admin` defaults to op.

The range is stored with Paper's `Beacon.setEffectRange`. A beacon still refreshes only while its chunk is ticking. Simulation distance 8 stops that at 128 blocks. The config accepts a radius up to 512. Above 128, Paper checks the online player list instead of searching chunks.

Build with JDK 25 from this directory:

```
gradle build
```

The jar is `build/libs/beacon-range-1.0.0.jar`. Copy it to `/opt/minecraft/plugins/` and restart. The running server has `BeaconRange-1.0.0.jar`, SHA256 `cafcc52728c428969418af23ec6157a2ea88572dd6d8458732ca7196c5c87b50`.
