"""Import the supplied Blockbench pet, including its embedded linear animation."""
import json, pathlib, shutil, sys
source=pathlib.Path(sys.argv[1]); assets=pathlib.Path(__file__).resolve().parents[1]/'src/main/resources/assets/axial_cosmetics'
model=json.loads(source.read_text()); elements={e['uuid']:e for e in model['elements']}; bones=[]
def shifted(v): return [v[0]-4,v[1]+20,v[2]]
def bone(group,parent=None):
    result={'name':group['name'],'pivot':shifted(group['origin']),'rotation':group.get('rotation',[0,0,0]),'cubes':[]}
    if parent: result['parent']=parent
    bones.append(result)
    for child in group['children']:
        if isinstance(child,dict): bone(child,group['name']); continue
        e=elements[child]; faces={}
        for name,f in e['faces'].items():
            if f.get('texture') is None: continue
            if f.get('rotation',0): raise ValueError('Unsupported rotated face')
            u,v,u1,v1=f['uv']; faces[name]={'uv':[u,v],'uv_size':[u1-u,v1-v]}
        result['cubes'].append({'origin':shifted(e['from']),'size':[b-a for a,b in zip(e['from'],e['to'])],'pivot':shifted(e['origin']),'rotation':e.get('rotation',[0,0,0]),'uv':faces})
for group in model['outliner']: bone(group)
# Keep the root at the torso origin; only the pet's geometry moves to the shoulder.
bones[0]['pivot']=[0,24,0]
geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.pookles','texture_width':96,'texture_height':8},'bones':bones}]}
(assets/'geo/pookles.geo.json').write_text(json.dumps(geo,indent=2)+'\n')
animations={}
for animation in model['animations']:
    tracks={}
    for animator in animation['animators'].values():
        channels={}
        for key in animator['keyframes']:
            if key['interpolation']!='linear' or len(key['data_points'])!=1: raise ValueError('Unsupported interpolation')
            v=key['data_points'][0];xyz=[float(v[k]) for k in ('x','y','z')]
            # Blockbench's Bedrock animation export negates X/Y rotation.
            if key['channel']=='rotation': xyz=[-xyz[0],-xyz[1],xyz[2]]
            channels.setdefault(key['channel'],{})[str(key['time'])]=xyz
        tracks[animator['name']]=channels
    animations['animation.pookles.float']={'loop':True,'animation_length':animation['length'],'bones':tracks}
(assets/'animations/pookles.animation.json').write_text(json.dumps({'format_version':'1.8.0','animations':animations},indent=2)+'\n')
shutil.copyfile(sys.argv[2],assets/'textures/entity/accessories/pookles.png')
