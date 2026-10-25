"""Native client empty/occupied/empty model selection with actual capture and transfer."""
import json,time
from control import rpc,snapshot,wait_for,TEST_ROOT

MOD='fantasy_feudalism_mobs:'
result={}
def cmd(s):return rpc('server','command',command=s)
def model(occupied):
    expected=MOD+'item/bat_net'+('_occupied' if occupied else '')
    value=wait_for(lambda:(m if (m:=rpc('owner','heldItemModel'))['occupied']==occupied and m['sprite']==expected else None))
    assert not value['customRenderer'],value
    return value

def run():
    rpc('server','snapshot',timeout=120);rpc('owner','snapshot',timeout=180);rpc('owner','connect')
    wait_for(lambda:len(snapshot('server')['players'])==1)
    rpc('server','setup',timeout=90);rpc('server','clear');cmd('time set 6000');cmd('gamemode survival FFMOwner')
    rpc('owner','close');rpc('owner','cleanView',value=False)
    rpc('server','teleport',player='FFMOwner',x=8.5,y=64,z=6,yaw=0,pitch=0)
    cmd('setblock 8 64 8 minecraft:stone');cmd('setblock 8 65 8 '+MOD+'bat_enclosure[facing=north]')
    rpc('server','inventory',player='FFMOwner',slots=[{'slot':0,'item':MOD+'bat_net'}]);rpc('owner','select',slot=0)
    result['empty']=model(False)
    bat=rpc('server','spawn',type='minecraft:bat',x=8.5,y=65.4,z=6.6)
    wait_for(lambda:any(e['id']==bat['id'] for e in snapshot('owner')['entities']))
    rpc('owner','interact',entity=bat['id']);result['captured']=model(True)
    assert snapshot('owner')['player']['inventory'][0]['batUUID']==bat['uuid']
    rpc('owner','inventory');time.sleep(1)
    result['occupied_screenshot']=rpc('owner','screenshot',file='occupied-net-024/occupied.png');rpc('owner','close')
    result['reload']=rpc('owner','reload',timeout=90);result['after_reload']=model(True)
    rpc('server','save');rpc('owner','disconnect');wait_for(lambda:len(snapshot('server')['players'])==0)
    rpc('owner','connect');wait_for(lambda:len(snapshot('server')['players'])==1);rpc('owner','select',slot=0)
    result['after_reconnect']=model(True)
    rpc('owner','useBlock',x=8,y=65,z=8)
    result['emptied']=model(False)
    home=rpc('server','enclosure',x=8,y=65,z=8)
    assert home['inside']==1 and home['ids']==[bat['uuid']],home
    assert not snapshot('owner')['player']['inventory'][0].get('batUUID')
    result['home']=home
    rpc('owner','inventory');time.sleep(.5)
    result['empty_screenshot']=rpc('owner','screenshot',file='occupied-net-024/empty-again.png');rpc('owner','close')
    assert not snapshot('owner')['renderErrors']
    (TEST_ROOT/'occupied-net-0.1.24-results.json').write_text(json.dumps(result,indent=2))
    print('PASS: original empty sprite -> occupied reference sprite on actual capture -> original sprite on actual transfer; reload and reconnect preserve occupied state',flush=True)

if __name__=='__main__':run()
