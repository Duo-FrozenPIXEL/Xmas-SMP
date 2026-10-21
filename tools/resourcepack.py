#!/usr/bin/env python3
"""Validiert und baut das Xmas-SMP-Ressourcenpaket.

    python3 tools/resourcepack.py validate   # Referenzen prüfen
    python3 tools/resourcepack.py build      # prüfen + build/Xmas-SMP-resourcepack.zip erzeugen
"""
import hashlib
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACK = ROOT / "resourcepack"
OUT = ROOT / "build" / "Xmas-SMP-resourcepack.zip"

# Vanilla-Parents dürfen nur aus diesen Ordnern kommen ("generated" allein ist ungültig).
VANILLA_PARENT_PREFIXES = ("item/", "block/", "builtin/")


def split_id(ref, default_ns="minecraft"):
    ns, _, path = ref.rpartition(":")
    return (ns or default_ns), path


def load_json(path, errors):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (ValueError, UnicodeDecodeError) as e:
        errors.append(f"{path.relative_to(PACK)}: ungültiges JSON ({e})")
        return None


def walk_item_models(node):
    """Liefert alle (model-id, threshold) aus einer Item-Definition."""
    if isinstance(node, dict):
        if node.get("type", "").removeprefix("minecraft:") == "model" and "model" in node:
            yield node["model"]
        for value in node.values():
            yield from walk_item_models(value)
    elif isinstance(node, list):
        for value in node:
            yield from walk_item_models(value)


def validate():
    errors, warnings = [], []
    assets = PACK / "assets"

    meta = load_json(PACK / "pack.mcmeta", errors) or {}
    pack = meta.get("pack", {})
    if "min_format" not in pack or "max_format" not in pack:
        errors.append("pack.mcmeta: min_format/max_format fehlen (ab 1.21.9 Pflicht)")

    def model_file(ref):
        ns, path = split_id(ref)
        return assets / ns / "models" / f"{path}.json"

    def texture_file(ref):
        ns, path = split_id(ref)
        return assets / ns / "textures" / f"{path}.png"

    used_textures = set()

    for item_def in sorted(assets.glob("*/items/*")):
        rel = item_def.relative_to(PACK)
        if item_def.suffix != ".json":
            warnings.append(f"{rel}: keine .json-Datei, wird vom Spiel ignoriert")
            continue
        data = load_json(item_def, errors)
        if data is None:
            continue
        for ref in walk_item_models(data):
            ns, path = split_id(ref)
            if model_file(ref).exists():
                continue
            # Vanilla-Modelle liegen nicht im Pack; erlaubt ist nur das eigene Basis-Item.
            if ns == "minecraft" and path == f"item/{item_def.stem}":
                continue
            errors.append(f"{rel}: Modell {ref} existiert nicht")
        entries = data.get("model", {}).get("entries", [])
        thresholds = [e.get("threshold") for e in entries]
        if len(thresholds) != len(set(thresholds)):
            errors.append(f"{rel}: doppelte Schwellenwerte")
        if thresholds != sorted(thresholds):
            warnings.append(f"{rel}: Schwellenwerte nicht aufsteigend sortiert")

    for model in sorted(assets.glob("*/models/**/*.json")):
        rel = model.relative_to(PACK)
        data = load_json(model, errors)
        if data is None:
            continue
        parent = data.get("parent")
        if parent:
            ns, path = split_id(parent)
            if not model_file(parent).exists() and not (
                ns == "minecraft" and path.startswith(VANILLA_PARENT_PREFIXES)
            ):
                errors.append(f"{rel}: Parent {parent} existiert nicht")
        for ref in data.get("textures", {}).values():
            if ref.startswith("#"):
                continue
            ns, path = split_id(ref)
            if ns == "minecraft" and not texture_file(ref).exists():
                continue  # Vanilla-Textur
            used_textures.add(texture_file(ref))
            if not texture_file(ref).exists():
                errors.append(f"{rel}: Textur {ref} existiert nicht")

    for font in sorted(assets.glob("*/font/*.json")):
        data = load_json(font, errors) or {}
        for provider in data.get("providers", []):
            if "file" in provider:
                ns, path = split_id(provider["file"])
                tex = assets / ns / "textures" / path
                used_textures.add(tex)
                if not tex.exists():
                    errors.append(f"{font.relative_to(PACK)}: Font-Textur {provider['file']} existiert nicht")

    for tex in sorted(assets.glob("*/textures/**/*.png")):
        if tex not in used_textures:
            warnings.append(f"{tex.relative_to(PACK)}: wird nicht verwendet")

    # Alle Sprachen eines Namespaces müssen dieselben Schlüssel haben (de_de ist die Referenz).
    for lang_dir in sorted(assets.glob("*/lang")):
        langs = {f.stem: load_json(f, errors) for f in sorted(lang_dir.glob("*.json"))}
        reference = langs.get("de_de")
        if reference is None:
            errors.append(f"{lang_dir.relative_to(PACK)}: de_de.json fehlt")
            continue
        for name, data in langs.items():
            if data is None or name == "de_de":
                continue
            rel = lang_dir.relative_to(PACK) / f"{name}.json"
            for key in sorted(reference.keys() - data.keys()):
                errors.append(f"{rel}: Schlüssel fehlt: {key}")
            for key in sorted(data.keys() - reference.keys()):
                warnings.append(f"{rel}: Schlüssel nicht in de_de.json: {key}")

    for w in warnings:
        print(f"WARNUNG {w}")
    for e in errors:
        print(f"FEHLER  {e}")
    print(f"{len(errors)} Fehler, {len(warnings)} Warnungen")
    return not errors


def build():
    if not validate():
        sys.exit(1)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    files = sorted(p for p in PACK.rglob("*") if p.is_file())
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as zf:
        for f in files:
            # Fester Zeitstempel -> gleicher Inhalt ergibt gleichen SHA-1 (Server-Resourcepack-Hash).
            info = zipfile.ZipInfo(f.relative_to(PACK).as_posix(), date_time=(2025, 12, 24, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            zf.writestr(info, f.read_bytes())
    sha1 = hashlib.sha1(OUT.read_bytes()).hexdigest()
    print(f"{OUT.relative_to(ROOT)} ({len(files)} Dateien) sha1={sha1}")


if __name__ == "__main__":
    command = sys.argv[1] if len(sys.argv) > 1 else "validate"
    if command == "build":
        build()
    elif command == "validate":
        sys.exit(0 if validate() else 1)
    else:
        sys.exit(__doc__)
