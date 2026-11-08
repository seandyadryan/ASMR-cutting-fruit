"""Shared Android SDK helpers for local verification and native asset capture."""
from pathlib import Path
import os
import subprocess
import time

ROOT=Path(__file__).resolve().parents[1]
ADB=Path(os.getenv('ANDROID_HOME',str(Path.home()/'AppData/Local/Android/Sdk')))/'platform-tools'/('adb.exe' if os.name=='nt' else 'adb')
PACKAGE='com.deploydulupulangnanti.asmrcuttingfruit'
RAW=ROOT/'store/raw'
RAW.mkdir(parents=True,exist_ok=True)

def adb(*args,binary=False):
    result=subprocess.run([str(ADB),*map(str,args)],capture_output=True,timeout=45,check=True)
    return result.stdout if binary else result.stdout.decode(errors='replace').replace('\r','').strip()

def launch():
    adb('shell','am','force-stop',PACKAGE)
    adb('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
    time.sleep(2)
