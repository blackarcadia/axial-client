"""Render the supplied Swim Bands geometry into the wardrobe and admin thumbnail."""
from pathlib import Path
import json, math
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/axial_cosmetics'
texture=Image.open(ASSETS/'textures/entity/accessories/swim_bands.png').convert('RGBA')
geo=json.loads((ASSETS/'geo/swim_bands.geo.json').read_text())['minecraft:geometry'][0]
canvas=Image.new('RGBA',(256,256));depth=np.full((256,256),-1e9)
# Orthographic three-quarter view, keeping both supplied arm-band cubes visible.
yaw=.38;pitch=.32
rotation=np.array([[math.cos(yaw),0,math.sin(yaw)],[-math.sin(yaw)*math.sin(pitch),math.cos(pitch),math.cos(yaw)*math.sin(pitch)],[-math.sin(yaw)*math.cos(pitch),-math.sin(pitch),math.cos(yaw)*math.cos(pitch)]])
def quad(points,uvs,shade):
    pts=[]
    for p in points:
        q=rotation@np.array([p[0],p[1]-19.5,p[2]])
        pts.append((128+q[0]*10,130-q[1]*10,q[2]))
    for indices in [(0,1,2),(0,2,3)]:
        a,b,c=[pts[i] for i in indices];ua,ub,uc=[uvs[i] for i in indices]
        den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
        if abs(den)<.001:continue
        for y in range(max(0,int(min(a[1],b[1],c[1]))),min(256,int(max(a[1],b[1],c[1]))+1)):
            for x in range(max(0,int(min(a[0],b[0],c[0]))),min(256,int(max(a[0],b[0],c[0]))+1)):
                aa=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den
                bb=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den;cc=1-aa-bb
                if min(aa,bb,cc)<0:continue
                z=aa*a[2]+bb*b[2]+cc*c[2]
                if z<=depth[y,x]:continue
                u=aa*ua[0]+bb*ub[0]+cc*uc[0];v=aa*ua[1]+bb*ub[1]+cc*uc[1]
                color=texture.getpixel((max(0,min(31,int(u))),max(0,min(31,int(v)))))
                if color[3]==0:continue
                canvas.putpixel((x,y),tuple(round(k*shade) for k in color[:3])+(color[3],));depth[y,x]=z
for bone in geo['bones']:
    for cube in bone.get('cubes',[]):
        x,y,z=cube['origin'];w,h,d=cube['size'];u,v=cube['uv']
        faces=[([(x,y,z+d),(x+w,y,z+d),(x+w,y+h,z+d),(x,y+h,z+d)],(u+d+w+d,v+d,u+d+w+d+w,v+d+h),.82),
               ([(x+w,y,z),(x,y,z),(x,y+h,z),(x+w,y+h,z)],(u+d,v+d,u+d+w,v+d+h),1),
               ([(x+w,y,z+d),(x+w,y,z),(x+w,y+h,z),(x+w,y+h,z+d)],(u+d+w,v+d,u+d+w+d,v+d+h),.88),
               ([(x,y,z),(x,y,z+d),(x,y+h,z+d),(x,y+h,z)],(u,v+d,u+d,v+d+h),.92),
               ([(x,y+h,z),(x,y+h,z+d),(x+w,y+h,z+d),(x+w,y+h,z)],(u+d,v,u+d+w,v+d),1),
               ([(x,y,z+d),(x,y,z),(x+w,y,z),(x+w,y,z+d)],(u+d+w,v,u+d+w+w,v+d),.7)]
        for points,(u0,v0,u1,v1),shade in faces:quad(points,[(u0,v1-.001),(u1-.001,v1-.001),(u1-.001,v0),(u0,v0)],shade)
image=canvas.resize((128,128),Image.Resampling.LANCZOS)
image.save(ASSETS/'textures/gui/swim_bands_preview.png')
image.save(ROOT/'backend/cosmetics/public/assets/swim_bands.png')
