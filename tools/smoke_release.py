"""Install the signed release only on the dedicated QA emulator; resets its test app data."""
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from android_device import adb, ROOT, PACKAGE
import time

def run():
    if adb('shell','getprop','ro.kernel.qemu')!='1' or not adb('emu','avd','name').startswith('ASMR_Cutting_QA'):
        raise RuntimeError('This reset/install check requires the dedicated ASMR_Cutting_QA emulator')
    lines=[]
    lines.append(adb('uninstall',PACKAGE))
    lines.append(adb('uninstall',PACKAGE+'.test'))
    lines.append(adb('install',str(ROOT/'app/build/outputs/apk/release/app-release.apk')))
    adb('logcat','-c')
    lines.append(adb('shell','am','start','-W','-n',PACKAGE+'/.MainActivity'))
    time.sleep(4)
    pid=adb('shell','pidof',PACKAGE)
    crash=adb('logcat','-d','-s','AndroidRuntime:E')
    if not pid or 'FATAL EXCEPTION' in crash: raise RuntimeError('Signed release launch failed: '+crash)
    (ROOT/'output/qa/release-home.png').write_bytes(adb('exec-out','screencap','-p',binary=True))
    lines.extend(['Release process running: '+pid,crash,'PASS: signed, minified release APK installed and launched.'])
    (ROOT/'output/qa/release-smoke-results.txt').write_text('\n'.join(lines))
    print(lines[-1],flush=True)

if __name__=='__main__': run()
