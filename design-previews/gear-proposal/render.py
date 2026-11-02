from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math
ROOT=Path(__file__).resolve().parent
S=16
mask=Image.new('L',(24*S,24*S));d=ImageDraw.Draw(mask)
d.ellipse((4.7*S,4.7*S,19.3*S,19.3*S),fill=255)
for i in range(8):
 a=i*math.pi/4
 points=[]
 for r,t in [(6.9,-.23),(10.5,-.145),(10.5,.145),(6.9,.23)]:
  points.append(((12+math.cos(a+t)*r)*S,(12+math.sin(a+t)*r)*S))
 d.polygon(points,fill=255)
d.ellipse((8.6*S,8.6*S,15.4*S,15.4*S),fill=0)
new=Image.new('RGBA',mask.size,'white');new.putalpha(mask)
new.resize((128,128),Image.Resampling.LANCZOS).save(ROOT/'proposed-gear.png')
old=Image.open('src/main/resources/assets/axial_cosmetics/textures/gui/sprites/mods/gear.png').convert('RGBA')
canvas=Image.new('RGB',(960,640),'#14171b');draw=ImageDraw.Draw(canvas)
font='/System/Library/Fonts/Supplemental/Arial.ttf'
def f(size): return ImageFont.truetype(font,size)
def text(x,y,t,size=18,color='#f0f2f3'): draw.text((x,y),t,font=f(size),fill=color)
def icon(img,x,y,size,color='#f0f2f3'):
 alpha=img.getchannel('A').resize((size,size),Image.Resampling.LANCZOS)
 tile=Image.new('RGB',(size,size),color);canvas.paste(tile,(x,y),alpha)
text(40,28,'GEAR ICON PROPOSAL',28)
text(40,69,'Solid body · eight broad teeth · small centre opening',17,'#a0a7ae')
for x,title,img in [(40,'CURRENT',old),(500,'PROPOSED',new)]:
 draw.rounded_rectangle((x,112,x+420,365),radius=12,fill='#202429',outline='#454c53')
 text(x+20,130,title,17,'#a0a7ae')
 icon(img,x+135,175,150)
text(40,390,'IN THE MENU',17,'#a0a7ae')
for x,label,size in [(40,'Mods',24),(355,'Settings',20),(670,'Small / 16 px',16)]:
 draw.rounded_rectangle((x,430,x+270,490),radius=7,fill='#2b323a',outline='#7c858e')
 icon(new,x+18,460-size//2,size,'#c78df5' if label=='Mods' else '#f0f2f3')
 text(x+58,449,label,19)
text(40,534,'A heavier silhouette that stays readable at small sizes.',19)
text(40,574,'Preview only — the client icon has not been changed.',16,'#a0a7ae')
canvas.save(ROOT/'gear-proposal.png')
