"""Import UFO's authored cubes and animation, with an optional supplied texture."""
import base64, json, pathlib, sys
assets=pathlib.Path(__file__).resolve().parents[1]/'src/main/resources/assets/axial_cosmetics'
m=json.loads(pathlib.Path(sys.argv[1]).read_text())
elements={e['uuid']:e for e in m['elements']}; groups={g['uuid']:g for g in m['groups']}; bones=[]
# Match Ghostly Ghost’s shoulder centre while retaining UFO’s authored rotation pivot.
def shift(v): return [v[0]+13.5,v[1]+26.575,v[2]-.1]
def visit(node,parent=None):
 g=groups[node['uuid']]; b=dict(name=g['name'],pivot=shift(g['origin']),rotation=g.get('rotation',[0,0,0]),cubes=[])
 if parent: b['parent']=parent
 bones.append(b)
 for child in node['children']:
  if isinstance(child,dict): visit(child,g['name']); continue
  e=elements[child]; faces={}
  for name,f in e['faces'].items():
   if f.get('texture') is None: continue
   assert not f.get('rotation',0), 'Rotated UV face'
   # Blockbench names X faces geometrically; the Bedrock baker uses reflected X names.
   face={'east':'west','west':'east'}.get(name,name)
   u,v,u1,v1=f['uv']; faces[face]=dict(uv=[u,v],uv_size=[u1-u,v1-v])
  b['cubes'].append(dict(origin=shift(e['from']),size=[y-x for x,y in zip(e['from'],e['to'])],pivot=shift(e['origin']),rotation=e.get('rotation',[0,0,0]),uv=faces,inflate=e.get('inflate',0)))
for node in m['outliner']: visit(node)
# Retain the authored pivot for animated rotations; attach the whole model to the torso.
geo={'format_version':'1.12.0','minecraft:geometry':[{'description':dict(identifier='geometry.ufo',texture_width=32,texture_height=32),'bones':bones}]}
(assets/'geo/ufo.geo.json').write_text(json.dumps(geo,indent=2)+'\n')
assert len(m['animations'])==1
animation=m['animations'][0]; tracks={}
for animator in animation['animators'].values():
 channels={}
 for key in animator.get('keyframes',[]):
  assert key['interpolation']=='linear' and len(key['data_points'])==1
  v=key['data_points'][0]; xyz=[float(v[k]) for k in ('x','y','z')]
  if key['channel']=='rotation': xyz=[-xyz[0],-xyz[1],xyz[2]]
  channels.setdefault(key['channel'],{})[str(key['time'])]=xyz
 if channels: tracks[animator['name']]=channels
out={'format_version':'1.8.0','animations':{'animation.ufo.float':dict(loop=True,animation_length=animation['length'],bones=tracks)}}
(assets/'animations/ufo.animation.json').write_text(json.dumps(out,indent=2)+'\n')
texture=pathlib.Path(sys.argv[2]).read_bytes() if len(sys.argv)>2 else base64.b64decode(m['textures'][0]['source'].split(',',1)[1])
(assets/'textures/entity/accessories/ufo.png').write_bytes(texture)
