#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Sanitize or restore Java/JDK11 source files from a YAML dictionary.

Examples
--------
Sanitize files in place:
    python java_sanitizer.py sanitize -c sanitization-aviation.yaml src/**/*.java --in-place

Restore files into another folder:
    python java_sanitizer.py restore -c sanitization-aviation.yaml src/**/*.java -o restored_sources

Dry run:
    python java_sanitizer.py sanitize -c sanitization-aviation.yaml MyClass.java --dry-run

Also rename filenames:
    python java_sanitizer.py sanitize -c sanitization-aviation.yaml src/**/*.java --in-place --rename-files
"""

from __future__ import annotations

import argparse
import difflib
import re
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Tuple

try:
    import yaml
except ImportError as exc:
    raise SystemExit(
        "PyYAML est requis. Installe-le avec : pip install pyyaml"
    ) from exc


@dataclass(frozen=True)
class ReplacementPlan:
    name: str
    mapping: Dict[str, str]

    @property
    def reverse_mapping(self) -> Dict[str, str]:
        reverse: Dict[str, str] = {}
        conflicts: List[Tuple[str, str, str]] = []

        for original, sanitized in self.mapping.items():
            if sanitized in reverse and reverse[sanitized] != original:
                conflicts.append((sanitized, reverse[sanitized], original))
            reverse[sanitized] = original

        if conflicts:
            details = "\n".join(
                f"  - '{key}' <= '{left}' et '{right}'"
                for key, left, right in conflicts
            )
            raise ValueError(
                "Le dictionnaire n'est pas réversible sans ambiguïté.\n"
                "Plusieurs clés d'origine pointent vers la même valeur cible :\n"
                f"{details}"
            )

        return reverse


def load_plan(config_path: Path, section: str) -> ReplacementPlan:
    with config_path.open("r", encoding="utf-8") as fh:
        data = yaml.safe_load(fh)

    if not isinstance(data, dict):
        raise ValueError("Le YAML doit contenir un objet racine.")

    sanitization = data.get("sanitization")
    if not isinstance(sanitization, dict):
        raise ValueError("Clé 'sanitization' absente ou invalide dans le YAML.")

    selected = sanitization.get(section)
    if not isinstance(selected, dict):
        available = ", ".join(sorted(sanitization.keys())) or "<aucune section>"
        raise ValueError(
            f"Section '{section}' introuvable. Sections disponibles : {available}"
        )

    mapping = selected.get("mapping")
    if not isinstance(mapping, dict) or not mapping:
        raise ValueError(
            f"La section 'sanitization.{section}.mapping' est absente ou vide."
        )

    normalized: Dict[str, str] = {}
    for key, value in mapping.items():
        if not isinstance(key, str) or not isinstance(value, str):
            raise ValueError("Toutes les clés/valeurs du mapping doivent être des chaînes.")
        normalized[key] = value

    return ReplacementPlan(name=section, mapping=normalized)


def build_regex(mapping: Dict[str, str]) -> re.Pattern[str]:
    """
    One-pass regex replacement, longest keys first to avoid partial overlaps.
    """
    keys = sorted(mapping.keys(), key=len, reverse=True)
    pattern = "|".join(re.escape(key) for key in keys)
    if not pattern:
        raise ValueError("Aucune entrée de mapping disponible.")
    return re.compile(pattern)


def transform_text(content: str, mapping: Dict[str, str]) -> Tuple[str, int]:
    regex = build_regex(mapping)
    count = 0

    def repl(match: re.Match[str]) -> str:
        nonlocal count
        count += 1
        return mapping[match.group(0)]

    return regex.sub(repl, content), count


def transform_filename(path: Path, mapping: Dict[str, str]) -> Path:
    new_name, _ = transform_text(path.name, mapping)
    return path.with_name(new_name)


def iter_input_files(inputs: Sequence[str]) -> List[Path]:
    resolved: List[Path] = []
    seen = set()

    for raw in inputs:
        p = Path(raw)

        if any(ch in raw for ch in "*?[]"):
            matches = sorted(Path().glob(raw))
            for match in matches:
                if match.is_file():
                    rp = match.resolve()
                    if rp not in seen:
                        resolved.append(rp)
                        seen.add(rp)
            continue

        if p.is_dir():
            for match in sorted(p.rglob("*.java")):
                rp = match.resolve()
                if rp not in seen:
                    resolved.append(rp)
                    seen.add(rp)
            continue

        if p.is_file():
            rp = p.resolve()
            if rp not in seen:
                resolved.append(rp)
                seen.add(rp)
            continue

        raise FileNotFoundError(f"Entrée introuvable : {raw}")

    if not resolved:
        raise FileNotFoundError("Aucun fichier à traiter.")
    return resolved


def ensure_parent(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def make_backup(path: Path) -> Path:
    backup = path.with_suffix(path.suffix + ".bak")
    index = 1
    while backup.exists():
        backup = path.with_suffix(path.suffix + f".bak{index}")
        index += 1
    shutil.copy2(path, backup)
    return backup


def relative_to_best_effort(path: Path, base: Path) -> Path:
    try:
        return path.relative_to(base)
    except ValueError:
        return Path(path.name)


def compute_destination(
    src: Path,
    output_dir: Path | None,
    rename_files: bool,
    mapping: Dict[str, str],
    root_base: Path,
) -> Path:
    rel = relative_to_best_effort(src, root_base)
    target = rel
    if rename_files:
        target = transform_filename(target, mapping)

    if output_dir is None:
        if rename_files:
            return src.with_name(target.name)
        return src

    return output_dir / target


def show_diff(before: str, after: str, path: Path) -> None:
    diff = difflib.unified_diff(
        before.splitlines(),
        after.splitlines(),
        fromfile=f"{path} (avant)",
        tofile=f"{path} (après)",
        lineterm="",
        n=2,
    )
    for line in diff:
        print(line)


def process_files(
    files: Sequence[Path],
    mapping: Dict[str, str],
    output_dir: Path | None,
    in_place: bool,
    dry_run: bool,
    rename_files: bool,
    make_backups: bool,
    show_diffs: bool,
) -> int:
    root_base = Path.cwd().resolve()
    total_replacements = 0

    for src in files:
        content = src.read_text(encoding="utf-8")
        transformed, replacements = transform_text(content, mapping)
        dest = compute_destination(
            src=src,
            output_dir=output_dir,
            rename_files=rename_files,
            mapping=mapping,
            root_base=root_base,
        )

        if replacements == 0 and dest == src:
            print(f"[SKIP] {src} (aucun remplacement)")
            continue

        total_replacements += replacements

        if show_diffs:
            show_diff(content, transformed, src)

        if dry_run:
            print(f"[DRY] {src} -> {dest} | remplacements={replacements}")
            continue

        if in_place and output_dir is not None:
            raise ValueError("Utilise soit --in-place, soit --output-dir, pas les deux.")

        if not in_place and output_dir is None:
            raise ValueError("Spécifie --in-place ou --output-dir.")

        if in_place:
            if make_backups:
                backup = make_backup(src)
                print(f"[BACKUP] {src} -> {backup}")

            if rename_files and dest != src:
                ensure_parent(dest)
                dest.write_text(transformed, encoding="utf-8")
                src.unlink()
                print(f"[WRITE] {src} -> {dest} | remplacements={replacements}")
            else:
                src.write_text(transformed, encoding="utf-8")
                print(f"[WRITE] {src} | remplacements={replacements}")
        else:
            ensure_parent(dest)
            dest.write_text(transformed, encoding="utf-8")
            print(f"[WRITE] {src} -> {dest} | remplacements={replacements}")

    return total_replacements


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Sanitize ou restore un ou plusieurs fichiers Java via un dictionnaire YAML."
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    def add_common_arguments(sp: argparse.ArgumentParser) -> None:
        sp.add_argument(
            "-c", "--config", required=True, type=Path,
            help="Chemin du fichier YAML de configuration."
        )
        sp.add_argument(
            "-s", "--section", default="aviation",
            help="Section sous sanitization à utiliser. Par défaut: aviation"
        )
        sp.add_argument(
            "inputs", nargs="+",
            help="Fichiers, dossiers et/ou patterns glob à traiter (ex: src/**/*.java)."
        )
        sp.add_argument(
            "--in-place", action="store_true",
            help="Modifie les fichiers directement."
        )
        sp.add_argument(
            "-o", "--output-dir", type=Path,
            help="Dossier de sortie si tu ne veux pas modifier les fichiers en place."
        )
        sp.add_argument(
            "--dry-run", action="store_true",
            help="N'écrit rien, affiche seulement ce qui serait fait."
        )
        sp.add_argument(
            "--rename-files", action="store_true",
            help="Renomme aussi les noms de fichiers si un terme du mapping est présent."
        )
        sp.add_argument(
            "--backup", action="store_true",
            help="Crée un backup .bak avant modification en place."
        )
        sp.add_argument(
            "--diff", action="store_true",
            help="Affiche un diff unifié des changements."
        )

    add_common_arguments(subparsers.add_parser("sanitize", help="Applique les termes sanitizés."))
    add_common_arguments(subparsers.add_parser("restore", help="Restaure les termes originaux."))

    return parser


def validate_args(args: argparse.Namespace) -> None:
    if args.in_place and args.output_dir is not None:
        raise ValueError("Impossible d'utiliser --in-place avec --output-dir.")
    if not args.in_place and args.output_dir is None and not args.dry_run:
        raise ValueError("Il faut choisir --in-place ou --output-dir.")
    if args.backup and not args.in_place:
        raise ValueError("--backup n'a de sens qu'avec --in-place.")


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    try:
        validate_args(args)
        plan = load_plan(args.config, args.section)
        files = iter_input_files(args.inputs)

        if args.command == "sanitize":
            mapping = plan.mapping
        elif args.command == "restore":
            mapping = plan.reverse_mapping
        else:
            raise ValueError(f"Commande inconnue : {args.command}")

        total = process_files(
            files=files,
            mapping=mapping,
            output_dir=args.output_dir,
            in_place=args.in_place,
            dry_run=args.dry_run,
            rename_files=args.rename_files,
            make_backups=args.backup,
            show_diffs=args.diff,
        )

        print(f"\nTerminé. Nombre total de remplacements: {total}")
        return 0

    except Exception as exc:  # pragma: no cover
        print(f"Erreur: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
