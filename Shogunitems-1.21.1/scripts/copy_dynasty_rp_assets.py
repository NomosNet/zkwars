#!/usr/bin/env python3
"""Copy RP Samurai Dynasty textures/geo/models into nomosiki-work (author permission)."""
from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = Path(__file__).resolve().parents[2] / "samurai_dynasty-0.1.8-1.21.1/assets/samurai_dynasty"
DST = ROOT / "src/main/resources/assets/nomosiki-work"

ITEM_PNG = [
    "cloth", "steel_ingot", "steel_nugget", "katana_handle", "spear_handle", "knive_handle",
    "steel_samurai_helmet", "steel_samurai_helmet_overlay", "steel_samurai_helmet_overlay2",
    "steel_samurai_chestplate", "steel_samurai_chestplate_overlay", "steel_samurai_chestplate_overlay2",
    "steel_samurai_leggings", "steel_samurai_leggings_overlay", "steel_samurai_leggings_overlay2",
    "steel_samurai_boots", "steel_samurai_boots_overlay", "steel_samurai_boots_overlay2",
    "netherite_samurai_helmet", "netherite_samurai_helmet_overlay",
    "netherite_samurai_chestplate", "netherite_samurai_chestplate_overlay",
    "netherite_samurai_leggings", "netherite_samurai_leggings_overlay",
    "netherite_samurai_boots", "netherite_samurai_boots_overlay",
    "light_samurai_helmet", "light_samurai_chestplate", "light_samurai_leggings", "light_samurai_boots",
    "steel_ninja_helmet", "steel_ninja_chestplate", "ninja_leggings", "steel_ninja_boots",
    "netherite_ninja_helmet", "netherite_ninja_chestplate", "netherite_ninja_boots",
    "kimono", "straw_hat",
    "katana", "katana_netherite", "kunai", "kunai_netherite", "kama", "kama_netherite",
    "sai", "sai_netherite", "shuko", "shuko_netherite", "shuriken",
    "naginata", "naginata_netherite", "nagamaki", "nagamaki_netherite",
    "tetsubo", "tetsubo_netherite",
    "yumi", "yumi_pull_1", "yumi_pull_2",
]

WEAPON_PNG = [
    "kabutowari", "kabutowari_netherite", "kamayari", "kamayari_netherite",
    "katana", "katana_netherite", "kunai", "kunai_netherite",
    "masakari", "masakari_netherite", "nagamaki", "nagamaki_netherite",
    "naginata", "naginata_netherite", "odachi", "odachi_netherite",
    "tekko_kagi", "tekko_kagi_netherite", "tetsubo", "tetsubo_netherite",
    "tonbukiri", "tonbukiri_netherite", "yumi",
]

ARMOR_3D = [
    "kimono.png", "light_samurai.png", "netherite_samurai.png",
    "ninja_netherite.png", "ninja_steel.png", "steel_samurai_dyeable.png",
    "steel_samurai_layer.png", "steel_samurai_overlay.png", "straw_hat.png",
]

GEO = [
    "samurai_armor_new", "light_samurai_armor", "ninja_armor", "kimono", "straw_hat",
    "katana", "wakizashi", "odachi", "naginata", "nagamaki", "kamayari", "tonbukiri",
    "tetsubo", "masakari", "kabutowari", "kunai", "tekko_kagi", "shuriken",
]

ITEM_MODELS = [
    "steel_samurai_helmet", "steel_samurai_chestplate", "steel_samurai_leggings", "steel_samurai_boots",
    "netherite_samurai_helmet", "netherite_samurai_chestplate", "netherite_samurai_leggings", "netherite_samurai_boots",
    "light_samurai_helmet", "light_samurai_chestplate", "light_samurai_leggings", "light_samurai_boots",
    "steel_ninja_helmet", "steel_ninja_chestplate", "ninja_leggings", "steel_ninja_boots",
    "netherite_ninja_helmet", "netherite_ninja_chestplate", "netherite_ninja_boots",
    "kimono", "straw_hat",
    "katana", "wakizashi", "odachi", "naginata", "nagamaki", "kamayari", "tonbukiri",
    "tetsubo", "masakari", "kabutowari", "kunai", "kama", "sai", "shuko", "shuriken", "yumi",
    "yumi_pulling_0", "yumi_pulling_1", "yumi_pulling_2",
    "katana_netherite", "wakizashi_netherite", "odachi_netherite", "naginata_netherite",
    "nagamaki_netherite", "kamayari_netherite", "tonbukiri_netherite", "tetsubo_netherite",
    "masakari_netherite", "kabutowari_netherite", "kunai_netherite", "kama_netherite",
    "sai_netherite", "shuko_netherite",
    "steel_ingot", "steel_nugget", "cloth", "katana_handle", "spear_handle", "knive_handle",
]


def copy_file(src: Path, dst: Path) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)


def rewrite_model(text: str) -> str:
    text = text.replace("samurai_dynasty:", "nomosiki-work:")
    text = text.replace('"particle": "item/', '"particle": "nomosiki-work:item/')
    return text


def main() -> None:
    if not SRC.exists():
        raise SystemExit(f"missing dynasty assets: {SRC}")

    copied = 0
    for name in ITEM_PNG:
        src = SRC / "textures/item" / f"{name}.png"
        if src.exists():
            copy_file(src, DST / "textures/item" / f"{name}.png")
            copied += 1
    for name in WEAPON_PNG:
        src = SRC / "textures/weapon" / f"{name}.png"
        if src.exists():
            copy_file(src, DST / "textures/weapon" / f"{name}.png")
            copied += 1
    for name in ARMOR_3D:
        src = SRC / "textures/models/armor" / name
        if src.exists():
            copy_file(src, DST / "textures/models/armor" / name)
            copied += 1
    steel_colors = SRC / "textures/models/armor/steel_samurai"
    if steel_colors.exists():
        dst_colors = DST / "textures/models/armor/steel_samurai"
        shutil.copytree(steel_colors, dst_colors, dirs_exist_ok=True)
        copied += sum(1 for _ in dst_colors.glob("*.png"))
    for name in GEO:
        src = SRC / "geo" / f"{name}.geo.json"
        if src.exists():
            copy_file(src, DST / "geo" / f"{name}.geo.json")
            copied += 1
    for name in ITEM_MODELS:
        src = SRC / "models/item" / f"{name}.json"
        if src.exists():
            dst = DST / "models/item" / f"{name}.json"
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_text(rewrite_model(src.read_text(encoding="utf-8")), encoding="utf-8")
            copied += 1

    placeholder_geo = DST / "geo/item"
    if placeholder_geo.exists():
        shutil.rmtree(placeholder_geo)
    for leftover in ("yumi_pulling_0.png", "yumi_pulling_1.png", "yumi_pulling_2.png"):
        path = DST / "textures/item" / leftover
        if path.exists():
            path.unlink()

    print(f"copied {copied} RP asset files from Samurai Dynasty")


if __name__ == "__main__":
    main()
