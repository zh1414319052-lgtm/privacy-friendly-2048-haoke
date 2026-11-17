"""Mechanical Android density/mask export for the generated ceramic icon.
Artwork is unchanged; Android foreground has a 108dp canvas and <=66dp main silhouette.
Numeral reference and optional monochrome layer use Noto Sans SC Black, SIL OFL 1.1.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/"art/app-icon"
RES=ROOT/"app/src/main/res"
master=Image.open(ART/"ceramic-2048.png").convert("RGBA")
foreground=Image.new("RGBA",(1080,1080))
scaled=master.resize((680,680),Image.Resampling.LANCZOS)
foreground.alpha_composite(scaled,(200,200))
foreground.save(RES/"drawable-nodpi/ceramic_launcher.png",optimize=True)
background="#154B56"
canvas=Image.new("RGBA",(1080,1080),background)
canvas.alpha_composite(foreground)
# Android renders the centre 72dp of the 108dp adaptive layers.
viewport=canvas.crop((180,180,900,900))
rounded=Image.new("L",(720,720))
ImageDraw.Draw(rounded).rounded_rectangle((0,0,719,719),radius=166,fill=255)
legacy=viewport.copy();legacy.putalpha(rounded)
legacy.save(ART/"app-logo.png",optimize=True)
legacy.save(ROOT/"app/src/main/assets/ceramic/app-logo.png",optimize=True)
for density,size in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
    legacy.resize((size,size),Image.Resampling.LANCZOS).save(RES/f"mipmap-{density}/ic_launcher.png",optimize=True)
    splash=RES/f"mipmap-{density}/ic_splash.png"
    with Image.open(splash) as old: splash_size=old.size
    legacy.resize(splash_size,Image.Resampling.LANCZOS).save(splash,optimize=True)
# Original licensed numeral glyphs, without generative glyph changes, for themed launchers.
reference=Image.open(ART/"numerals-noto-reference.png").convert("RGBA")
mono=Image.new("RGBA",(1080,1080))
glyphs=reference.resize((630,630),Image.Resampling.LANCZOS)
mono.alpha_composite(glyphs,(225,225))
mono.save(RES/"drawable-nodpi/ceramic_launcher_monochrome.png",optimize=True)
# Diagnostic preview only; shipped artwork is the full layer, not these masks.
preview=Image.new("RGBA",(920,360),"#E9EFED")
font_path=ROOT/"app/src/main/assets/ceramic/NotoSansSC-VF.ttf"
assert font_path.is_file(),"Commercial font Noto Sans SC required"
font=ImageFont.truetype(str(font_path),22);font.set_variation_by_axes([650])
draw=ImageDraw.Draw(preview)
for i,(label,radius) in enumerate([("圆角桌面",54),("圆形桌面",128),("小尺寸 48px",54)]):
    size=256 if i<2 else 48
    tile=viewport.resize((size,size),Image.Resampling.LANCZOS)
    mask=Image.new("L",(size,size));d=ImageDraw.Draw(mask)
    if i==1:d.ellipse((0,0,size-1,size-1),fill=255)
    else:d.rounded_rectangle((0,0,size-1,size-1),radius=round(radius*size/256),fill=255)
    tile.putalpha(mask)
    x=28+i*300+(256-size)//2;y=24+(256-size)//2
    preview.alpha_composite(tile,(x,y))
    draw.text((28+i*300+128,306),label,fill="#173D4C",font=font,anchor="mm")
preview.convert("RGB").save(ART/"android-icon-preview.png",optimize=True)
(ART/"export-metadata.json").write_text(json.dumps({"adaptiveCanvasDp":108,"sourceScaleDp":68,"viewportDp":72,"foregroundImage":"ceramic-2048.png","font":"Noto Sans SC Black, SIL OFL 1.1","background":background},indent=2),encoding="utf-8")
print("Exported adaptive, monochrome, five legacy densities and mask preview.")
