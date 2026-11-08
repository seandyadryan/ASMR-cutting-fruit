"""English Play Store art composed from real app captures, plus a 1080p promo MP4.
Usage: python tools/create_store_assets.py [--no-video]
Requires Pillow, NumPy, imageio-ffmpeg. No network calls.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import subprocess
import sys
import json
import wave
import numpy as np
import imageio_ffmpeg

ROOT = Path(__file__).resolve().parents[1]
STORE = ROOT/'store'
CREAM = '#F7F4ED'; GREEN = '#254D3E'; MUTED = '#71796D'
FONT = ROOT/'app/src/main/res/font'
CAPTIONS = [
    ('studio', 'A little slice\nof quiet.', 'Fresh produce. Gentle sounds. Your pace.'),
    ('cutting', 'Find your\nrhythm.', 'Swipe, slice, and enjoy the little details.'),
    ('pantry', 'Fresh things.\nLovely textures.', '12 fruits and vegetables, ready to slice.'),
    ('focus', 'One quiet\nminute.', 'Take a gentle 60-second slicing break.'),
    ('journal', 'Small moments,\nwell spent.', 'Keep your own little Journal of slices.'),
    ('daily', 'A fresh little\nritual.', 'Five ingredients. A new basket each day.'),
]

def font(size, serif=False):
    return ImageFont.truetype(str(FONT/('lora.ttf' if serif else 'dm_sans.ttf')),size)

def tracked(draw, pos, text, size=18, fill=GREEN, tracking=4):
    x,y = pos; f=font(size)
    for char in text:
        draw.text((x,y),char,font=f,fill=fill); x += draw.textlength(char,font=f)+tracking

def device(canvas, screen, box, tablet=False):
    x,y,w,h=map(int,box)
    border=max(10,int(w*.027)); radius=int(w*.085) if not tablet else int(w*.045)
    scale=min((w-2*border)/screen.width,(h-2*border)/screen.height)
    sw,sh=round(screen.width*scale),round(screen.height*scale)
    actual_w,actual_h=sw+2*border,sh+2*border
    x+=(w-actual_w)//2; y+=(h-actual_h)//2; w,h=actual_w,actual_h
    shadow=Image.new('RGBA',canvas.size)
    sd=ImageDraw.Draw(shadow); sd.rounded_rectangle((x+6,y+20,x+w+6,y+h+20),radius,fill=(25,42,31,65))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(18)))
    d=ImageDraw.Draw(canvas)
    d.rounded_rectangle((x,y,x+w,y+h),radius,fill='#19251E',outline='#465148',width=3)
    screen=screen.convert('RGB').resize((sw,sh),Image.Resampling.LANCZOS)
    mask=Image.new('L',(sw,sh)); ImageDraw.Draw(mask).rounded_rectangle((0,0,sw,sh),max(1,radius-border),fill=255)
    canvas.paste(screen,(x+border,y+border),mask)
    if not tablet:
        d=ImageDraw.Draw(canvas)
        d.ellipse((x+w//2-8,y+border+12,x+w//2+8,y+border+28),fill='#19251E')
    return canvas

def base(size):
    im=Image.new('RGBA',size,CREAM)
    d=ImageDraw.Draw(im)
    d.ellipse((size[0]*.48,size[1]*.37,size[0]*1.5,size[1]*1.1),fill='#EBEBDD')
    return im

def phone_cards():
    folder=STORE/'screenshots/phone'; folder.mkdir(parents=True,exist_ok=True)
    for i,(name,title,subtitle) in enumerate(CAPTIONS):
        im=base((1080,1920)); d=ImageDraw.Draw(im)
        tracked(d,(76,62),'ASMR CUTTING FRUIT',18)
        d.multiline_text((72,118),title,font=font(76,True),fill=GREEN,spacing=2)
        d.text((76,324),subtitle,font=font(25),fill=MUTED)
        screen=Image.open(STORE/f'raw/phone-{name}.png')
        device(im,screen,(219,426,642,1428))
        d=ImageDraw.Draw(im); tracked(d,(76,1865),f'{i+1:02d}   /   THE QUIET KITCHEN',14,tracking=2)
        im.convert('RGB').save(folder/f'{i+1:02d}-{name}.png',optimize=True)

def large_cards(kind, tablet=True):
    folder=STORE/f'screenshots/{kind}'; folder.mkdir(parents=True,exist_ok=True)
    for i,(name,title,subtitle) in enumerate(CAPTIONS[:4]):
        im=base((1920,1080)); d=ImageDraw.Draw(im)
        tracked(d,(90,80),'ASMR CUTTING FRUIT',18)
        d.multiline_text((84,255),title,font=font(76,True),fill=GREEN,spacing=8)
        # Keep the caption readable in a deliberately narrow editorial column.
        words=subtitle.split(); lines=[]; line=''
        for word in words:
            if d.textlength(line+' '+word,font=font(25)) > 540: lines.append(line); line=word
            else: line=(line+' '+word).strip()
        lines.append(line)
        d.multiline_text((90,475),'\n'.join(lines),font=font(25),fill=MUTED,spacing=10)
        tracked(d,(90,943),'OFFLINE BY NATURE',15,tracking=3)
        screen=Image.open(STORE/f'raw/{kind}-{name}.png')
        if tablet:
            ratio=screen.width/screen.height
            w=1060; h=int(w/ratio)
            if h>820: h=820; w=int(h*ratio)
            device(im,screen,(760+(1080-w)//2,(1080-h)//2,w,h),True)
        else:
            # Desktop frame represents a real, wide Android emulator capture.
            device(im,screen,(730,188,1110,670),True)
            d=ImageDraw.Draw(im); d.rounded_rectangle((1210,846,1360,920),8,fill='#34483A'); d.rounded_rectangle((1085,918,1485,940),10,fill='#34483A')
        im.convert('RGB').save(folder/f'{i+1:02d}-{name}.png',optimize=True)

def feature():
    folder=STORE/'feature'; folder.mkdir(exist_ok=True)
    im=Image.new('RGB',(1024,500),GREEN); d=ImageDraw.Draw(im)
    d.text((48,44),'ASMR CUTTING FRUIT',font=font(17),fill='#E4DFC9')
    d.multiline_text((44,111),'A little slice\nof quiet.',font=font(61,True),fill=CREAM,spacing=5)
    d.text((48,306),'Fresh produce. Gentle sounds.',font=font(20),fill='#D0DAC7')
    d.text((48,337),'Your pace.',font=font(20),fill='#D0DAC7')
    d.text((48,437),'FRUIT & VEGETABLE SLICING',font=font(13),fill='#D0DAC7')
    hero=Image.open(ROOT/'art/source/kitchen-hero.png').convert('RGB')
    # A contained portrait crop leaves the whole title clearly readable.
    hero=hero.resize((720,480),Image.Resampling.LANCZOS).crop((110,0,610,480))
    mask=Image.new('L',(500,480)); ImageDraw.Draw(mask).rounded_rectangle((0,0,499,479),28,fill=255)
    im.paste(hero,(524,10),mask)
    im.save(folder/'feature-graphic-1024x500.png',optimize=True)

def video():
    folder=STORE/'video'; folder.mkdir(exist_ok=True)
    ffmpeg=imageio_ffmpeg.get_ffmpeg_exe()
    width,height=1920,1080; fps=30; duration=24
    args=[ffmpeg,'-y','-f','rawvideo','-vcodec','rawvideo','-pix_fmt','rgb24','-s',f'{width}x{height}','-r',str(fps),'-i','-', '-an','-c:v','libx264','-preset','fast','-crf','20','-pix_fmt','yuv420p','-movflags','+faststart',str(folder/'visual.mp4')]
    proc=subprocess.Popen(args,stdin=subprocess.PIPE,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
    # Real device captures and real screenrecord frames form the visual content.
    cards=[]
    scenes=[('studio','A little slice\nof quiet.','Fresh produce. Gentle sounds. Your pace.'),('cutting','Find your\nrhythm.','Satisfying slices. A softer kind of play.'),('pantry','Fresh things.\nLovely textures.','12 ingredients. Always ready to slice.'),('focus','Make a moment\nfor yourself.','Offline. No ads. Your own little ritual.')]
    for name,title,note in scenes:
        im=base((width,height)); d=ImageDraw.Draw(im)
        tracked(d,(110,115),'ASMR CUTTING FRUIT',20)
        d.multiline_text((103,292),title,font=font(88,True),fill=GREEN,spacing=8)
        d.text((110,560),note,font=font(25),fill=MUTED)
        tracked(d,(110,925),'THE QUIET KITCHEN',15)
        screen=Image.open(STORE/f'raw/phone-{name}.png'); device(im,screen,(1240,66,425,946))
        cards.append(im.convert('RGB'))
    # Decode the captured gameplay (if present) and place it inside the device.
    clip=STORE/'raw/gameplay.mp4'; clip_frames=[]
    if clip.exists():
        reader=imageio_ffmpeg.read_frames(str(clip),pix_fmt='rgb24',output_params=['-vf','fps=15,scale=360:800'])
        info=next(reader)
        for data in reader:
            clip_frames.append(Image.frombytes('RGB',(360,800),data))
            if len(clip_frames)>=180: break
        reader.close()
    for frame in range(fps*duration):
        scene=min(3,frame//(fps*6)); current=cards[scene].copy()
        if scene==1 and clip_frames:
            current=current.convert('RGBA')
            live=clip_frames[min(len(clip_frames)-1,int((frame-fps*6)/fps*15))]
            device(current,live,(1240,66,425,946)); current=current.convert('RGB')
        # A short restrained dissolve at each scene change.
        within=frame%(fps*6)
        if scene>0 and within<12: current=Image.blend(cards[scene-1],current,within/12)
        proc.stdin.write(current.tobytes())
    proc.stdin.close(); error=proc.stderr.read(); code=proc.wait()
    if code: raise RuntimeError(error.decode(errors='replace')[-2000:])
    sr=44100; sound=np.zeros(sr*duration,dtype=np.float32)
    with wave.open(str(ROOT/'app/src/main/res/raw/ambience.wav'),'rb') as f: room=np.frombuffer(f.readframes(f.getnframes()),dtype='<i2').astype(np.float32)/32767
    sound+=np.resize(room,len(sound))*.10
    with wave.open(str(ROOT/'app/src/main/res/raw/soft.wav'),'rb') as f: cut=np.frombuffer(f.readframes(f.getnframes()),dtype='<i2').astype(np.float32)/32767
    for second in [6.35,7.0,8.0,9.0,10.0,11.0]:
        start=int(second*sr); sound[start:start+len(cut)]+=cut*.8
    sound[:sr]*=np.linspace(0,1,sr); sound[-sr:]*=np.linspace(1,0,sr)
    audio=folder/'promo-audio.wav'
    with wave.open(str(audio),'wb') as f:
        f.setnchannels(1); f.setsampwidth(2); f.setframerate(sr); f.writeframes((np.clip(sound,-.95,.95)*32767).astype('<i2').tobytes())
    subprocess.run([ffmpeg,'-y','-i',str(folder/'visual.mp4'),'-i',str(audio),'-c:v','copy','-c:a','aac','-b:a','192k','-shortest','-movflags','+faststart',str(folder/'asmr-cutting-fruit-promo-1080p.mp4')],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    (folder/'visual.mp4').unlink(); audio.unlink()

def validate():
    entries=[]
    for path in sorted(STORE.rglob('*.png')):
        if 'raw' in path.parts: continue
        with Image.open(path) as im:
            w,h=im.size
            assert path.stat().st_size<8*1024*1024 or 'feature' in path.parts
            if 'screenshots' in path.parts: assert (w*9==h*16 or w*16==h*9),path
            entries.append({'file':str(path.relative_to(ROOT)).replace('\\','/'),'width':w,'height':h,'bytes':path.stat().st_size})
    short=(STORE/'short-description.txt').read_text().strip(); full=(STORE/'full-description.txt').read_text().strip()
    assert len(short)<=80 and len(full)<=4000
    (STORE/'asset-manifest.json').write_text(json.dumps({'short_description_characters':len(short),'full_description_characters':len(full),'images':entries},indent=2))
    print(f'Validated {len(entries)} store images; descriptions {len(short)}/80 and {len(full)}/4000 characters.')

def preview():
    sheet=Image.new('RGB',(1080,1280),CREAM)
    for i,path in enumerate(sorted((STORE/'screenshots/phone').glob('*.png'))):
        sheet.paste(Image.open(path).resize((360,640),Image.Resampling.LANCZOS),((i%3)*360,(i//3)*640))
    sheet.save(STORE/'preview.jpg',quality=92)

if __name__=='__main__':
    phone_cards()
    for name in ['tablet-7','tablet-10','desktop']: large_cards(name,name!='desktop')
    feature()
    preview()
    if '--no-video' not in sys.argv: video()
    validate()
