"""Mode emblems rendered from the same ceramic skins and licensed Noto Sans SC."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'materials'
meta=json.loads((OUT/'materials.json').read_text('utf-8'))
FONT=str(ROOT.parents[1]/'app/src/main/assets/ceramic/NotoSansSC-VF.ttf')
assert Path(FONT).is_file(), 'Noto Sans SC commercial font is required'
def font(sz):
 f=ImageFont.truetype(FONT,sz);f.set_variation_by_axes([800]);return f

def skin(name,w,h):
 m=meta[name]; raw=Image.open(OUT/m['file']).convert('RGBA');x,y,bw,bh=m['body'];sx=w/bw;sy=h/bh
 return raw.resize((round(raw.width*sx),round(raw.height*sy)),Image.Resampling.LANCZOS),(round(x*sx),round(y*sy))
def place(im,name,x,y,w,h):
 tile,(px,py)=skin(name,w,h);im.alpha_composite(tile,(round(x-px),round(y-py)))
def tile(im,name,num,x,y,w=100,angle=0):
 s,off=skin(name,w,w); d=ImageDraw.Draw(s)
 if num:
  ink='white' if name in ['tile-teal','tile-deep'] else '#173D4C'
  sz=round(w*(.56 if len(str(num))<3 else .36));d.text((off[0]+w/2,off[1]+w/2-2),str(num),font=font(sz),anchor='mm',fill=ink)
 if name=='tile-stone':
  ox,oy=off;d.line([(ox+8,oy+w-10),(ox+w*.45,oy+w*.5),(ox+w*.65,oy+w*.45),(ox+w-5,oy+6)],fill='#425E68',width=2)
  d.line([(ox+w*.47,oy+w*.5),(ox+w*.45,oy+12)],fill='#CBDCE0',width=2)
 if angle:
  s=s.rotate(angle,Image.Resampling.BICUBIC,expand=True)
  im.alpha_composite(s,(round(x+w/2-s.width/2),round(y+w/2-s.height/2)))
 else: im.alpha_composite(s,(round(x-off[0]),round(y-off[1])))

for mode in range(5):
 im=Image.new('RGBA',(300,300))
 if mode==0:
  place(im,'tray-light',26,26,248,246)
  vals=[2,4,0,0,16,8,0,32,256]
  for i,v in enumerate(vals):
   x=39+i%3*77;y=39+i//3*77
   if v: tile(im,{2:'tile-ivory',4:'tile-sand',8:'tile-orange',16:'tile-gold',32:'tile-orange',256:'tile-gold'}[v],v,x,y,69)
   else:place(im,'hole-light',x,y,69,69)
 elif mode==1:
  tile(im,'tile-gold',512,107,33,126,-12)
  tile(im,'tile-ivory',128,29,125,128,13)
  tile(im,'tile-orange',64,146,147,126,-11)
 elif mode==2:
  place(im,'tray-light',25,32,250,238)
  for i,(n,v) in enumerate([('tile-ivory',4),('tile-stone',0),('tile-orange',8),('tile-orange',16)]):tile(im,n,v,40+i%2*117,47+i//2*111,105)
 elif mode==3:
  d=ImageDraw.Draw(im)
  d.arc((76,36,231,173),start=190,end=345,fill='#42A5B3',width=12)
  d.polygon([(228,74),(229,111),(191,98)],fill='#2693A3')
  tile(im,'tile-ivory',2,18,133,123,12)
  tile(im,'tile-gold',3,144,119,126,-6)
 else:
  place(im,'tile-ivory',40,72,220,202)
  d=ImageDraw.Draw(im)
  d.rounded_rectangle((42,52,258,125),radius=35,fill='#1D8A99',outline='#105866',width=3)
  d.rectangle((43,94,257,123),fill='#1D8A99')
  d.line((57,62,240,62),fill='#71C4CB',width=3)
  for rx in [93,211]:
   d.rounded_rectangle((rx-13,29,rx+13,85),radius=12,fill='#DCC89A',outline='#F4EACA',width=3)
  for i in range(6):
   x=76+i%3*50;y=145+i//3*48
   d.rounded_rectangle((x,y,x+38,y+36),radius=7,fill='#F3B84C' if i==5 else '#B4C8CC')
 name='mode-icon-'+str(mode);im.save(OUT/(name+'.png'),optimize=True)
 meta[name]={'file':name+'.png','width':300,'height':300,'body':[0,0,300,300],'kind':'static-emblem','font':'Noto Sans SC; SIL OFL 1.1'}
(OUT/'materials.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),'utf-8')
print('Five differentiated ceramic mode emblems rendered.')
