import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root=path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const ui=path.join(root,'art/ui');
const target=path.join(root,'app/src/main/assets/ceramic');
await fs.mkdir(target,{recursive:true});
const doc=JSON.parse(await fs.readFile(path.join(ui,'direct-design/ceramic-direct.haoke-ui.json'),'utf8'));
const hand=JSON.parse(await fs.readFile(path.join(ui,'direct-design/preview/handoff.json'),'utf8'));
const manifest=JSON.parse(await fs.readFile(path.join(ui,'materials/materials.json'),'utf8'));
const hashes={};
for(const m of Object.values(manifest)){
 const bytes=await fs.readFile(path.join(ui,'materials',m.file));
 await fs.writeFile(path.join(target,m.file),bytes);
 hashes[m.file]=crypto.createHash('sha256').update(bytes).digest('hex');
}
const appLogo=await fs.readFile(path.join(root,'art/app-icon/app-logo.png'));
await fs.writeFile(path.join(target,'app-logo.png'),appLogo);
hashes['app-logo.png']=crypto.createHash('sha256').update(appLogo).digest('hex');
for(let i=0;i<doc.boards.length;i++){
 const metrics=new Map(hand.boards[i].objects.filter(o=>o.type==='text').map(o=>[o.id,o]));
 function annotate(nodes){for(const n of nodes){if(n.type==='text'){const m=metrics.get(n.id);if(m){n.baseline=m.font.baselines[0]-m.bounds.y;n.naturalWidth=m.font.naturalWidth;}}if(n.children)annotate(n.children);}}
 annotate(doc.boards[i].children);
}
// Runtime components refer to the identical PNG assets, avoiding embedded base64 duplication.
for(const [name,c]of Object.entries(doc.components)){c.children[0].src=name+'.png';}
await fs.writeFile(path.join(target,'design.json'),JSON.stringify(doc));
await fs.writeFile(path.join(target,'materials.json'),JSON.stringify(manifest));
await fs.writeFile(path.join(target,'asset-hashes.json'),JSON.stringify(hashes,null,2));
const raw=path.join(root,'app/src/main/res/raw');await fs.mkdir(raw,{recursive:true});
for(const name of ['ui-select','tile-slide','tile-merge','stone-contact','undo','puzzle-clear','out-of-moves'])await fs.copyFile(path.join(root,'art/sound',name+'.wav'),path.join(raw,'ceramic_'+name.replaceAll('-','_')+'.wav'));
console.log(JSON.stringify({boards:doc.boards.length,skins:Object.keys(manifest).length,sounds:7,target}));
