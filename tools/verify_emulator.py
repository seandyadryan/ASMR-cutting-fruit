"""Wait for the project's Android test emulator and run on-device flow tests."""
from pathlib import Path
import subprocess
import time
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from android_device import adb, launch, PACKAGE, ADB, ROOT

def run():
    ready=False
    for attempt in range(80):
        try:
            if adb('shell','getprop','sys.boot_completed')=='1': ready=True; break
        except (subprocess.SubprocessError, OSError): pass
        if attempt%6==0: print('Waiting for Android first boot...',flush=True)
        time.sleep(3)
    if not ready: raise RuntimeError('Android did not finish booting')
    adb('shell','input','keyevent','82')
    adb('install','-r',str(ROOT/'app/build/outputs/apk/debug/app-debug.apk'))
    adb('install','-r',str(ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'))
    adb('logcat','-c')
    result=subprocess.run([str(ADB),'shell','am','instrument','-w','-e','class',PACKAGE+'.StudioFlowTest',PACKAGE+'.test/androidx.test.runner.AndroidJUnitRunner'],capture_output=True,timeout=300)
    output=result.stdout.decode(errors='replace')+result.stderr.decode(errors='replace')
    output=output.replace('\r','').strip()+'\n'
    (ROOT/'output/qa/instrumentation-results.txt').write_text(output)
    print(output,flush=True)
    if 'OK (3 tests)' not in output: raise RuntimeError('On-device flow tests failed')
    crash=adb('logcat','-d','-s','AndroidRuntime:E')
    (ROOT/'output/qa/runtime-crash-check.txt').write_text(crash)
    if 'FATAL EXCEPTION' in crash: raise RuntimeError('Runtime crash found')
    launch(); print('On-device flow tests passed, with no app runtime crashes.',flush=True)

if __name__=='__main__': run()
