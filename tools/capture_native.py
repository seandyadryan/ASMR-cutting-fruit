"""Export real native View captures from the running Android app's instrumentation.
OS status icons and transient system dialogs are not part of native View captures.
"""
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from android_device import adb, ADB, ROOT, PACKAGE, RAW
import subprocess
import tarfile
import io
import imageio_ffmpeg
import time

def export():
    data=adb('exec-out','run-as',PACKAGE,'tar','-cf','-','-C','files/store-captures','.',binary=True)
    with tarfile.open(fileobj=io.BytesIO(data)) as archive:
        for member in archive.getmembers():
            if member.isfile() and Path(member.name).name==member.name.removeprefix('./'):
                (RAW/Path(member.name).name).write_bytes(archive.extractfile(member).read())

def run():
    for _ in range(80):
        try:
            if adb('shell','getprop','sys.boot_completed')=='1': break
        except subprocess.SubprocessError: pass
        time.sleep(3)
    for kind,size,density in [('phone','1080x2400',420),('tablet-7','1280x800',213),('tablet-10','1920x1200',240),('desktop','1920x1080',160)]:
        expected=['studio','cutting','pantry','focus']+(['journal','daily'] if kind=='phone' else [])
        if all((RAW/f'{kind}-{name}.png').exists() for name in expected) and (ROOT/f'output/qa/capture-{kind}.txt').exists() and 'OK (1 test)' in (ROOT/f'output/qa/capture-{kind}.txt').read_text():
            continue
        adb('shell','wm','size',size); adb('shell','wm','density',density)
        result=subprocess.run([str(ADB),'shell','am','instrument','-w','-e','class',PACKAGE+'.StoreCaptureTest','-e','captureKind',kind,PACKAGE+'.test/androidx.test.runner.AndroidJUnitRunner'],capture_output=True,timeout=600)
        output=result.stdout.decode(errors='replace')+result.stderr.decode(errors='replace')
        output=output.replace('\r','').strip()+'\n'
        (ROOT/f'output/qa/capture-{kind}.txt').write_text(output)
        print(kind,output,flush=True)
        if 'OK (1 test)' not in output: raise RuntimeError(f'Capture failed for {kind}')
        export()
    ffmpeg=imageio_ffmpeg.get_ffmpeg_exe()
    subprocess.run([ffmpeg,'-y','-framerate','10','-i',str(RAW/'gameplay-frame-%03d.png'),'-c:v','libx264','-pix_fmt','yuv420p','-crf','20',str(RAW/'gameplay.mp4')],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    for frame in RAW.glob('gameplay-frame-*.png'): frame.unlink()
    adb('shell','wm','size','reset'); adb('shell','wm','density','reset')
    print('Exported 18 native app captures and an actual gameplay frame sequence.',flush=True)

if __name__=='__main__': run()
