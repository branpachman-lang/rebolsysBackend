// Contrato público API 1.3: api/dependencies/apisperu-facturacion.openapi.json.
// Los documentos y respuestas son ficticios. No se llama a SUNAT ni a APIsPerú.
const http=require('http');
const documentos=new Map();
let envios=0,consultas=0;
const cdrZip='UEsFBgAAAAAAAAAAAAAAAAAAAAAAAA=='; // ZIP vacío, exclusivamente fixture local.
function send(res,status,body,type='application/json') {
  if(res.destroyed)return;
  res.writeHead(status,{'Content-Type':type});
  res.end(type==='application/json'?JSON.stringify(body):body);
}
http.createServer(async(req,res)=>{
  try {
    const url=new URL(req.url,'http://localhost');
    const ruta=url.pathname;
    let body=req.body;
    if(body===undefined) {
      let raw='';for await(const chunk of req)raw+=chunk;
      try{body=raw?JSON.parse(raw):{};}catch{return send(res,400,[{propertyPath:'body',message:'JSON inválido'}]);}
    }
    if(typeof body==='string') {try{body=JSON.parse(body);}catch{return send(res,400,[]);}}
    if(ruta==='/__control' && req.method==='GET')return send(res,200,{envios,consultas,documentos:[...documentos.values()].map(d=>({numero:d.numero,envios:d.envios,solicitud:d.solicitud}))});
    if(ruta==='/api/v1/auth/login' && req.method==='POST') {
      if(body.username!=='prueba' || body.password!=='solo-simulacion')return send(res,401,{code:401,message:'Credencial de prueba incorrecta'});
      return send(res,200,{token:'token-facturacion-local'});
    }
    if(req.headers.authorization!=='Bearer token-facturacion-local')return send(res,401,{message:'Credencial de prueba incorrecta'});
    const company=ruta.match(/^\/api\/v1\/companies\/(\d+)$/);
    if(company && req.method==='GET')return send(res,200,{id:Number(company[1]),ruc:'20123456789',environment:{nombre:company[1]==='2'?'produccion':'beta',fe_url:company[1]==='2'?'https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService':'https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService'}});
    if(ruta==='/api/v1/invoice/status' && req.method==='GET') {
      consultas++;
      const key=[url.searchParams.get('ruc'),url.searchParams.get('tipo'),url.searchParams.get('serie'),url.searchParams.get('numero')].join('-');
      const doc=documentos.get(key);
      return doc?send(res,200,doc.resultado.sunatResponse):send(res,200,{success:false,error:{code:'0127',message:'Fixture sin CDR confirmado'}});
    }
    if(!['/api/v1/invoice/send','/api/v1/invoice/pdf'].includes(ruta) || req.method!=='POST')return send(res,501,{message:'Operación no modelada'});
    if(!body.company || !body.client || !['01','03'].includes(body.tipoDoc) || !body.details?.length
        || !body.serie?.match(/^[FB][A-Z0-9]{3}$/) || !String(body.correlativo).match(/^[1-9][0-9]{0,7}$/)
        || body.company.address?.ubigueo!=='150101' || !body.formaPago || !body.legends?.[0]?.value)
      return send(res,400,[{propertyPath:'invoice',message:'La solicitud no cumple el contrato usado por la prueba'}]);
    const total=body.details.reduce((sum,d)=>sum+d.mtoValorVenta+d.igv,0);
    if(Math.abs(total-body.mtoImpVenta)>0.005 || Math.abs(body.valorVenta+body.mtoIGV-body.mtoImpVenta)>0.005)
      return send(res,422,[{propertyPath:'mtoImpVenta',message:'Totales inconsistentes'}]);
    if(ruta.endsWith('/pdf'))return send(res,200,Buffer.from('%PDF-1.4\n% PDF ficticio de simulación, sin validez tributaria\n%%EOF'),'application/pdf');
    envios++;
    const key=[body.company.ruc,body.tipoDoc,body.serie,body.correlativo].join('-');
    const porNombre=String(body.client.rznSocial).match(/^SIM_(ACEPTADO|RECHAZO|TIMEOUT|AUTH|SINCDR|JSON|ERROR|OTRO|CARRERA)/)?.[1];
    const escenario=porNombre||({'22222222':'RECHAZO','33333333':'TIMEOUT','44444444':'AUTH','55555555':'SINCDR','66666666':'JSON','77777777':'ERROR','88888888':'OTRO','99999999':'CARRERA'}[body.client.numDoc]||'ACEPTADO');
    if(escenario==='AUTH')return send(res,401,{code:401,message:'Token vencido de prueba'});
    if(escenario==='ERROR')return send(res,503,{message:'Fallo transitorio de prueba'});
    if(escenario==='JSON')return send(res,200,'{json roto','text/plain');
    const rechazado=escenario==='RECHAZO';
    const resultado={xml:`<?xml version="1.0"?><Invoice><ID>${body.serie}-${body.correlativo}</ID></Invoice>`,hash:'hash-ficticio',
      sunatResponse:{success:true,cdrZip,cdrResponse:{id:`${body.serie}-${body.correlativo}`,code:rechazado?'2335':'0',
        accepted:!rechazado,description:rechazado?'RECHAZO FICTICIO':'ACEPTACIÓN FICTICIA',notes:[]}}};
    const previo=documentos.get(key);
    documentos.set(key,{numero:key,solicitud:body,envios:(previo?.envios||0)+1,resultado});
    if(escenario==='SINCDR')return send(res,200,{sunatResponse:{success:false,error:{code:'0139',message:'No se confirmó el envío de prueba'}}});
    if(escenario==='OTRO')resultado.sunatResponse.cdrResponse.id='OTRO-999';
    if(escenario==='TIMEOUT' || escenario==='CARRERA')return setTimeout(()=>send(res,200,resultado),1600);
    return send(res,200,resultado);
  } catch {send(res,500,{message:'Fallo en fixture local'});}
}).listen(process.env.PORT||4921,'127.0.0.1');
