"""Rasterize the native exported shoe mesh for the VPS catalogue and compact wardrobe preview."""
from pathlib import Path
import json, math
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
mesh=ROOT.parent/'axialclientwebsite/img/store/models/black_sneakers.json'
vertices=np.array(json.loads(mesh.read_text())['vertices']).reshape((-1,12))
yaw=3.49;pitch=-.22
ry=np.array([[math.cos(yaw),0,math.sin(yaw)],[0,1,0],[-math.sin(yaw),0,math.cos(yaw)]])
rx=np.array([[1,0,0],[0,math.cos(pitch),-math.sin(pitch)],[0,math.sin(pitch),math.cos(pitch)]])
rotation=rx@ry
positions=vertices[:,:3]@rotation.T
lo=positions.min(axis=0);hi=positions.max(axis=0);center=(lo+hi)/2
scale=min(220/(hi[0]-lo[0]),210/(hi[1]-lo[1]))
positions=(positions-center)*scale
texture=Image.open(ROOT/'src/main/resources/assets/axial_cosmetics/textures/entity/accessories/black_sneakers.png').convert('RGBA')
canvas=Image.new('RGBA',(256,256));depth=np.full((256,256),-np.inf)
light=np.array([-.4,.8,.65]);light/=np.linalg.norm(light)
for i in range(0,len(vertices),3):
 pts=positions[i:i+3].copy();pts[:,0]+=128;pts[:,1]=128-pts[:,1]
 a,b,c=pts;uv=vertices[i:i+3,3:5]*32
 den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
 if abs(den)<1e-8:continue
 normal=rotation@vertices[i,5:8];shade=.72+.28*max(float(normal@light),0)
 for y in range(max(0,int(min(pts[:,1]))),min(255,int(max(pts[:,1])))+1):
  for x in range(max(0,int(min(pts[:,0]))),min(255,int(max(pts[:,0])))+1):
   aa=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den
   bb=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den;cc=1-aa-bb
   if min(aa,bb,cc)<-1e-6:continue
   z=aa*a[2]+bb*b[2]+cc*c[2]
   if z<=depth[y,x]:continue
   u,v=aa*uv[0]+bb*uv[1]+cc*uv[2];color=texture.getpixel((max(0,min(31,int(u))),max(0,min(31,int(v)))))
   if color[3]==0:continue
   canvas.putpixel((x,y),tuple(round(k*shade) for k in color[:3])+(color[3],));depth[y,x]=z
canvas=canvas.resize((128,128),Image.Resampling.LANCZOS)
for dest in ['src/main/resources/assets/axial_cosmetics/textures/gui/black_sneakers_preview.png','backend/cosmetics/public/assets/black_sneakers.png']:
 canvas.save(ROOT/dest)
