"""Verify the timer correction and refresh Focus captures on the dedicated QA emulator."""
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from android_device import adb, ROOT, PACKAGE, ADB
from capture_native import export
import subprocess
import time

def instrument(arguments, name):
    result=subprocess.run([str(ADB),'shell','am','instrument','-w',*arguments,PACKAGE+'.test/androidx.test.runner.AndroidJUnitRunner'],capture_output=True,timeout=300)
    output=result.stdout.decode(errors='replace')+result.stderr.decode(errors='replace')
    output=output.replace('\r','').strip()+'\n'
    (ROOT/f'output/qa/{name}.txt').write_text(output)
    print(name,output,flush=True)
    if 'OK (1 test)' not in output: raise RuntimeError('Final Focus verification failed')

def run():
    for _ in range(80):
        try:
            if adb('shell','getprop','sys.boot_completed')=='1': break
        except subprocess.SubprocessError: pass
        time.sleep(3)
    if not adb('emu','avd','name').startswith('ASMR_Cutting_QA'): raise RuntimeError('Dedicated QA emulator required')
    adb('uninstall',PACKAGE)
    adb('install',str(ROOT/'app/build/outputs/apk/debug/app-debug.apk'))
    adb('install','-r',str(ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'))
    instrument(['-e','class',PACKAGE+'.StudioFlowTest#focusPausesInBackgroundAndFinishesAtExpiry'],'focus-lifecycle-final')
    for kind,size,density in [('phone','1080x2400',420),('tablet-7','1280x800',213),('tablet-10','1920x1200',240),('desktop','1920x1080',160)]:
        adb('shell','wm','size',size); adb('shell','wm','density',density)
        instrument(['-e','class',PACKAGE+'.StoreCaptureTest','-e','screen','focus','-e','captureKind',kind],'focus-capture-'+kind)
        export()
    adb('shell','wm','size','reset'); adb('shell','wm','density','reset')
    from smoke_release import run as smoke
    smoke()
    adb('emu','kill')

if __name__=='__main__': run()
