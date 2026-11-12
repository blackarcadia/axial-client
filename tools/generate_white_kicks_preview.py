"""Render the supplied sneaker bones/cube rotations using the same transforms as the client."""
from pathlib import Path
import json, math
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/axial_cosmetics'
texture=Image.open(ASSETS/'textures/entity/accessories/white_kicks.png').convert('RGBA')
geo=json.loads((ASSETS/'geo/white_kicks.geo.json').read_text())['minecraft:geometry'][0]
def position(p):return np.array([p[0],24-p[1],p[2]],dtype=float)
def translate(p):
    matrix=np.eye(4);matrix[:3,3]=p;return matrix

def rotation(p):
    x,y,z=np.radians([-p[0],p[1],-p[2]])
    rx=np.array([[1,0,0],[0,math.cos(x),-math.sin(x)],[0,math.sin(x),math.cos(x)]])
    ry=np.array([[math.cos(y),0,math.sin(y)],[0,1,0],[-math.sin(y),0,math.cos(y)]])
    rz=np.array([[math.cos(z),-math.sin(z),0],[math.sin(z),math.cos(z),0],[0,0,1]])
    matrix=np.eye(4);matrix[:3,:3]=rz@ry@rx;return matrix
matrices={};pivots={};faces=[]
for bone in geo['bones']:
    pivot=position(bone.get('pivot',[0,0,0]));parent=bone.get('parent')
    matrix=matrices.get(parent,np.eye(4))@translate(pivot-pivots.get(parent,np.zeros(3)))@rotation(bone.get('rotation',[0,0,0]))
    matrices[bone['name']]=matrix;pivots[bone['name']]=pivot
    for cube in bone.get('cubes',[]):
        cp=cube.get('pivot',bone.get('pivot',[0,0,0]));cm=matrix@translate(position(cp)-pivot)@rotation(cube.get('rotation',[0,0,0]))
        x,y,z=cube['origin'];w,h,d=cube['size'];u,v=cube['uv']
        raw=[([(x,y,z+d),(x+w,y,z+d),(x+w,y+h,z+d),(x,y+h,z+d)],(u+d+w+d,v+d,u+d+w+d+w,v+d+h),.78),
             ([(x+w,y,z),(x,y,z),(x,y+h,z),(x+w,y+h,z)],(u+d,v+d,u+d+w,v+d+h),.96),
             ([(x+w,y,z+d),(x+w,y,z),(x+w,y+h,z),(x+w,y+h,z+d)],(u+d+w,v+d,u+d+w+d,v+d+h),.84),
             ([(x,y,z),(x,y,z+d),(x,y+h,z+d),(x,y+h,z)],(u,v+d,u+d,v+d+h),.9),
             ([(x,y+h,z),(x,y+h,z+d),(x+w,y+h,z+d),(x+w,y+h,z)],(u+d,v,u+d+w,v+d),1),
             ([(x,y,z+d),(x,y,z),(x+w,y,z),(x+w,y,z+d)],(u+d+w,v,u+d+w+w,v+d),.65)]
        for points,uv,shade in raw:
            transformed=[]
            for p in points:
                native=cm@np.array([p[0]-cp[0],cp[1]-p[1],p[2]-cp[2],1])
                transformed.append(np.array([native[0],24-native[1],native[2]]))
            faces.append((transformed,uv,shade))
yaw=.55;pitch=.4
camera=np.array([[math.cos(yaw),0,math.sin(yaw)],[-math.sin(yaw)*math.sin(pitch),math.cos(pitch),math.cos(yaw)*math.sin(pitch)],[-math.sin(yaw)*math.cos(pitch),-math.sin(pitch),math.cos(yaw)*math.cos(pitch)]])
points=np.array([camera@p for face,_,_ in faces for p in face]);minimum=points.min(axis=0);maximum=points.max(axis=0);center=(minimum+maximum)/2
scale=min(218/(maximum[0]-minimum[0]),190/(maximum[1]-minimum[1]))
canvas=Image.new('RGBA',(256,256));depth=np.full((256,256),-1e9)
for face,(u0,v0,u1,v1),shade in faces:
    pts=[]
    for p in face:
        q=camera@p-center;pts.append((128+q[0]*scale,128-q[1]*scale,q[2]))
    uvs=[(u0,v1-.001),(u1-.001,v1-.001),(u1-.001,v0),(u0,v0)]
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
image=canvas.resize((128,128),Image.Resampling.LANCZOS)
image.save(ASSETS/'textures/gui/white_kicks_preview.png')
image.save(ROOT/'backend/cosmetics/public/assets/white_kicks.png')
