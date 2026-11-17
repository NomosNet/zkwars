#!/usr/bin/env python3
"""Check RP gear assets, recipes, and tags used by the playtest checklist."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/nomosiki-work"
DATA = ROOT / "src/main/resources/data"
ITEMS_JAVA = ROOT / "src/main/java/com/nomosiki/work/registry/ModItems.java"


def main() -> int:
    java = ITEMS_JAVA.read_text(encoding="utf-8")
    ids = re.findall(r'(?:item|armor|sword)\("([^"]+)"', java)
    missing: list[str] = []
    for item_id in ids:
        if not (ASSETS / "textures/item" / f"{item_id}.png").exists():
            missing.append(f"texture {item_id}")
        if not (ASSETS / "models/item" / f"{item_id}.json").exists():
            missing.append(f"model {item_id}")
    en = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
    ru = json.loads((ASSETS / "lang/ru_ru.json").read_text(encoding="utf-8"))
    for item_id in ids:
        key = f"item.nomosiki-work.{item_id}"
        if key not in en:
            missing.append(f"en {item_id}")
        if key not in ru:
            missing.append(f"ru {item_id}")
    recipes = list((DATA / "nomosiki-work/recipe").glob("*.json"))
    if len(recipes) < 50:
        missing.append(f"recipe count {len(recipes)}")
    for path in recipes:
        json.loads(path.read_text(encoding="utf-8"))
    for name in (
        "samurai_steel",
        "samurai_netherite",
        "samurai_ronin",
        "ninja_steel",
        "ninja_netherite",
        "kimono",
        "straw_hat",
    ):
        if not (ASSETS / "textures/entity/armor" / f"{name}.png").exists():
            missing.append(f"armor tex {name}")
    for geo in (
        "samurai_armor",
        "ninja_armor",
        "kimono",
        "straw_hat",
        "sword",
        "spear",
        "club",
        "knife",
    ):
        if not (ASSETS / "geo/item" / f"{geo}.geo.json").exists():
            missing.append(f"geo {geo}")
    for layer in (
        "samurai_steel_layer_1",
        "samurai_steel_layer_2",
        "samurai_steel_dyeable_layer_1",
        "samurai_steel_dyeable_layer_2",
        "samurai_netherite_layer_1",
        "samurai_netherite_layer_2",
        "samurai_ronin_layer_1",
        "samurai_ronin_layer_2",
        "ninja_steel_layer_1",
        "ninja_steel_layer_2",
        "ninja_netherite_layer_1",
        "ninja_netherite_layer_2",
        "straw_layer_1",
        "straw_layer_2",
        "cloth_layer_1",
        "cloth_layer_2",
    ):
        if not (ASSETS / "textures/models/armor" / f"{layer}.png").exists():
            missing.append(f"layer {layer}")
    yumi = json.loads((ASSETS / "models/item/yumi.json").read_text(encoding="utf-8"))
    if len(yumi.get("overrides", [])) != 3:
        missing.append("yumi pull overrides")
    dye = json.loads((DATA / "minecraft/tags/item/dyeable.json").read_text(encoding="utf-8"))
    if len(dye.get("values", [])) != 4:
        missing.append("dyeable tag")
    smithing = list((DATA / "nomosiki-work/recipe").glob("*smithing.json"))
    if len(smithing) != 21:
        missing.append(f"smithing count {len(smithing)}")
    if "itemGroup.nomosiki-work.main" not in en:
        missing.append("creative tab lang")
    print(f"items={len(ids)} recipes={len(recipes)} smithing={len(smithing)}")
    if missing:
        print("MISSING:")
        for line in missing:
            print(" ", line)
        return 1
    print("playtest assets ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())
