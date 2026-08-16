#!/usr/bin/env python3
"""Transpile an existing CQ weights bundle into a runnable Cactus bundle.

Reads a directory containing config.txt + *.weights (a CQ weights bundle),
runs the transpiler to produce components/manifest.json + runtime_plan.json,
and writes the runnable bundle to the output directory.
"""
import argparse
import shutil
import sys
from pathlib import Path


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("weights_dir", help="Directory with config.txt + *.weights")
    ap.add_argument("output_dir", help="Where to write the runnable bundle")
    ap.add_argument("--model-id", default="Cactus-Compute/gemma-3-270m-it",
                    help="Model id used for the transpile profile")
    ap.add_argument("--source-model", default=None,
                    help="Original HF model id the converter loads for graph capture "
                         "(defaults to --model-id). Use the stock google/... id when the "
                         "Cactus repo only ships CQ weights.")
    args = ap.parse_args()

    weights_dir = Path(args.weights_dir).expanduser()
    output_dir = Path(args.output_dir).expanduser()

    if not (weights_dir / "config.txt").exists():
        print(f"error: {weights_dir}/config.txt not found", file=sys.stderr)
        return 1

    # Import the transpiler from the cactus package
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from cactus.cli.transpiler import build_transpiled_bundle

    print(f"Transpiling {args.model_id} from {weights_dir} -> {output_dir}")
    source_model = args.source_model or args.model_id
    try:
        build_transpiled_bundle(
            source_model,  # converter loads this for graph capture
            weights_dir=weights_dir,
            output_dir=output_dir,
            profile_model_id=args.model_id,
        )
    except Exception as exc:
        print(f"error: transpile failed: {exc}", file=sys.stderr)
        return 1

    manifest = output_dir / "components" / "manifest.json"
    runtime_plan = output_dir / "runtime_plan.json"
    if not manifest.exists():
        print("error: components/manifest.json was not generated", file=sys.stderr)
        return 1
    print(f"✓ manifest.json: {manifest}")
    if runtime_plan.exists():
        print(f"✓ runtime_plan.json: {runtime_plan}")
    print(f"Runnable bundle ready at {output_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
