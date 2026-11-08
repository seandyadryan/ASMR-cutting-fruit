"""Package signed app outputs and the finished Play Store upload kit, excluding credentials."""
from pathlib import Path
import shutil
import hashlib
import zipfile
import json

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/delivery'

def run():
    OUT.mkdir(parents=True,exist_ok=True)
    outputs={
        'ASMR-Cutting-Fruit-1.0.0.apk':ROOT/'app/build/outputs/apk/release/app-release.apk',
        'ASMR-Cutting-Fruit-1.0.0.aab':ROOT/'app/build/outputs/bundle/release/app-release.aab',
        'mapping-1.0.0.txt':ROOT/'app/build/outputs/mapping/release/mapping.txt',
    }
    for name,path in outputs.items(): shutil.copy2(path,OUT/name)
    kit=OUT/'ASMR-Cutting-Fruit-Play-Store-Kit.zip'
    with zipfile.ZipFile(kit,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
        for path in sorted((ROOT/'store').rglob('*')):
            if path.is_file() and 'raw' not in path.relative_to(ROOT/'store').parts:
                archive.write(path,path.relative_to(ROOT))
        archive.write(ROOT/'docs/index.html','privacy-policy/index.html')
        archive.write(ROOT/'art/prompts.md','art/prompts.md')
        archive.write(ROOT/'art/README.md','art/README.md')
    files=sorted(p for p in OUT.iterdir() if p.suffix in ['.apk','.aab','.zip','.txt'])
    (OUT/'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in files))
    print(json.dumps({p.name:p.stat().st_size for p in files},indent=2))

if __name__=='__main__': run()
