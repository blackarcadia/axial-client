"""Copy the supplied preview; --from-model renders a separate geometry diagnostic."""
from pathlib import Path
import json, math, sys, shutil, argparse
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--from-model',action='store_true')
parser.add_argument('--cosmetic',choices=['white_kicks','white_sneakers','ribcage'],default='white_kicks')
args=parser.parse_args()
cosmetic=args.cosmetic
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/axial_cosmetics'
if not args.from_model:
    source=ROOT/f'assets/{cosmetic}/preview.png'
    shutil.copy2(source,ASSETS/f'textures/gui/{cosmetic}_preview.png')
    shutil.copy2(source,ROOT/f'backend/cosmetics/public/assets/{cosmetic}.png')
    sys.exit(0)
import numpy as np
from PIL import Image
texture=Image.open(ASSETS/f'textures/entity/accessories/{cosmetic}.png').convert('RGBA')
geo=json.loads((ASSETS/f'geo/{cosmetic}.geo.json').read_text())['minecraft:geometry'][0]
def position(p):return np.array([p[0],24-p[1],p[2]],dtype=float)
def translate(p):
    matrix=np.eye(4);matrix[:3,3]=p;return matrix

def rotation(p):
    x,y,z=np.radians(p)
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
        # Bedrock box UVs use integer texel spans even for fractional geometry.
        tw,th,td=[math.floor(value+1e-7) for value in (w,h,d)]
        inflate=cube.get('inflate',bone.get('inflate',0))
        x-=inflate;y-=inflate;z-=inflate
        w+=2*inflate;h+=2*inflate;d+=2*inflate
        # Match ModelPart.Cuboid's vertex order and box UV layout. The source
        # coordinates below remain Bedrock Y-up until the bone transforms apply.
        raw=[([(x,y+h,z+d),(x+w,y+h,z+d),(x+w,y,z+d),(x,y,z+d)],(u+td+tw+td,v+td,u+td+tw+td+tw,v+td+th),.85),
             ([(x+w,y+h,z),(x,y+h,z),(x,y,z),(x+w,y,z)],(u+td,v+td,u+td+tw,v+td+th),1),
             ([(x+w,y+h,z+d),(x+w,y+h,z),(x+w,y,z),(x+w,y,z+d)],(u+td+tw,v+td,u+td+tw+td,v+td+th),.9),
             ([(x,y+h,z),(x,y+h,z+d),(x,y,z+d),(x,y,z)],(u,v+td,u+td,v+td+th),.92),
             ([(x+w,y+h,z+d),(x,y+h,z+d),(x,y+h,z),(x+w,y+h,z)],(u+td,v,u+td+tw,v+td),1),
             ([(x+w,y,z),(x,y,z),(x,y,z+d),(x+w,y,z+d)],(u+td+tw,v+td,u+td+tw+tw,v),.7)]
        if cube.get('mirror',bone.get('mirror',False)):
            # Native mirrored box UVs swap east/west and reverse each U span.
            raw[2],raw[3]=(raw[2][0],raw[3][1],raw[2][2]),(raw[3][0],raw[2][1],raw[3][2])
            raw=[(points,(uv[2],uv[1],uv[0],uv[3]),shade) for points,uv,shade in raw]
        for points,uv,shade in raw:
            transformed=[]
            for p in points:
                native=cm@np.array([p[0]-cp[0],cp[1]-p[1],p[2]-cp[2],1])
                transformed.append(np.array([native[0],24-native[1],native[2]]))
            faces.append((transformed,uv,shade))
yaw=2.65;pitch=.22 if cosmetic=='ribcage' else .5
camera=np.array([[math.cos(yaw),0,math.sin(yaw)],[math.sin(yaw)*math.sin(pitch),math.cos(pitch),-math.cos(yaw)*math.sin(pitch)],[-math.sin(yaw)*math.cos(pitch),math.sin(pitch),math.cos(yaw)*math.cos(pitch)]])
points=np.array([camera@p for face,_,_ in faces for p in face]);minimum=points.min(axis=0);maximum=points.max(axis=0);center=(minimum+maximum)/2
scale=min(218/(maximum[0]-minimum[0]),190/(maximum[1]-minimum[1]))
canvas=Image.new('RGBA',(256,256));depth=np.full((256,256),-1e9)
for face,(u0,v0,u1,v1),shade in faces:
    pts=[]
    for p in face:
        q=camera@p-center;pts.append((128+q[0]*scale,128-q[1]*scale,q[2]))
    uvs=[(u1,v0),(u0,v0),(u0,v1),(u1,v1)]
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
                color=texture.getpixel((max(0,min(texture.width-1,int(u))),max(0,min(texture.height-1,int(v)))))
                if color[3]==0:continue
                canvas.putpixel((x,y),tuple(round(k*shade) for k in color[:3])+(color[3],));depth[y,x]=z
image=canvas.resize((128,128),Image.Resampling.LANCZOS)
output=ROOT/f'build/previews/{cosmetic}_model.png'
output.parent.mkdir(parents=True,exist_ok=True)
image.save(output)
