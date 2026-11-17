"""Produce the app's Noto Sans SC regular/bold subsets (SIL OFL 1.1)."""
from pathlib import Path
import sys
from fontTools.subset import Options, Subsetter
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

res = Path(__file__).resolve().parents[1] / "app/src/main/res"
text = "".join(chr(i) for i in range(32, 127)) + "•×…“”‘’–—"
for qualifier in ("values", "values-zh"):
    for name in ("strings.xml", "challenge_strings.xml"):
        text += (res / qualifier / name).read_text(encoding="utf-8")
options = Options()
options.name_IDs = ["*"]
weights = {"regular": 400, "bold": 700}
for name, weight in weights.items():
    font = instantiateVariableFont(TTFont(sys.argv[1]), {"wght": weight}, updateFontNames=True)
    subsetter = Subsetter(options=options)
    subsetter.populate(text=text)
    subsetter.subset(font)
    font.save(res / "font" / ("board_sans_" + name + ".ttf"))
