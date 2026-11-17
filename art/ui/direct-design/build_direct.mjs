import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

// Directly authored design. 432 logical units; 2x exports are 864x1920.
// Shared ceramic skins are production assets, while text and controls stay editable.
// Noto Sans SC / SIL OFL 1.1. No generated full-screen image is used.
const root=path.dirname(fileURLToPath(import.meta.url));
const source=path.dirname(root);
const destination=process.argv[2]||path.join(root,'ceramic-direct.haoke-ui.json');
const catalog=JSON.parse(await fs.readFile(path.join(source,'materials/materials.json'),'utf8'));
const components={};
for(const [name,m] of Object.entries(catalog)){
  components[name]={width:m.width,height:m.height,children:[{id:'skin',type:'image',x:0,y:0,width:m.width,height:m.height,src:'data:image/png;base64,'+(await fs.readFile(path.join(source,'materials',m.file))).toString('base64'),code:'art/ui/materials/'+m.file}]};
}
components['app-logo']={width:720,height:720,children:[{id:'skin',type:'image',x:0,y:0,width:720,height:720,src:'data:image/png;base64,'+(await fs.readFile(path.join(source,'../app-icon/app-logo.png'))).toString('base64'),code:'art/app-icon/app-logo.png'}]};
const C={bg:'#F1F6F5',ink:'#173E49',muted:'#627B80',line:'#D8E5E6',white:'#F9FCFB',blue:'#286B7B',gold:'#C4974E'};
const tx=(id,x,y,w,h,text,size=16,color=C.ink,weight=500,align='left')=>({id,type:'text',x,y,width:w,height:h,text,fontFamily:'Noto Sans SC',fontSize:size,fontWeight:weight,lineHeight:1.12,verticalAlign:'center',align,fill:color});
const rr=(id,x,y,w,h,fill,radius=0,stroke='none',strokeWidth=0)=>({id,type:'rect',x,y,width:w,height:h,fill,radius,stroke,strokeWidth});
const gg=(id,x,y,w,h,children,code='')=>({id,type:'group',x,y,width:w,height:h,children,...(code?{code}:{})});
const pp=(id,x,y,w,h,d,stroke=C.ink,strokeWidth=2,fill='none')=>({id,type:'path',x,y,width:w,height:h,d,stroke,strokeWidth,fill});
function skin(id,x,y,w,h,name){const m=catalog[name];const [px,py,bw,bh]=m.body;return {id,type:'instance',x:x-px*w/bw,y:y-py*h/bh,width:m.width*w/bw,height:m.height*h/bh,component:name,code:'ceramic/'+name};}
function arrow(id,x,y,color=C.ink,back=false){return pp(id,x,y,24,24,back?'M 16 5 L 9 12 L 16 19 M 9 12 L 23 12':'M 8 5 L 15 12 L 8 19',color,2.3);}
function surface(id,x,y,w,h,children){return gg(id,x,y,w,h,[rr(id+'-shadow',0,3,w,h,'#274D5A10',20),rr(id+'-body',0,0,w,h,C.white,20,'#E0ECEB',1),...children]);}
function status(){return [tx('system-time',28,5,80,24,'09:41',13,C.ink,650),pp('system-signal',340,12,16,12,'M 0 10 L 0 8 M 5 10 L 5 5 M 10 10 L 10 1',C.ink,2.6),pp('system-wifi',360,9,20,16,'M 1 5 Q 10 -2 19 5 M 5 9 Q 10 5 15 9 M 9 13 L 11 13',C.ink,1.7),rr('system-battery',390,12,17,9,'none',2,C.ink,1.3),rr('system-battery-fill',392,14,11,5,C.ink,1),rr('system-battery-cap',408,15,2,3,C.ink,1),rr('system-gesture',157,945,118,4,'#173E4966',2)];}
function logo(x,y){return gg('app-mark',x,y,46,46,[{id:'app-mark-icon',type:'instance',x:0,y:0,width:46,height:46,component:'app-logo'}]);}
function circleButton(id,x,y,icon){return surface(id,x,y,48,48,[icon]);}
function button(id,x,y,w,h,label,primary=false,icon=null){return gg(id,x,y,w,h,[skin(id+'-skin',0,0,w,h,primary?'primary':'secondary'),tx(id+'-label',icon?35:8,-1,w-(icon?47:16),h,label,18,primary?'#F4FAFB':C.ink,700,'center'),...(icon?[icon]:[])],'Button');}
function tile(id,x,y,w,h,value){if(!value)return skin(id,x,y,w,h,'hole-light');const pal={2:'ivory',4:'ice',8:'cyan',16:'teal',32:'deep',64:'gold',128:'sand',256:'orange',512:'gold',1024:'deep',2048:'gold'};const name=pal[value]||'gold';return gg(id,x,y,w,h,[skin(id+'-skin',0,0,w,h,'tile-'+name),tx(id+'-number',0,-2,w,h,String(value),value>=1024?25:value>=128?29:36,['teal','deep'].includes(name)?'#F6FCFC':C.ink,800,'center')],'GameTile');}
const cells=[2,4,8,0,4,16,32,0,2,8,64,0,0,2,0,0];
function board(id,x,y){const a=[skin(id+'-tray',0,0,384,384,'tray-light')];const p=16,gap=9,w=(384-p*2-gap*3)/4;for(let i=0;i<16;i++)a.push(tile(id+'-cell-'+i,p+(i%4)*(w+gap),p+Math.floor(i/4)*(w+gap),w,w,cells[i]));return gg(id,x,y,384,398,a,'GameBoard');}
function score(id,x,y,w,label,value,accent=false){return surface(id,x,y,w,66,[tx(id+'-label',16,8,w-32,22,label,12,C.muted,500),tx(id+'-value',16,27,w-32,35,value,24,accent?C.blue:C.ink,750)]);}
function page(id,name,children,state){return {id,name,width:432,height:960,unit:'px',scale:1,background:C.bg,state,code:'direct-design/'+id,children:[...status(),...children]};}
const pages=[];

const home=[logo(25,55),tx('brand-title',86,51,230,39,'隐私友好2048',23,C.ink,750),tx('brand-subtitle',87,87,220,24,'离线畅玩 · 进度自动保存',11,C.muted,500),surface('open-modes',342,55,66,48,[tx('open-modes-label',0,0,66,48,'玩法',15,C.blue,650,'center')]),tx('home-heading',24,136,384,47,'继续上次的挑战',29,C.ink,750),tx('home-subtitle',25,182,380,27,'经典模式，慢慢拼出你的 2048。',14,C.muted),score('current-score',24,224,184,'本局得分','512',true),score('best-score',224,224,184,'历史最高','2048'),board('home-board',24,315)];
home.push(tx('board-type',25,724,180,28,'经典 · 4 × 4',14,C.ink,650),tx('save-status',240,724,168,28,'已自动保存',12,C.muted,500,'right'),button('continue',24,778,384,62,'继续游戏',true,pp('play',110,20,20,22,'M 2 1 L 18 11 L 2 21 Z','#F4FAFB',0,'#F4FAFB')),tx('start-new',24,860,384,40,'开始新游戏',16,C.blue,650,'center'));
pages.push(page('home','首页 · 继续游戏',home,'有经典4×4存档；合成分数与盘面'));

const modes=[circleButton('modes-back',24,53,arrow('back-icon',10,12,C.ink,true)),tx('modes-kicker',89,62,230,28,'探索数字的另一面',13,C.muted,550),tx('modes-title',24,127,384,57,'五种玩法',35,C.ink,750),tx('modes-subtitle',25,184,380,28,'换个规则，换种思路。',15,C.muted)];
const definitions=[['3×3 速玩','小棋盘，大考验','拼出 256，再挑战 512'],['残局挑战','每一步都有答案','9 个固定棋局，限步解开'],['障碍棋盘','绕过石板，寻找空间','障碍会阻挡数字移动'],['斐波那契','熟悉的棋盘，新规则','合并数列中相邻的数字'],['每日挑战','今天，来一道新题','每天一道固定题']];
for(let i=0;i<5;i++){
 const y=232+i*132;
 modes.push(gg('mode-'+i,24,y,384,116,[skin('mode-'+i+'-skin',0,0,384,116,'mode-card'),skin('mode-'+i+'-illustration',10,10,96,96,'mode-icon-'+i),tx('mode-'+i+'-title',124,12,225,34,definitions[i][0],21,C.ink,750),tx('mode-'+i+'-sub',124,48,235,25,definitions[i][1],13,C.blue,600),tx('mode-'+i+'-rule',124,75,235,24,definitions[i][2],13,C.muted,500),arrow('mode-'+i+'-chevron',349,23,C.blue)],'ModeCard'));
}
pages.push(page('modes','玩法 · 五种挑战',modes,'五种现有玩法，不增加联网或排行榜能力'));

const game=[circleButton('game-back',24,53,arrow('game-back-icon',10,12,C.ink,true)),tx('game-title',97,50,238,35,'经典模式',21,C.ink,750,'center'),tx('game-subtitle',97,85,238,23,'4 × 4 · 目标 2048',12,C.muted,500,'center'),circleButton('game-menu',360,53,gg('dots',13,22,24,5,[rr('dot1',0,0,4,4,C.ink,2),rr('dot2',9,0,4,4,C.ink,2),rr('dot3',18,0,4,4,C.ink,2)])),score('game-score',24,147,184,'本局得分','512',true),score('game-best',224,147,184,'历史最高','2048'),tx('game-hint',24,240,384,29,'滑动棋盘，合并相同数字',14,C.muted,500,'center'),board('game-board',24,298)];
game.push(button('undo',24,738,184,60,'撤销',false,pp('undo-icon',39,21,22,22,'M 5 2 L 0 7 L 5 12 M 1 7 L 13 7 Q 21 7 21 14 Q 21 21 13 21',C.blue,2)),button('restart',224,738,184,60,'重开',false,pp('restart-icon',40,20,24,24,'M 20 6 A 9 9 0 1 0 21 15 M 20 0 L 20 7 L 13 7',C.blue,2)),rr('tip-divider',104,839,224,1,C.line),tx('game-tip',40,858,352,28,'不必着急，每一步都可以想一想。',13,C.muted,450,'center'));
pages.push(page('game','游戏 · 经典4×4',game,'进行中，撤销可用，自动保存；与首页同一盘面'));

const modal=structuredClone(pages[2]);modal.id='restart';modal.name='游戏 · 重开确认';modal.state='取消保留当前棋盘；确认重开，仅影响本局';modal.children.push(rr('modal-scrim',0,0,432,960,'#11384480'),skin('modal-surface',24,315,384,278,'dialog'),skin('modal-symbol',185,341,62,62,'tile-ice'),pp('modal-restart-icon',202,358,27,27,'M 23 7 A 10 10 0 1 0 24 17 M 23 0 L 23 8 L 15 8',C.blue,2.4),tx('modal-title',47,422,338,42,'重新开始这一局？',23,C.ink,750,'center'),tx('modal-copy',51,472,330,54,'当前棋盘将被重置。\n历史最高分会保留。',14,C.muted,450,'center'),button('modal-cancel',44,540,160,48,'继续这局',false),button('modal-confirm',228,540,160,48,'重新开始',true));
pages.push(modal);

const document={format:'haoke-ui-design',version:1,id:'pf2048-ceramic-direct',name:'隐私友好2048 · 青瓷直接设计',revision:1,tokens:{'color.bg':C.bg,'color.ink':C.ink,'color.muted':C.muted,'color.action':C.blue,'color.gold':C.gold,'space.page':24,'space.grid':9,'font.family':'Noto Sans SC'},components,boards:pages};
await fs.writeFile(destination,JSON.stringify(document));
console.log(JSON.stringify({output:destination,boards:pages.length,logicalSize:[432,960],exportScale:2}));
