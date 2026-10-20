from PIL import Image
from pathlib import Path
res=Path('V2rayNG/app/src/main/res')
im=Image.open('design/icon/ff-approved.png').convert('RGBA')
# Asset packaging only: preserve the approved artwork, scale into Android's adaptive safe zone.
for density,scale in [('mdpi',1),('hdpi',1.5),('xhdpi',2),('xxhdpi',3),('xxxhdpi',4)]:
    d=res/f'mipmap-{density}'; d.mkdir(exist_ok=True)
    size=round(48*scale)
    icon=im.resize((size,size),Image.Resampling.LANCZOS)
    icon.save(d/'ic_launcher.png'); icon.save(d/'ic_launcher_round.png')
    size=round(108*scale); art=im.resize((round(72*scale),round(72*scale)),Image.Resampling.LANCZOS)
    fg=Image.new('RGBA',(size,size),'white'); fg.alpha_composite(art,((size-art.width)//2,(size-art.height)//2)); fg.save(d/'ic_launcher_foreground.png')
# Android notification/themed icons use only the approved mark's silhouette, without its white canvas.
small=im.resize((432,432),Image.Resampling.LANCZOS)
alpha=Image.new('L',small.size) # System icons require a monochrome alpha mask.
pixels=list(small.get_flattened_data()); alpha.putdata([max(0,min(255,(b-r-12)*5)) for r,g,b,a in pixels])
mark=Image.new('RGBA',small.size,'white'); mark.putalpha(alpha)
bounds=mark.getbbox(); mark=mark.crop(bounds)
for density,scale in [('mdpi',1),('hdpi',1.5),('xhdpi',2),('xxhdpi',3),('xxxhdpi',4)]:
    size=round(24*scale); m=mark.copy(); m.thumbnail((round(20*scale),round(20*scale)),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(size,size)); canvas.alpha_composite(m,((size-m.width)//2,(size-m.height)//2))
    d=res/f'drawable-{density}'; d.mkdir(exist_ok=True)
    for name in ['ic_stat_name','ic_stat_proxy','ic_stat_direct']: canvas.save(d/(name+'.png'))
    black=canvas.copy(); black.paste((0,0,0,255),(0,0,size,size),canvas.getchannel('A')); black.putalpha(canvas.getchannel('A')); black.save(d/'ic_stat_name_black.png')
    d=res/f'mipmap-{density}'; size=round(108*scale); m=mark.copy(); m.thumbnail((round(48*scale),round(48*scale)),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(size,size)); canvas.alpha_composite(m,((size-m.width)//2,(size-m.height)//2)); canvas.save(d/'ic_launcher_monochrome.png')
for p in (res/'mipmap-anydpi-v26').glob('ic_launcher*.xml'):
    p.write_text(p.read_text().replace('<monochrome android:drawable="@mipmap/ic_launcher_foreground"','<monochrome android:drawable="@mipmap/ic_launcher_monochrome"'))
