# VS: Electronic Warfare

## Toolchain

- Minecraft `1.20.1`, Forge `47.4.0`, Java `17`.
- The displayed mod name is `VS: Electronic Warfare (Alpha)`; use the mod ID `vs_electronic_warfare` consistently for registry and resource identifiers.
- Required runtime mods are CC:Tweaked `1.113.1` and Valkyrien Skies `2.3.0-beta.5` or newer.
- DH is optional at runtime; the build uses `compileOnly 'maven.modrinth:DistantHorizonsApi:5.1.0'`.
- CC:Tweaked is also a `compileOnly` jar at the absolute path in `build.gradle`; update that path or provide the pinned jar before compiling elsewhere.

## Build

Run from the repository root (`vs_electronic_warfare`):

```powershell
.\gradlew.bat build
```

There are currently no test sources, so `build` is the primary verification command. The reobfuscated jar is `build\libs\vs_electronic_warfare-0.3.1.jar`.

## Architecture

- `VSElectronicWarfare` registers blocks, networking, the server config, and the ComputerCraft peripheral provider.
- `RadarScanner` is the server-authoritative entrypoint for all radar methods and owns the stable Lua result maps.
- `RadarScanner` uses exact VS-aware `clipIncludeShips` LOS up to 256 blocks. Beyond that, it validates 128-block exact endpoint segments first and uses `DhCompat` only for the remaining central terrain segment on a `ServerLevel`.
- Keep CC internal/API references isolated from unrelated code and verify against the pinned CC version before changing them.
- Keep DH access optional: `DhCompat` must remain safe when the `distanthorizons` mod is absent, uninitialized, missing a matching server-level wrapper/cache, or returns an error; those cases must use exact server LOS.
- The radar server ceiling is the Forge server config key `radar.maxRadarScanRadius`, default `2048.0`; Lua radii are finite, non-negative, and clamped to that value.

## Runtime Checks

- Test in a dedicated or single-player Forge instance containing CC:Tweaked, Valkyrien Skies, and VMod; DH may be omitted to verify exact-LOS fallback.
- For ROM computers, enable CC HTTP when startup scripts download remote modules; trigger startup with one redstone signal and verify success ends in `completed` while failures report the line and use two beeps.
- Test moving VS ships, save/reload, schematic copies, manual Start/Shutdown, and CC HTTP-disabled behavior before changing ROM persistence or networking.
- For radar changes, verify short-range terrain/ship occlusion, long-range clear and ridge-blocked terrain, endpoint obstructions, intervening ships, radius clamping, and `getConfigInfo().max_radius`.

## Deployment

Stop Minecraft before replacing the loaded jar. The known JPCreate2.5 deployment target is:

```powershell
$source = '.\build\libs\vs_electronic_warfare-0.3.1.jar'
$target = 'C:\Users\lauya\curseforge\minecraft\Instances\JPCreate2.5\mods\vs_electronic_warfare-0.3.1.jar'
Copy-Item -Force $source $target
Get-FileHash -Algorithm SHA256 $source, $target
```

The source and target SHA-256 values must match before launching the instance.
