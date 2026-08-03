# VS: Electronic Warfare Fresh

## Project

- Minecraft `1.20.1`
- Forge `47.4.0`
- CC:Tweaked `1.20.1-1.113.1`
- Valkyrien Skies `2.3.0-beta.5`
- Mod id: `vs_electronic_warfare_fresh`

The ROM computer implementation depends on CC:Tweaked internal classes. Keep those references isolated and verify against the pinned CC version before changing the dependency.

## Build

Run from this directory:

```powershell
.\gradlew.bat build
```

The reobfuscated jar is written to:

```text
build\libs\vs_electronic_warfare_fresh-0.3.0.jar
```

## Deploy

Stop Minecraft before replacing a loaded mod jar. Deploy to the JPCreate2.5 instance with:

```powershell
Copy-Item -Force .\build\libs\vs_electronic_warfare_fresh-0.3.0.jar `
  'C:\Users\lauya\curseforge\minecraft\Instances\JPCreate2.5\mods\vs_electronic_warfare_fresh-0.3.0.jar'
```

Verify the deployed file when needed:

```powershell
Get-FileHash 'C:\Users\lauya\curseforge\minecraft\Instances\JPCreate2.5\mods\vs_electronic_warfare_fresh-0.3.0.jar'
```

The target instance must also contain CC:Tweaked, Valkyrien Skies, and VMod. The ROM computer is server-authoritative and should be tested in a dedicated or single-player Forge instance with CC HTTP enabled when its startup commands download remote modules.

## Runtime Checks

1. Place a ROM Computer and enter one CraftOS shell command per line in its Startup Commands screen, for example `pastebin get ID module` followed by `module`.
2. Apply a redstone signal once. Startup produces one beep; command failure produces two beeps and reports the failed line. Successful scripts end as `completed`.
3. Save and reload a schematic. The startup script should remain, while each placed copy receives a fresh CC identity and filesystem.
4. Test a moving VS ship, world save/reload, CC HTTP disabled, and manual Start/Shutdown controls.
