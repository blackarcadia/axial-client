# Enchantment guide

The Enchantments card opens a server-backed reference menu. It has no toggle.
Search matches names, applicability and rarity; the column buttons sort the list.
Click a row to read the server description. Escape returns to the list, then to
Mod Menu.

Install the updated **PrisonsCore-1.0-SNAPSHOT.jar** on the server and the updated
client. This transport is part of PrisonsCore, not a separate plugin. It reads the
active CustomEnchantService and GearEnchantService registries, which are loaded
from `enchants.yml` and `gear-enchants.yml`. PrisonsCore's normal configuration
reload rebuilds the cached catalogue. An open guide checks for changes every five
seconds; no requests run while the guide is closed. Disconnecting clears the
catalogue so another server cannot display stale entries.

Mining enchants apply to the prison pickaxe, except Buster Efficiency, which
applies to Cell Busters (wooden shovels). Gear applicability uses the configured
target categories/materials. Names and descriptions have legacy formatting removed;
max levels and rarity values come from the active server definitions.

The `axial_cosmetics:enchantments` channel uses protocol version 1. All numbers
are Java DataInput/DataOutput big-endian values; strings use modified UTF-8.

- Request: byte `1`, long known revision (zero for no catalogue).
- Unchanged acknowledgement: byte `2`, long revision.
- Fragment: byte `1`, long revision, int compressed size, unsigned short fragment
  index, unsigned short fragment count, up to 28,000 bytes of gzip data.
- Inflated catalogue: int entry count, then each entry's UTF namespaced key,
  UTF display name, int max level, UTF rarity, int rarity rank, UTF description,
  int target count and UTF target tokens.

Compressed catalogues are limited to 2 MB; the client also limits inflation to
8 MB and entries to 4,096. Fragments are assembled and decoded before replacing
the displayed catalogue. The compatibility fixture was produced by PrisonsCore's
encoder from its 33 mining and 66 gear definitions. `EnchantmentCatalogueTest`
covers that fixture, updates, acknowledgements, malformed/incomplete transfers,
multiple fragments and size limits.
