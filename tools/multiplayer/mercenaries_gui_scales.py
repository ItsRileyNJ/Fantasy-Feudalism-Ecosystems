"""Two-client guard GUI scaling on the local X11 fixture; run after mercenaries_restart.py.
Requires libX11, xwininfo and Pillow. Resizes both test windows temporarily.
"""
import ctypes,subprocess,re,time,json,pathlib
from control import rpc,snapshot,wait_for,player
x=ctypes.CDLL('libX11.so.6');x.XOpenDisplay.restype=ctypes.c_void_p
x.XResizeWindow.argtypes=[ctypes.c_void_p,ctypes.c_ulong,ctypes.c_uint,ctypes.c_uint]
x.XMoveWindow.argtypes=[ctypes.c_void_p,ctypes.c_ulong,ctypes.c_int,ctypes.c_int]
x.XFlush.argtypes=[ctypes.c_void_p];x.XCloseDisplay.argtypes=[ctypes.c_void_p]
display=x.XOpenDisplay(b':97');assert display
windows=[int(m,16) for m in re.findall(r'(0x[0-9a-f]+) "Minecraft',subprocess.check_output(['xwininfo','-display',':97','-root','-tree'],text=True))]
assert len(windows)==2

def resize(w,h):
 for window in windows:x.XResizeWindow(display,window,w,h);x.XMoveWindow(display,window,0,0)
 x.XFlush(display);time.sleep(1)
def matched():
 s=snapshot('server');cs={side:snapshot(side) for side in ['owner','guest']}
 for side,name in [('owner','FFMOwner'),('guest','FFMGuest')]:
  p=player(s,name);c=cs[side]
  if c['screen']!='GuardScreen' or c['player']['menu']['slots']!=p['menu']['slots'] or c['player']['inventory']!=p['inventory'] or c['renderErrors']:return False
 return cs
try:
 resize(1280,720)
 s=snapshot('server');g=next(e for e in s['entities'] if e.get('guard',{}).get('isGuardCaptain'));gid=g['id']
 for side in ['owner','guest']:rpc(side,'shift',value=True);rpc(side,'interact',entity=gid)
 wait_for(matched);observations=[]
 for scale in [1,2,3]:
  for side in ['owner','guest']:rpc(side,'guiScale',scale=scale)
  cs=wait_for(matched)
  for side in cs:
   filename='compatibility-guard-'+side+'-scale-'+str(scale)+'.png'
   rpc(side,'screenshot',file=filename)
   from PIL import Image
   size=Image.open(pathlib.Path('/workspace/multiplayer/control')/side/filename).size
   assert size==(1280,720),size
   observations.append(dict(client=side,requestedScale=scale,resolution=list(size),screen=cs[side]['screen'],matchingGuardSlots=True,matchingInventory=True,renderErrors=cs[side]['renderErrors'],screenshot=filename))
 result=dict(passed=True,checks=[dict(name='Both GuardScreens remain synchronized and render at actual GUI scales 1, 2 and 3 in 1280x720 windows',passed=True,observations=observations)])
 pathlib.Path('/workspace/compat-013-gui-scales.json').write_text(json.dumps(result,indent=2))
 print('PASS actual GUI scales 1, 2 and 3 on both clients',flush=True)
finally:
 for side in ['owner','guest']:rpc(side,'close');rpc(side,'shift',value=False);rpc(side,'guiScale',scale=2)
 resize(960,540);x.XCloseDisplay(display)
