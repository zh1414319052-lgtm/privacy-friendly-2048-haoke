import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url));
const output=process.argv[2]; if(!output)throw Error('Pass a draft output path; registered design is committed through the workbench API.');
const existing=JSON.parse(await fs.readFile(path.join(root,'ceramic-2048.haoke-ui.json'),'utf8'));
const materials=JSON.parse(await fs.readFile(path.join(root,'materials/materials.json'),'utf8'));
const components={};
for(const [name,m] of Object.entries(materials)){
 const src='data:image/png;base64,'+(await fs.readFile(path.join(root,'materials',m.file))).toString('base64');
 components[name]={width:m.width,height:m.height,children:[{id:'skin',type:'image',x:0,y:0,width:m.width,height:m.height,src,code:'materials/'+m.file}]};
}
const ink='#133B4B',muted='#52727B',bg='#E8F5FA';
function text(id,x,y,w,h,s,size=44,color=ink,weight=700,align='left'){return {id,type:'text',x,y,width:w,height:h,text:s,fontSize:size,fontFamily:'Noto Sans SC',fontWeight:weight,lineHeight:1.12,fill:color,align,verticalAlign:'center'};}
function rect(id,x,y,w,h,color,radius=0){return {id,type:'rect',x,y,width:w,height:h,fill:color,radius};}
function group(id,x,y,w,h,children,code=''){return {id,type:'group',x,y,width:w,height:h,children,...(code?{code}:{})};}
function skin(id,x,y,w,h,name){const m=materials[name];const [px,py,bw,bh]=m.body;return {id,type:'instance',x:x-px*w/bw,y:y-py*h/bh,width:m.width*w/bw,height:m.height*h/bh,component:name,code:'ceramic/'+name};}
function button(id,x,y,w,h,label,kind='primary',fontSize=52){return group(id,x,y,w,h,[skin(id+'-material',0,0,w,h,kind),text(id+'-label',10,-2,w-20,h,label,fontSize,kind==='primary'||kind==='accent'?'#F7FBFC':kind==='disabled'?'#8BA3AC':ink,800,'center')],'Button');}
function arrow(id,x,y,w=44,color=ink,back=false){return {id,type:'path',x,y,width:w,height:w,d:back?'M 24 4 L 5 22 L 24 40 M 6 22 L 43 22':'M 11 4 L 29 22 L 11 40',stroke:color,strokeWidth:5,fill:'none'};}
function header(title,W=899,dark=false,home=false,status=false){
 const offset=status?90:0;const a=[];
 if(status){a.push(text('status-time',40,0,180,64,'10:24',32,ink,650),text('status-power',W-220,0,175,64,'100%',30,ink,650,'right'));}
 a.push(rect('toolbar',0,offset,W,home?134:116,dark?'#153F50':bg));
 for(let i=0;i<12;i++)a.push(rect('toolbar-shadow-'+i,0,offset+(home?134:116)+i,W,1,'#527988'+Math.round((1-i/12)*32).toString(16).padStart(2,'0')));
 if(home){a.push(group('toolbar-back',40,44,57,50,[rect('bar1',0,0,55,7,ink,3),rect('bar2',0,19,55,7,ink,3),rect('bar3',0,38,55,7,ink,3)]));
 a.push(text('toolbar-title',140,18,530,94,title,56,ink,850),text('modes-action',691,25,168,84,'新玩法',43,'#246C80',750,'right'));}
 else {a.push(arrow('toolbar-back',46,offset+37,47,dark?'#F1FAFC':ink,true),text('toolbar-title',status?145:110,offset+12,status?W-205:W-220,92,title,status?54:53,dark?'#F1FAFC':ink,800,status?'left':'center'));}
 return a;
}
function tile(id,x,y,w,h,value,theme='light'){
 const palette=theme==='warm'?{2:'warm-ivory',4:'sand',8:'gold',16:'orange',32:'deep',64:'gold'}:{2:'ivory',4:'ice',8:'cyan',16:'teal',32:'deep',64:'gold'};
 if(value===0)return skin(id,x,y,w,h,theme==='dark'?'hole-dark':theme==='warm'?'hole-teal':'hole-light');
 const name=value===-1?'stone':palette[value]||'gold';const arr=[skin(id+'-material',0,0,w,h,'tile-'+name)];
 if(value>0)arr.push(text(id+'-number',4,-6,w-8,h,String(value),Math.min(w*.53,w/(String(value).length*.56+1)*1.5),['teal','deep'].includes(name)?'#F3FBFC':ink,850,'center'));
 else arr.push({id:id+'-crack',type:'path',x:18,y:18,width:w-36,height:h-36,d:'M 0 136 L 61 77 L 99 68 L 140 4 M 61 77 L 60 6',stroke:'#4A626D',strokeWidth:3,fill:'none'});
 return group(id,x,y,w,h,arr,'Element / ChallengeBoard.tile');
}
function board(id,x,y,w,h,values,theme='light',n=4){
 const skinName=theme==='warm'?'tray-teal':theme==='dark'?'tray-dark':'tray-light';const a=[skin(id+'-tray',0,0,w,h,skinName)];
 if(theme==='warm')a.push(skin(id+'-inner-well',20,20,w-40,h-40,'well-teal'));
 const pad=w*(theme==='warm'?.045:.039),top=pad+(theme==='warm'?5:0);const gap=w*.022;const cw=(w-pad*2-gap*(n-1))/n,ch=(h-top-pad-gap*(n-1))/n;
 for(let i=0;i<n*n;i++)a.push(tile(id+'-c'+i,pad+(i%n)*(cw+gap),top+Math.floor(i/n)*(ch+gap),cw,ch,values[i]||0,theme));
 return group(id,x,y,w,h+22,a,'GameActivity.number_field / ChallengeBoard');
}
function stat(id,x,y,w,label,value,dark=false){return group(id,x,y,w,130,[skin(id+'-surface',0,0,w,130,dark?'stat-dark':'stat'),text(id+'-caption',8,8,w-16,47,label,32,'#DAEEF3',700,'center'),text(id+'-value',8,49,w-16,80,value,55,'#FFFFFF',800,'center')]);}
function page(id,name,w,h,children,state,code){return {id,name,width:w,height:h,background:bg,unit:'px',scale:1,state,code,children};}
const boards=[];
const homeCells=[2,4,0,0,0,16,8,0,0,0,64,0,0,0,0,0];
const puzzleCells=[2,0,2,4,4,4,0,8,0,0,0,8,0,0,0,0];
let h=header('隐私友好2048',899,false,true);
for(let i=0;i<4;i++)h.push(button('size-'+(4+i),44+i*211,240,194,90,(4+i)+'×'+(4+i),i?'nav-light':'nav-primary',48));
// Navigation chip label colors are independently editable.
for(const n of h.filter(n=>n.id?.startsWith('size-')))n.children[1].fill=n.id==='size-4'?'#F8FCFE':ink;
h.push(board('home-board',36,425,827,846,homeCells),button('continue',50,1403,799,124,'继续游戏','primary',56));
h.push(group('start-game',50,1576,799,122,[{...rect('start-outline',0,0,799,122,'#E8F5FA',31),stroke:'#26788B',strokeWidth:3},text('start-label',12,0,775,119,'开始新游戏',54,'#266C80',800,'center')],'Button'));
boards.push(page('home','首页 · 原图尺寸',899,1750,h,'与 reference/home.png 同尺寸；数字与文字独立编辑','MainActivity'));
let m=header('新玩法',853,false,false,true);
const modeNames=['3×3 速玩','残局挑战','障碍棋盘','斐波那契','每日挑战'];
const modeSub=['拼出 256，再挑战 512','全部 9 关','石板会挡住方块','合并数列相邻的数字','每天一道固定题'];
const ys=[260,546,820,1101,1372];const hs=[249,244,252,237,249];
for(let i=0;i<5;i++)m.push(group('mode-'+i,27,ys[i],799,hs[i],[skin('mode-'+i+'-card',0,0,799,hs[i],'mode-card'),skin('mode-'+i+'-icon',30,17,230,230,'mode-icon-'+i),text('mode-'+i+'-title',304,41,426,84,modeNames[i],56,ink,800),text('mode-'+i+'-subtitle',304,123,440,74,modeSub[i],35,'#4C626A',500),arrow('mode-'+i+'-arrow',731,101,40)],'ChallengeMenuActivity.mode'));
boards.push(page('modes','新玩法 · 原图尺寸',853,1844,m,'五种玩法入口；同源材质，每个图标分别设计','ChallengeMenuActivity'));
function puzzlePage(id='puzzle',solved=false){
 let a=header('第 1 关',898,true);
 a.push(text('rule',40,160,818,65,'滑动后不会生成新方块',43,muted,650,'center'),text('goal',40,231,818,144,'拼出 16',99,ink,850,'center'));
 let values=solved?[4,4,0,0,8,8,0,0,8,0,0,0,0,0,0,0]:puzzleCells;
 if(solved)values=[2,4,2,4,4,0,0,16,0,0,0,0,0,0,0,0];
 a.push(board('play-board',36,406,826,846,values,'warm'));
 a.push(stat('score',42,1327,263,'得分',solved?'16':'0'),stat('moves',321,1327,263,'剩余步数',solved?'1':'2'),stat('best',600,1327,257,'最少步数',solved?'1':'—'));
 if(solved){a.push(text('notice',40,1483,818,55,'用 1 步解开',37,'#2C7888',750,'center'),button('undo-act',42,1560,251,111,'撤销','secondary',45),button('reset-act',323,1560,251,111,'重开','secondary',45),button('next-level',604,1560,251,111,'下一关','accent',44));}
 else a.push(button('undo-act',42,1511,394,112,'撤销','disabled',56),button('reset-act',465,1511,391,112,'重开','accent',56));
 return page(id,solved?'通关反馈':'残局 · 原图尺寸',898,1751,a,solved?'第 1 关通关；下一关直接可见':'第 1 关：固定开局、目标 16、限 2 步','ChallengeActivity');
}
boards.push(puzzlePage());
let lev=header('选择残局',899,false);
lev.push(text('level-intro',43,146,813,80,'9 个棋局，逐步解开',39,muted,550));
const goals=['拼出 16','拼出 32','腾出 5 个空格','达到 96 分','拼出 128','腾出 5 个空格','石板棋盘 · 拼出 64','达到 224 分','拼出 256'];
const limits=[2,5,5,4,4,6,6,5,11];
for(let i=0;i<9;i++)lev.push(group('level-'+i,45,249+i*158,809,132,[skin('level-'+i+'-card',0,0,809,132,'level-card'),skin('level-'+i+'-badge',20,20,83,83,i<3?'tile-ivory':i<6?'tile-ice':'tile-gold'),text('level-'+i+'-index',20,14,83,83,String(i+1),44,ink,800,'center'),text('level-'+i+'-goal',139,10,580,62,goals[i],40,ink,750),text('level-'+i+'-limit',139,73,450,42,'限 '+limits[i]+' 步',29,muted,500),arrow('level-'+i+'-arrow',737,42,40)],'ChallengeMenuActivity.puzzle'));
boards.push(page('levels','残局列表',899,1750,lev,'九关，步数与 ChallengeCatalog 对齐','ChallengeMenuActivity'));
let cl=header('2048',899,true);cl.push(text('game-size',45,157,809,71,'4×4',48,muted,700,'center'),board('classic-board',36,279,827,846,homeCells),stat('score',47,1190,391,'得分','512'),stat('best',462,1190,391,'最高分','2048'),button('undo-act',46,1392,391,116,'撤销','secondary',54),button('reset-act',463,1392,391,116,'重开','accent',54));
boards.push(page('classic','经典棋盘',899,1750,cl,'经典游戏，保留分数与撤销语义','GameActivity'));
boards.push(puzzlePage('solved',true));
let restart=structuredClone(boards.find(b=>b.id==='puzzle'));restart.id='restart';restart.name='重开确认';restart.state='保留取消路径，重开不会删除历史成绩';
restart.children.push(rect('dim',0,0,898,1751,'#14354866'),skin('dialog',54,615,790,470,'dialog'),text('dialog-title',94,654,710,88,'重新开始这局？',52,ink,800),text('dialog-body',94,765,710,110,'棋盘将恢复开局状态，历史最佳成绩会保留。',34,muted,500),button('cancel',94,960,332,97,'取消','secondary',45),button('confirm',472,960,332,97,'重新开始','accent',45));
boards.push(restart);
let dark=header('障碍棋盘',1750,true);dark[0].fill='#122C3C';dark.push(board('wide-board',56,180,620,620,[2,4,0,8,0,-1,4,0,16,0,-1,2,0,0,8,0],'dark'),text('game-goal',756,203,935,96,'拼出 1024',66,'#E5F5FA',800),text('game-rules',756,314,935,74,'石板会挡住滑动的方块。',39,'#AAC4CF',500),stat('score',756,424,290,'得分','64',true),stat('moves',1070,424,290,'步数','12',true),stat('best',1384,424,307,'最高分','512',true),button('wide-undo',756,624,442,112,'撤销','secondary',51),button('wide-restart',1249,624,442,112,'重开','accent',51));
const darkPage=page('dark','横屏深色棋盘',1750,899,dark,'障碍模式横屏；棋盘与操作区同屏','ChallengeActivity.landscape');darkPage.background='#122633';boards.push(darkPage);
const baseStates=new Set(['home','modes','levels','classic','puzzle','solved','restart','dark']);
const baseTransitions=new Set(['open-modes','home-continue','modes-puzzle','open-level','restart-open','cancel-restart','do-restart','back-modes','back-classic','back-levels','back-puzzle','back-solved','back-restart']);
const motion={version:1,initial:'home',states:structuredClone(existing.motion.states.filter(s=>baseStates.has(s.id))),transitions:structuredClone(existing.motion.transitions.filter(t=>baseTransitions.has(t.id)))};
const add=(id,from,to,node)=>motion.transitions.push({id,from,to,trigger:'click',node,easing:'outCubic',duration:180,pageEffect:'fade'});
add('home-start','home','classic','start-game');
for(const id of ['modes','levels','classic','puzzle','solved','dark'])add('tap-back-'+id,id,id==='modes'||id==='classic'?'home':id==='puzzle'||id==='solved'?'levels':'modes','toolbar-back');


const challengeDefs=[
 {id:'sprint',title:'3×3 速玩',rule:'拼出 256，再挑战 512',goal:'拼出 256',n:3,cells:[2,4,0,0,16,8,0,32,0],score:'160',moves:'38'},
 {id:'obstacles',title:'障碍棋盘',rule:'石板会挡住滑动的方块',goal:'拼出 1024',n:4,cells:[2,4,0,8,0,-1,4,0,16,0,-1,2,0,0,8,0],score:'64',moves:'12'},
 {id:'fibonacci',title:'斐波那契',rule:'合并数列相邻的数字',goal:'拼出 2584',n:4,cells:[1,2,0,0,3,5,0,8,0,13,21,0,0,0,0,0],score:'128',moves:'42'},
 {id:'daily',title:'每日挑战',rule:'2026 年 11 月 17 日',goal:'达到 224 分',n:5,cells:[4,4,0,32,4,4,2,16,2,2,32,16,8,8,16,4,16,0,4,4,16,8,8,16,4],score:'0',moves:'5'}
];
for(const c of challengeDefs){
 const nodes=header(c.title,898,true);nodes.push(text('rule',40,160,818,65,c.rule,40,muted,600,'center'),text('goal',40,231,818,c.id==='daily'?119:129,c.goal,c.id==='daily'?70:89,ink,850,'center'));
 nodes.push(board('play-board',36,406,826,846,c.cells,'warm',c.n),stat('score',42,1327,263,'得分',c.score),stat('moves',321,1327,263,c.id==='daily'?'剩余步数':'步数',c.moves),stat('best',600,1327,257,c.id==='daily'?'最少步数':'最高分','—'),button('undo-act',42,1511,394,112,'撤销',c.id==='daily'?'disabled':'secondary',56),button('reset-act',465,1511,391,112,'重开','accent',56));
 boards.push(page(c.id,c.title,898,1751,nodes,'玩法视觉样例；真实开局和每日题由游戏状态提供','ChallengeActivity.'+c.id));
 motion.states.push({id:c.id,board:c.id,name:c.title,overrides:{}});
 add('open-'+c.id,'modes',c.id,'mode-'+({sprint:0,obstacles:2,fibonacci:3,daily:4}[c.id]));
 add('back-'+c.id,c.id,'modes','toolbar-back');
}

// A separate next-level state preserves the actual next action in the prototype.
const p2=structuredClone(boards.find(b=>b.id==='puzzle'));
p2.id='puzzle-2';p2.name='第 2 关';p2.state='第 2 关实际固定盘面：目标 32，限 5 步';
p2.children=p2.children.filter(n=>n.id!=='play-board');
p2.children.find(n=>n.id==='toolbar-title').text='第 2 关';p2.children.find(n=>n.id==='goal').text='拼出 32';
p2.children.find(n=>n.id==='moves').children.find(n=>n.id==='moves-value').text='5';
p2.children.splice(8,0,board('play-board',36,406,826,846,[4,0,2,16,4,0,0,8,2],'warm',3));
boards.push(p2);motion.states.push({id:p2.id,board:p2.id,name:p2.name,overrides:{}});
add('open-level-2','levels','puzzle-2','level-1');add('next-example','solved','puzzle-2','next-level');add('back-level-2','puzzle-2','levels','toolbar-back');
for(const id of ['classic','sprint','obstacles','fibonacci','daily','puzzle-2']){
 const src=boards.find(b=>b.id===id), modal=structuredClone(src);modal.id='restart-'+id;modal.name=src.name+' · 重开确认';modal.state='重开确认：取消返回原来的玩法';
 const modalNodes=boards.find(b=>b.id==='restart').children.filter(n=>['dim','dialog','dialog-title','dialog-body','cancel','confirm'].includes(n.id));
 modal.children.push(...structuredClone(modalNodes));boards.push(modal);motion.states.push({id:modal.id,board:modal.id,name:modal.name,overrides:{}});
 add('reset-open-'+id,id,modal.id,'reset-act');add('reset-cancel-'+id,modal.id,id,'cancel');add('reset-confirm-'+id,modal.id,id,'confirm');
}
const doc={format:'haoke-ui-design',version:1,id:existing.id,name:'隐私友好2048 · 原稿保真还原',revision:existing.revision,tokens:{'color.bg':bg,'color.ink':ink,'color.muted':muted,'motion.fast':115,'motion.page':180},components,boards,motion};
await fs.writeFile(output,JSON.stringify(doc));
console.log(JSON.stringify({output,boards:boards.length,components:Object.keys(components).length,bytes:JSON.stringify(doc).length}));
