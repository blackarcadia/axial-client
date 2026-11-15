"""Convert the supplied Blockbench shoulder pet to the client's native GEO format."""
import json, pathlib, shutil, sys
source=pathlib.Path(sys.argv[1]); assets=pathlib.Path(__file__).resolve().parents[1]/'src/main/resources/assets/axial_cosmetics'
model=json.loads((source/'pink_slime.bbmodel').read_text()); elements={e['uuid']:e for e in model['elements']}; bones=[]
def shifted(v): return [v[0],v[1]+24,v[2]]
def bone(group,parent=None):
    result={'name':group['name'],'pivot':shifted(group['origin']),'rotation':group.get('rotation',[0,0,0]),'cubes':[]}
    if parent: result['parent']=parent
    bones.append(result)
    for child in group['children']:
        if isinstance(child,dict): bone(child,group['name']); continue
        e=elements[child]; faces={}
        for name,f in e['faces'].items():
            if f.get('texture') is None: continue
            u,v,u1,v1=f['uv']; faces[name]={'uv':[u,v],'uv_size':[u1-u,v1-v]}
        result['cubes'].append({'origin':shifted(e['from']),'size':[b-a for a,b in zip(e['from'],e['to'])],'pivot':shifted(e['origin']),'rotation':e.get('rotation',[0,0,0]),'uv':faces})
for group in model['outliner']: bone(group)
geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.pink_slime','texture_width':32,'texture_height':8},'bones':bones}]}
(assets/'geo/pink_slime.geo.json').write_text(json.dumps(geo,indent=2)+'\n')
(assets/'animations').mkdir(exist_ok=True)
shutil.copyfile(source/'pink_slime.animation.json',assets/'animations/pink_slime.animation.json')
shutil.copyfile(sys.argv[2],assets/'textures/entity/accessories/pink_slime.png')
