import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

// Alta explícitamente solicitada por el usuario. Solo usa la API local, sin acceder a secretos.
// Las revisiones de precios/configuración requieren --actualizar. No borra historial.
const base = 'http://localhost:8080/api/v1';
const aplicar = process.argv.includes('--aplicar');
const actualizar = process.argv.includes('--actualizar');
const inventario = JSON.parse(await readFile(new URL('../fixtures/habitaciones-real-bolognesi.json', import.meta.url), 'utf8'));
const request = async (method, ruta, body) => {
  const res = await fetch(base + ruta, { method, signal: AbortSignal.timeout(20000),
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined });
  const data = await res.json();
  if (!res.ok) throw new Error(`${method} ${ruta}: ${res.status} ${data.message ?? JSON.stringify(data)}`);
  return data;
};
const [tipos, habitaciones, items, catalogos] = await Promise.all([
  request('GET', '/tipos-habitacion'), request('GET', '/habitaciones/configuracion'),
  request('GET', '/items'), request('GET', '/recepcion/catalogos'),
]);
const capacidades = new Map();
for (const h of inventario.habitaciones) capacidades.set(h.tipo, Math.max(capacidades.get(h.tipo) ?? 0, h.camas * h.plazasPorCama + inventario.maxMenoresSinCamaInicial));
for (const h of inventario.habitaciones) {
  const actual = habitaciones.find(a => a.numeroCuarto === h.numero);
  if (actual) capacidades.set(h.tipo, Math.max(capacidades.get(h.tipo), actual.capacidadPersonas));
}
const camaTipo = catalogos.tiposItem.find(t => t.nombre.trim().toLocaleLowerCase('es') === 'cama');
if (!camaTipo || !catalogos.usuarios.length) throw new Error('Se necesita el tipo de ítem Cama y un operador activo.');
for (const h of inventario.habitaciones) {
  const existentes = habitaciones.filter(actual => actual.numeroCuarto === h.numero);
  if (existentes.length > 1) throw new Error(`Número repetido en el backend: ${h.numero}`);
  const actual = existentes[0];
  const tipo = tipos.find(t => t.nombreTipo === h.tipo);
  if (!actualizar && actual && (!tipo || actual.idTipoHabitacion !== tipo.idTipoHabitacion ||
    actual.plazasCama !== h.camas * h.plazasPorCama || actual.capacidadPersonas < h.camas * h.plazasPorCama ||
    (h.precioPorNoche != null && actual.precioPorNoche !== h.precioPorNoche)))
    throw new Error(`La habitación ${h.numero} ya existe con otra configuración; no se sobrescribe.`);
}
const resumen = { modo: aplicar ? 'aplicación' : 'vista previa', actualizar, habitaciones: inventario.habitaciones.length,
  camas: inventario.habitaciones.reduce((s,h) => s+h.camas,0),
  pisos: Object.fromEntries([2,3,4,5].map(p => [p,inventario.habitaciones.filter(h=>h.numero.startsWith(String(p))).length])),
  tarifasPendientes: inventario.tarifasPendientes, tiposNuevos: [...capacidades.keys()].filter(n => !tipos.some(t => t.nombreTipo === n)) };
console.log(JSON.stringify(resumen,null,2));
if (aplicar) {
const reporte = { fecha: new Date().toISOString(), tiposCreados: [], tiposActualizados: [], habitacionesCreadas: [], habitacionesActualizadas: [], camasCreadas: [], asignaciones: [], habitaciones: [] };
for (const [nombreTipo, capacidadMax] of capacidades) {
  let tipo = tipos.find(t => t.nombreTipo === nombreTipo);
  if (!tipo) {
    tipo = await request('POST', '/tipos-habitacion', {nombreTipo,capacidadMax,precioBase:0});
    tipos.push(tipo); reporte.tiposCreados.push(tipo.idTipoHabitacion);
  }
  if (tipo.capacidadMax < capacidadMax) {
    if (!actualizar) throw new Error(`La capacidad del tipo ${nombreTipo} es insuficiente.`);
    const actualizado = await request('PUT', `/tipos-habitacion/${tipo.idTipoHabitacion}`, {nombreTipo,capacidadMax,precioBase:tipo.precioBase});
    Object.assign(tipo,actualizado); reporte.tiposActualizados.push(tipo.idTipoHabitacion);
  }
}
for (const h of inventario.habitaciones) {
  let habitacion = habitaciones.find(actual => actual.numeroCuarto === h.numero);
  const tipo = tipos.find(t => t.nombreTipo === h.tipo);
  if (!habitacion) {
    habitacion = await request('POST', '/habitaciones', { numeroCuarto:h.numero, idTipoHabitacion:tipo.idTipoHabitacion,
      estadoHabitacion:'DISPONIBLE', capacidadPersonas:h.camas*h.plazasPorCama+inventario.maxMenoresSinCamaInicial,
      plazasCama:h.camas*h.plazasPorCama, maxMenoresSinCama:inventario.maxMenoresSinCamaInicial,
      ...(h.precioPorNoche != null ? {precioPorNoche:h.precioPorNoche} : {}) });
    habitaciones.push(habitacion); reporte.habitacionesCreadas.push(habitacion.idHabitacion);
  }
  const cambios = habitacion.idTipoHabitacion !== tipo.idTipoHabitacion || habitacion.plazasCama !== h.camas*h.plazasPorCama ||
    habitacion.capacidadPersonas < h.camas*h.plazasPorCama || (h.precioPorNoche != null && habitacion.precioPorNoche !== h.precioPorNoche);
  if (cambios && actualizar) {
    habitacion = await request('PUT', `/habitaciones/${habitacion.idHabitacion}`, {
      numeroCuarto:h.numero,idTipoHabitacion:tipo.idTipoHabitacion,estadoHabitacion:habitacion.estadoHabitacion,
      capacidadPersonas:Math.max(habitacion.capacidadPersonas,h.camas*h.plazasPorCama),
      plazasCama:h.camas*h.plazasPorCama,maxMenoresSinCama:habitacion.maxMenoresSinCama,
      ...(h.precioPorNoche != null ? {precioPorNoche:h.precioPorNoche} : {}) });
    reporte.habitacionesActualizadas.push(habitacion.idHabitacion);
  }
  for (let n=1;n<=h.camas;n++) {
    const nombreItem = `Cama ${h.numero}-${String(n).padStart(2,'0')} - ${h.medidaCama}`;
    let item = items.find(i => i.nombreItem === nombreItem);
    if (!item) {
      item = await request('POST', '/items', {nombreItem,idTipoItem:camaTipo.id,estadoItem:'Activo'});
      items.push(item); reporte.camasCreadas.push(item.idItem);
    }
    if (item.idHabitacion && item.idHabitacion !== habitacion.idHabitacion)
      throw new Error(`El ítem ${nombreItem} ya está asignado a otra habitación.`);
    if (!item.idHabitacion) {
      const asignado = await request('PUT', `/items/${item.idItem}/asignacion`, {
        claveOperacion:`${inventario.version}:${h.numero}:cama:${n}`, idUsuario:catalogos.usuarios[0].id,
        idHabitacion:habitacion.idHabitacion, estadoActualItem:'BUENO', observacion:'Registro inicial según inventario proporcionado por el usuario' });
      Object.assign(item,asignado); reporte.asignaciones.push(item.idItem);
    }
  }
  const mobiliario = await request('GET', `/habitaciones/${habitacion.idHabitacion}/items`);
  if (mobiliario.filter(i => i.nombreItem.startsWith(`Cama ${h.numero}-`)).length !== h.camas)
    throw new Error(`No coincide el número de camas de la habitación ${h.numero}`);
  reporte.habitaciones.push({numero:h.numero,idHabitacion:habitacion.idHabitacion,tipo:h.tipo,precio:habitacion.precioBase,camas:h.camas});
}
await writeFile(fileURLToPath(new URL('../../.local/importacion-habitaciones-real-bolognesi.json',import.meta.url)),JSON.stringify(reporte,null,2));
console.log(JSON.stringify({tiposCreados:reporte.tiposCreados.length,habitacionesCreadas:reporte.habitacionesCreadas.length,
  tiposActualizados:reporte.tiposActualizados.length,habitacionesActualizadas:reporte.habitacionesActualizadas.length,
  camasCreadas:reporte.camasCreadas.length,asignaciones:reporte.asignaciones.length,habitacionesVerificadas:reporte.habitaciones.length},null,2));
}
