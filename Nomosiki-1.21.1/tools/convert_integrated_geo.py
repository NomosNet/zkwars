#!/usr/bin/env python3
"""Convert Bedrock geometry.* blocks to minecraft:geometry 1.12.0 for GeckoLib."""
import json
import sys
from pathlib import Path

def convert_legacy_geo(data: dict) -> dict:
    keys = [k for k in data if k.startswith("geometry.")]
    if not keys:
        return data
    geometries = []
    for key in keys:
        ident = key.replace("geometry.", "geometry.", 1) if key.startswith("geometry.") else key
        ident = "geometry." + key.split(".", 1)[1]
        body = data[key]
        desc = {
            "identifier": ident,
            "texture_width": body.get("texturewidth", body.get("texture_width", 64)),
            "texture_height": body.get("textureheight", body.get("texture_height", 64)),
        }
        if "visible_bounds_width" in body:
            desc["visible_bounds_width"] = body["visible_bounds_width"]
        if "visible_bounds_height" in body:
            desc["visible_bounds_height"] = body["visible_bounds_height"]
        if "visible_bounds_offset" in body:
            desc["visible_bounds_offset"] = body["visible_bounds_offset"]
        geometries.append({"description": desc, "bones": body.get("bones", [])})
    return {"format_version": "1.12.0", "minecraft:geometry": geometries}


def main() -> None:
    root = Path(__file__).resolve().parents[1] / "src/main/resources/assets/nomosiki/geo/entity"
    for name in ("centaur_beastman.geo.json", "arachne_undead.geo.json"):
        path = root / name
        data = json.loads(path.read_text(encoding="utf-8"))
        if "minecraft:geometry" in data:
            print(f"skip (already 1.12.0): {name}")
            continue
        out = convert_legacy_geo(data)
        path.write_text(json.dumps(out, indent="\t", ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"converted: {name}")


if __name__ == "__main__":
    main()
