"""Prepare game resources from the original generated artwork; synthesize original audio.
Requires Pillow and NumPy. Source artwork and prompts are retained in art/.
"""
from pathlib import Path
from PIL import Image
import numpy as np
import wave
import math

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
NAMES = ['watermelon','kiwi','orange','apple','strawberry','dragonfruit','cucumber','carrot','tomato','avocado','lemon','eggplant']

def ingredient_bounds(cell):
    """Find the main sprite's bounds so neighboring atlas-edge fragments are not exported."""
    mask=np.array(cell.getchannel('A'))>128
    best=[]; height,width=mask.shape
    for y,x in zip(*np.where(mask)):
        if not mask[y,x]: continue
        stack=[(int(x),int(y))]; mask[y,x]=False; group=[]
        while stack:
            px,py=stack.pop(); group.append((px,py))
            for nx,ny in [(px-1,py),(px+1,py),(px,py-1),(px,py+1)]:
                if 0<=nx<width and 0<=ny<height and mask[ny,nx]:
                    mask[ny,nx]=False; stack.append((nx,ny))
        if len(group)>len(best): best=group
    if not best: return None
    xs,ys=zip(*best)
    return max(0,min(xs)-1),max(0,min(ys)-1),min(width,max(xs)+2),min(height,max(ys)+2)

def images():
    (ASSETS / 'produce').mkdir(parents=True, exist_ok=True)
    atlas = Image.open(ROOT / 'art/source/produce-atlas.png').convert('RGBA')
    for i, name in enumerate(NAMES):
        x, y = i % 4, i // 4
        cell = atlas.crop((x*atlas.width//4,y*atlas.height//3,(x+1)*atlas.width//4,(y+1)*atlas.height//3))
        # Crop empty atlas margins; keep a square around the complete ingredient.
        bbox = ingredient_bounds(cell)
        if bbox:
            cell = cell.crop(bbox)
        side = max(cell.size)
        sprite = Image.new('RGBA',(side,side))
        sprite.alpha_composite(cell,((side-cell.width)//2,(side-cell.height)//2))
        sprite.resize((512,512),Image.Resampling.LANCZOS).save(ASSETS / f'produce/{name}.png', optimize=True)
    hero = Image.open(ROOT / 'art/source/kitchen-hero.png').convert('RGB')
    hero.resize((1200,800),Image.Resampling.LANCZOS).save(ASSETS / 'kitchen-hero.webp', quality=90)
    icon_path = ROOT / 'art/source/app-icon.png'
    if icon_path.exists():
        icon = Image.open(icon_path).convert('RGB')
        (ROOT/'store/icon').mkdir(parents=True,exist_ok=True)
        icon.resize((512,512),Image.Resampling.LANCZOS).save(ROOT/'store/icon/app-icon-512.png')
        for density, size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
            folder = ROOT/f'app/src/main/res/mipmap-{density}'
            folder.mkdir(parents=True,exist_ok=True)
            for name in ['ic_launcher','ic_launcher_round']:
                icon.resize((size,size),Image.Resampling.LANCZOS).save(folder/f'{name}.png')
        folder = ROOT/'app/src/main/res/drawable'
        folder.mkdir(exist_ok=True)
        icon.resize((432,432),Image.Resampling.LANCZOS).save(folder/'icon_foreground.png')
        folder = ROOT/'app/src/main/res/mipmap-anydpi-v26'
        folder.mkdir(exist_ok=True)
        xml = '<?xml version="1.0" encoding="utf-8"?><adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/icon_bg"/><foreground android:drawable="@drawable/icon_foreground"/></adaptive-icon>'
        for name in ['ic_launcher','ic_launcher_round']:
            (folder/f'{name}.xml').write_text(xml)
        (ROOT/'app/src/main/res/values/colors.xml').write_text('<resources><color name="icon_bg">#254D3E</color></resources>')

def save_wav(path, data, sr=44100):
    data = np.clip(data,-.95,.95)
    with wave.open(str(path),'wb') as out:
        out.setnchannels(1); out.setsampwidth(2); out.setframerate(sr)
        out.writeframes((data*32767).astype('<i2').tobytes())

def audio():
    raw = ROOT/'app/src/main/res/raw'; raw.mkdir(parents=True,exist_ok=True)
    rng = np.random.default_rng(248)
    sr = 44100
    for name, duration, crunch, softness in [('juicy',.42,22,11),('soft',.38,8,25),('crisp',.34,42,4)]:
        t = np.arange(int(sr*duration))/sr
        noise = rng.normal(0,1,len(t))
        low = np.convolve(noise,np.ones(softness)/softness,'same')
        env = (1-np.exp(-t*220))*np.exp(-t*15)
        data = low*env*.95
        for _ in range(crunch):
            start = int(rng.uniform(.018,.17)*sr)
            length = min(int(.015*sr), len(t)-start)
            kt = np.arange(length)/sr
            burst = rng.normal(0,.12,length)*np.exp(-kt*350)
            data[start:start+length] += burst
        data += .17*np.sin(2*math.pi*(135*t-80*t*t))*np.exp(-t*45)
        data *= np.minimum(1,(duration-t)*200)
        save_wav(raw/f'{name}.wav',data)
    # Seamless, low-level filtered room texture. No microphone recordings or samples.
    n = sr*4
    noise = rng.normal(0,1,n)
    spectrum = np.fft.rfft(noise)
    frequency = np.fft.rfftfreq(n,1/sr)
    spectrum *= np.exp(-frequency/650)/np.maximum(frequency,45)**.35
    room = np.fft.irfft(spectrum,n)
    room = room/max(abs(room))*.18
    save_wav(raw/'ambience.wav',room)
    print('Prepared 12 sprites, hero, icon densities, and 4 original audio assets.')

if __name__ == '__main__':
    images(); audio()
