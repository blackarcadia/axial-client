from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math
ROOT=Path(__file__).resolve().parent
S=16
mask=Image.new('L',(24*S,24*S));d=ImageDraw.Draw(mask)
# Four independent solid arrows, with an open central gap.
arrow=[(12,1.5),(16.5,6.5),(13.4,6.5),(13.4,9.2),(10.6,9.2),(10.6,6.5),(7.5,6.5)]
for i in range(4):
 a=i*math.pi/2
 points=[]
 for x,y in arrow:
  x,y=x-12,y-12
  points.append(((12+x*math.cos(a)-y*math.sin(a))*S,(12+x*math.sin(a)+y*math.cos(a))*S))
 d.polygon(points,fill=255)
new=Image.new('RGBA',mask.size,'white');new.putalpha(mask)
new.resize((128,128),Image.Resampling.LANCZOS).save(ROOT/'proposed-move.png')
old=Image.open('src/main/resources/assets/axial_cosmetics/textures/gui/sprites/mods/move.png').convert('RGBA')
canvas=Image.new('RGB',(960,640),'#14171b');draw=ImageDraw.Draw(canvas)
font='/System/Library/Fonts/Supplemental/Arial.ttf'
def f(size): return ImageFont.truetype(font,size)
def text(x,y,t,size=18,color='#f0f2f3'): draw.text((x,y),t,font=f(size),fill=color)
def icon(img,x,y,size,color='#f0f2f3'):
 alpha=img.getchannel('A').resize((size,size),Image.Resampling.LANCZOS)
 tile=Image.new('RGB',(size,size),color);canvas.paste(tile,(x,y),alpha)
text(40,28,'MOVE HUD ICON PROPOSAL',28)
text(40,69,'Four separate arrows · clear centre gap · bolder silhouettes',17,'#a0a7ae')
for x,title,img in [(40,'CURRENT',old),(500,'PROPOSED',new)]:
 draw.rounded_rectangle((x,112,x+420,365),radius=12,fill='#202429',outline='#454c53')
 text(x+20,130,title,17,'#a0a7ae')
 icon(img,x+135,175,150)
text(40,390,'IN THE MENU',17,'#a0a7ae')
for x,label,size in [(40,'Move HUD',24),(355,'Edit HUD',20),(670,'Small / 16 px',16)]:
 draw.rounded_rectangle((x,430,x+270,490),radius=7,fill='#2b323a',outline='#7c858e')
 icon(new,x+18,460-size//2,size,'#c78df5' if label=='Move HUD' else '#f0f2f3')
 text(x+58,449,label,19)
text(40,534,'Four distinct directions, readable even at small menu sizes.',19)
text(40,574,'Preview only — the client icon has not been changed.',16,'#a0a7ae')
canvas.save(ROOT/'move-hud-proposal.png')
