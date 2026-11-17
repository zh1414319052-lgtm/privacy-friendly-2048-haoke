"""Deterministic ceramic UI skins; original references are read-only visual targets.
No page screenshot is used as an interactive background. Text stays editable.
All text uses Noto Sans SC (SIL OFL 1.1).
"""
from pathlib import Path
import json, math
import numpy as np
from PIL import Image, ImageFilter

ROOT=Path(__file__).resolve().parent
OUT=ROOT/'materials'; OUT.mkdir(exist_ok=True)

def rgb(c): return np.array([int(c[i:i+2],16) for i in (1,3,5)],dtype=np.float32)
def sdf(xx,yy,x,y,w,h,r):
    dx=np.abs(xx-(x+w/2))-(w/2-r); dy=np.abs(yy-(y+h/2))-(h/2-r)
    return np.sqrt(np.maximum(dx,0)**2+np.maximum(dy,0)**2)+np.minimum(np.maximum(dx,dy),0)-r

def composite(dest,col,alpha):
    da=dest[:,:,3:4]/255; sa=alpha[:,:,None]
    oa=sa+da*(1-sa)
    dest[:,:,:3]=np.divide(col*sa+dest[:,:,:3]*da*(1-sa),oa,out=np.zeros_like(dest[:,:,:3]),where=oa>1e-6)
    dest[:,:,3:4]=oa*255

MANIFEST={}
def surface(name,w,h,color,r=24,depth=8,pad=28,kind='raised',shadow=.23,bevel=5):
    W=w+2*pad; H=h+depth+2*pad
    yy,xx=np.mgrid[:H,:W].astype(np.float32)
    dist=sdf(xx+.5,yy+.5,pad,pad,w,h,r)
    shape=np.clip(.5-dist,0,1)
    dest=np.zeros((H,W,4),dtype=np.float32)
    if kind!='inset' and shadow:
        mask=Image.fromarray((shape*255).astype('uint8'))
        soft=np.asarray(mask.filter(ImageFilter.GaussianBlur(max(3,pad*.24))),dtype=np.float32)/255
        soft=np.roll(soft,round(depth+pad*.16),axis=0)*shadow
        composite(dest,rgb('#183E4A'),soft)
        contact=np.asarray(mask.filter(ImageFilter.GaussianBlur(max(1,depth*.3))),dtype=np.float32)/255
        composite(dest,rgb('#194552'),np.roll(contact,depth+2,axis=0)*shadow*.6)
    base=rgb(color)
    v=np.clip((yy-pad)/h,0,1); u=np.clip((xx-pad)/w,0,1)
    if kind!='inset' and depth:
        side_d=sdf(xx+.5,yy+.5,pad,pad+depth,w,h,r)
        side_mask=np.clip(.5-side_d,0,1)
        side_level=.93 if 'card' in name else (.82 if base.mean()>150 else .73)
        side=np.clip(base*(side_level+.045*(1-v[:,:,None])),0,255)
        composite(dest,side,side_mask)
    ny,nx=np.gradient(dist)
    light=-.62*nx-.78*ny
    edge=np.exp(-np.maximum(-dist,0)/bevel)
    if kind=='inset':
        col=base+((1-v)*4+(1-u)*2)[:,:,None]
        blurred=np.asarray(Image.fromarray((shape*255).astype('uint8')).filter(ImageFilter.GaussianBlur(9)),dtype=np.float32)/255
        inset_shadow=(1-np.roll(blurred,(7,6),axis=(0,1)))*64
        inset_light=(1-np.roll(blurred,(-3,-3),axis=(0,1)))*31
        rim=17*np.maximum(-light,0)*np.exp(-((dist+1.2)/2.1)**2)
        col=col+(-inset_shadow+inset_light+rim)[:,:,None]
    else:
        col=base+((.5-v)*9+(.5-u)*4)[:,:,None]
        col=col+(33*np.maximum(light,0)-22*np.maximum(-light,0))[:,:,None]*edge[:,:,None]
        highlight=(18*np.maximum(light,0)+4)*np.exp(-((dist+1.8)/1.4)**2)
        col=col+highlight[:,:,None]
        if kind=='gloss':
            glow=14*np.exp(-((v-.08)/.07)**2)*(1-u*.5)
            col+=glow[:,:,None]
    composite(dest,np.clip(col,0,255),shape)
    im=Image.fromarray(np.round(np.clip(dest,0,255)).astype('uint8'),'RGBA')
    path=OUT/(name+'.png'); im.save(path,optimize=True)
    MANIFEST[name]={'file':path.name,'width':W,'height':H,'body':[pad,pad,w,h],
                    'radius':r,'depth':depth,'kind':kind,'base':color,'bevel':bevel}

surface('tile-ivory',200,200,'#ECECEB',r=25,depth=8,kind='gloss',shadow=.30,bevel=9)
surface('tile-warm-ivory',200,200,'#F1ECE2',r=25,depth=8,kind='gloss',shadow=.30,bevel=9)
surface('tile-ice',200,200,'#B7E5ED',r=25,depth=8,kind='gloss',shadow=.33)
surface('tile-cyan',200,200,'#A3DEEA',r=25,depth=9,kind='gloss',shadow=.34)
surface('tile-teal',200,200,'#2E86A0',r=25,depth=9,kind='gloss',shadow=.38)
surface('tile-deep',200,200,'#164E63',r=25,depth=9,kind='gloss',shadow=.38)
surface('tile-sand',200,200,'#EDD8B3',r=25,depth=9,kind='gloss',shadow=.32)
surface('tile-gold',200,200,'#F3C068',r=25,depth=9,kind='gloss',shadow=.34)
surface('tile-orange',200,200,'#E5A054',r=25,depth=9,kind='gloss',shadow=.33)
surface('tile-stone',200,200,'#82949B',r=21,depth=8,kind='gloss',shadow=.35)
surface('hole-light',200,200,'#B5D1DD',r=24,depth=0,pad=28,kind='inset',bevel=10)
surface('hole-teal',200,200,'#6595A4',r=24,depth=0,pad=28,kind='inset',bevel=10)
surface('hole-dark',200,200,'#203F50',r=24,depth=0,pad=28,kind='inset',bevel=10)
surface('tray-light',827,846,'#ACCCD9',r=46,depth=22,pad=34,kind='gloss',shadow=.30,bevel=12)
surface('tray-teal',826,846,'#4FA0AE',r=46,depth=22,pad=34,kind='gloss',shadow=.34,bevel=12)
surface('well-teal',786,806,'#3D7F92',r=30,depth=0,pad=28,kind='inset',bevel=10)
surface('tray-dark',620,620,'#316174',r=34,depth=14,pad=26,kind='gloss',shadow=.44,bevel=10)
surface('nav-primary',200,94,'#123D50',r=46,depth=6,pad=20,kind='gloss',shadow=.28,bevel=6)
surface('nav-light',200,94,'#E8F5F8',r=46,depth=6,pad=20,kind='gloss',shadow=.22,bevel=6)
surface('primary',800,124,'#103C50',r=34,depth=7,pad=25,kind='gloss',shadow=.27,bevel=5)
surface('accent',395,112,'#218CA2',r=29,depth=8,pad=24,kind='gloss',shadow=.26,bevel=5)
surface('disabled',395,112,'#BACCD4',r=29,depth=8,pad=24,shadow=.20,bevel=3)
surface('secondary',395,112,'#E5F1F5',r=29,depth=7,pad=24,kind='gloss',shadow=.21,bevel=4)
surface('mode-card',802,245,'#EFF8FC',r=36,depth=4,pad=25,shadow=.22,bevel=3)
surface('level-card',805,130,'#EFF8FC',r=28,depth=4,pad=20,shadow=.19,bevel=3)
surface('stat',263,130,'#18566E',r=24,depth=0,pad=8,shadow=.07,bevel=2)
surface('stat-dark',260,130,'#213E50',r=24,depth=0,pad=8,shadow=.07,bevel=2)
surface('dialog',790,470,'#EAF5F9',r=44,depth=12,pad=40,shadow=.30,bevel=5)
(OUT/'materials.json').write_text(json.dumps(MANIFEST,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'skins':len(MANIFEST),'output':str(OUT)},ensure_ascii=False))
