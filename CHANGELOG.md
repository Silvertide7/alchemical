1.0.1
---
Ported to Minecraft 1.21.1 / NeoForge 21.1.
- Fix Bastion Stone recipe not loading (used the old `scute` item id instead of `turtle_scute`)
- Fix ingredients getting stuck in the hidden Athanor slot when the flask filled up or was removed; they now return to your inventory
- Fix "still recovering" message showing a cooldown from a previous world or server after relogging
- Fix Essence Stones missing from the creative tab when connected to a dedicated server
- Fix the max-stones message always saying 3 regardless of config
- A stone whose `effect` id doesn't exist now logs an error at load instead of silently doing nothing
- Stones in the creative tab are now sorted by id
- The Athanor no longer uses a block entity. Existing worlds will log a one-time "Skipping BlockEntity" warning per chunk with an Athanor; the blocks themselves are unaffected

1.0.0
---
Initial release
