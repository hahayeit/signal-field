const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../assets/map.html'),'utf8');
const script=[...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)].map(x=>x[1]).join('\n');
const groups=[],views=[],markers=[];
const chain=()=>({addTo(){return this},on(){return this}});
const context={document:{createElement:()=>({textContent:''}),getElementById:()=>({textContent:''})},L:{
 map:()=>({setView(p,z){views.push([p,z]);return this}}),
 tileLayer:()=>chain(),control:{layers:()=>chain()},
 layerGroup:()=>{const g={...chain(),items:[],clearLayers(){this.items=[]}};groups.push(g);return g},
 circleMarker:(p,style)=>({bindPopup(label){this.label=label;return this},addTo(g){g.items.push({p,style,label:this.label});markers.push(this);return this}}),
 circle:(p,style)=>({addTo(g){g.items.push({p,style});return this}})
}};
vm.createContext(context);vm.runInContext(script,context);
context.updateData({observations:[]});assert.equal(groups[0].items.length,0);
context.updateData({observations:[{kind:'BLE',name:'<img src=x onerror=alert(1)>',latitude:41,longitude:-87,rssi:-60,time:1},{kind:'Wi-Fi',name:'no fix',rssi:-80,time:1}],location:{latitude:42,longitude:-88,accuracy:12}});
assert.equal(groups[0].items.length,1,'only geotagged observations are mapped');
assert.ok(groups[0].items[0].label.textContent.includes('<img'),'broadcast name remains literal text');
assert.equal(groups[0].items[0].label.innerHTML,undefined,'broadcast names never inserted as HTML');
assert.equal(groups[1].items.length,2,'phone position and accuracy circle');
assert.equal(groups[1].items[0].style.radius,12);
assert.equal(views.at(-1)[0][0],42,'center uses phone fix');
context.updateData({observations:[]});assert.equal(groups[0].items.length,0,'cleared log removes markers');assert.equal(groups[1].items.length,0,'expired fix removes phone marker');
console.log('Map integration checks passed');
