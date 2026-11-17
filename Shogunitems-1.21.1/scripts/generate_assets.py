#!/usr/bin/env python3
"""Generate original placeholder textures, geo, item models, recipes, tags, and lang."""
from __future__ import annotations

import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/nomosiki-work"
DATA = ROOT / "src/main/resources/data"
NS = "nomosiki-work"

ITEMS = {
    "steel_ingot": (0x8A8A8A, "Steel Ingot", "Стальной слиток"),
    "steel_nugget": (0x9A9A9A, "Steel Nugget", "Стальной самородок"),
    "cloth": (0xE8DCC8, "Cloth", "Ткань"),
    "katana_handle": (0x5C4033, "Katana Handle", "Рукоять катаны"),
    "spear_handle": (0x6B4F3A, "Spear Handle", "Древко"),
    "knive_handle": (0x4A3728, "Knife Handle", "Рукоять ножа"),
    "steel_samurai_helmet": (0x7A1F1F, "Samurai Helmet", "Шлем самурая"),
    "steel_samurai_chestplate": (0x7A1F1F, "Samurai Chestplate", "Доспех самурая"),
    "steel_samurai_leggings": (0x7A1F1F, "Samurai Leggings", "Набедренники самурая"),
    "steel_samurai_boots": (0x7A1F1F, "Samurai Boots", "Сапоги самурая"),
    "netherite_samurai_helmet": (0x2B1A18, "Netherite Samurai Helmet", "Незеритовый шлем самурая"),
    "netherite_samurai_chestplate": (0x2B1A18, "Netherite Samurai Chestplate", "Незеритовый доспех самурая"),
    "netherite_samurai_leggings": (0x2B1A18, "Netherite Samurai Leggings", "Незеритовые набедренники самурая"),
    "netherite_samurai_boots": (0x2B1A18, "Netherite Samurai Boots", "Незеритовые сапоги самурая"),
    "light_samurai_helmet": (0xC4A35A, "Ronin Hat", "Шляпа ронина"),
    "light_samurai_chestplate": (0x8B6D4A, "Ronin Chestplate", "Доспех ронина"),
    "light_samurai_leggings": (0x8B6D4A, "Ronin Leggings", "Набедренники ронина"),
    "light_samurai_boots": (0x8B6D4A, "Ronin Boots", "Сапоги ронина"),
    "steel_ninja_helmet": (0x2B2B2B, "Ninja Mask", "Маска ниндзя"),
    "steel_ninja_chestplate": (0x2B2B2B, "Ninja Robe", "Одеяние ниндзя"),
    "ninja_leggings": (0x3A3A3A, "Ninja Pants", "Штаны ниндзя"),
    "steel_ninja_boots": (0x2B2B2B, "Ninja Boots", "Сапоги ниндзя"),
    "netherite_ninja_helmet": (0x1A1210, "Netherite Ninja Mask", "Незеритовая маска ниндзя"),
    "netherite_ninja_chestplate": (0x1A1210, "Netherite Ninja Robe", "Незеритовое одеяние ниндзя"),
    "netherite_ninja_boots": (0x1A1210, "Netherite Ninja Boots", "Незеритовые сапоги ниндзя"),
    "kimono": (0xC45A6E, "Kimono", "Кимоно"),
    "straw_hat": (0xC9B45A, "Straw Hat", "Соломенная шляпа"),
    "katana": (0xC0C4C8, "Katana", "Катана"),
    "wakizashi": (0xB8BCC0, "Wakizashi", "Вакидзаси"),
    "odachi": (0xD0D4D8, "Odachi", "Одати"),
    "naginata": (0xA8B0B8, "Naginata", "Нагината"),
    "nagamaki": (0xA0A8B0, "Nagamaki", "Нагамаки"),
    "kamayari": (0x98A0A8, "Kamayari", "Камаяри"),
    "tonbukiri": (0x90A0B0, "Tonbukiri", "Тонбокири"),
    "tetsubo": (0x5A5A5A, "Tetsubo", "Тэцубо"),
    "masakari": (0x6A6A6A, "Masakari", "Масакари"),
    "kabutowari": (0x707070, "Kabutowari", "Кабутовари"),
    "kunai": (0x888888, "Kunai", "Кунай"),
    "kama": (0x7A7A7A, "Kama", "Кама"),
    "sai": (0x9A9A9A, "Sai", "Саи"),
    "shuko": (0x4A4A4A, "Tekko-Kagi", "Тэкко-каги"),
    "shuriken": (0x666666, "Shuriken", "Сюрикен"),
    "yumi": (0x6B4F3A, "Yumi Bow", "Юми"),
    "yumi_pulling_0": (0x8A6A4A, "Yumi Bow", "Юми"),
    "yumi_pulling_1": (0xC4A070, "Yumi Bow", "Юми"),
    "yumi_pulling_2": (0xF0D9A0, "Yumi Bow", "Юми"),
    "katana_netherite": (0x3D2A28, "Netherite Katana", "Незеритовая катана"),
    "wakizashi_netherite": (0x3D2A28, "Netherite Wakizashi", "Незеритовый вакидзаси"),
    "odachi_netherite": (0x3D2A28, "Netherite Odachi", "Незеритовое одати"),
    "naginata_netherite": (0x3D2A28, "Netherite Naginata", "Незеритовая нагината"),
    "nagamaki_netherite": (0x3D2A28, "Netherite Nagamaki", "Незеритовая нагамаки"),
    "kamayari_netherite": (0x3D2A28, "Netherite Kamayari", "Незеритовая камаяри"),
    "tonbukiri_netherite": (0x3D2A28, "Netherite Tonbukiri", "Незеритовое тонбокири"),
    "tetsubo_netherite": (0x2A2220, "Netherite Tetsubo", "Незеритовый тэцубо"),
    "masakari_netherite": (0x2A2220, "Netherite Masakari", "Незеритовый масакари"),
    "kabutowari_netherite": (0x2A2220, "Netherite Kabutowari", "Незеритовый кабутовари"),
    "kunai_netherite": (0x2A2220, "Netherite Kunai", "Незеритовый кунай"),
    "kama_netherite": (0x2A2220, "Netherite Kama", "Незеритовая кама"),
    "sai_netherite": (0x2A2220, "Netherite Sai", "Незеритовый саи"),
    "shuko_netherite": (0x2A2220, "Netherite Tekko-Kagi", "Незеритовые тэкко-каги"),
}

HANDHELD = {
    "katana", "wakizashi", "odachi", "naginata", "nagamaki", "kamayari", "tonbukiri",
    "tetsubo", "masakari", "kabutowari", "kunai", "kama", "sai", "shuko",
    "katana_netherite", "wakizashi_netherite", "odachi_netherite", "naginata_netherite",
    "nagamaki_netherite", "kamayari_netherite", "tonbukiri_netherite", "tetsubo_netherite",
    "masakari_netherite", "kabutowari_netherite", "kunai_netherite", "kama_netherite",
    "sai_netherite", "shuko_netherite",
}

SWORDS = [
    "katana", "wakizashi", "odachi", "naginata", "nagamaki", "shuko", "tetsubo",
    "tonbukiri", "kamayari", "masakari", "kunai", "sai", "kabutowari", "kama",
    "katana_netherite", "wakizashi_netherite", "odachi_netherite", "naginata_netherite",
    "nagamaki_netherite", "shuko_netherite", "tetsubo_netherite", "tonbukiri_netherite",
    "kamayari_netherite", "masakari_netherite", "kunai_netherite", "sai_netherite",
    "kabutowari_netherite", "kama_netherite",
]

SMITHING = [
    ("katana", "katana_netherite"),
    ("wakizashi", "wakizashi_netherite"),
    ("odachi", "odachi_netherite"),
    ("naginata", "naginata_netherite"),
    ("nagamaki", "nagamaki_netherite"),
    ("kamayari", "kamayari_netherite"),
    ("tonbukiri", "tonbukiri_netherite"),
    ("tetsubo", "tetsubo_netherite"),
    ("masakari", "masakari_netherite"),
    ("kabutowari", "kabutowari_netherite"),
    ("kunai", "kunai_netherite"),
    ("kama", "kama_netherite"),
    ("sai", "sai_netherite"),
    ("shuko", "shuko_netherite"),
    ("steel_samurai_helmet", "netherite_samurai_helmet"),
    ("steel_samurai_chestplate", "netherite_samurai_chestplate"),
    ("steel_samurai_leggings", "netherite_samurai_leggings"),
    ("steel_samurai_boots", "netherite_samurai_boots"),
    ("steel_ninja_helmet", "netherite_ninja_helmet"),
    ("steel_ninja_chestplate", "netherite_ninja_chestplate"),
    ("steel_ninja_boots", "netherite_ninja_boots"),
]

ARMOR_TEX = {
    "samurai_steel": 0x7A1F1F,
    "samurai_netherite": 0x2B1A18,
    "samurai_ronin": 0x8B6D4A,
    "ninja_steel": 0x2B2B2B,
    "ninja_netherite": 0x1A1210,
    "kimono": 0xC45A6E,
    "straw_hat": 0xC9B45A,
}


def chunk(tag: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def write_png(path: Path, width: int, height: int, rgba: tuple[int, int, int, int]) -> None:
    raw = b""
    row = bytes(rgba) * width
    for _ in range(height):
        raw += b"\x00" + row
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def rgb(color: int) -> tuple[int, int, int, int]:
    return ((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255)


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def item_ref(name: str) -> dict:
    return {"item": f"{NS}:{name}"}


def tag_ref(tag: str) -> dict:
    return {"tag": tag}


def shaped(pattern: list[str], key: dict, result: str, count: int = 1, category: str | None = None) -> dict:
    data = {"type": "minecraft:crafting_shaped", "key": key, "pattern": pattern, "result": {"id": f"{NS}:{result}"}}
    if count != 1:
        data["result"]["count"] = count
    if category:
        data["category"] = category
    return data


def armor_geo() -> dict:
    def bone(name: str, parent: str | None, pivot: list, cubes: list | None = None) -> dict:
        entry = {"name": name, "pivot": pivot}
        if parent:
            entry["parent"] = parent
        if cubes:
            entry["cubes"] = cubes
        return entry

    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.nomosiki_work_armor",
                "texture_width": 64,
                "texture_height": 64,
                "visible_bounds_width": 3,
                "visible_bounds_height": 3.5,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": [
                bone("bipedHead", None, [0, 24, 0]),
                bone("armorHead", "bipedHead", [0, 24, 0], [
                    {"origin": [-4, 24, -4], "size": [8, 8, 8], "inflate": 0.6, "uv": [0, 0]},
                ]),
                bone("bipedBody", None, [0, 24, 0]),
                bone("armorBody", "bipedBody", [0, 24, 0], [
                    {"origin": [-4, 12, -2], "size": [8, 12, 4], "inflate": 0.35, "uv": [16, 16]},
                ]),
                bone("bipedRightArm", None, [-5, 22, 0]),
                bone("armorRightArm", "bipedRightArm", [-5, 22, 0], [
                    {"origin": [-8, 12, -2], "size": [4, 12, 4], "inflate": 0.3, "uv": [40, 16]},
                ]),
                bone("bipedLeftArm", None, [5, 22, 0]),
                bone("armorLeftArm", "bipedLeftArm", [5, 22, 0], [
                    {"origin": [4, 12, -2], "size": [4, 12, 4], "inflate": 0.3, "uv": [40, 16]},
                ]),
                bone("bipedRightLeg", None, [-1.9, 12, 0]),
                bone("armorRightLeg", "bipedRightLeg", [-1.9, 12, 0], [
                    {"origin": [-4, 0, -2], "size": [4, 12, 4], "inflate": 0.2, "uv": [0, 16]},
                ]),
                bone("armorRightBoot", "bipedRightLeg", [-1.9, 12, 0], [
                    {"origin": [-4, 0, -2], "size": [4, 12, 4], "inflate": 0.35, "uv": [0, 32]},
                ]),
                bone("bipedLeftLeg", None, [1.9, 12, 0]),
                bone("armorLeftLeg", "bipedLeftLeg", [1.9, 12, 0], [
                    {"origin": [0, 0, -2], "size": [4, 12, 4], "inflate": 0.2, "uv": [0, 16]},
                ]),
                bone("armorLeftBoot", "bipedLeftLeg", [1.9, 12, 0], [
                    {"origin": [0, 0, -2], "size": [4, 12, 4], "inflate": 0.35, "uv": [0, 32]},
                ]),
            ],
        }],
    }


def hat_geo() -> dict:
    geo = armor_geo()
    geo["minecraft:geometry"][0]["description"]["identifier"] = "geometry.nomosiki_work_hat"
    geo["minecraft:geometry"][0]["bones"][1]["cubes"] = [
        {"origin": [-6, 27, -6], "size": [12, 2, 12], "uv": [0, 0]},
        {"origin": [-4, 29, -4], "size": [8, 4, 8], "uv": [0, 16]},
    ]
    return geo


def kimono_geo() -> dict:
    geo = armor_geo()
    geo["minecraft:geometry"][0]["description"]["identifier"] = "geometry.nomosiki_work_kimono"
    geo["minecraft:geometry"][0]["bones"][3]["cubes"] = [
        {"origin": [-5, 10, -3], "size": [10, 14, 6], "inflate": 0.1, "uv": [0, 16]},
    ]
    return geo


def item_geo(identifier: str, cubes: list) -> dict:
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": identifier,
                "texture_width": 16,
                "texture_height": 16,
                "visible_bounds_width": 3,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 0.5, 0],
            },
            "bones": [{"name": "root", "pivot": [0, 0, 0], "cubes": cubes}],
        }],
    }


def main() -> None:
    # Visuals come from copy_dynasty_rp_assets.py (author permission).
    # This script only regenerates recipes, tags, and language files.
    recipes = DATA / NS / "recipe"
    steel = tag_ref("c:ingots/steel")
    nugget = tag_ref("c:nuggets/steel")
    gold = tag_ref("c:ingots/gold")
    gold_nugget = tag_ref("c:nuggets/gold")

    recipes.mkdir(parents=True, exist_ok=True)
    write_json(recipes / "steel_ingot_from_blasting_iron_ingot.json", {
        "type": "minecraft:blasting",
        "category": "misc",
        "cookingtime": 300,
        "experience": 1.5,
        "ingredient": {"item": "minecraft:iron_ingot"},
        "result": {"id": f"{NS}:steel_ingot"},
    })
    write_json(recipes / "steel_ingot_from_smelting_iron_ingot.json", {
        "type": "minecraft:smelting",
        "category": "misc",
        "cookingtime": 400,
        "experience": 1.0,
        "ingredient": {"item": "minecraft:iron_ingot"},
        "result": {"id": f"{NS}:steel_ingot"},
    })
    write_json(recipes / "steel_nugget.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [item_ref("steel_ingot")],
        "result": {"count": 9, "id": f"{NS}:steel_nugget"},
    })
    write_json(recipes / "steel_ingot_from_nugget.json", shaped(
        ["XXX", "XXX", "XXX"], {"X": nugget}, "steel_ingot"))
    write_json(recipes / "cloth.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"tag": "minecraft:wool"}],
        "result": {"count": 4, "id": f"{NS}:cloth"},
    })
    write_json(recipes / "katana_handle.json", shaped(
        [" X ", "LSX", "XL "],
        {"S": {"item": "minecraft:stick"}, "X": gold_nugget, "L": {"item": "minecraft:leather"}},
        "katana_handle"))
    write_json(recipes / "spear_handle.json", shaped(
        ["  S", " X ", "S  "],
        {"S": {"item": "minecraft:stick"}, "X": item_ref("katana_handle")},
        "spear_handle"))
    write_json(recipes / "knive_handle.json", shaped(
        [" X ", " SX", "X  "],
        {"S": {"item": "minecraft:stick"}, "X": nugget},
        "knive_handle"))
    write_json(recipes / "katana.json", shaped(
        ["  S", "GS ", "WG "],
        {"S": steel, "G": gold, "W": item_ref("katana_handle")},
        "katana"))
    write_json(recipes / "wakizashi.json", shaped(
        ["  S", "GS ", "WG "],
        {"S": steel, "G": gold_nugget, "W": item_ref("katana_handle")},
        "wakizashi"))
    write_json(recipes / "naginata.json", shaped(
        ["  S", "GS ", "WG "],
        {"S": steel, "G": gold, "W": item_ref("spear_handle")},
        "naginata"))
    write_json(recipes / "odachi.json", shaped(
        ["  S", " K ", "S  "],
        {"S": steel, "K": item_ref("katana")},
        "odachi"))
    write_json(recipes / "nagamaki.json", shaped(
        ["  G", " S ", "W  "],
        {"S": item_ref("katana"), "G": nugget, "W": item_ref("spear_handle")},
        "nagamaki"))
    write_json(recipes / "kamayari.json", shaped(
        [" GS", " SG", "W  "],
        {"S": steel, "G": nugget, "W": item_ref("katana_handle")},
        "kamayari"))
    write_json(recipes / "tonbukiri.json", shaped(
        ["  S", "SS ", "WS "],
        {"S": steel, "W": item_ref("katana_handle")},
        "tonbukiri"))
    write_json(recipes / "tetsubo.json", shaped(
        [" SS", "GSS", "WG "],
        {"S": steel, "G": gold, "W": item_ref("katana_handle")},
        "tetsubo"))
    write_json(recipes / "kunai.json", shaped(
        ["S", "W", "N"],
        {"S": steel, "N": nugget, "W": item_ref("knive_handle")},
        "kunai"))
    write_json(recipes / "kama.json", shaped(
        ["SN", "W "],
        {"S": steel, "N": nugget, "W": item_ref("knive_handle")},
        "kama"))
    write_json(recipes / "sai.json", shaped(
        ["N", "S", "W"],
        {"S": steel, "N": nugget, "W": item_ref("knive_handle")},
        "sai"))
    write_json(recipes / "shuko.json", shaped(
        ["I I", "S S", "GWG"],
        {"S": steel, "G": gold, "I": nugget, "W": item_ref("katana_handle")},
        "shuko"))
    write_json(recipes / "masakari.json", shaped(
        ["SS", "NS", "W "],
        {"S": steel, "N": nugget, "W": item_ref("knive_handle")},
        "masakari"))
    write_json(recipes / "kabutowari.json", shaped(
        [" N", "NS", " W"],
        {"S": steel, "N": nugget, "W": item_ref("knive_handle")},
        "kabutowari"))
    write_json(recipes / "shuriken.json", shaped(
        [" S ", "S S", " S "], {"S": steel}, "shuriken", count=8))
    write_json(recipes / "yumi.json", shaped(
        [" N ", "LBL", " N "],
        {"B": {"item": "minecraft:bow"}, "L": {"item": "minecraft:leather"}, "N": steel},
        "yumi"))
    write_json(recipes / "straw_hat.json", shaped(
        [" W ", "WWW", "L L"],
        {"W": {"item": "minecraft:wheat"}, "L": {"item": "minecraft:leather"}},
        "straw_hat"))
    write_json(recipes / "kimono.json", shaped(
        ["X X", "XXX", "XXX"], {"X": item_ref("cloth")}, "kimono"))
    write_json(recipes / "steel_samurai_helmet.json", shaped(
        [" G ", "XGX", "X X"],
        {"X": steel, "G": gold},
        "steel_samurai_helmet", category="equipment"))
    write_json(recipes / "steel_samurai_chestplate.json", shaped(
        ["X X", "XGX", "XXX"],
        {"X": steel, "G": {"item": "minecraft:iron_ingot"}},
        "steel_samurai_chestplate", category="equipment"))
    write_json(recipes / "steel_samurai_leggings.json", shaped(
        ["XGX", "X X", "X X"],
        {"X": steel, "G": {"item": "minecraft:iron_ingot"}},
        "steel_samurai_leggings", category="equipment"))
    write_json(recipes / "steel_samurai_boots.json", shaped(
        ["XGX", "X X"],
        {"X": steel, "G": {"item": "minecraft:iron_ingot"}},
        "steel_samurai_boots", category="equipment"))
    write_json(recipes / "light_samurai_helmet.json", shaped(
        [" L ", "LWL", " L "],
        {"W": item_ref("straw_hat"), "L": steel},
        "light_samurai_helmet"))
    write_json(recipes / "light_samurai_chestplate.json", shaped(
        ["X X", "XGX", "XGX"],
        {"X": steel, "G": item_ref("cloth")},
        "light_samurai_chestplate", category="equipment"))
    write_json(recipes / "light_samurai_leggings.json", shaped(
        ["XGX", "G G", "G G"],
        {"X": steel, "G": item_ref("cloth")},
        "light_samurai_leggings", category="equipment"))
    write_json(recipes / "light_samurai_boots.json", shaped(
        ["XGX", "X X"],
        {"X": steel, "G": item_ref("cloth")},
        "light_samurai_boots", category="equipment"))
    write_json(recipes / "steel_ninja_helmet.json", shaped(
        ["XXX", "S S"],
        {"X": steel, "S": item_ref("cloth")},
        "steel_ninja_helmet"))
    write_json(recipes / "steel_ninja_chestplate.json", shaped(
        ["X X", "SXS", "XXX"],
        {"X": steel, "S": item_ref("cloth")},
        "steel_ninja_chestplate"))
    write_json(recipes / "ninja_leggings.json", shaped(
        ["XXX", "X X", "X X"], {"X": item_ref("cloth")}, "ninja_leggings"))
    write_json(recipes / "steel_ninja_boots.json", shaped(
        ["S S", "X X"],
        {"X": steel, "S": item_ref("cloth")},
        "steel_ninja_boots"))

    for base, result in SMITHING:
        write_json(recipes / f"{result}_smithing.json", {
            "type": "minecraft:smithing_transform",
            "addition": {"item": "minecraft:netherite_ingot"},
            "base": item_ref(base),
            "result": {"id": f"{NS}:{result}"},
            "template": {"item": "minecraft:netherite_upgrade_smithing_template"},
        })

    write_json(DATA / "c/tags/item/ingots/steel.json", {"replace": False, "values": [f"{NS}:steel_ingot"]})
    write_json(DATA / "c/tags/item/nuggets/steel.json", {"replace": False, "values": [f"{NS}:steel_nugget"]})
    write_json(DATA / "minecraft/tags/item/swords.json", {"replace": False, "values": [f"{NS}:{n}" for n in SWORDS]})
    write_json(DATA / "minecraft/tags/item/dyeable.json", {
        "replace": False,
        "values": [
            f"{NS}:steel_samurai_helmet",
            f"{NS}:steel_samurai_chestplate",
            f"{NS}:steel_samurai_leggings",
            f"{NS}:steel_samurai_boots",
        ],
    })

    en = {"itemGroup.nomosiki-work.main": "Nomosiki-work"}
    ru = {"itemGroup.nomosiki-work.main": "Nomosiki-work"}
    en["tag.item.c.ingots.steel"] = "Steel Ingots"
    en["tag.item.c.nuggets.steel"] = "Steel Nuggets"
    ru["tag.item.c.ingots.steel"] = "Стальные слитки"
    ru["tag.item.c.nuggets.steel"] = "Стальные самородки"
    for name, (_c, en_name, ru_name) in ITEMS.items():
        if name.startswith("yumi_pulling"):
            continue
        en[f"item.{NS}.{name}"] = en_name
        ru[f"item.{NS}.{name}"] = ru_name
    write_json(ASSETS / "lang/en_us.json", en)
    write_json(ASSETS / "lang/ru_ru.json", ru)
    print("generated recipes, tags, and lang")


if __name__ == "__main__":
    main()
