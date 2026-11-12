import { chromium, expect } from '../../../frontend/node_modules/@playwright/test/index.mjs';
import { writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
const dir=new URL('./',import.meta.url);
const out=(name,data)=>writeFileSync(new URL(name,dir),data);
const results=[], errors=[];
async function api(method,path,body,headers={}) {
 const start=performance.now();
 const r=await fetch('http://127.0.0.1:8080'+path,{method,headers:{'Content-Type':'application/json',...headers},body:body?JSON.stringify(body):undefined});
 const data=await r.json();
 results.push({method,path:path.replace(/\/surveys\/[^/]+/,'/surveys/[redacted]'),status:r.status,ms:Math.round(performance.now()-start),code:data.error?.code});
 return {status:r.status,data};
}
const key={'X-Service-Key':process.env.E2E_SERVICE_KEY};
expect((await api('POST','/api/auth/login',{username:'marketing',password:'incorrect'})).status).toBe(401);
expect((await api('POST','/api/survey-invitations',{ticket_id:999999999},key)).status).toBe(404);
expect((await api('POST','/api/survey-invitations',{ticket_id:404},key)).status).toBe(422);
const inv=await api('POST','/api/survey-invitations',{ticket_id:401},key);expect(inv.status).toBe(201);
expect((await api('POST','/api/survey-invitations',{ticket_id:401},key)).status).toBe(409);
expect((await api('POST','/api/survey-invitations',{ticket_id:402})).status).toBe(401);
expect((await api('POST','/api/survey-invitations',{ticket_id:402},{'X-Service-Key':'incorrect'})).status).toBe(401);
expect((await api('GET','/api/survey-responses')).status).toBe(401);
expect((await api('GET','/api/survey-responses',null,{Authorization:'Bearer incorrect'})).status).toBe(401);
expect((await api('POST','/api/survey-invitations/'+inv.data.invitation_id+'/send',{channel:'ZALO'},key)).status).toBe(200);
const token=inv.data.survey_url.split('/').pop();
const browser=await chromium.launch();
const context=await browser.newContext();
const page=await context.newPage();
page.on('pageerror',e=>errors.push(e.message));
const snap=async name=>page.screenshot({path:new URL(name,dir).pathname.replace(/^\/([A-Za-z]:)/,'$1'),fullPage:true});
await page.goto('http://127.0.0.1:5173/khao-sat/'+token);
await expect(page.getByRole('radio',{name:'5 điểm'})).toBeVisible();
for(const [width,height] of [[1920,1080],[1366,768],[390,844],[375,667]]){
 await page.setViewportSize({width,height});
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await snap('survey-'+width+'.png');
}
await page.getByRole('button',{name:'GỬI PHẢN HỒI'}).click();
expect(await page.locator('form').count()).toBe(1);
await page.getByRole('radio',{name:'5 điểm'}).check();
await page.getByLabel(/Nhận xét/).fill('Nghiệm thu website: dữ liệu tổng hợp');
await page.getByRole('button',{name:'GỬI PHẢN HỒI'}).click();
await expect(page.getByRole('heading',{name:'Đánh giá đã được ghi nhận'})).toBeVisible();
await snap('survey-success.png');
expect((await api('POST','/api/surveys/'+token+'/responses',{score:1,comment:'overwrite'})).status).toBe(409);
async function login(user){
 await page.goto('http://127.0.0.1:5173/');
 await page.getByLabel('Tên đăng nhập').fill(user);
 await page.getByLabel('Mật khẩu').fill(process.env.E2E_PASSWORD);
 await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
 await expect(page.getByRole('button',{name:'Đăng xuất'})).toBeVisible();
 await expect(page.getByRole('table')).toBeVisible();
}
for(const [user,routes] of [['manager101',['/thong-ke/ky-thuat-vien','/phan-hoi','/thong-ke/ty-le']],['marketing',['/phan-hoi']],['director',['/thong-ke/trung-tam']]]){
 await login(user);
 for(const route of routes) for(const [width,height] of [[1920,1080],[1366,768],[390,844],[375,667]]){
   await page.setViewportSize({width,height});
   await page.goto('http://127.0.0.1:5173'+route);
   await expect(page.getByRole('table')).toBeVisible();
   const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth);
   results.push({screen:route,user,width,height,overflow});
   expect(overflow).toBe(false);
   await snap(user+'-'+route.split('/').pop()+'-'+width+'.png');
 }
 await page.getByRole('button',{name:'Đăng xuất'}).click();
}
await login('marketing');
await page.setViewportSize({width:1366,height:768});
await page.route('**/api/survey-responses?**',async route=>{await new Promise(r=>setTimeout(r,1800));await route.continue()});
await page.reload();
await expect(page.getByText('Đang tải dữ liệu…')).toBeVisible();
await snap('ui-loading-delayed-real-api.png');
await expect(page.getByRole('table')).toBeVisible();
await page.unroute('**/api/survey-responses?**');
await page.route('**/api/survey-responses?**',route=>route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({error:{code:'SERVICE_UNAVAILABLE',message:'Lỗi mô phỏng nghiệm thu',fields:{}}})}));
await page.reload();await expect(page.getByRole('alert')).toBeVisible();await snap('ui-error-mocked-503.png');
await page.unroute('**/api/survey-responses?**');
await page.getByRole('button',{name:'Thử lại'}).click();await expect(page.getByRole('table')).toBeVisible();
await page.route('**/api/survey-responses?**',route=>route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({page:1,size:20,total:0,items:[]})}));
await page.reload();await expect(page.getByText('Chưa có dữ liệu trong khoảng thời gian đã chọn.')).toBeVisible();await snap('ui-empty-mocked.png');
await page.unroute('**/api/survey-responses?**');
await page.reload();await expect(page.getByRole('table')).toBeVisible();
const downloadP=page.waitForEvent('download');await page.getByRole('button',{name:'Xuất Excel'}).click();const download=await downloadP;await download.saveAs(new URL('responses.xlsx',dir).pathname.replace(/^\/([A-Za-z]:)/,'$1'));
const auth=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('smartcrm-session')));
const report=await api('GET','/api/survey-responses',null,{Authorization:'Bearer '+auth.access_token});
out('api-response-list.json',JSON.stringify(report.data,null,2));
expect(report.data.items.some(r=>r.ticket_code==='DEMO-L8-0001'&&r.score===5)).toBe(true);
await page.getByRole('button',{name:'Đăng xuất'}).click();
await login('manager101');
const manager=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('smartcrm-session')));
expect((await api('GET','/api/survey-responses?center_id=102',null,{Authorization:'Bearer '+manager.access_token})).status).toBe(403);

// Separate minute window: the public API intentionally permits only 10 requests/min/IP.
await new Promise(resolve=>setTimeout(resolve,60000));
const mysql='C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe';
function sql(statement){
 const r=spawnSync(mysql,['--no-defaults','--host=127.0.0.1','--port=3307','--user=root','--database=smartcrm_l8_test','--execute='+statement],{encoding:'utf8'});
 if(r.status!==0)throw new Error('Test SQL failed');
 return r.stdout;
}
async function fresh(n){
 const id=Date.now()+n;
 sql("INSERT INTO ticket(ticket_id,ticket_code,customer_id,center_id,technician_id,device_id,status,closed_at) VALUES("+id+",'A"+id+"',201,101,301,501,'DA_DONG',CURRENT_TIMESTAMP)");
 const made=await api('POST','/api/survey-invitations',{ticket_id:id},key);
 expect(made.status).toBe(201);
 await api('POST','/api/survey-invitations/'+made.data.invitation_id+'/send',{channel:'SMS'},key);
 return {id,invitationId:made.data.invitation_id,token:made.data.survey_url.split('/').pop()};
}
for(const score of [1,2,3,4]){
 const f=await fresh(score);
 if(score===1){
  expect((await api('POST','/api/surveys/'+f.token+'/responses',{score:0})).status).toBe(400);
  expect((await api('POST','/api/surveys/'+f.token+'/responses',{score:1,comment:'a'.repeat(1001)})).status).toBe(400);
 }
 expect((await api('POST','/api/surveys/'+f.token+'/responses',{score,comment:'Synthetic score boundary '+score})).status).toBe(201);
}
const expired=await fresh(5);
sql('UPDATE survey_invitation SET expires_at=NOW()-INTERVAL 1 DAY WHERE invitation_id='+expired.invitationId);
expect((await api('GET','/api/surveys/'+expired.token)).status).toBe(410);
expect((await api('GET','/api/surveys/invalid')).status).toBe(404);
const raceId=Date.now()+100;
sql("INSERT INTO ticket(ticket_id,ticket_code,customer_id,center_id,technician_id,device_id,status,closed_at) VALUES("+raceId+",'R"+raceId+"',201,101,301,501,'DA_DONG',CURRENT_TIMESTAMP)");
const race=await Promise.all([api('POST','/api/survey-invitations',{ticket_id:raceId},key),api('POST','/api/survey-invitations',{ticket_id:raceId},key)]);
expect(race.map(r=>r.status).sort()).toEqual([201,409]);
const fk=spawnSync(mysql,['--no-defaults','--host=127.0.0.1','--port=3307','--user=root','--database=smartcrm_l8_test','--execute=START TRANSACTION; INSERT INTO survey_response(ticket_id,score) VALUES(-999999999,3); ROLLBACK;'],{encoding:'utf8'});
expect(fk.status).not.toBe(0);
expect(fk.stderr).toContain('1452');
out('foreign-key.txt',fk.stderr);
out('score-check.txt',sql('SELECT score,COUNT(*) AS responses FROM survey_response GROUP BY score ORDER BY score; SELECT COUNT(*) AS unexpected_invalid_responses FROM survey_response WHERE ticket_id=-999999999;'));

out('browser-checks.json',JSON.stringify({results,pageErrors:errors,uiMocks:['503 error','empty'],loading:'delayed real request'},null,2));
expect(errors).toEqual([]);
await browser.close();
console.log('Acceptance browser/API checks PASS; evidence saved; no tokens logged.');


